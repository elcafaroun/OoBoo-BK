package io.c4us.masterbackend.ressource;

import java.sql.Date;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.c4us.masterbackend.DTOs.CommandDto;
import io.c4us.masterbackend.DTOs.SettleCreditDto;
import io.c4us.masterbackend.domain.Command;
import io.c4us.masterbackend.service.CommandService;
import io.c4us.masterbackend.service.DepenseService;
import lombok.RequiredArgsConstructor;


@RestController
@RequestMapping("/command")
@RequiredArgsConstructor
public class CommandRessource {

    private final CommandService commandService;
    private final DepenseService depenseService;

    /**
     * ENDPOINT DE SYNCHRONISATION (Crucial pour Flutter)
     * Permet de récupérer les commandes passées par d'autres tablettes/utilisateurs
     */
    @GetMapping("/sync/{codeStructure}")
    public ResponseEntity<List<Command>> syncCommands(
            @PathVariable String codeStructure,
            @RequestParam(value = "lastSync", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime lastSync) {

        return ResponseEntity.ok(commandService.getCommandsUpdates(codeStructure, lastSync));
    }

    // POST : Créer une nouvelle commande (ID généré par Flutter)
    @PostMapping
    public ResponseEntity<Command> createCommand(@RequestBody CommandDto commandDto) {
        // Ce log DOIT apparaître dans votre console IDE si l'appel arrive
        System.out.println(
                ">>> APPEL RECU : Commande ID " + commandDto.getId() + " pour " + commandDto.getCustomerName());

        try {
            Command savedCommand = commandService.createCommand(commandDto);
            return new ResponseEntity<>(savedCommand, HttpStatus.CREATED);
        } catch (Exception e) {
            System.err.println(">>> ERREUR SERVICE : " + e.getMessage());
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping
    public List<Command> getAllCommand() {
        return commandService.findAllCommand();
    }

    // Changement de Long vers String pour l'ID UUID
    @GetMapping("/{id}")
    public Command getOrderById(@PathVariable String id) {
        return commandService.findCommandById(id);
    }

    @PutMapping("/{id}/status")
    public Command updateOrderStatus(@PathVariable String id, @RequestBody String newStatus) {
        return commandService.updateStatus(id, newStatus);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable String id) {
        commandService.deleteOrder(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/structure/{codeStructure}")
    public List<Command> getCommandsByStructure(@PathVariable String codeStructure) {
        return commandService.findCommandsByStructure(codeStructure);
    }

    @GetMapping("/dashboard/summary")
    public ResponseEntity<Map<String, Double>> getSummary(@RequestParam Date date, @RequestParam String code) {
        Double totalDepenses = depenseService.getSumByDate(date, code);
        Double totalCommandes = commandService.getSumByDate(date, code);

        Map<String, Double> summary = new HashMap<>();
        summary.put("totalDepenses", totalDepenses != null ? totalDepenses : 0.0);
        summary.put("totalCommandes", totalCommandes != null ? totalCommandes : 0.0);
        summary.put("benefice",
                (totalCommandes != null ? totalCommandes : 0.0) - (totalDepenses != null ? totalDepenses : 0.0));

        return ResponseEntity.ok(summary);
    }

    @GetMapping("/stats/payment-methods")
    public ResponseEntity<Map<String, Double>> getPaymentStats(
            @RequestParam java.sql.Date date,
            @RequestParam String code) {
        return ResponseEntity.ok(commandService.getSumByPaymentMethod(date, code));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<Command> cancelCommand(@PathVariable String id) {
       // System.out.println(">>> REQUETE SETTLE RECUE : ID=" + id + ", Montant=" + dto.getAmountPaid());
        Command updatedCommand = commandService.cancelOrder(id);
        return ResponseEntity.ok(updatedCommand);
    }

    @PutMapping("/settle/{id}")
    public ResponseEntity<Command> settleCredit(@PathVariable String id, @RequestBody SettleCreditDto dto) {
        Command updatedCommand = commandService.settleCredit(
                id,
                dto.getAmountPaid(),
                dto.getPaymentMethod());
                System.out.println(">>> REQUETE SETTLE RECUE : ID=" + id + ", Montant=" + dto.getAmountPaid());
        return ResponseEntity.ok(updatedCommand);
    }
}