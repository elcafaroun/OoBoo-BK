package io.c4us.masterbackend.config;

import java.util.UUID;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import io.c4us.masterbackend.domain.AppUser;
import io.c4us.masterbackend.repo.AppUserRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor // ✅ Génère proprement le constructeur avec les dépendances 'final'
public class EmailService {

    private final JavaMailSender mailSender;
    private final AppUserRepo appUserRepo;

    public void sendConfirmationEmail(AppUser user) {
        SimpleMailMessage message = new SimpleMailMessage();

        // 1. Définir l'URL de confirmation
        String confirmationUrl = "http://localhost:8080/user/confirm?token=" + user.getConfirmationToken();

        // 2. Préparer le message
        message.setTo(user.getUserEmail());
        message.setSubject("PB-M : Confirmation de la création de votre compte : " + user.getUserName());
        message.setText("Bonjour " + user.getUserName() + ",\n\n"
                + "Veuillez cliquer sur le lien ci-dessous pour activer votre compte utilisateur et finaliser l'inscription :\n"
                + confirmationUrl + "\n\n"
                + "Merci.");

        // 3. Envoyer l'e-mail avec gestion des erreurs
        try {
            mailSender.send(message);
            log.info("📧 E-mail de confirmation envoyé avec succès à : {}", user.getUserEmail());
        } catch (Exception e) {
            log.error("❌ Impossible d'envoyer l'e-mail de confirmation à {} : {}", user.getUserEmail(), e.getMessage());
        }
    }

    public void sendStockAlertEmail(String targetEmail, String productName, double currentQuantity, double alertThreshold) {
        SimpleMailMessage message = new SimpleMailMessage();

        // 1. Préparer le destinataire et l'objet
        message.setTo(targetEmail);
        message.setSubject("⚠️ ALERTE STOCK CRITIQUE : " + productName);

        // 2. Préparer le contenu du message
        String emailText = "ALERTE DE RUPTURE DE STOCK - PB-M\n"
                + "-----------------------------------\n"
                + "Produit : " + productName + "\n"
                + "Quantité actuelle : " + currentQuantity + "\n"
                + "Seuil d'alerte défini : " + alertThreshold + "\n"
                + "-----------------------------------\n"
                + "Veuillez prévoir un réapprovisionnement immédiat pour éviter une rupture de stock.\n\n"
                + "Ceci est un message automatique généré par le système de gestion PB-M.";

        message.setText(emailText);

        // 3. Envoyer l'e-mail
        try {
            mailSender.send(message);
            log.info("📧 E-mail d'alerte stock envoyé pour le produit : {}", productName);
        } catch (Exception e) {
            log.error("❌ Erreur lors de l'envoi de l'e-mail d'alerte stock : {}", e.getMessage());
        }
    }

    public boolean resendConfirmationEmail(String email) {
        AppUser user = appUserRepo.findByUserEmail(email);
        
        // ✅ Correction ici : Utilisation de getIsActive() généré par Lombok pour le type Boolean
        if (user == null || Boolean.TRUE.equals(user.getActive())) {
            return false; // compte inexistant ou déjà actif/confirmé
        }
        
        user.setConfirmationToken(UUID.randomUUID().toString());
        appUserRepo.save(user);
        sendConfirmationEmail(user);
        return true;
    }
}