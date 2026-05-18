package io.c4us.masterbackend.ressource;

import io.c4us.masterbackend.domain.Depense;
import io.c4us.masterbackend.service.DepenseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Date;
import java.util.List;

@RestController
@RequestMapping("/depense")
@RequiredArgsConstructor
//@CrossOrigin(originPatterns = "*", allowCredentials = "true") // ✅ Change origins par originPatterns
//@CrossOrigin(origins = "*")
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
}