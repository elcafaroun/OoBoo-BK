package io.c4us.masterbackend.ressource;

import io.c4us.masterbackend.domain.Depense;
import io.c4us.masterbackend.service.DepenseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Date;
import java.util.List;
import java.util.Map; // <-- Importation ajoutée pour la Map

@RestController
@RequestMapping("/depense")
@RequiredArgsConstructor
public class DepenseRessource {

    private final DepenseService depenseService;

    @PostMapping
    public ResponseEntity<Depense> add(@RequestBody Depense depense) {
        return ResponseEntity.ok(depenseService.saveDepense(depense));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        depenseService.deleteDepense(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/structure/{code}")
    public ResponseEntity<List<Depense>> list(@PathVariable String code) {
        return ResponseEntity.ok(depenseService.getAllByStructure(code));
    }

    @GetMapping("/sum/day")
    public ResponseEntity<Double> getDaySum(@RequestParam Date date, @RequestParam String code) {
        return ResponseEntity.ok(depenseService.getSumByDate(date, code));
    }

    @GetMapping("/sum/period")
    public ResponseEntity<Double> getPeriodSum(
            @RequestParam Date start, 
            @RequestParam Date end, 
            @RequestParam String code) {
        return ResponseEntity.ok(depenseService.getSumBetween(start, end, code));
    }

    @GetMapping("/structure/{code}/monthly-daily-expenses")
    public ResponseEntity<Map<String, Double>> getMonthlyDailyExpenses(
            @PathVariable String code,
            @RequestParam String period) { // Prend en paramètre la période (ex: "2026-07")
        return ResponseEntity.ok(depenseService.getMonthlyDailyExpenses(code, period));
    }

    @GetMapping("/structure/{code}/by-user")
    public ResponseEntity<List<Map<String, Object>>> getExpensesGroupedByUser(
            @PathVariable String code) {
        return ResponseEntity.ok(depenseService.getExpensesGroupedByUser(code));
    }
}