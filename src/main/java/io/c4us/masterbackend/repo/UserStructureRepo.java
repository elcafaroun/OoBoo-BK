package io.c4us.masterbackend.repo;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import io.c4us.masterbackend.domain.AppUser;
import io.c4us.masterbackend.domain.UserStructure;

@Repository
public interface UserStructureRepo extends JpaRepository<UserStructure, String> {

    // ✅ 1. Récupérer toutes les liaisons d'un utilisateur
    List<UserStructure> findByUserId(String userId);

    // ✅ 2. Trouver la liaison unique par utilisateur et ID de la structure
    Optional<UserStructure> findByUserIdAndStructure_IdStructure(String userId, String idStructure);

    // ✅ 3. Récupérer les liaisons actives d'une structure via son code
    List<UserStructure> findByStructure_CodeStructureAndDeletedFalse(String codeStructure);

    // ✅ 4. Compter les liaisons pour générer le CodeUser séquentiel
    long countByStructure_CodeStructure(String codeStructure);

    @Query("SELECT us FROM UserStructure us JOIN FETCH us.structure WHERE us.user.id = :userId AND us.deleted = false")
    List<UserStructure> findByUserIdWithStructures(@Param("userId") String userId);

    List<UserStructure> findByStructure_IdStructureAndDeletedFalse(String structureId);

    @Query("SELECT us.user FROM UserStructure us WHERE us.structure.idStructure = :structId AND us.deleted = false")
    List<AppUser> findUsersByStructureId(@Param("structId") String structId);

}