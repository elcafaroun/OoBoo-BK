package io.c4us.masterbackend.service;

import static io.c4us.masterbackend.constant.Constant.PHOTO_DIRECTORY;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import io.c4us.masterbackend.domain.Structure;
import io.c4us.masterbackend.DTOs.StructureResponseDTO;
import io.c4us.masterbackend.domain.AppUser;
import io.c4us.masterbackend.domain.SubscriptionPlan;
import io.c4us.masterbackend.domain.UserStructure;
import io.c4us.masterbackend.repo.StructureRepo;
import io.c4us.masterbackend.repo.AppUserRepo;
import io.c4us.masterbackend.repo.SubscriptionPlanRepo;
import io.c4us.masterbackend.repo.UserStructureRepo;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@Transactional(rollbackOn = Exception.class)
@RequiredArgsConstructor
public class StructureService {

    private final StructureRepo structureRepo;
    private final AppUserRepo appUserRepo;
    private final SubscriptionPlanRepo planRepository;
    private final UserStructureRepo userStructureRepo;

    /**
     * 🔹 Méthode privée réutilisable pour copier l'intégralité des attributs du Plan vers la Structure
     */
    private void copyPlanFeaturesToStructure(Structure struct, SubscriptionPlan plan) {
        struct.setPlanStructure(plan.getName());
        struct.setCout(plan.getCout() != null ? plan.getCout() : 0.0);
        struct.setPriorite(plan.getPriorite());

        // Copie des fonctionnalités et drapeaux d'accès
        struct.setSmsAlerte(plan.getSmsAlerte());
        struct.setStockAlerte(plan.getStockAlerte());
        struct.setEmailAlerte(plan.getEmailAlerte());
        struct.setDashboard(plan.getDashboard());
        struct.setLoyaltyAccess(plan.getLoyaltyAccess());
struct.setIaActive(plan.getIaActive());
    struct.setMiniDashboard(plan.getMiniDashboard());
        // Copie des quotas et durées
        struct.setGracePeriode(plan.getGracePeriode());
        struct.setNombreJourSouscription(plan.getNombreJourSouscription());
        struct.setNombreCategorieParBusiness(plan.getNombreCategorieParBusiness());
        struct.setNombreProdParBusiness(plan.getNombreProdParBusiness());
    }

    /**
     * 🔹 Création d'une structure ET copie des règles du plan souscrit
     */
    public Structure createStructure(Structure struct, String userId) {
        log.info("Création d'une nouvelle structure pour l'utilisateur ID: {}", userId);

        AppUser creator = appUserRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable (ID: " + userId + ")"));

        SubscriptionPlan plan = planRepository.findByName(struct.getPlanStructure())
                .orElseThrow(() -> new RuntimeException("Plan introuvable : " + struct.getPlanStructure()));

        LocalDateTime today = LocalDateTime.now();

        // 1. Définition des dates de souscription
        struct.setStartSub(today);
        int days = plan.getNombreJourSouscription() != null ? plan.getNombreJourSouscription() : 0;
        struct.setEndSub(today.plusDays(days));
        struct.setLastUpdated(today);

        // 2. Copie de l'intégralité des paramètres et limites du plan
        copyPlanFeaturesToStructure(struct, plan);

        // 3. Génération du matricule
        long nextSequence = structureRepo.count() + 1;
        struct.setCodeStructure(String.format("E%03d", nextSequence));

        // 4. Sauvegarde de la structure
        Structure savedStructure = structureRepo.save(struct);

        // 5. Gestion de la liaison pivot (UserStructure)
        UserStructure userStructure = new UserStructure();
        userStructure.setUser(creator);
        userStructure.setStructure(savedStructure);
        userStructure.setRoleInStructure("PROPRIETAIRE");
        userStructure.setDeleted(false);
        userStructure.setUpdatedAt(today);
        userStructureRepo.save(userStructure);

        return savedStructure;
    }

