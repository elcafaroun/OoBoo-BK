package io.c4us.masterbackend.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import io.c4us.masterbackend.domain.Structure;

@Repository
public interface StructureRepo extends JpaRepository<Structure, String> {

    List<Structure> findByCodeStructure(String codeStructure);

    boolean existsByNomStructureIgnoreCase(String nomStructure);

    Optional<Structure> findFirstByCodeStructure(String codeStructure);

    Optional<Structure> findByIdStructureAndDeletedFalse(String idStructure);

  
}