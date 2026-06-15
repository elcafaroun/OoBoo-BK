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
    public AppUser createAppUser(AppUser user, String codeStructureCible, String roleInitial) {
        // 1. Double sécurité d'unicité
        if (!isEmailUnique(user.getUserEmail())) {
            throw new RuntimeException("L'adresse email '" + user.getUserEmail() + "' est déjà utilisée.");
        }
        if (!isPhoneUnique(user.getUserPhone())) {
            throw new RuntimeException("Le numéro de téléphone '" + user.getUserPhone() + "' est déjà utilisé.");
        }

        boolean isSuperAdmin = "Super admin".equalsIgnoreCase(user.getUserProfile());
        String clearPassword = user.getUserPassword();

        // 2. Chiffrement et Token
        user.setUserPassword(passwordEncoder.encode(user.getUserPassword()));
        user.setConfirmationToken(generateAndSetToken(user));

        // Validation de la structure obligatoire pour les comptes enfants
        if (!isSuperAdmin && (codeStructureCible == null || codeStructureCible.trim().isEmpty())) {
            throw new RuntimeException("Impossible de créer un profil utilisateur sans l'associer à une structure valide.");
        }

        // 3. Génération séquentielle du Code Utilisateur
        if (isSuperAdmin) {
            long globalAdminCount = appUserRepo.countByCodeUserStartingWith("ROOT_") + 1;
            user.setCodeUser(String.format("ROOT_%04d", globalAdminCount));
        } else {
            String cleanCodeStruct = codeStructureCible.trim().toUpperCase();
            long nextAgentSequence = userStructureRepo.countByStructure_CodeStructure(cleanCodeStruct) + 1;
            user.setCodeUser(String.format("%s_%03d", cleanCodeStruct, nextAgentSequence));
        }

        // 4. Enregistrement initial de l'utilisateur
        AppUser savedUser = appUserRepo.save(user);

        // 5. Création du lien associatif si ce n'est pas un Super Admin
        if (!isSuperAdmin) {
            Structure structure = structureRepo.findByCodeStructure(codeStructureCible.trim().toUpperCase())
                    .stream().findFirst()
                    .orElseThrow(() -> new RuntimeException("Structure introuvable avec le code: " + codeStructureCible));

            UserStructure link = new UserStructure();
            link.setUser(savedUser);
            link.setStructure(structure);
            link.setRoleInStructure(roleInitial != null ? roleInitial : "COLLABORATEUR"); 

            userStructureRepo.save(link);

            // Notification WhatsApp
            this.sendCredentialsViaWhatsApp(savedUser.getUserPhone(), structure.getCodeStructure(), clearPassword);
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

        Optional<UserStructure> existingLink = userStructureRepo.findByUserIdAndStructure_IdStructure(user.getId(), structure.getIdStructure());

        if (existingLink.isPresent()) {
            UserStructure link = existingLink.get();
            link.setDeleted(false);
            link.setRoleInStructure(role); 
            link.setUpdatedAt(LocalDateTime.now());
            userStructureRepo.save(link);
        } else {
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
        UserStructure link = userStructureRepo.findByUserIdAndStructure_IdStructure(userId, structureId)
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
        user.setActive(false); // ✅ Remplacé user.setIsActive par user.setActive
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
            .build();
}

}