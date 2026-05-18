package io.c4us.masterbackend.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import io.c4us.masterbackend.domain.AppUser;
import io.c4us.masterbackend.domain.Structure;
import io.c4us.masterbackend.repo.AppUserRepo;
import io.c4us.masterbackend.repo.StructureRepo; // ✅ Ajout du repo structure
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class DataInitializer {
@Bean
CommandLineRunner initDatabase(
        AppUserRepo userRepository,
        StructureRepo structureRepository,
        DefaultUserProperties defaultUser,
        PasswordEncoder passwordEncoder) {

    return args -> {
        // 1. On récupère ou on crée la structure
        Structure structure = structureRepository.findByCodeStructure("STR-DEFAULT")
                .orElseGet(() -> {
                    Structure newStruct = new Structure();
                    newStruct.setNomStructure("Structure Siège");
                    newStruct.setCodeStructure("STR-DEFAULT");
                    newStruct.setActive(true);
                    return structureRepository.save(newStruct); 
                });

        // 2. On vérifie l'utilisateur
        AppUser existingUser = userRepository.findByUserEmail(defaultUser.getEmail());

        if (existingUser == null) {
            AppUser user = new AppUser();
            user.setUserName(defaultUser.getName());
            user.setUserEmail(defaultUser.getEmail());
            user.setUserPhone(defaultUser.getPhone());
            user.setUserPassword(passwordEncoder.encode(defaultUser.getPassword()));
            user.setUserProfile(defaultUser.getProfile());
            
            // ✅ CRITIQUE : Utilisez l'ID technique (UUID) généré par Hibernate
            // car votre service fait un findById(id)
            user.setCodeStructure(structure.getIdStructure()); 

            userRepository.save(user);
            System.out.println("✅ Utilisateur lié à l'ID : " + structure.getIdStructure());
        }
    };
}
}