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
import io.c4us.masterbackend.domain.AppUser;
import io.c4us.masterbackend.domain.SubscriptionPlan;
import io.c4us.masterbackend.domain.UserStructure;
import io.c4us.masterbackend.repo.StructureRepo;
import io.c4us.masterbackend.repo.AppUserRepo;
import io.c4us.masterbackend.repo.SubscriptionPlanRepo;
import io.c4us.masterbackend.repo.UserStructureRepo;
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
     * 🔹 Création d'une structure ET liaison automatique dans la table
     * user_structures
     */
    /**
     * 🔹 Création d'une structure ET liaison sécurisée
     */
    public Structure createStructure(Structure struct, String userId) {
        log.info("Création d'une nouvelle structure pour l'utilisateur ID: {}", userId);

        // 1. Récupération des entités gérées par l'EntityManager
        AppUser creator = appUserRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable (ID: " + userId + ")"));

        SubscriptionPlan plan = planRepository.findByName(struct.getPlanStructure())
                .orElseThrow(() -> new RuntimeException("Plan introuvable : " + struct.getPlanStructure()));

        // 2. Initialisation des données
        LocalDateTime today = LocalDateTime.now();
        struct.setStartSub(today);
        struct.setEndSub(
                today.plusDays(plan.getNombreJourSouscription() != null ? plan.getNombreJourSouscription() : 0));
        struct.setCout(plan.getCout() != null ? plan.getCout() : 0);
        struct.setPriorite(plan.getPriorite());
        struct.setLastUpdated(today);

        // 3. Génération du matricule
        long nextSequence = structureRepo.count() + 1;
        struct.setCodeStructure(String.format("E%03d", nextSequence));

        // 4. Sauvegarde de la structure
        Structure savedStructure = structureRepo.save(struct);

        // 5. Gestion de la table pivot (UserStructure)
        // IMPORTANT : On instancie l'entité de liaison sans forcer l'ID manuellement
        // si @UuidGenerator est configuré sur l'entité UserStructure
        UserStructure userStructure = new UserStructure();
        userStructure.setUser(creator);
        userStructure.setStructure(savedStructure);
        userStructure.setRoleInStructure("PROPRIETAIRE"); // Rôle par défaut
        userStructure.setDeleted(false);
        userStructure.setUpdatedAt(today);

        // Au lieu de save, on utilise persist si l'id est généré par @UuidGenerator
        userStructureRepo.save(userStructure);

        // Force la synchronisation de la transaction pour éviter le conflit
        // Hibernate verra l'insertion comme une seule unité de travail
        return savedStructure;
    }

    /**
     * 🔹 Récupération des structures associées à un utilisateur (Multi-structure
     * PB-M)
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
     * 🔹 Méthode de synchronisation incrémentale optimisée pour le Offline de
     * Flutter
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

    // Suppression logique (Soft Delete)
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

    // Gestion de l'upload des logos/photos
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

    public Structure updateStructurePlan(String id, String planName) {
        Structure structure = getStructure(id);
        SubscriptionPlan plan = planRepository.findByName(planName)
                .orElseThrow(() -> new RuntimeException("Plan de souscription introuvable : " + planName));

        structure.setPlanStructure(plan.getName());
        structure.setCout(plan.getCout());
        structure.setPriorite(plan.getPriorite());

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
}