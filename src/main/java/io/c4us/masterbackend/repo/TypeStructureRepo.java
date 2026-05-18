package io.c4us.masterbackend.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import io.c4us.masterbackend.domain.TypeStructure;

public interface TypeStructureRepo extends JpaRepository<TypeStructure, String>{
    
}
