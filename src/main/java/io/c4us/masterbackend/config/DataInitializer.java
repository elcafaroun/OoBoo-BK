package io.c4us.masterbackend.config;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import io.c4us.masterbackend.domain.AppUser;
import io.c4us.masterbackend.domain.Structure;
import io.c4us.masterbackend.domain.UserStructure; // 👈 Import de votre entité ou table de liaison Many-to-Many
import io.c4us.masterbackend.repo.AppUserRepo;
import io.c4us.masterbackend.repo.StructureRepo;
import io.c4us.masterbackend.repo.UserStructureRepo; // 👈 Import du repo d'association si existant
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class DataInitializer {
    
    @Bean
    CommandLineRunner initDatabase(
            AppUserRepo userRepository,
            StructureRepo structureRepository,
            UserStructureRepo userStructureRepository, // 👈 Injecté pour créer l'association multi-structure
            DefaultUserProperties defaultUser,
            PasswordEncoder passwordEncoder) {

        return args -> {
            // 1. On récupère la liste des structures correspondant au code
            List<Structure> structures = structureRepository.findByCodeStructure("STR-DEFAULT");
            Structure structure;

            if (structures.isEmpty()) {
                Structure newStruct = new Structure();
                newStruct.setNomStructure("Structure Siège");
                newStruct.setCodeStructure("STR-DEFAULT");
                newStruct.setActive(true);
                structure = structureRepository.save(newStruct);
                log.info("ℹ️ Structure par défaut 'STR-DEFAULT' créée avec succès.");
            } else {
                structure = structures.get(0);
            }

            // 2. On vérifie la présence de l'utilisateur d'initialisation
            AppUser existingUser = userRepository.findByUserEmail(defaultUser.getEmail());

            if (existingUser == null) {
                AppUser user = new AppUser();
                user.setUserName(defaultUser.getName());
                user.setUserEmail(defaultUser.getEmail());
                user.setUserPhone(defaultUser.getPhone());
                user.setUserPassword(passwordEncoder.encode(defaultUser.getPassword()));
                user.setUserProfile(defaultUser.getProfile());
                user.setFirstLogin(false); // 👈 Le compte racine n'a pas besoin de forcer le changement de PIN
                user.setActive(true);
                
                // Sauvegarde de l'utilisateur principal d'abord
                AppUser savedUser = userRepository.save(user);
                log.info("✅ Utilisateur principal '{}' créé.", savedUser.getUserName());

                // 3. ✅ Association Many-to-Many (Liaison via l'entité intermédiaire UserStructure)
                UserStructure association = new UserStructure();
                association.setUser(savedUser);
                association.setStructure(structure);
                association.setRoleInStructure(savedUser.getUserProfile()); // "Super admin" ou profil par défaut
                association.setUpdatedAt(LocalDateTime.now());
                
                userStructureRepository.save(association);
                log.info("🔗 Utilisateur '{}' lié avec succès à la structure '{}' avec le rôle : {}", 
                        savedUser.getUserName(), structure.getNomStructure(), association.getRoleInStructure());
            }
        };
    }
}