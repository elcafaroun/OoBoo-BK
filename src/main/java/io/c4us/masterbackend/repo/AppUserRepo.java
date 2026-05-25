package io.c4us.masterbackend.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import io.c4us.masterbackend.domain.AppUser;

public interface AppUserRepo extends JpaRepository<AppUser, String> {
    Optional<AppUser> findByConfirmationToken(String confirmationToken);

    Optional<AppUser> findById(String id);

    AppUser findByUserEmail(String confirmationToken);

    AppUser findByUserPhone(String phone); // 👈 ajoute cette ligne

    List<AppUser> findByCodeStructureAndIsActiveTrue(String codeStructure);

    List<AppUser> findByCodeStructure(String codeStructure);

    long countByCodeStructure(String codeStructure);

    long countByCodeUserStartingWith(String prefix);

    // Dans votre AppUserRepo.java
    boolean existsByUserEmail(String email);

    boolean existsByUserPhone(String phone);


    
    AppUser findByCodeUser(String code);

}
