package io.c4us.masterbackend.ressource;

import static io.c4us.masterbackend.constant.Constant.PHOTO_DIRECTORY;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import io.c4us.masterbackend.domain.Structure;
import io.c4us.masterbackend.service.StructureService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/structure")
@RequiredArgsConstructor
// @CrossOrigin(origins = "http://localhost:5173")
public class StructureRessource {

    private final StructureService structureService;

    @PostMapping
    public ResponseEntity<Structure> createStructure(@RequestBody Structure struct) {
        try {
            // Utilisation de l'ID de l'objet créé pour l'URI
            Structure created = structureService.createStructure(struct);
            return ResponseEntity.created(URI.create("/structure/" + created.getIdStructure()))
                    .body(created);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * ENDPOINT DE SYNCHRONISATION (Crucial pour le mode Offline)
     * Flutter appellera : /structure/sync/user123?lastSync=2026-03-13T10:00:00
     */
    @GetMapping("/sync/{userId}")
    public ResponseEntity<List<Structure>> syncStructures(
            @PathVariable String userId,
            @RequestParam(value = "lastSync", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime lastSync) {

        return ResponseEntity.ok(structureService.getUpdatesForSync(userId, lastSync));
    }

    @GetMapping(path = "/image/{filename}", produces = {
            MediaType.IMAGE_PNG_VALUE,
            MediaType.IMAGE_JPEG_VALUE
    })
    public byte[] getPhoto(@PathVariable("filename") String filename) throws IOException {
        return Files.readAllBytes(Paths.get(PHOTO_DIRECTORY + filename));
    }

    @PutMapping("/photo")
    public ResponseEntity<String> uploadPhoto(@RequestParam("id") String id, @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok().body(structureService.uploadPhoto(id, file));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Structure>> getByUser(@PathVariable String userId) {
        // Retourne uniquement les structures actives pour l'affichage standard
        return ResponseEntity.ok(structureService.getStructuresByUser(userId));
    }

    @GetMapping
    public ResponseEntity<List<Structure>> getAllStructure() {
        return ResponseEntity.ok(structureService.getAllStructure());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Structure> delStructure(@PathVariable(value = "id") String id) {
        // Le service effectue maintenant un Soft Delete
        return ResponseEntity.ok().body(structureService.delStructure(id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Structure> getStructureById(@PathVariable String id) {
        return ResponseEntity.ok(structureService.getStructure(id));
    }

    /**
     * Permet de renouveler ou changer le plan d'une structure.
     * Appelée par l'application Flutter lors du renouvellement.
     */
    @PutMapping("/update-plan")
    public ResponseEntity<Structure> updateStructurePlan(
            @RequestParam String id,
            @RequestParam String plan) {
        try {
            return ResponseEntity.ok(structureService.updateStructurePlan(id, plan));
        } catch (Exception e) {
            // Retourne 400 si le plan n'existe pas ou si l'id est invalide
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/exists")
    public ResponseEntity<Boolean> checkName(@RequestParam String nom) {
        boolean exists = structureService.checkIfNameExists(nom);
        return ResponseEntity.ok(exists);
    }

}