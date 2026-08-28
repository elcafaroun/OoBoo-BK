package io.c4us.masterbackend.repo;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

import io.c4us.masterbackend.domain.Customer;



public interface CustomerRepo extends JpaRepository<Customer, String>{
    
    @SuppressWarnings("null")
    Optional<Customer> findById(String id);
    Optional<Customer> findByNumCust(String numCust);
   List<Customer> findByCodeStructure(String codeStructure); 
   Optional<Customer> findByNumCustAndCodeStructure(String numCust, String codeStructure);// ✅ À ajouter si ce n'est pas déjà fait
}
