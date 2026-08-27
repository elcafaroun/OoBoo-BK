package io.c4us.masterbackend.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import io.c4us.masterbackend.DTOs.AppUserDTO;
import io.c4us.masterbackend.domain.AppUser;
import io.c4us.masterbackend.domain.Structure;
import io.c4us.masterbackend.domain.UserStructure;
import io.c4us.masterbackend.exception.QuotaExceededException;
import io.c4us.masterbackend.repo.AppUserRepo;
import io.c4us.masterbackend.repo.StructureRepo;
import io.c4us.masterbackend.repo.UserStructureRepo;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@Transactional(rollbackOn = Exception.class)
@RequiredArgsConstructor
public class AppUserService {

    private final AppUserRepo appUserRepo;
    private final StructureRepo structureRepo;
    private final UserStructureRepo userStructureRepo;
    private final PasswordEncoder passwordEncoder;

    /**
     * Création d'un utilisateur et affectation à sa première structure
     */
/**
     * Création d'un utilisateur et affectation à sa première structure
     */
public AppUser createAppUser(AppUser user, String codeStructureCible, String roleInitial) {
    // 1. Nettoyage des chaînes vides (convertir "" en null pour éviter les violations de contrainte SQL unique)
    if (user.getUserEmail() != null && user.getUserEmail().trim().isEmpty()) {
        user.setUserEmail(null);
    }
    if (user.getUserPhone() != null) {
        user.setUserPhone(user.getUserPhone().trim());
    }

    // 2. Double sécurité d'unicité
    if (user.getUserEmail() != null && !isEmailUnique(user.getUserEmail())) {
        throw new RuntimeException("L'adresse email '" + user.getUserEmail() + "' est déjà utilisée.");
    }
    if (!isPhoneUnique(user.getUserPhone())) {
        throw new RuntimeException("Le numéro de téléphone '" + user.getUserPhone() + "' est déjà utilisé.");
    }

    boolean isSuperAdmin = "Super admin".equalsIgnoreCase(user.getUserProfile());
    String clearPassword = user.getUserPassword();

    // Validation de la structure obligatoire pour les comptes enfants
    if (!isSuperAdmin && (codeStructureCible == null || codeStructureCible.trim().isEmpty())) {
        throw new RuntimeException("Impossible de créer un profil utilisateur sans l'associer à une structure valide.");
    }

    Structure structureCible = null;

    // 🔹 VÉRIFICATION DE LA STRUCTURE ET DU QUOTA
    if (!isSuperAdmin) {
        String cleanCodeStruct = codeStructureCible.trim().toUpperCase();
        
        structureCible = structureRepo.findByCodeStructure(cleanCodeStruct)
                .stream().findFirst()
                .orElseThrow(() -> new RuntimeException("Structure introuvable avec le code: " + cleanCodeStruct));

        // Vérification de la limite du quota d'utilisateurs
        checkUserQuotaForStructure(structureCible);
    }

    // 3. Chiffrement et Token
    user.setUserPassword(passwordEncoder.encode(user.getUserPassword()));
    user.setConfirmationToken(generateAndSetToken(user));

    // 4. Génération séquentielle du Code Utilisateur
    if (isSuperAdmin) {
        long globalAdminCount = appUserRepo.countByCodeUserStartingWith("ROOT_") + 1;
        user.setCodeUser(String.format("ROOT_%04d", globalAdminCount));
    } else {
        String cleanCodeStruct = codeStructureCible.trim().toUpperCase();
        long nextAgentSequence = userStructureRepo.countByStructure_CodeStructure(cleanCodeStruct) + 1;
        user.setCodeUser(String.format("%s_%03d", cleanCodeStruct, nextAgentSequence));
    }

    // 5. Enregistrement initial de l'utilisateur
    AppUser savedUser = appUserRepo.save(user);

    // 6. Création du lien associatif si ce n'est pas un Super Admin
    if (!isSuperAdmin && structureCible != null) {
        UserStructure link = new UserStructure();
        link.setUser(savedUser);
        link.setStructure(structureCible);
        link.setRoleInStructure(roleInitial != null ? roleInitial : "COLLABORATEUR"); 

        userStructureRepo.save(link);

        // Notification WhatsApp
        this.sendCredentialsViaWhatsApp(savedUser.getUserPhone(), structureCible.getCodeStructure(), clearPassword);
    }

    return savedUser;
}

    /**
     * Associer un utilisateur existant à une nouvelle structure supplémentaire (Multi-structure)
     */
    public void associateUserToStructure(String userId, String codeStructure, String role) {
        AppUser user = getAppUser(userId);
        Structure structure = structureRepo.findByCodeStructure(codeStructure.trim().toUpperCase())
                .stream().findFirst()
                .orElseThrow(() -> new RuntimeException("Structure introuvable"));

        Optional<UserStructure> existingLink = userStructureRepo.findByUser_IdAndStructure_IdStructure(user.getId(), structure.getIdStructure());

        if (existingLink.isPresent()) {
            UserStructure link = existingLink.get();
            // Si la liaison était supprimée et qu'on la réactive, il faut ré-appliquer la vérification de quota
            if (Boolean.TRUE.equals(link.getDeleted())) {
                checkUserQuotaForStructure(structure);
            }
            link.setDeleted(false);
            link.setRoleInStructure(role); 
            link.setUpdatedAt(LocalDateTime.now());
            userStructureRepo.save(link);
        } else {
            // 🔹 Vérification du quota pour une nouvelle association
            checkUserQuotaForStructure(structure);

            UserStructure newLink = new UserStructure();
            newLink.setUser(user);
            newLink.setStructure(structure);
            newLink.setRoleInStructure(role); 
            userStructureRepo.save(newLink);
        }
    }

  