    /**
     * 🔹 Mise à jour ou changement de Plan de souscription
     */
    public Structure updateStructurePlan(String id, String planName) {
        Structure structure = getStructure(id);
        SubscriptionPlan plan = planRepository.findByName(planName)
                .orElseThrow(() -> new RuntimeException("Plan de souscription introuvable : " + planName));

        // 1. Copie des nouvelles règles et options du plan
        copyPlanFeaturesToStructure(structure, plan);

        // 2. Recalcul des dates d'abonnement
        LocalDateTime now = LocalDateTime.now();
        Integer duration = plan.getNombreJourSouscription() != null ? plan.getNombreJourSouscription() : 0;
        LocalDateTime currentEndSub = structure.getEndSub();

        if (currentEndSub != null && currentEndSub.isAfter(now)) {
            structure.setEndSub(currentEndSub.plusDays(duration));
        } else {
            structure.setStartSub(now);
            structure.setEndSub(now.plusDays(duration));
        }

        structure.setLastUpdated(now);
        return structureRepo.save(structure);
    }

    /**
     * 🔹 Récupération des structures associées à un utilisateur
     */
    public List<Structure> getStructuresByUser(String userId) {
        log.info("Récupération optimisée des structures pour le user : {}", userId);
        return userStructureRepo.findByUserIdWithStructures(userId).stream()
                .map(UserStructure::getStructure)
                .collect(Collectors.toList());
    }

    public List<Structure> getStructuresByCodeStructure(String codeStructure) {
        return structureRepo.findByCodeStructure(codeStructure);
    }

    /**
     * 🔹 Méthode de synchronisation incrémentale pour le mode Offline Flutter
     */
    public List<Structure> getUpdatesForSync(String userId, LocalDateTime lastSyncDate) {
        List<Structure> userStructures = getStructuresByUser(userId);
        if (lastSyncDate == null) {
            return userStructures;
        }
        return userStructures.stream()
                .filter(s -> s.getLastUpdated() != null && s.getLastUpdated().isAfter(lastSyncDate))
                .collect(Collectors.toList());
    }

    public Structure delStructure(String id) {
        Structure structure = getStructure(id);
        structure.setDeleted(true);
        structure.setLastUpdated(LocalDateTime.now());
        return structureRepo.save(structure);
    }

    public Structure getStructure(String id) {
        return structureRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Structure non trouvée avec l'Id : " + id));
    }

    public String uploadPhoto(String id, MultipartFile file) {
        log.info("Upload photo pour la structure : {}", id);
        Structure struct = getStructure(id);
        String photoUrl = photoFunction.apply(id, file);
        struct.setStructPhotoUrl(photoUrl);
        struct.setLastUpdated(LocalDateTime.now());
        structureRepo.save(struct);
        return photoUrl;
    }

    private final Function<String, String> fileExtension = filename -> Optional.ofNullable(filename)
            .filter(name -> name.contains("."))
            .map(name -> "." + name.substring(filename.lastIndexOf(".") + 1)).orElse(".png");

    private final BiFunction<String, MultipartFile, String> photoFunction = (id, image) -> {
        String filename = id + fileExtension.apply(image.getOriginalFilename());
        try {
            Path fileStorageLocation = Paths.get(PHOTO_DIRECTORY).toAbsolutePath().normalize();
            if (!Files.exists(fileStorageLocation)) {
                Files.createDirectories(fileStorageLocation);
            }
            Files.copy(image.getInputStream(), fileStorageLocation.resolve(filename), REPLACE_EXISTING);
            return ServletUriComponentsBuilder.fromCurrentContextPath().path("/structure/image/" + filename)
                    .toUriString();
        } catch (Exception exception) {
            throw new RuntimeException("Impossible d'enregistrer le fichier image", exception);
        }
    };

    public List<Structure> getAllStructure() {
        return structureRepo.findAll();
    }

    public boolean checkIfNameExists(String nom) {
        return structureRepo.existsByNomStructureIgnoreCase(nom.trim());
    }

    public Structure updateActiveStatus(String id, boolean newStatus) {
        Structure struct = getStructure(id);
        struct.setActive(newStatus);
        struct.setLastUpdated(LocalDateTime.now());
        return structureRepo.save(struct);
    }

