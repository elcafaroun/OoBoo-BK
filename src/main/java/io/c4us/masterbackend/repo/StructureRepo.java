package io.c4us.masterbackend.repo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import io.c4us.masterbackend.domain.Structure;

public interface StructureRepo extends JpaRepository<Structure, String> {

    // Pour récupérer les structures d'un utilisateur spécifique
    List<Structure> findByCreatedUserId(String userId);
    boolean existsByNomStructureIgnoreCase(String nomStructure);

    /**
     * SYNCHRONISATION : Récupère uniquement les structures modifiées
     * ou créées après une date précise pour un utilisateur.
     * 
     */
    Optional<Structure> findByCodeStructure(String codeStructure);

    List<Structure> findByCreatedUserIdAndLastUpdatedAfter(String userId, LocalDateTime lastSync);

    /**
     * Optionnel : Récupérer uniquement les structures non supprimées
     */
    List<Structure> findByCreatedUserIdAndDeletedFalse(String userId);
}