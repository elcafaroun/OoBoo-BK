package io.c4us.masterbackend.service;

import java.util.List;
import org.springframework.stereotype.Service; // Import correct
import io.c4us.masterbackend.domain.VilleStructure;
import io.c4us.masterbackend.repo.VilleStructureRepo;
import lombok.RequiredArgsConstructor;

@Service // Indique à Spring que c'est un composant métier
@RequiredArgsConstructor
public class VilleStructureService {

    private final VilleStructureRepo villeStructureRepo;

    public VilleStructure createVilleStructure(VilleStructure villeStructure) {
        return villeStructureRepo.save(villeStructure);
    }

    public List<VilleStructure> getAllVilleStructure() {
        return villeStructureRepo.findAll();
    }
}