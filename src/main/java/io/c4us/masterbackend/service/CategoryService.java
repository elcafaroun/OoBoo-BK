package io.c4us.masterbackend.service;

import static io.c4us.masterbackend.constant.Constant.PHOTO_DIRECTORY;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

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
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import io.c4us.masterbackend.domain.Category;
import io.c4us.masterbackend.repo.CategoryRepo;
import io.c4us.masterbackend.repo.ProductRepo;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@Transactional(rollbackOn = Exception.class)
@RequiredArgsConstructor // Remplace @Autowired pour une meilleure injection
public class CategoryService {

    private final CategoryRepo categoryRepo;
    private final ProductRepo productRepo;

    // --- LOGIQUE DE SYNCHRONISATION (OFFLINE) ---

    /**
     * Récupère les catégories modifiées pour une structure précise
     */
    public List<Category> getCategoriesUpdates(String codeStructure, LocalDateTime lastSync) {
        if (lastSync == null) {
            return categoryRepo.findByCodeStructure(codeStructure);
        }
        return categoryRepo.findByCodeStructureAndLastUpdatedAfter(codeStructure, lastSync);
    }

    // --- CRUD ADAPTÉ ---

    public Category createCategory(Category category) {
        category.setLastUpdated(LocalDateTime.now());
        category.setDeleted(false);
        return categoryRepo.save(category);
    }

    public Category updateCategory(Category newcat) {
        Category category = getCategory(newcat.getId());
        category.setNameCat(newcat.getNameCat());
        category.setDescription(newcat.getDescription());
        category.setCategoryId(newcat.getCategoryId());

        // Crucial : marquer la modification pour Flutter
        category.setLastUpdated(LocalDateTime.now());
        return categoryRepo.save(category);
    }

    public Category delCategory(String id) {
        Category category = getCategory(id);
        // Soft Delete : on ne supprime pas la ligne, on la marque
        category.setDeleted(true);
        category.setLastUpdated(LocalDateTime.now());
        return categoryRepo.save(category);
    }

    public Category updateActiveStatus(String id, boolean newStatus) {
        Category category = getCategory(id);
        category.setActive(newStatus);
        category.setLastUpdated(LocalDateTime.now());
        return categoryRepo.save(category);
    }

    // --- RECHERCHE ET AFFICHAGE ---

    public Category getCategory(String id) {
        return categoryRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Category Not found"));
    }

    public List<Category> getCategoryByStructure(String codeStruct) {
        // On ne retourne que les catégories non supprimées pour l'UI
        List<Category> categories = categoryRepo.findByCodeStructureAndDeletedFalse(codeStruct);

        // Calcul du nombre de produits
        categories.forEach(cat -> {
            cat.setProductCount(productRepo.countByCategoryId(cat.getId()));
        });
        return categories;
    }

    public Page<Category> getAllCategories(int page, int size) {
        return categoryRepo.findAll(PageRequest.of(page, size, Sort.by("createdDate")));
    }

    // --- GESTION DES PHOTOS ---

    public String uploadPhoto(String id, MultipartFile file) {
        log.info("Upload photo for category : {}", id);
        Category category = getCategory(id);
        String photoUrl = photoFunction.apply(id, file);
        category.setCategoryPhotoUrl(photoUrl);
        category.setLastUpdated(LocalDateTime.now()); // Mark for sync
        categoryRepo.save(category);
        return photoUrl;
    }

    public boolean isNameDuplicate(String name, String codeStructure) {
        return categoryRepo.existsByNameCatIgnoreCaseAndCodeStructure(name, codeStructure);
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
            return ServletUriComponentsBuilder.fromCurrentContextPath().path("/category/image/" + filename)
                    .toUriString();
        } catch (Exception exception) {
            throw new RuntimeException("Error storing category photo", exception);
        }
    };
}