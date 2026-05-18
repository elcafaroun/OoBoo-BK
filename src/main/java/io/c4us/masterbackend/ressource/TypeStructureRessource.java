package io.c4us.masterbackend.ressource;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody; // <-- C'EST CET IMPORT QU'IL FAUT
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.c4us.masterbackend.domain.TypeStructure;
import io.c4us.masterbackend.service.TypeStructureService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/typestructure")
@RequiredArgsConstructor
public class TypeStructureRessource {
    
    private final TypeStructureService typeStructureService;

    @PostMapping
    public ResponseEntity<TypeStructure> createTypeStructure(@RequestBody TypeStructure typeStructure) {
        try {
            // L'objet arrive ici rempli grâce au bon import @RequestBody de Spring
            TypeStructure createdType = typeStructureService.createTypeStructure(typeStructure);
            
            URI location = URI.create("/typestructure/" + createdType.getId());
            return ResponseEntity.created(location).body(createdType);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping
    public List<TypeStructure> getAllTypeStructure() {
        return typeStructureService.getAllTypeStructure();
    }
}