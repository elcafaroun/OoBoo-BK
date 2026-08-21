package io.c4us.masterbackend.repo;


import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import io.c4us.masterbackend.domain.AppUser;

@Repository
public interface AppUserRepo extends JpaRepository<AppUser, String> {
    
    Optional<AppUser> findByConfirmationToken(String confirmationToken);

    // Optional<AppUser> findById(String id);

    AppUser findByUserEmail(String email);

    AppUser findByUserPhone(String phone); 

    AppUser findByCodeUser(String code);

    long countByCodeUserStartingWith(String prefix);

    boolean existsByUserEmail(String email);

    boolean existsByUserPhone(String phone);

    

    // ❌ Les méthodes findByCodeStructure, findByCodeStructureAndIsActiveTrue 
    // ❌ et countByCodeStructure ont été définitivement supprimées.
}