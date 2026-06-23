package io.c4us.masterbackend.ressource;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.query.Page;
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
    public ResponseEntity<Category> createCategory(@RequestBody Category category) {
        try {
            Category created = categoryService.createCategory(category);
            return ResponseEntity.created(URI.create("/category/" + created.getId()))
                    .body(created);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
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
}