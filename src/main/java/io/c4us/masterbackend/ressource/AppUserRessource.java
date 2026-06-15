package io.c4us.masterbackend.ressource;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import io.c4us.masterbackend.DTOs.AppUserDTO;
import io.c4us.masterbackend.DTOs.LoginRequest;
import io.c4us.masterbackend.domain.AppUser;
import io.c4us.masterbackend.repo.AppUserRepo;
import io.c4us.masterbackend.service.AppUserService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class AppUserRessource {

    private final AppUserService appUserService;
    private final PasswordEncoder passwordEncoder;
    private final io.c4us.masterbackend.config.EmailService emailService;
    private final AppUserRepo appUserRepo;


    @PostMapping
    public ResponseEntity<AppUser> createAppUser(
            @RequestBody AppUser user,
            @RequestParam(required = false) String codeStructure,
            @RequestParam(required = false, defaultValue = "COLLABORATEUR") String role) {
        try {
            AppUser createdUser = appUserService.createAppUser(user, codeStructure, role);
            return ResponseEntity.created(URI.create("/user/" + createdUser.getId()))
                    .body(createdUser);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Associer un utilisateur existant à une structure supplémentaire
     * (Multi-structure)
     */
    @PostMapping("/{userId}/associate")
    public ResponseEntity<Map<String, String>> associateToStructure(
            @PathVariable String userId,
            @RequestParam String codeStructure,
            @RequestParam(required = false, defaultValue = "COLLABORATEUR") String role) {
        try {
            appUserService.associateUserToStructure(userId, codeStructure, role);
            return ResponseEntity
                    .ok(Map.of("message", "Utilisateur associé avec succès à la structure " + codeStructure));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/confirm")
    public ResponseEntity<String> confirmStructure(@RequestParam("token") String confirmationToken) {
        Optional<AppUser> userOpt = appUserService.findByConfirmationToken(confirmationToken);

        if (userOpt.isEmpty()) {
            URI redirectUri = URI.create("http://localhost:5173/confirmation-error");
            return ResponseEntity.status(HttpStatus.FOUND).location(redirectUri).build();
        }

        AppUser appUser = userOpt.get();
        appUser.setActive(true); // ✅ Mis à jour (setActive au lieu de setIsActive)
        appUser.setConfirmationToken(null);
        appUserService.updateAppUser(appUser);

        URI redirectUri = URI.create("http://localhost:5173/login?confirmed=true");
        return ResponseEntity.status(HttpStatus.FOUND).location(redirectUri).build();
    }

    @PostMapping("/resend-confirmation")
    public ResponseEntity<String> resendConfirmation(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        boolean sent = emailService.resendConfirmationEmail(email);

        if (sent) {
            return ResponseEntity.ok("Un nouveau lien de confirmation a été envoyé à " + email);
        } else {
            return ResponseEntity.badRequest().body("Aucun compte trouvé pour cet e-mail ou déjà confirmé.");
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody LoginRequest request) {
        String identifier = request.getIdentifier();
        String password = request.getPassword();

        AppUser user = appUserRepo.findByUserEmail(identifier);
        if (user == null) {
            user = appUserRepo.findByUserPhone(identifier);
            if (user == null) {
                user = appUserRepo.findByCodeUser(identifier);
            }
        }

        if (user == null || !passwordEncoder.matches(password, user.getUserPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Identifiants incorrects");
        }

        if (!Boolean.TRUE.equals(user.getActive())) { // ✅ Mis à jour (getActive au lieu de getIsActive)
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Veuillez confirmer votre compte avant de vous connecter.");
        }

        // Extraction des structures associées pour l'application mobile PB-M
        List<Map<String, Object>> structuresAssociees = user.getStructures().stream()
                .filter(link -> !Boolean.TRUE.equals(link.getDeleted()))
                .map(link -> {
                    Map<String, Object> sMap = new HashMap<>();
                    sMap.put("idStructure", link.getStructure().getIdStructure());
                    sMap.put("nomStructure", link.getStructure().getNomStructure());
                    sMap.put("codeStructure", link.getStructure().getCodeStructure());
                    sMap.put("roleInStructure", link.getRoleInStructure());
                    return sMap;
                }).collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Connexion réussie");
        response.put("id", user.getId());
        response.put("userName", user.getUserName());
        response.put("userPhone", user.getUserPhone());
        response.put("userProfile", user.getUserProfile());
        response.put("userEmail", user.getUserEmail());
        response.put("isFirstLogin", user.getFirstLogin()); // ✅ Mis à jour (getFirstLogin au lieu de getIsFirstLogin)
        response.put("structures", structuresAssociees);

        return ResponseEntity.ok(response);
    }

    // Dans StructureRessource.java
   @GetMapping("/users/{codeStructure}")
public ResponseEntity<List<AppUserDTO>> getUsersByStructure(@PathVariable String codeStructure) {
    return ResponseEntity.ok(appUserService.getAllUsersByStructure(codeStructure));
}

    @PutMapping("/update/{id}")
    public ResponseEntity<AppUser> updateAppUser(@PathVariable String id, @RequestBody AppUser user) {
        return ResponseEntity.ok(appUserService.updateUser(id, user));
    }

    @PatchMapping("/disable/{id}")
    public ResponseEntity<Void> disableAppUser(@PathVariable String id) {
        appUserService.disableUser(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deleteAppUser(@PathVariable String id) {
        appUserService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/enable/{id}")
    public ResponseEntity<Void> enableAppUser(@PathVariable String id) {
        AppUser user = appUserService.getAppUser(id);
        user.setActive(true); // ✅ Mis à jour (setActive au lieu de setIsActive)
        appUserRepo.save(user);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/reset-password/{id}")
    public ResponseEntity<Void> resetPassword(@PathVariable String id, @RequestBody Map<String, String> request) {
        appUserService.changePassword(id, request.get("newPassword"));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/check-email")
    public ResponseEntity<Map<String, Boolean>> checkEmailAvailability(@RequestParam String email) {
        return ResponseEntity.ok(Map.of("available", appUserService.isEmailUnique(email)));
    }

    @GetMapping("/check-phone")
    public ResponseEntity<Map<String, Boolean>> checkPhoneAvailability(@RequestParam String phone) {
        return ResponseEntity.ok(Map.of("available", appUserService.isPhoneUnique(phone)));
    }

    @PatchMapping("/change-password/{id}")
    public ResponseEntity<?> changeFirstPassword(@PathVariable String id, @RequestBody Map<String, String> request) {
        AppUser user = appUserRepo.findById(id).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Utilisateur non trouvé");
        }

        String newPassword = request.get("newPassword");
        if (newPassword == null || newPassword.length() != 4) {
            return ResponseEntity.badRequest().body("Le code PIN doit contenir exactement 4 chiffres");
        }

        user.setUserPassword(passwordEncoder.encode(newPassword));
        user.setFirstLogin(false); // ✅ Mis à jour (setFirstLogin au lieu de setIsFirstLogin)
        appUserRepo.save(user);

        return ResponseEntity.ok(Map.of("message", "Mot de passe mis à jour avec succès"));
    }

    /**
     * Supprimer l'affectation d'un utilisateur à une structure
     */
    @DeleteMapping("/{userId}/structure/{structureId}")
    public ResponseEntity<Void> removeUserFromStructure(@PathVariable String userId, @PathVariable String structureId) {
        appUserService.removeUserFromStructure(userId, structureId);
        return ResponseEntity.noContent().build();
    }
}