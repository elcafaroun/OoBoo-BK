package io.c4us.masterbackend.repo;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import io.c4us.masterbackend.domain.Structure;
import io.c4us.masterbackend.domain.UserStructure;

@Repository
public interface StructureRepo extends JpaRepository<Structure, String> {

    List<Structure> findByCodeStructure(String codeStructure);

    boolean existsByNomStructureIgnoreCase(String nomStructure);

  
}