package io.c4us.masterbackend.exception;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. Quotas explicites d'utilisateurs/licences (QuotaExceededException)
    @ExceptionHandler(QuotaExceededException.class)
    public ResponseEntity<Map<String, Object>> handleQuotaExceededException(QuotaExceededException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "QUOTA_EXCEEDED", ex.getMessage());
    }

    // 2. Erreurs d'état et limites d'abonnement (Catégories & Produits via IllegalStateException)
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalStateException(IllegalStateException ex) {
        String message = ex.getMessage() != null ? ex.getMessage() : "";
        // Détecte "Limite atteinte" pour mapper sur QUOTA_EXCEEDED
        String code = message.contains("Limite atteinte") ? "QUOTA_EXCEEDED" : "ILLEGAL_STATE";
        return buildResponse(HttpStatus.BAD_REQUEST, code, message);
    }

    // 3. EntityNotFoundException (Exceptions JPA/Service explicites)
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleEntityNotFoundException(EntityNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", ex.getMessage());
    }

    // 4. Arguments invalides (Ex: quantité <= 0 dans le stock)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "INVALID_ARGUMENT", ex.getMessage());
    }

    // 5. Validation des annotations DTO (@Valid, @NotBlank, etc.)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException ex) {
        String firstError = ex.getBindingResult().getAllErrors().isEmpty() 
            ? "Données de formulaire invalides." 
            : ex.getBindingResult().getAllErrors().get(0).getDefaultMessage();
            
        return buildResponse(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", firstError);
    }

    // 6. RuntimeExceptions générales (Doublons Email/Téléphone, "Category Not found", "Structure introuvable")
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        String message = ex.getMessage() != null ? ex.getMessage() : "Erreur métier lors du traitement.";
        String code = "BAD_REQUEST_ERROR";
        String lowerMsg = message.toLowerCase();

        if (lowerMsg.contains("not found") || lowerMsg.contains("introuvable") || lowerMsg.contains("non trouvé")) {
            code = "RESOURCE_NOT_FOUND";
        } else if (lowerMsg.contains("email")) {
            code = "EMAIL_ALREADY_EXISTS";
        } else if (lowerMsg.contains("téléphone") || lowerMsg.contains("phone")) {
            code = "PHONE_ALREADY_EXISTS";
        }

        return buildResponse(HttpStatus.BAD_REQUEST, code, message);
    }

    // 7. Catch-all pour erreurs serveur non contrôlées (HTTP 500)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(Exception ex) {
        String message = ex.getMessage() != null ? ex.getMessage() : "Une erreur interne s'est produite.";
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", message);
    }

    // Centralisation du format de réponse JSON
    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String code, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("code", code);
        body.put("message", message);

        return new ResponseEntity<>(body, status);
    }
}