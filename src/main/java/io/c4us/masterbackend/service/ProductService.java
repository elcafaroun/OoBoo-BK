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
import io.c4us.masterbackend.domain.Structure;
import io.c4us.masterbackend.DTOs.ProductScanResponse;
import io.c4us.masterbackend.DTOs.StockEntryRequest;
import io.c4us.masterbackend.repo.ProductRepo;
import io.c4us.masterbackend.repo.StructureRepo;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@Transactional(rollbackOn = Exception.class)
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepo productRepo;
    private final StructureRepo structureRepo;

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

    // --- SCAN ET GESTION PAR QR CODE ---

    /**
     * Vérifie si un produit existe déjà via son QR Code au sein d'une structure donnée.
     */
    public io.c4us.masterbackend.DTOs.ProductScanResponse checkProductByQrCode(String qrCode, String codeStructure) {
        Optional<Product> productOpt = productRepo.findByProductQrCodeAndCodeStructureAndDeletedFalse(qrCode, codeStructure);

        if (productOpt.isPresent()) {
            return new ProductScanResponse(true, "Le produit existe déjà dans la base.", productOpt.get());
        } else {
            return new ProductScanResponse(false, "Produit non trouvé.", null);
        }
    }

    /**
     * Ajoute directement la quantité spécifiée au stock actuel du produit scanné.
     */
    @Transactional
    public Product addStockByQrCode(StockEntryRequest request) {
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new IllegalArgumentException("La quantité à ajouter doit être supérieure à 0.");
        }

        Product product = productRepo.findByProductQrCodeAndCodeStructureAndDeletedFalse(
                request.getProductQrCode(), request.getCodeStructure()
        ).orElseThrow(() -> new RuntimeException("Produit introuvable avec le code QR : " + request.getProductQrCode()));

        double currentStock = product.getProductQte() != null ? product.getProductQte() : 0.0;
        product.setProductQte(currentStock + request.getQuantity());
        
        // Signalement de la modification pour la synchronisation mobile
        product.setLastUpdated(LocalDateTime.now());

        log.info("Entrée en stock effectuée pour le produit ID: {}. Ancien stock: {}, Ajout: {}, Nouveau stock: {}", 
                product.getId(), currentStock, request.getQuantity(), product.getProductQte());

        return productRepo.save(product);
    }

    // --- CRUD ADAPTÉ ---

    public Product createProduct(Product product) {
        String codeStructure = product.getCodeStructure();
        String categoryId = product.getCategoryId();

        // Récupération de la structure
        Structure structure = structureRepo.findById(codeStructure)
                .orElseThrow(() -> new RuntimeException("Structure introuvable avec le code : " + codeStructure));

        // Récupération du quota de produits pour cette structure
        Integer maxAllowed = structure.getNombreProdParBusiness();

        // Vérification du quota si défini
        if (maxAllowed != null) {
            long currentCount = productRepo.countByCategoryIdAndCodeStructureAndDeletedFalse(categoryId, codeStructure);
            if (currentCount >= maxAllowed) {
                log.warn("Quota de produits atteint pour la structure {} dans la catégorie {}. Nombre actuel : {}, Limite autorisée : {}",
                        codeStructure, categoryId, currentCount, maxAllowed);
                throw new IllegalStateException(
                        "Limite atteinte : Votre abonnement vous autorise un maximum de " + maxAllowed
                                + " produit(s) par catégorie.");
            }
        }

        product.setLastUpdated(LocalDateTime.now());
        log.info("Code QR reçu lors de la création : {}", product.getProductQrCode());
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
        existing.setProductQrCode(updatedProduct.getProductQrCode());

        // Signalement de la modification pour la synchro
        existing.setLastUpdated(LocalDateTime.now());
        return productRepo.save(existing);
    }

    public Product delProduct(String id) {
        Product product = getProduct(id);
        // Soft Delete pour synchro mobile
        product.setDeleted(true);
        product.setLastUpdated(LocalDateTime.now());
        return productRepo.save(product);
    }

    // --- GESTION DU STOCK ---

    @Transactional
    public void updateStock(String productId, double deductQuantity) {
        Product product = getProduct(productId);

        product.setProductQte(product.getProductQte() - deductQuantity);
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
        log.info("Upload photo pour le produit : {}", id);
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
                name, categoryId, codeStructure);
    }

    public long countProductsByStructure(String codeStructure) {
        return productRepo.countByCodeStructureAndDeletedFalse(codeStructure);
    }
}