package io.c4us.masterbackend.ressource;

import static io.c4us.masterbackend.constant.Constant.PHOTO_DIRECTORY;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.MediaType;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import io.c4us.masterbackend.DTOs.CategoryStatusUpdateDTO;
import io.c4us.masterbackend.domain.Category;
import io.c4us.masterbackend.service.CategoryService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/category")
@RequiredArgsConstructor
public class CategoryRessource {

    private final CategoryService categoryService;

    /**
     * SYNC : Récupère les catégories modifiées pour une structure donnée.
     * Exemple : /category/sync/STR001?lastSync=2026-03-13T10:00:00
     */
    @GetMapping("/sync/{codeStructure}")
    public ResponseEntity<List<Category>> syncCategories(
            @PathVariable String codeStructure,
            @RequestParam(value = "lastSync", required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime lastSync) {
        
        return ResponseEntity.ok(categoryService.getCategoriesUpdates(codeStructure, lastSync));
    }

@PostMapping
    public ResponseEntity<?> createCategory(@RequestBody Category category) {
        try {
            Category created = categoryService.createCategory(category);
            return ResponseEntity.created(URI.create("/category/" + created.getId()))
                    .body(created);
        } catch (Exception e) {
            e.printStackTrace(); // Log l'erreur complète dans les logs du serveur
            // Renvoie le message d'erreur exact (ex: "Limite atteinte...") au lieu d'un 400 vide
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PatchMapping("/updateStatus/{id}")
    public ResponseEntity<Category> updateStatus(
            @PathVariable String id,
            @RequestBody CategoryStatusUpdateDTO dto) {
        Category updated = categoryService.updateActiveStatus(id, dto.isActive());
        return ResponseEntity.ok(updated);
    }

/*  @GetMapping
public List<Category> getAllCategories() {
    return categoryService.getAllCategories(0, 100).getContent(); 
} */

@GetMapping
public ResponseEntity<org.springframework.data.domain.Page<Category>> getAllCategories(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size) {
    
    return ResponseEntity.ok(categoryService.getAllCategories(page, size));
}

    @GetMapping("/{id}")
    public ResponseEntity<Category> getCategory(@PathVariable(value = "id") String id) {
        return ResponseEntity.ok().body(categoryService.getCategory(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Category> delCategory(@PathVariable(value = "id") String id) {
        return ResponseEntity.ok().body(categoryService.delCategory(id));
    }

    @GetMapping("/structure/{codeStructure}")
    public ResponseEntity<List<Category>> getByStructure(@PathVariable String codeStructure) {
        return ResponseEntity.ok(categoryService.getCategoryByStructure(codeStructure));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Category> updateCategory(
            @PathVariable String id,
            @RequestBody Category categoryDetails) {
        // On s'assure que l'ID est bien celui de l'URL
        categoryDetails.setId(id);
        Category updatedCategory = categoryService.updateCategory(categoryDetails);
        return ResponseEntity.ok(updatedCategory);
    }

    @PutMapping("/photo")
    public ResponseEntity<String> uploadPhoto(@RequestParam("id") String id, @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok().body(categoryService.uploadPhoto(id, file));
    }

    @GetMapping("/exists")
    public ResponseEntity<Boolean> checkCategoryExists(
            @RequestParam String name,
            @RequestParam String codeStructure) {
        
        boolean exists = categoryService.isNameDuplicate(name, codeStructure);
        return ResponseEntity.ok(exists);
    }
@GetMapping(path = "/image/{filename}")
public ResponseEntity<byte[]> getPhoto(@PathVariable("filename") String filename) throws IOException {
    
    // 1. Protection contre le "null"
    if (filename == null || filename.equals("null") || filename.isEmpty()) {
        return ResponseEntity.notFound().build();
    }

    Path path = Paths.get(PHOTO_DIRECTORY + filename);
    
    if (!Files.exists(path)) {
        return ResponseEntity.notFound().build();
    }

    byte[] image = Files.readAllBytes(path);

    // 2. Détection intelligente du type MIME
    // Si le fichier se termine par .png, on utilise image/png, sinon image/jpeg par défaut
    MediaType mediaType = filename.toLowerCase().endsWith(".png") 
                          ? MediaType.IMAGE_PNG 
                          : MediaType.IMAGE_JPEG;

    return ResponseEntity.ok()
            .contentType(mediaType) // Ici on utilise un objet MediaType valide, pas de wildcard
            .body(image);
}

@GetMapping("/count/structure/{codeStructure}")
    public ResponseEntity<Long> countCategoriesByStructure(@PathVariable String codeStructure) {
        long count = categoryService.countCategoriesByStructure(codeStructure);
        return ResponseEntity.ok(count);
    }

}