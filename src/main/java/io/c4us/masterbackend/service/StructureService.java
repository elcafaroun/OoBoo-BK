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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import io.c4us.masterbackend.domain.Structure;
import io.c4us.masterbackend.domain.SubscriptionPlan;
import io.c4us.masterbackend.repo.StructureRepo;
import io.c4us.masterbackend.repo.SubscriptionPlanRepo;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@Transactional(rollbackOn = Exception.class)
@RequiredArgsConstructor
public class StructureService {

    private final StructureRepo structureRepo;

    @Autowired
    private SubscriptionPlanRepo planRepository;

    // Création
    public Structure createStructure(Structure struct) {
    struct.setLastUpdated(LocalDateTime.now());
    
    // 1. Récupération du plan de souscription
    SubscriptionPlan plan = planRepository.findByName(struct.getPlanStructure())
            .orElseThrow(() -> new RuntimeException("Plan de souscription introuvable : " + struct.getPlanStructure()));

    // 2. Calculer les dates et détails du plan
    LocalDateTime today = LocalDateTime.now();
    Integer duration = plan.getNombreJourSouscription() != null ? plan.getNombreJourSouscription() : 0;
    double cout = plan.getCout() != null ? plan.getCout() : 0;
    Long priorite = plan.getPriorite();

    // 3. Appliquer les informations financières et de calendrier
    struct.setStartSub(today);
    struct.setEndSub(today.plusDays(duration));
    struct.setCout(cout);
    struct.setPriorite(priorite);

    // 4. GÉNÉRATION AUTOMATIQUE ET UNIQUE DU CODE STRUCTURE (Format EXXX)
    // On récupère le nombre actuel de structures et on ajoute 1
    long nextSequence = structureRepo.count() + 1;
    
    // Le format "%03d" garantit un nombre sur 3 chiffres minimum avec des zéros (001, 002, 015, 125)
    String generatedCode = String.format("E%03d", nextSequence);
    struct.setCodeStructure(generatedCode);

    // 5. Sauvegarde finale
    return structureRepo.save(struct);
}

    // Récupération pour affichage (exclut les supprimés)
    public List<Structure> getStructuresByUser(String userId) {
        return structureRepo.findByCreatedUserIdAndDeletedFalse(userId);
    }

    // Méthode de synchronisation pour Flutter
    public List<Structure> getUpdatesForSync(String userId, LocalDateTime lastSyncDate) {
        if (lastSyncDate == null) {
            return structureRepo.findByCreatedUserId(userId);
        }
        return structureRepo.findByCreatedUserIdAndLastUpdatedAfter(userId, lastSyncDate);
    }

    // Suppression logique (Soft Delete)
    public Structure delStructure(String id) {
        Structure structure = getStructure(id);
        structure.setDeleted(true);
        structure.setLastUpdated(LocalDateTime.now()); // Crucial pour que Flutter reçoive l'info
        return structureRepo.save(structure);
    }

    public Structure getStructure(String id) {
        return structureRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Structure not found : Id " + id));
    }

    // Gestion des photos
    public String uploadPhoto(String id, MultipartFile file) {
        log.info("Upload photo for structure : {}", id);
        Structure struct = getStructure(id);
        String photoUrl = photoFunction.apply(id, file);
        struct.setStructPhotoUrl(photoUrl);
        struct.setLastUpdated(LocalDateTime.now()); // On marque le changement
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
            throw new RuntimeException("Could not store file", exception);
        }
    };

    public List<Structure> getAllStructure() {
        return structureRepo.findAll();
    }
    public boolean checkIfNameExists(String nom) {
    return structureRepo.existsByNomStructureIgnoreCase(nom.trim());
}

    public Structure updateStructurePlan(String id, String planName) {
        // 1. Récupérer la structure existante
        Structure structure = getStructure(id);

        // 2. Vérifier si le plan existe
        SubscriptionPlan plan = planRepository.findByName(planName)
                .orElseThrow(() -> new RuntimeException("Plan de souscription introuvable : " + planName));

        // 3. Mettre à jour le plan, le coût et la priorité
        structure.setPlanStructure(plan.getName());
        structure.setCout(plan.getCout());
        structure.setPriorite(plan.getPriorite());

        // 4. Calcul des dates
        LocalDateTime now = LocalDateTime.now();
        Integer duration = plan.getNombreJourSouscription() != null ? plan.getNombreJourSouscription() : 0;

        // Récupérer la date de fin actuelle (si elle existe)
        LocalDateTime currentEndSub = structure.getEndSub();

        if (currentEndSub != null && currentEndSub.isAfter(now)) {
            // --- CAS 1 : Abonnement ACTIF ---
            // On ajoute la durée du nouveau plan à la date de fin existante
            structure.setEndSub(currentEndSub.plusDays(duration));
            // On ne touche pas à startSub, car l'abonnement a déjà commencé
        } else {
            // --- CAS 2 : Abonnement EXPIRÉ ou NOUVEAU ---
            // On repart de "Maintenant"
            structure.setStartSub(now);
            structure.setEndSub(now.plusDays(duration));
        }

        // 5. Mise à jour du flag lastUpdated
        structure.setLastUpdated(now);

        return structureRepo.save(structure);
    }
}