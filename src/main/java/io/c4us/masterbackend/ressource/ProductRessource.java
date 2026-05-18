package io.c4us.masterbackend.ressource;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import static io.c4us.masterbackend.constant.Constant.PHOTO_DIRECTORY;

import io.c4us.masterbackend.DTOs.CategoryStatusUpdateDTO;
import io.c4us.masterbackend.domain.Product;
import io.c4us.masterbackend.service.ProductService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/product")
@RequiredArgsConstructor
//@CrossOrigin(origins = "*") // À adapter selon vos besoins
public class ProductRessource {

    private final ProductService productService;

    /**
     * ENDPOINT DE SYNCHRONISATION (Crucial pour Flutter)
     * Récupère les produits modifiés/supprimés pour une structure donnée.
     */
    @GetMapping("/sync/{codeStructure}")
    public ResponseEntity<List<Product>> syncProducts(
            @PathVariable String codeStructure,
            @RequestParam(value = "lastSync", required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime lastSync) {
        
        return ResponseEntity.ok(productService.getProductsUpdates(codeStructure, lastSync));
    }

 @PostMapping
public ResponseEntity<?> createProduct(@RequestBody Product product) {
    try {
        Product created = productService.createProduct(product);
        return ResponseEntity.created(URI.create("/product/" + created.getId())).body(created);
    } catch (Exception e) {
        // AJOUTEZ CETTE LIGNE : C'est crucial pour voir l'erreur dans la console
        e.printStackTrace(); 
        
        // Optionnel : renvoyer le message d'erreur au client Flutter
        return ResponseEntity.badRequest().body("Erreur : " + e.getMessage());
    }
}

    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(@PathVariable String id, @RequestBody Product product) {
        try {
            return ResponseEntity.ok(productService.updateProduct(id, product));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping
    public ResponseEntity<Page<Product>> getProducts(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        return ResponseEntity.ok().body(productService.getAllProduct(page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProduct(@PathVariable(value = "id") String id) {
        return ResponseEntity.ok().body(productService.getProduct(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Product> delProduct(@PathVariable(value = "id") String id) {
        // Le service effectue maintenant un Soft Delete
        return ResponseEntity.ok().body(productService.delProduct(id));
    }

    @PutMapping("/photo")
    public ResponseEntity<String> uploadPhoto(@RequestParam("id") String id, @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok().body(productService.uploadPhoto(id, file));
    }

    @GetMapping(path = "/image/{filename}", produces = { MediaType.IMAGE_PNG_VALUE, MediaType.IMAGE_JPEG_VALUE })
    public byte[] getPhoto(@PathVariable("filename") String filename) throws IOException {
        return Files.readAllBytes(Paths.get(PHOTO_DIRECTORY + filename));
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<Product>> getProductsByCategoryId(@PathVariable String categoryId) {
        return ResponseEntity.ok(productService.getAllProductByCat(categoryId));
    }

    @PostMapping("/update-stock")
    public ResponseEntity<?> updateStock(@RequestBody Map<String, Object> request) {
        try {
            String productId = (String) request.get("productId");
            // Utilisation de Number pour supporter Integer ou Double venant du JSON
            double deductQuantity = Double.parseDouble(request.get("deductQuantity").toString());

            productService.updateStock(productId, deductQuantity);

            return ResponseEntity.ok(Map.of("message", "Stock mis à jour avec succès"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/status/low-stock/{codeStructure}")
    public ResponseEntity<List<Product>> getAlerts(@PathVariable String codeStructure) {
        return ResponseEntity.ok(productService.getProductsInAlert(codeStructure));
    }

    @GetMapping("/structure/{codeStructure}")
    public ResponseEntity<List<Product>> getProductsByStructure(@PathVariable String codeStructure) {
        return ResponseEntity.ok(productService.getAllProductByStructure(codeStructure));
    }

    @PatchMapping("/updateStatus/{id}")
    public ResponseEntity<Product> updateStatus(
            @PathVariable String id,
            @RequestBody CategoryStatusUpdateDTO dto) {
        Product updated = productService.updateActiveStatus(id, dto.isActive());
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/check-duplicate")
    public ResponseEntity<Boolean> checkDuplicate(
            @RequestParam String name,
            @RequestParam String categoryId,
            @RequestParam String codeStructure) {
        
        boolean exists = productService.checkIfExists(name, categoryId, codeStructure);
        return ResponseEntity.ok(exists);
    }

    
}