    public Structure updateStructureFields(String id, Structure newDetails) {
        log.info("Mise à jour des informations de la structure ID: {}", id);

        Structure existingStructure = getStructure(id);

        if (newDetails.getNomStructure() != null) {
            existingStructure.setNomStructure(newDetails.getNomStructure().trim());
        }
        if (newDetails.getTypeStructure() != null) {
            existingStructure.setTypeStructure(newDetails.getTypeStructure());
        }
        if (newDetails.getDescriptionStructure() != null) {
            existingStructure.setDescriptionStructure(newDetails.getDescriptionStructure().trim());
        }
        if (newDetails.getDisponibiliteStructure() != null) {
            existingStructure.setDisponibiliteStructure(newDetails.getDisponibiliteStructure().trim());
        }
        if (newDetails.getPaysStructure() != null) {
            existingStructure.setPaysStructure(newDetails.getPaysStructure().trim());
        }
        if (newDetails.getVilleStructure() != null) {
            existingStructure.setVilleStructure(newDetails.getVilleStructure());
        }
        if (newDetails.getRueStructure() != null) {
            existingStructure.setRueStructure(newDetails.getRueStructure().trim());
        }
        if (newDetails.getCodePoste() != null) {
            existingStructure.setCodePoste(newDetails.getCodePoste().trim());
        }
        if (newDetails.getGeoLocStructure() != null) {
            existingStructure.setGeoLocStructure(newDetails.getGeoLocStructure().trim());
        }
        existingStructure.setLastUpdated(LocalDateTime.now());

        return structureRepo.save(existingStructure);
    }

    public Structure updateStructureAndPhoto(String id, Structure newDetails, MultipartFile file) {
        log.info("Mise à jour combinée (Texte + Image) pour la structure ID: {}", id);

        Structure updatedStructure = updateStructureFields(id, newDetails);

        if (file != null && !file.isEmpty()) {
            log.info("Fichier image valide détecté, écriture sur le disque...");
            String photoUrl = photoFunction.apply(id, file);
            updatedStructure.setStructPhotoUrl(photoUrl);
            updatedStructure.setLastUpdated(LocalDateTime.now());
            updatedStructure = structureRepo.save(updatedStructure);
        }

        return updatedStructure;
    }

    @Transactional
    public StructureResponseDTO getStructureById(String id) {
        Structure structure = structureRepo.findByIdStructureAndDeletedFalse(id)
                .orElseThrow(() -> new EntityNotFoundException("Structure non trouvée avec l'ID : " + id));

        return mapToDTO(structure);
    }

    // Mapping Entity -> DTO
    private StructureResponseDTO mapToDTO(Structure entity) {
        return StructureResponseDTO.builder()
                .idStructure(entity.getIdStructure())
                .nomStructure(entity.getNomStructure())
                .phone1Structure(entity.getPhone1Structure())
                .phone2Structure(entity.getPhone2Structure())
                .paysStructure(entity.getPaysStructure())
                .villeStructure(entity.getVilleStructure())
                .rueStructure(entity.getRueStructure())
                .codePoste(entity.getCodePoste())
                .structPhotoUrl(entity.getStructPhotoUrl())
                .emailStructure(entity.getEmailStructure())
                .typeStructure(entity.getTypeStructure())
                .disponibiliteStructure(entity.getDisponibiliteStructure())
                .geoLocStructure(entity.getGeoLocStructure())
                .descriptionStructure(entity.getDescriptionStructure())
                .codeStructure(entity.getCodeStructure())
                .planStructure(entity.getPlanStructure())
                .startSub(entity.getStartSub())
                .endSub(entity.getEndSub())
                .isActive(entity.isActive())
                .cout(entity.getCout())
                .priorite(entity.getPriorite())
                // Snapshot des règles
                .smsAlerte(entity.getSmsAlerte())
                .stockAlerte(entity.getStockAlerte())
                .emailAlerte(entity.getEmailAlerte())
                .dashboard(entity.getDashboard())
                .iaActive(entity.getIaActive())
            .miniDashboard(entity.getMiniDashboard())
                .nombreUsers(entity.getNombreUsers())
                .loyaltyAccess(entity.getLoyaltyAccess())
                .gracePeriode(entity.getGracePeriode())
                .nombreJourSouscription(entity.getNombreJourSouscription())
                .nombreCategorieParBusiness(entity.getNombreCategorieParBusiness())
                .nombreProdParBusiness(entity.getNombreProdParBusiness())
                .createdDate(entity.getCreatedDate())
                .lastUpdated(entity.getLastUpdated())
                .build();
    }
}