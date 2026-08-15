package io.c4us.masterbackend.repo;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.c4us.masterbackend.DTOs.UserSalesDto;
import io.c4us.masterbackend.domain.Command;

public interface CommandRepo extends JpaRepository<Command, String> { // 👈 Changé Long en String

        List<Command> findByCodeStructureOrderByOrderDateDesc(String codeStructure);

        List<Command> findByCodeStructure(String codeStructure);

        /**
         * SYNCHRONISATION (OFFLINE) :
         * Récupère les commandes créées/modifiées depuis la dernière synchro.
         */
        List<Command> findByCodeStructureAndLastUpdatedAfter(String codeStructure, LocalDateTime lastSync);

        // ✅ Somme Globale (Encaissé + Crédit) sur une période
        @Query("SELECT SUM(c.totalAmount + c.totalCredit) FROM Command c " +
                        "WHERE c.orderDate BETWEEN :startDate AND :endDate " +
                        "AND c.codeStructure = :code " +
                        "AND c.status <> 'CANCELLED'")
        Double sumGlobalBetweenDates(@Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate,
                        @Param("code") String code);

        // ✅ Somme par date (Total Encaissé + Total Crédit)
        @Query("SELECT SUM(c.totalAmount + c.totalCredit) FROM Command c " +
                        "WHERE c.codeStructure = :code " +
                        "AND c.orderDate >= :startOfDay AND c.orderDate <= :endOfDay " +
                        "AND c.status <> 'CANCELLED'")
        Double sumCommandesByDate(@Param("startOfDay") LocalDateTime startOfDay,
                        @Param("endOfDay") LocalDateTime endOfDay,
                        @Param("code") String code);

        // ✅ Somme uniquement des Crédits (Dettes en cours)
        @Query("SELECT SUM(c.totalCredit) FROM Command c " +
                        "WHERE c.codeStructure = :code " +
                        "AND c.orderDate >= :startOfDay AND c.orderDate <= :endOfDay " +
                        "AND c.status <> 'CANCELLED'")
        Double sumTotalCreditByDate(@Param("startOfDay") LocalDateTime startOfDay,
                        @Param("endOfDay") LocalDateTime endOfDay,
                        @Param("code") String code);

        // ✅ Répartition par mode de paiement
        @Query("SELECT c.paymentMethod, SUM(c.totalAmount + c.totalCredit) FROM Command c " +
                        "WHERE c.codeStructure = :code " +
                        "AND c.orderDate >= :start AND c.orderDate <= :end " +
                        "AND c.status <> 'CANCELLED' " +
                        "GROUP BY c.paymentMethod")
        List<Object[]> sumByPaymentMethod(
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end,
                        @Param("code") String code);

        @Query("SELECT new io.c4us.masterbackend.DTOs.UserSalesDto(" +
                        "c.userId, " +
                        "COALESCE(c.userName, 'Agent inconnu'), " +
                        "SUM(c.totalAmount + c.totalCredit)) " +
                        "FROM Command c " +
                        "WHERE c.codeStructure = :codeStructure " +
                        "AND c.status <> 'CANCELLED' " +
                        "GROUP BY c.userId, c.userName")
        List<UserSalesDto> getSalesGroupedByUser(@Param("codeStructure") String codeStructure);

@Query(value = "SELECT TO_CHAR(c.order_date, 'DD') AS jour, SUM(c.total_amount) AS total " +
                   "FROM commands c " +
                   "WHERE c.code_structure = :codeStructure " +
                   "AND TO_CHAR(c.order_date, 'YYYY-MM') = :yearMonth " +
                   "AND c.status = 'COMPLETED' " +
                   "AND c.deleted = false " +
                   "GROUP BY TO_CHAR(c.order_date, 'DD') " +
                   "ORDER BY jour ASC", 
           nativeQuery = true)
    List<Object[]> findDailySalesForMonth(
        @Param("codeStructure") String codeStructure, 
        @Param("yearMonth") String yearMonth
    );
}