package io.c4us.masterbackend.ressource;

import static io.c4us.masterbackend.constant.Constant.PHOTO_DIRECTORY;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import io.c4us.masterbackend.domain.Structure;
import io.c4us.masterbackend.service.StructureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/structure")
@RequiredArgsConstructor
@Slf4j
public class StructureRessource {

    private final StructureService structureService;

    /**
     * 🔹 Création d'une structure (Sécurisée contre les paramètres manquants)
     */
    @PostMapping
    public ResponseEntity<?> createStructure(
            @RequestBody Structure struct,
            @RequestParam(value = "userId", required = false) String userId) { 
        
        // Validation manuelle propre pour éviter le crash brut DefaultHandlerExceptionResolver
        if (userId == null || userId.trim().isEmpty()) {
            log.error("⚠️ Tentative de création de structure refusée : 'userId' manquant dans les query parameters.");
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Erreur : Le paramètre 'userId' est obligatoire pour lier cette structure à un utilisateur.");
        }

        try {
            Structure created = structureService.createStructure(struct, userId.trim());
            return ResponseEntity.created(URI.create("/structure/" + created.getIdStructure()))
                    .body(created);
        } catch (Exception e) {
            log.error("❌ Erreur lors de la création de la structure : {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

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
        return ResponseEntity.ok(structureService.getStructuresByUser(userId));
    }

    @GetMapping("/structure/{codeStructure}")
    public ResponseEntity<List<Structure>> getByCodeStructure(@PathVariable String codeStructure) {
        return ResponseEntity.ok(structureService.getStructuresByCodeStructure(codeStructure));
    }

    @GetMapping
    public ResponseEntity<List<Structure>> getAllStructure() {
        return ResponseEntity.ok(structureService.getAllStructure());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Structure> delStructure(@PathVariable(value = "id") String id) {
        return ResponseEntity.ok().body(structureService.delStructure(id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Structure> getStructureById(@PathVariable String id) {
        return ResponseEntity.ok(structureService.getStructure(id));
    }

    @PutMapping("/update-plan")
    public ResponseEntity<Structure> updateStructurePlan(
            @RequestParam String id,
            @RequestParam String plan) {
        try {
            return ResponseEntity.ok(structureService.updateStructurePlan(id, plan));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/exists")
    public ResponseEntity<Boolean> checkName(@RequestParam String nom) {
        boolean exists = structureService.checkIfNameExists(nom);
        return ResponseEntity.ok(exists);
    }
}