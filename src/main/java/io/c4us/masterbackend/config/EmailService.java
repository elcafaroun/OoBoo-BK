package io.c4us.masterbackend.config;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import io.c4us.masterbackend.domain.AppUser;
import io.c4us.masterbackend.repo.AppUserRepo;
import lombok.extern.slf4j.Slf4j; // 1. Importez ceci

@Slf4j // 2. Ajoutez ceci

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private AppUserRepo appUserRepo;

    public void sendConfirmationEmail(AppUser user) {
        SimpleMailMessage message = new SimpleMailMessage();

        // 1. Définir l'URL de confirmation
        // L'URL devrait pointer vers votre contrôleur de confirmation, e.g.,
        // "http://localhost:8080/api/structures/confirm?token=" +
        // structure.getConfirmationToken()
        String confirmationUrl = "http://localhost:8080/" + "user/confirm?token=" + user.getConfirmationToken();

        // 2. Préparer le message
        message.setTo(user.getUserEmail());
        message.setSubject("Confirmation de la création de votre structure : " + user.getUserName());
        message.setText("Bonjour " + user.getUserName() + ",\n\n"
                + "Veuillez cliquer sur le lien ci-dessous pour activer votre structure et finaliser l'inscription :\n"
                + confirmationUrl + "\n\n"
                + "Merci.");

        // 3. Envoyer l'e-mail
        mailSender.send(message);
    }

public void sendStockAlertEmail(String targetEmail, String productName, double currentQuantity, double alertThreshold) {
    SimpleMailMessage message = new SimpleMailMessage();

    // 1. Préparer le destinataire et l'objet
    message.setTo(targetEmail);
    message.setSubject("⚠️ ALERTE STOCK CRITIQUE : " + productName);

    // 2. Préparer le contenu du message
    String emailText = "ALERTE DE RUPTURE DE STOCK\n"
            + "-----------------------------------\n"
            + "Produit : " + productName + "\n"
            + "Quantité actuelle : " + currentQuantity + "\n"
            + "Seuil d'alerte défini : " + alertThreshold + "\n"
            + "-----------------------------------\n"
            + "Veuillez prévoir un réapprovisionnement immédiat pour éviter une rupture de stock.\n\n"
            + "Ceci est un message automatique généré par MasterBackend.";

    message.setText(emailText);

    // 3. Envoyer l'e-mail
    try {
        mailSender.send(message);
        log.info("📧 E-mail d'alerte stock envoyé pour le produit : {}", productName);
    } catch (Exception e) {
        log.error("❌ Erreur lors de l'envoi de l'e-mail d'alerte : {}", e.getMessage());
    }
}

    public boolean resendConfirmationEmail(String email) {
        AppUser user = appUserRepo.findByUserEmail(email);
        if (user == null || user.isActive()) {
            return false; // compte inexistant ou déjà confirmé
        }
        user.setConfirmationToken(UUID.randomUUID().toString());
        appUserRepo.save(user);
        sendConfirmationEmail(user);
        return true;
    }

}