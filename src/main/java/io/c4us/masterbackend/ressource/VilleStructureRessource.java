package io.c4us.masterbackend.ressource;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody; // <-- L'IMPORT CORRECT EST CELUI-CI
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.c4us.masterbackend.domain.VilleStructure;
import io.c4us.masterbackend.service.VilleStructureService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/villestructure")
@RequiredArgsConstructor
public class VilleStructureRessource {

    private final VilleStructureService villeStructureService;

    @PostMapping
    public ResponseEntity<VilleStructure> createVilleStructure(@RequestBody VilleStructure villeStructure) {
        try {
            // Maintenant, villeStructure.getNomVille() ne sera plus null !
            VilleStructure createdType = villeStructureService.createVilleStructure(villeStructure);
            
            URI location = URI.create("/villestructure/" + createdType.getId());
            return ResponseEntity.created(location).body(createdType);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping
    public List<VilleStructure> getAllVilleStructure() {
        return villeStructureService.getAllVilleStructure();
    }
}