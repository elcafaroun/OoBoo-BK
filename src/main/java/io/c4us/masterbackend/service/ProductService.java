package io.c4us.masterbackend.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import static io.c4us.masterbackend.constant.Constant.PHOTO_DIRECTORY;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import org.springframework.stereotype.Service;

import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import io.c4us.masterbackend.domain.Product;
import io.c4us.masterbackend.repo.ProductRepo;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@Transactional(rollbackOn = Exception.class)
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepo productRepo;

    // --- LOGIQUE DE SYNCHRONISATION (OFFLINE) ---

    /**
     * Récupère les produits modifiés/créés/supprimés pour la synchro Flutter
     */
    public List<Product> getProductsUpdates(String codeStructure, LocalDateTime lastSync) {
        if (lastSync == null) {
            return productRepo.findByCodeStructureAndDeletedFalse(codeStructure);
        }
        return productRepo.findByCodeStructureAndLastUpdatedAfter(codeStructure, lastSync);
    }

    // --- CRUD ADAPTÉ ---

    public Product createProduct(Product product) {
        product.setLastUpdated(LocalDateTime.now());
        System.out.println("Code QR reçu : " + product.getProductQrCode()); // Regardez ce qui s'affiche ici
        product.setDeleted(false);
        return productRepo.save(product);
    }

    public Product updateProduct(String id, Product updatedProduct) {
        Product existing = getProduct(id);
        
        existing.setProductName(updatedProduct.getProductName());
        existing.setProductPrice(updatedProduct.getProductPrice());
        existing.setProductQte(updatedProduct.getProductQte());
        existing.setCategoryId(updatedProduct.getCategoryId());
        existing.setPrixAchat(updatedProduct.getPrixAchat());
        existing.setStockAlert(updatedProduct.getStockAlert());
        existing.setFavoris(updatedProduct.isFavoris());

        // Crucial : Marquer la modification pour la synchro
        existing.setLastUpdated(LocalDateTime.now());
        return productRepo.save(existing);
    }

    public Product delProduct(String id) {
        Product product = getProduct(id);
        // Soft Delete pour que le mobile sache qu'il doit le supprimer localement
        product.setDeleted(true);
        product.setLastUpdated(LocalDateTime.now());
        return productRepo.save(product);
    }

    // --- GESTION DU STOCK ---

    @Transactional
    public void updateStock(String productId, double deductQuantity) {
        Product product = getProduct(productId);
        
        // Mise à jour de la quantité
        product.setProductQte(product.getProductQte() - deductQuantity);
        
        // On marque la mise à jour pour que les autres terminaux mobiles 
        // récupèrent le nouveau stock lors de leur prochaine synchro
        product.setLastUpdated(LocalDateTime.now());
        
        productRepo.save(product);
    }

    // --- RECHERCHE ---

    public Product getProduct(String id) {
        return productRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Produit introuvable : " + id));
    }

    public List<Product> getAllProductByCat(String idCat) {
        return productRepo.findByCategoryIdAndDeletedFalse(idCat);
    }

    public List<Product> getAllProductByStructure(String codeStructure) {
        return productRepo.findByCodeStructureAndDeletedFalse(codeStructure);
    }

    public List<Product> getProductsInAlert(String codeStructure) {
        return productRepo.findByLowStockAndStructure(codeStructure);
    }

    public Page<Product> getAllProduct(int page, int size) {
        return productRepo.findAll(PageRequest.of(page, size, Sort.by("createdDate")));
    }

    // --- GESTION DES PHOTOS ---

    public String uploadPhoto(String id, MultipartFile file) {
        log.info("Upload photo for product : {}", id);
        Product product = getProduct(id);
        String photoUrl = photoFunction.apply(id, file);
        product.setProductPhotoUrl(photoUrl);
        product.setLastUpdated(LocalDateTime.now());
        productRepo.save(product);
        return photoUrl;
    }

    private final Function<String, String> fileExtension = filename -> Optional.ofNullable(filename)
            .filter(name -> name.contains("."))
            .map(name -> "." + name.substring(filename.lastIndexOf(".") + 1)).orElse(".png");

    private final BiFunction<String, MultipartFile, String> photoFunction = (id, image) -> {
        String filename = id + fileExtension.apply(image.getOriginalFilename());
        try {
            Path fileStorageLocation = Paths.get(PHOTO_DIRECTORY).toAbsolutePath().normalize();
            if (!Files.exists(fileStorageLocation)) {
                Files.createDirectories(fileStorageLocation);
            }
            Files.copy(image.getInputStream(), fileStorageLocation.resolve(filename), REPLACE_EXISTING);
            return ServletUriComponentsBuilder.fromCurrentContextPath().path("/product/image/" + filename)
                    .toUriString();
        } catch (Exception exception) {
            throw new RuntimeException("Erreur lors de l'enregistrement de l'image", exception);
        }
    };


    public Product updateActiveStatus(String id, boolean newStatus) {
        Product product = getProduct(id);
        product.setActive(newStatus);
        product.setLastUpdated(LocalDateTime.now());
        return productRepo.save(product);
    }

    public boolean checkIfExists(String name, String categoryId, String codeStructure) {
        return productRepo.existsByProductNameIgnoreCaseAndCategoryIdAndCodeStructure(
            name, categoryId, codeStructure
        );
    }
}