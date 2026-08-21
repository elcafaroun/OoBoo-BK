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

   
    List<UserStructure> findByUser_Id(String userId);

    
    Optional<UserStructure> findByUser_IdAndStructure_IdStructure(String userId, String idStructure);

    // ✅ Les autres méthodes sont correctes car elles utilisent bien la syntaxe 'Objet_Champ'
    List<UserStructure> findByStructure_CodeStructureAndDeletedFalse(String codeStructure);

    long countByStructure_CodeStructure(String codeStructure);

    long countByStructure_CodeStructureAndDeletedFalse(String codeStructure);

    // Cette requête est excellente, elle évite le problème N+1
    @Query("SELECT us FROM UserStructure us JOIN FETCH us.structure WHERE us.user.id = :userId AND us.deleted = false")
    List<UserStructure> findByUserIdWithStructures(@Param("userId") String userId);

    List<UserStructure> findByStructure_IdStructureAndDeletedFalse(String structureId);

    @Query("SELECT us.user FROM UserStructure us WHERE us.structure.idStructure = :structId AND us.deleted = false")
    List<AppUser> findUsersByStructureId(@Param("structId") String structId);
}