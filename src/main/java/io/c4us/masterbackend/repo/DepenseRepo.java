package io.c4us.masterbackend.repo;

import java.sql.Date;
import java.util.List;
import java.util.Map;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.c4us.masterbackend.domain.Depense;

public interface DepenseRepo extends JpaRepository<Depense, Long> {
    // Liste par structure
    List<Depense> findByCodeStructure(String codeStructure);

    // Somme des dépenses pour une date précise
    @Query("SELECT SUM(CAST(d.amount AS double)) FROM Depense d WHERE d.dateDepense = :date AND d.codeStructure = :code")
    Double sumByDate(@Param("date") Date date, @Param("code") String code);

    // Somme des dépenses entre deux dates
    @Query("SELECT SUM(CAST(d.amount AS double)) FROM Depense d WHERE d.dateDepense BETWEEN :startDate AND :endDate AND d.codeStructure = :code")
    Double sumBetweenDates(@Param("startDate") Date startDate, @Param("endDate") Date endDate, @Param("code") String code);
    
     List<Depense> findByCodeStructureAndDateDepenseBetween(
            String codeStructure, 
            Date start, 
            Date end
    );

    @Query("SELECT d.createdBy as userName, SUM(CAST(d.amount AS double)) as totalExpensesAmount " +
           "FROM Depense d " +
           "WHERE d.codeStructure = :codeStructure " +
           "GROUP BY d.createdBy")
    List<Map<String, Object>> getExpensesGroupedByUser(@Param("codeStructure") String codeStructure);

}
