package io.c4us.masterbackend.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import io.c4us.masterbackend.domain.AppUser;
import io.c4us.masterbackend.repo.AppUserRepo;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@Transactional(rollbackOn = Exception.class)
@RequiredArgsConstructor
public class AppUserService {

    @Autowired
    private AppUserRepo appUserRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public AppUser createAppUser(AppUser user) {
        user.setUserPassword(passwordEncoder.encode(user.getUserPassword()));
        user.setConfirmationToken(generateAndSetToken(user));
        // appUserRepo.save(user);

        AppUser users = appUserRepo.save(user);

        return users;

    }

    public String generateAndSetToken(AppUser user) {
        // 1. Générer un Token robuste (UUID)
        String token = UUID.randomUUID().toString();

        // 2. Définir la durée d'expiration (24 heures)
        LocalDateTime expiryDate = LocalDateTime.now().plusHours(24);

        // 3. Mettre à jour la structure
        user.setConfirmationToken(token);
        user.setTokenExpiryDate(expiryDate);

        // Le service doit ensuite persister ces changements
        // (repository.save(structure))

        return token;
    }

    public Optional<AppUser> findByConfirmationToken(String token) {
        // L'appel utilise l'objet injecté structureRepository
        return appUserRepo.findByConfirmationToken(token);
    }

    public AppUser getAppUser(String id) {
        return appUserRepo.findById(id).orElseThrow(() -> new RuntimeException("User Not found"));
    }

    public AppUser updateAppUser(AppUser us) {
        try {
            AppUser user = getAppUser(us.getId());
            user.setId(us.getId());
            appUserRepo.save(user);
            return user;
        } catch (Exception exception) {
            throw new RuntimeException();
        }
    }

    public List<AppUser> getActiveUsersByStructure(String codeStructure) {
        return appUserRepo.findByCodeStructureAndIsActiveTrue(codeStructure);
    }

    // Modifier un utilisateur
    public AppUser updateUser(String id, AppUser userDetails) {
        AppUser user = appUserRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        user.setUserName(userDetails.getUserName());
        user.setUserPhone(userDetails.getUserPhone());
        user.setUserProfile(userDetails.getUserProfile());
        // On ne modifie pas le mot de passe ici pour des raisons de sécurité

        return appUserRepo.save(user);
    }

    // Désactiver un utilisateur (Suppression logique)
    public void disableUser(String id) {
        AppUser user = appUserRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        user.setActive(false);
        appUserRepo.save(user);
    }

    // Supprimer définitivement
    public void deleteUser(String id) {
        appUserRepo.deleteById(id);
    }

    public List<AppUser> getAllUsersByStructure(String codeStructure) {
        return appUserRepo.findByCodeStructure(codeStructure);
    }

    // Changement de mot de passe
    public void changePassword(String userId, String newPassword) {
        AppUser user = appUserRepo.findById(userId)
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        
        // user.setUserPassword(passwordEncoder.encode(newPassword)); // Version sécurisée
                user.setUserPassword(passwordEncoder.encode(newPassword));

      // user.setUserPassword(newPassword); // Version simple
        appUserRepo.save(user);
    }

    public void toggleUserActive(String id, boolean status) {
    AppUser user = appUserRepo.findById(id)
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
    user.setActive(status);
    appUserRepo.save(user);
}

}
