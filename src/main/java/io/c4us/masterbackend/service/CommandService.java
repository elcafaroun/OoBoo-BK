package io.c4us.masterbackend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.c4us.masterbackend.DTOs.CommandDto;
import io.c4us.masterbackend.config.EmailService;
import io.c4us.masterbackend.domain.Command;
import io.c4us.masterbackend.domain.CommandLine;

import io.c4us.masterbackend.exception.ResourceNotFoundException;
import io.c4us.masterbackend.repo.CommandRepo;
import io.c4us.masterbackend.repo.ProductRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@jakarta.transaction.Transactional(rollbackOn = Exception.class)
@RequiredArgsConstructor
public class CommandService {

    private final CommandRepo commandRepo;
    private final ProductRepo productRepo;
    private final EmailService emailService;

    // --- CRÉATION & LOGIQUE MÉTIER ---

public Command createCommand(CommandDto commandDto) {
        log.info("Réception d'une commande avec l'ID: {}", commandDto.getId());

        // 1. VERIFICATION D'EXISTENCE (Idempotence)
        // Si l'ID existe déjà, on retourne l'existant sans rien faire
        Optional<Command> existing = commandRepo.findById(commandDto.getId());
        if (existing.isPresent()) {
            log.warn("La commande {} existe déjà. Ignorer l'insertion.", commandDto.getId());
            return existing.get();
        }

        // 2. MAPPING DTO -> ENTITY
        Command command = new Command();
        command.setId(commandDto.getId()); // On utilise l'ID de Flutter
        command.setVersion(0L); // Force le statut "isNew"
        
        command.setCustomerName(commandDto.getCustomerName());
        command.setCodeStructure(commandDto.getCodeStructure());
        command.setPaymentMethod(commandDto.getPaymentMethod());
        command.setLastUpdated(LocalDateTime.now());
        command.setOrderDate(LocalDateTime.now());

        // 3. GESTION DES LIGNES
        if (commandDto.getItems() != null) {
            for (var itemDto : commandDto.getItems()) {
                CommandLine line = new CommandLine();
                line.setProductId(itemDto.getProductId()); // Important pour le stock
                line.setProductName(itemDto.getProductName());
                line.setQuantity(itemDto.getQuantity());
                line.setUnitPrice(itemDto.getUnitPrice());
                line.setCodeStructure(commandDto.getCodeStructure());
                
                // Lie la ligne à la commande
                command.addLigneCommande(line);

                // Mise à jour du stock local au Burkina
                updateStockAndCheckAlert(
                    itemDto.getProductName(), 
                    itemDto.getQuantity(), 
                    commandDto.getCodeStructure()
                );
            }
        }

        // 4. CONFIGURATION DES MONTANTS (Crédit vs Cash)
        configurerMontants(command, commandDto);

        // 5. SAUVEGARDE
        try {
            Command saved = commandRepo.saveAndFlush(command);
            log.info("✅ Commande enregistrée avec succès: {}", saved.getId());
            return saved;
        } catch (Exception e) {
            log.error("❌ Erreur lors de l'enregistrement SQL: {}", e.getMessage());
            throw e; 
        }
    }

    private void configurerMontants(Command command, CommandDto dto) {
        String method = dto.getPaymentMethod() != null ? dto.getPaymentMethod().toLowerCase() : "";
        if (method.contains("credit") || method.contains("crédit")) {
            command.setTotalAmount(0.0);
            command.setTotalCredit(dto.getTotalAmount());
            command.setStatus("PENDING");
        } else {
            command.setTotalAmount(dto.getTotalAmount());
            command.setTotalCredit(0.0);
            command.setStatus("COMPLETED");
        }
    }
    // --- MÉTHODES DE RECHERCHE (Remises en place) ---

    public List<Command> findAllCommand() {
        return commandRepo.findAll();
    }

    public Command findCommandById(String id) {
        return commandRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Command not found with id " + id));
    }

    public List<Command> findCommandsByStructure(String codeStructure) {
        return commandRepo.findByCodeStructure(codeStructure);
    }

