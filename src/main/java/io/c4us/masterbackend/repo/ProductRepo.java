package io.c4us.masterbackend.repo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.c4us.masterbackend.domain.Product;

public interface ProductRepo extends JpaRepository<Product, String> {

    // --- REQUÊTES STANDARD (Excluent les produits supprimés) ---

    @Override
    Optional<Product> findById(String id);

    // Utile pour éviter les doublons lors de l'ajout
    Optional<Product> findByProductNameAndCodeStructureAndDeletedFalse(String productName, String codeStructure);

    boolean existsByProductNameIgnoreCaseAndCategoryIdAndCodeStructure(
            String productName,
            String categoryId,
            String codeStructure);

    List<Product> findByCodeStructureAndDeletedFalse(String codeStructure);

    List<Product> findByCategoryIdAndDeletedFalse(String idCat);

    long countByCategoryIdAndDeletedFalse(String categoryId);

    @Query("SELECT p FROM Product p WHERE p.productQte <= p.stockAlert AND p.deleted = false AND p.codeStructure = :codeStructure")
    List<Product> findByLowStockAndStructure(@Param("codeStructure") String codeStructure);
    // --- LOGIQUE DE SYNCHRONISATION OFFLINE ---

    /**
     * Delta Sync : Récupère TOUS les changements (ajouts, modifs, suppressions)
     * d'une structure depuis la dernière connexion de l'utilisateur.
     */
    List<Product> findByCodeStructureAndLastUpdatedAfter(String codeStructure, LocalDateTime lastSync);

    /**
     * Variante pour synchroniser par catégorie
     */
    List<Product> findByCategoryIdAndLastUpdatedAfter(String categoryId, LocalDateTime lastSync);

    // --- REQUÊTES COMPLEXES ---

    @Query("SELECT p FROM Product p WHERE p.productName = :name " +
            "AND p.deleted = false " +
            "AND p.categoryId IN (SELECT c.id FROM Category c WHERE c.codeStructure = :code)")
    Optional<Product> findProductByStructure(
            @Param("name") String name,
            @Param("code") String code);

    long countByCategoryId(String categoryId);

    @Query("SELECT COUNT(p) FROM Product p WHERE p.codeStructure = :codeStructure AND p.deleted = false AND p.isActive = true")
    long countActiveProductsByStructure(@Param("codeStructure") String codeStructure);

    long countByCodeStructureAndDeletedFalse(String codeStructure);

    long countByCategoryIdAndCodeStructureAndDeletedFalse(String categoryId, String codeStructure);

    Optional<Product> findByProductQrCodeAndCodeStructureAndDeletedFalse(String productQrCode, String codeStructure);
}