    public List<AppUser> getActiveUsersByStructure(String codeStructure) {
        return userStructureRepo.findByStructure_CodeStructureAndDeletedFalse(codeStructure)
                .stream()
                .filter(link -> link.getUser() != null && Boolean.TRUE.equals(link.getUser().getActive())) // ✅ Filtre propre sur l'utilisateur actif
                .map(UserStructure::getUser)
                .collect(Collectors.toList());
    }
public List<AppUserDTO> getAllUsersByStructure(String structureId) {
    return userStructureRepo.findUsersByStructureId(structureId)
            .stream()
            .map(this::convertToDTO) // Conversion ici
            .collect(Collectors.toList());
}
    // Retirer un utilisateur d'une structure
    public void removeUserFromStructure(String userId, String structureId) {
        UserStructure link = userStructureRepo.findByUser_IdAndStructure_IdStructure(userId, structureId)
                .orElseThrow(() -> new RuntimeException("Association introuvable"));
        link.setDeleted(true);
        userStructureRepo.save(link);
    }

    private void sendCredentialsViaWhatsApp(String phoneNumber, String codeStructure, String pinCode) {
        log.info("💬 [WhatsApp] Préparation de l'envoi vers {} - Structure: {} - PIN: {}", phoneNumber, codeStructure, pinCode);
    }

    public String generateAndSetToken(AppUser user) {
        String token = UUID.randomUUID().toString();
        user.setConfirmationToken(token);
        user.setTokenExpiryDate(LocalDateTime.now().plusHours(24));
        return token;
    }

    public Optional<AppUser> findByConfirmationToken(String token) {
        return appUserRepo.findByConfirmationToken(token);
    }

    public AppUser getAppUser(String id) {
        return appUserRepo.findById(id).orElseThrow(() -> new RuntimeException("User Not found"));
    }

    public AppUser updateAppUser(AppUser us) {
        AppUser user = getAppUser(us.getId());
        user.setUpdatedAt(LocalDateTime.now());
        return appUserRepo.save(user);
    }

    public AppUser updateUser(String id, AppUser userDetails) {
        AppUser user = appUserRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        user.setUserName(userDetails.getUserName());
        user.setUserPhone(userDetails.getUserPhone());
        user.setUserProfile(userDetails.getUserProfile());
        return appUserRepo.save(user);
    }

    public void disableUser(String id) {
        AppUser user = appUserRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        user.setActive(false); 
        appUserRepo.save(user);
    }

    public void deleteUser(String id) {
        appUserRepo.deleteById(id);
    }

    public void changePassword(String userId, String newPassword) {
        AppUser user = appUserRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        user.setUserPassword(passwordEncoder.encode(newPassword));
        appUserRepo.save(user);
    }

    public void toggleUserActive(String id, boolean status) {
        AppUser user = appUserRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        user.setActive(status); // ✅ Remplacé user.setIsActive par user.setActive
        appUserRepo.save(user);
    }

    public boolean isEmailUnique(String email) {
        if (email == null || email.trim().isEmpty()) return true;
        return !appUserRepo.existsByUserEmail(email.trim());
    }

    public boolean isPhoneUnique(String phone) {
        if (phone == null || phone.trim().isEmpty()) return true;
        return !appUserRepo.existsByUserPhone(phone.trim());
    }

    public AppUserDTO convertToDTO(AppUser user) {
    return AppUserDTO.builder()
            .id(user.getId())
            .userName(user.getUserName())
            .userEmail(user.getUserEmail())
            .userPhone(user.getUserPhone())
            .codeUser(user.getCodeUser())
            .active(user.getActive())
            .userProfile(user.getUserProfile())
            .lastSyncDate(user.getLastSyncDate()) 
            .build();
}

public List<UserStructure> getUserStructuresByUserId(String userId) {
        return userStructureRepo.findByUser_Id(userId);

}
public void updateLastSyncDate(String userId) {
    AppUser user = appUserRepo.findById(userId)
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID : " + userId));
    
    user.setLastSyncDate(LocalDateTime.now());
    appUserRepo.save(user);
    log.info("🔄 Date de synchronisation mise à jour pour l'utilisateur : {}", userId);
}

private void checkUserQuotaForStructure(Structure structure) {
    Integer maxAllowed = structure.getNombreUsers(); 

    if (maxAllowed != null) {
        long currentCount = userStructureRepo.countByStructure_CodeStructureAndDeletedFalse(structure.getCodeStructure());

        if (currentCount >= maxAllowed) {
            throw new QuotaExceededException(
                String.format("Quota d'utilisateurs atteint pour la structure %s. Nombre actuel : %d, Limite autorisée : %d",
                    structure.getCodeStructure(), currentCount, maxAllowed)
            );
        }
    }
}



}