    // --- ACTIONS SUR LES COMMANDES ---

    @Transactional
    public Command updateStatus(String id, String newStatus) {
        Command order = findCommandById(id);
        order.setStatus(newStatus);
        order.setLastUpdated(LocalDateTime.now());
        return commandRepo.save(order);
    }

    @Transactional
    public void deleteOrder(String id) {
        if (!commandRepo.existsById(id)) {
            throw new ResourceNotFoundException("Command not found with id " + id);
        }
        commandRepo.deleteById(id);
    }

    @Transactional
    public Command cancelOrder(String commandId) {
        Command command = findCommandById(commandId);
        if ("CANCELLED".equals(command.getStatus())) return command;

        for (CommandLine item : command.getItems()) {
            productRepo.findProductByStructure(item.getProductName(), command.getCodeStructure())
                .ifPresent(product -> {
                    product.setProductQte(product.getProductQte() + item.getQuantity());
                    product.setLastUpdated(LocalDateTime.now());
                    productRepo.save(product);
                });
        }
        command.setStatus("CANCELLED");
        command.setLastUpdated(LocalDateTime.now());
        return commandRepo.save(command);
    }

    @Transactional
    public Command settleCredit(String commandId, Double amountPaid, String newPaymentMethod) {
        Command command = findCommandById(commandId);

        double currentCredit = (command.getTotalCredit() != null) ? command.getTotalCredit() : 0.0;
        double currentAmount = (command.getTotalAmount() != null) ? command.getTotalAmount() : 0.0;

        if (amountPaid > currentCredit) amountPaid = currentCredit;

        command.setTotalCredit(currentCredit - amountPaid);
        command.setTotalAmount(currentAmount + amountPaid);
        command.setPaymentMethod(newPaymentMethod); 
        command.setLastUpdated(LocalDateTime.now());

        if (command.getTotalCredit() <= 0) {
            command.setStatus("COMPLETED");
        }
        return commandRepo.save(command);
    }

    // --- STATISTIQUES & SYNCHRO ---

    public List<Command> getCommandsUpdates(String codeStructure, LocalDateTime lastSync) {
        if (lastSync == null) return commandRepo.findByCodeStructure(codeStructure);
        return commandRepo.findByCodeStructureAndLastUpdatedAfter(codeStructure, lastSync);
    }

    public Double getSumByDate(java.sql.Date date, String code) {
        LocalDate localDate = date.toLocalDate();
        LocalDateTime startOfDay = localDate.atStartOfDay();
        LocalDateTime endOfDay = localDate.atTime(23, 59, 59);
        
        Double sum1 = commandRepo.sumCommandesByDate(startOfDay, endOfDay, code);
        Double sum2 = commandRepo.sumTotalCreditByDate(startOfDay, endOfDay, code);
        
        return (sum1 != null ? sum1 : 0.0) + (sum2 != null ? sum2 : 0.0);
    }

    public Map<String, Double> getSumByPaymentMethod(java.sql.Date dateSql, String code) {
        java.time.LocalDate date = dateSql.toLocalDate();
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(23, 59, 59);

        List<Object[]> results = commandRepo.sumByPaymentMethod(start, end, code);
        Map<String, Double> stats = new HashMap<>();
        for (Object[] row : results) {
            stats.put(row[0] != null ? (String) row[0] : "Inconnu", row[1] != null ? (Double) row[1] : 0.0);
        }
        return stats;
    }

    private void updateStockAndCheckAlert(String productName, double quantitySold, String codeStructure) {
        productRepo.findByProductNameAndCodeStructureAndDeletedFalse(productName, codeStructure).ifPresent(product -> {
            product.setProductQte(product.getProductQte() - quantitySold);
            product.setLastUpdated(LocalDateTime.now());
            
            if (product.getProductQte() <= (product.getStockAlert() != null ? product.getStockAlert() : 0.0)) {
                emailService.sendStockAlertEmail("bayala.m.olivier@gmail.com", product.getProductName(), product.getProductQte(), product.getStockAlert());
            }
            productRepo.save(product);
        });
    }
}