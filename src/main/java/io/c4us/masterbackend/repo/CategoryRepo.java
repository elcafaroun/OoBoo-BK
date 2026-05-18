package io.c4us.masterbackend.repo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import io.c4us.masterbackend.domain.Category;


public interface CategoryRepo extends JpaRepository<io.c4us.masterbackend.domain.Category, String> {
     Optional<io.c4us.masterbackend.domain.Category> findById(String id);

     List<Category> findByCodeStructure(String codestructure);

     boolean existsByNameCatIgnoreCaseAndCodeStructure(String nameCat, String codeStructure);

     // Pour l'affichage classique par structure
    List<Category> findByCodeStructureAndDeletedFalse(String codeStructure);

    // Pour la synchro offline par structure
    List<Category> findByCodeStructureAndLastUpdatedAfter(String codeStructure, LocalDateTime lastSync);

    // Optionnel : Synchro globale pour plusieurs structures (si l'utilisateur en gère plusieurs)
    List<Category> findByCodeStructureInAndLastUpdatedAfter(List<String> codeStructures, LocalDateTime lastSync);


}
