package io.c4us.masterbackend.service;

import io.c4us.masterbackend.domain.Depense;
import io.c4us.masterbackend.repo.DepenseRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
public class DepenseService {

    private final DepenseRepo depenseRepository;

    public Depense saveDepense(Depense depense) {
        return depenseRepository.save(depense);
    }

    public void deleteDepense(Long id) {
        depenseRepository.deleteById(id);
    }

    public List<Depense> getAllByStructure(String codeStructure) {
        return depenseRepository.findByCodeStructure(codeStructure);
    }

    public Double getSumByDate(Date date, String code) {
        Double sum = depenseRepository.sumByDate(date, code);
        return sum != null ? sum : 0.0;
    }

    public Double getSumBetween(Date start, Date end, String code) {
        Double sum = depenseRepository.sumBetweenDates(start, end, code);
        return sum != null ? sum : 0.0;
    }

    public List<Map<String, Object>> getExpensesGroupedByUser(String codeStructure) {
        return depenseRepository.getExpensesGroupedByUser(codeStructure);
    }

    public Map<String, Double> getMonthlyDailyExpenses(String codeStructure, String period) {
        // 1. Déterminer les dates de début et de fin de mois (ex pour "2026-07")
        String[] parts = period.split("-");
        int year = Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]);

        LocalDate startLocalDate = LocalDate.of(year, month, 1);
        LocalDate endLocalDate = startLocalDate.withDayOfMonth(startLocalDate.lengthOfMonth());

        // Conversion en java.sql.Date pour correspondre au type de l'entité / base de données
        Date startDate = Date.valueOf(startLocalDate);
        Date endDate = Date.valueOf(endLocalDate);

        // 2. Récupérer toutes les dépenses de la structure sur cette plage de dates (en utilisant java.sql.Date)
        List<Depense> depenses = depenseRepository.findByCodeStructureAndDateDepenseBetween(
                codeStructure, startDate, endDate
        );

        // 3. Initialiser une map triée (TreeMap) pour avoir les jours dans l'ordre (de "01" à "31")
        Map<String, Double> dailyMap = new TreeMap<>();
        
        // Remplir initialement tous les jours possibles du mois avec 0.0
        for (int day = 1; day <= startLocalDate.lengthOfMonth(); day++) {
            String dayKey = String.format("%02d", day); // Format "01", "02", ...
            dailyMap.put(dayKey, 0.0);
        }

        // 4. Ventiler et sommer les dépenses enregistrées
        Calendar cal = Calendar.getInstance();
        for (Depense d : depenses) {
            if (d.getDateDepense() != null && d.getAmount() != null) {
                // Extraction sécurisée du jour à partir du java.sql.Date
                cal.setTime(d.getDateDepense());
                int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
                String dayKey = String.format("%02d", dayOfMonth);
                
                try {
                    // Conversion de la String 'amount' en double
                    double amountValue = Double.parseDouble(d.getAmount().trim());
                    
                    double currentSum = dailyMap.getOrDefault(dayKey, 0.0);
                    dailyMap.put(dayKey, currentSum + amountValue);
                } catch (NumberFormatException e) {
                    // Ignorer ou logger la dépense mal formatée
                    System.err.println("Format de montant invalide pour la dépense ID : " + d.getId());
                }
            }
        }

        return dailyMap;
    }
}