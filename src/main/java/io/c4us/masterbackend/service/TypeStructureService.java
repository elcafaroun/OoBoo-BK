package io.c4us.masterbackend.service;



import java.util.List;

import org.springframework.stereotype.Service;

import io.c4us.masterbackend.domain.Structure;
import io.c4us.masterbackend.domain.TypeStructure;
import io.c4us.masterbackend.repo.TypeStructureRepo;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Service
@Slf4j
@Transactional(rollbackOn = Exception.class)
@RequiredArgsConstructor // R
public class TypeStructureService {

  private final TypeStructureRepo typeStructureRepo;
    public TypeStructure createTypeStructure(TypeStructure typeStructure) {
        
        return typeStructureRepo.save(typeStructure);
    }

      public List<TypeStructure> getAllTypeStructure() {
        return typeStructureRepo.findAll();
    }
    
}
