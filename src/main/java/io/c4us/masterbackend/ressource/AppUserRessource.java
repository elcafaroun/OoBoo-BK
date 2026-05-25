package io.c4us.masterbackend.ressource;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.c4us.masterbackend.DTOs.LoginRequest;
import io.c4us.masterbackend.config.EmailService;
import io.c4us.masterbackend.domain.AppUser;
import io.c4us.masterbackend.repo.AppUserRepo;
import io.c4us.masterbackend.service.AppUserService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class AppUserRessource {

    @Autowired
    private  AppUserService appUserService;

    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private EmailService emailService;

    @Autowired
    private AppUserRepo appUserRepo;

    @PostMapping
    public ResponseEntity<AppUser> createAppUser(@RequestBody AppUser user) {
        try {
            return ResponseEntity.created(URI.create("/user/userID"))
                    .body(appUserService.createAppUser(user));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/confirm")
    public ResponseEntity<String> confirmStructure(@RequestParam("token") String confirmationToken) {

        // 1. Trouver la structure par le token
        Optional<AppUser> userOpt = appUserService.findByConfirmationToken(confirmationToken);

        if (userOpt.isEmpty()) {

            URI redirectUri = URI.create("http://localhost:5173/confirmation-error");
            return ResponseEntity.status(HttpStatus.FOUND).location(redirectUri).build();

        }

        AppUser appUser = userOpt.get();

        // 2. Mettre à jour isActive à "true"
        appUser.setActive(true);
        appUser.setConfirmationToken(null); // Optionnel: Invalider le token après usage
        appUserService.updateAppUser(appUser);

        URI redirectUri = URI.create("http://localhost:5173/login?confirmed=true");
        return ResponseEntity.status(HttpStatus.FOUND).location(redirectUri).build();

        // Vous pouvez aussi utiliser un RedirectView pour rediriger vers une page de
        // succès
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

        // 1. Rechercher l’utilisateur (Email ou Téléphone)
        AppUser user = appUserRepo.findByUserEmail(identifier);
        if (user == null) {
            user = appUserRepo.findByUserPhone(identifier);
            if(user==null){
             user = appUserRepo.findByCodeUser(identifier);
            }
        }

        // 2. Vérification existence et mot de passe
        if (user == null || !passwordEncoder.matches(password, user.getUserPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Identifiants incorrects");
        }

        // 3. Vérification de l’activation du compte
        if (!user.isActive()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Veuillez confirmer votre compte avant de vous connecter.");
        }

        // ✅ 4. Connexion réussie : Utilisation de HashMap pour éviter le
        // NullPointerException
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Connexion réussie");
        response.put("id", user.getId());
        response.put("userName", user.getUserName());
        response.put("userPhone", user.getUserPhone());
        response.put("userProfile", user.getUserProfile()); // Indispensable pour Flutter
        response.put("codeStructure", user.getCodeStructure());
        // On peut mettre l'email même s'il est null, HashMap l'acceptera
        response.put("userEmail", user.getUserEmail());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/structure/{codeStructure}")
    public ResponseEntity<List<AppUser>> getUsersByStructure(@PathVariable String codeStructure) {
        return ResponseEntity.ok(appUserService.getAllUsersByStructure(codeStructure));
    }

    // 2. Modifier un utilisateur
    @PutMapping("/update/{id}")
    public ResponseEntity<AppUser> updateAppUser(@PathVariable String id, @RequestBody AppUser user) {
        return ResponseEntity.ok(appUserService.updateUser(id, user));
    }

    // 3. Désactiver un utilisateur (Recommandé à la place de supprimer)
    @PatchMapping("/disable/{id}")
    public ResponseEntity<Void> disableAppUser(@PathVariable String id) {
        appUserService.disableUser(id);
        return ResponseEntity.noContent().build();
    }

    // 4. Supprimer un utilisateur
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deleteAppUser(@PathVariable String id) {
        appUserService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/enable/{id}")
    public ResponseEntity<Void> enableAppUser(@PathVariable String id) {
        AppUser user = appUserService.getAppUser(id);
        user.setActive(true);
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
        boolean isUnique = appUserService.isEmailUnique(email);
        // Renvoie un JSON propre {"available": true/false}
        return ResponseEntity.ok(Map.of("available", isUnique));
    }

    /**
     * 🔹 Endpoint : Vérifier la disponibilité d'un numéro de téléphone
     * URL : GET /api/v1/users/check-phone?phone=70000000
     */
    @GetMapping("/check-phone")
    public ResponseEntity<Map<String, Boolean>> checkPhoneAvailability(@RequestParam String phone) {
        boolean isUnique = appUserService.isPhoneUnique(phone);
        // Renvoie un JSON propre {"available": true/false}
        return ResponseEntity.ok(Map.of("available", isUnique));
    }
}
