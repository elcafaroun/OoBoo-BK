package io.c4us.masterbackend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.c4us.masterbackend.DTOs.CommandDto;
import io.c4us.masterbackend.DTOs.UserSalesDto;
import io.c4us.masterbackend.config.EmailService;
import io.c4us.masterbackend.domain.Command;
import io.c4us.masterbackend.domain.CommandLine;
import io.c4us.masterbackend.domain.Customer;
import io.c4us.masterbackend.domain.SegmentRule;
import io.c4us.masterbackend.exception.ResourceNotFoundException;
import io.c4us.masterbackend.repo.CommandRepo;
import io.c4us.masterbackend.repo.CustomerRepo;
import io.c4us.masterbackend.repo.ProductRepo;
import io.c4us.masterbackend.repo.SegmentRuleRepo;
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
    private final CustomerRepo customerRepo;
    private final SegmentRuleRepo segmentRuleRepo;
    private final EmailService emailService;

    // --- CRÉATION & LOGIQUE MÉTIER ---

    public Command createCommand(CommandDto commandDto) {
        log.info("Réception d'une commande avec l'ID: {}", commandDto.getId());

        // 1. VERIFICATION D'EXISTENCE (Idempotence)
        Optional<Command> existing = commandRepo.findById(commandDto.getId());
        if (existing.isPresent()) {
            log.warn("La commande {} existe déjà. Ignorer l'insertion.", commandDto.getId());
            return existing.get();
        }

        // 2. MAPPING DTO -> ENTITY
        Command command = new Command();
        command.setId(commandDto.getId());
        command.setVersion(0L);

        command.setCustomerName(commandDto.getCustomerName());
        command.setCustomerNum(commandDto.getCustomerNum());
        command.setCodeStructure(commandDto.getCodeStructure());
        command.setPaymentMethod(commandDto.getPaymentMethod());
        command.setUserId(commandDto.getUserId());
        command.setUserName(commandDto.getUserName());
        command.setLastUpdated(LocalDateTime.now());
        command.setOrderDate(LocalDateTime.now());

        // 3. GESTION DES LIGNES
        if (commandDto.getItems() != null) {
            for (var itemDto : commandDto.getItems()) {
                CommandLine line = new CommandLine();
                line.setProductId(itemDto.getProductId());
                line.setProductName(itemDto.getProductName());
                line.setQuantity(itemDto.getQuantity());
                line.setUnitPrice(itemDto.getUnitPrice());
                line.setCodeStructure(commandDto.getCodeStructure());

                command.addLigneCommande(line);

                updateStockAndCheckAlert(
                        itemDto.getProductName(),
                        itemDto.getQuantity(),
                        commandDto.getCodeStructure());
            }
        }

        // 4. CONFIGURATION DES MONTANTS (Crédit vs Cash)
        configurerMontants(command, commandDto);

        // 5. ATTRIBUTION DES POINTS CLIENT
        if (commandDto.getCustomerNum() != null && !commandDto.getCustomerNum().trim().isEmpty()) {
            updateCustomerPoints(commandDto.getCustomerNum(), commandDto.getCodeStructure(), command.getTotalAmount());
        }

        // 6. SAUVEGARDE
        try {
            Command saved = commandRepo.saveAndFlush(command);
            log.info("✅ Commande enregistrée avec succès: {}", saved.getId());
            return saved;
        } catch (Exception e) {
            log.error("❌ Erreur lors de l'enregistrement SQL: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Calcule et met à jour les points fidélité du client
     */
 private void updateCustomerPoints(String phone, String codeStructure, Double amountPaid) {
    if (amountPaid == null || amountPaid <= 0) {
        return;
    }

    Optional<Customer> customerOpt = customerRepo.findByNumCustAndCodeStructure(phone, codeStructure);
    if (customerOpt.isEmpty()) {
        log.warn("⚠️ Client introuvable avec le numéro {} pour la structure {}", phone, codeStructure);
        return;
    }

    Customer customer = customerOpt.get();
    String segmentName = customer.getSegment() != null ? customer.getSegment() : "STANDARD";

    Optional<SegmentRule> ruleOpt = segmentRuleRepo.findBySegmentName(segmentName);

    if (ruleOpt.isPresent()) {
        SegmentRule rule = ruleOpt.get();

        if (amountPaid >= rule.getMinAmountOrder()) {
            double rate = (rule.getConversionRate() != null && rule.getConversionRate() > 0) ? rule.getConversionRate() : 1000.0;
            double pointsEarnedPerUnit = rule.getPointsEarned() != null ? rule.getPointsEarned() : 1.0;

            // 💥 CALCUL EN DOUBLE (autorise les décimales comme 1.5, 2.5)
            double earnedPoints = (amountPaid / rate) * pointsEarnedPerUnit;

            if (earnedPoints > 0) {
                double currentPoints = customer.getNombreDePoints() != null ? customer.getNombreDePoints() : 0.0;
                
                // Mettre à jour avec le total en Double
                customer.setNombreDePoints(currentPoints + earnedPoints);
                customerRepo.save(customer);

                log.info("⭐ {} points attribués au client {} (Segment: {}). Nouveau solde: {}",
                        earnedPoints, customer.getCustomerName(), segmentName, customer.getNombreDePoints());
            }
        }
    } else {
        log.warn("⚠️ Aucune règle de fidélité trouvée pour le segment {}", segmentName);
    }
}

    private void configurerMontants(Command command, CommandDto dto) {
        String method = dto.getPaymentMethod() != null ? dto.getPaymentMethod().toLowerCase() : "";
        if (method.contains("credit") || method.contains("crédit") || method.contains("pending")
                || method.contains("PENDING ")) {
            command.setTotalAmount(0.0);
            command.setTotalCredit(dto.getTotalAmount());
            command.setStatus("PENDING");
        } else {
            command.setTotalAmount(dto.getTotalAmount());
            command.setTotalCredit(0.0);
            command.setStatus("COMPLETED");
        }
    }

    // --- AUTRES MÉTHODES (Inchangées) ---

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
        if ("CANCELLED".equals(command.getStatus()))
            return command;

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

        if (amountPaid > currentCredit)
            amountPaid = currentCredit;

        command.setTotalCredit(currentCredit - amountPaid);
        command.setTotalAmount(currentAmount + amountPaid);
        command.setPaymentMethod(newPaymentMethod);
        command.setLastUpdated(LocalDateTime.now());

        if (command.getTotalCredit() <= 0) {
            command.setStatus("COMPLETED");
        }

        // Attribution des points sur le règlement de crédit effectué
        if (command.getCustomerNum() != null) {
            updateCustomerPoints(command.getCustomerNum(), command.getCodeStructure(), amountPaid);
        }

        return commandRepo.save(command);
    }

    public List<Command> getCommandsUpdates(String codeStructure, LocalDateTime lastSync) {
        if (lastSync == null)
            return commandRepo.findByCodeStructure(codeStructure);
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

    public List<UserSalesDto> getSalesByUserForStructure(String codeStructure) {
        return commandRepo.getSalesGroupedByUser(codeStructure);
    }

    private void updateStockAndCheckAlert(String productName, double quantitySold, String codeStructure) {
        productRepo.findByProductNameAndCodeStructureAndDeletedFalse(productName, codeStructure).ifPresent(product -> {
            product.setProductQte(product.getProductQte() - quantitySold);
            product.setLastUpdated(LocalDateTime.now());

            if (product.getProductQte() <= (product.getStockAlert() != null ? product.getStockAlert() : 0.0)) {
                emailService.sendStockAlertEmail("bayala.m.olivier@gmail.com", product.getProductName(),
                        product.getProductQte(), product.getStockAlert());
            }
            productRepo.save(product);
        });
    }

    public Map<String, Double> getDailySalesForMonth(String codeStructure, String yearMonth) {
        Map<String, Double> salesMap = new HashMap<>();

        for (int i = 1; i <= 31; i++) {
            String day = String.format("%02d", i);
            salesMap.put(day, 0.0);
        }

        List<Object[]> results = commandRepo.findDailySalesForMonth(codeStructure, yearMonth);

        for (Object[] row : results) {
            String day = (String) row[0];
            Double total = ((Number) row[1]).doubleValue();
            salesMap.put(day, total);
        }

        return salesMap;
    }
}