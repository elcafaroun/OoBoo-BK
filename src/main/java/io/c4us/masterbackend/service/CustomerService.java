package io.c4us.masterbackend.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import io.c4us.masterbackend.domain.Customer;
import io.c4us.masterbackend.repo.CustomerRepo;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@Transactional(rollbackOn = Exception.class)
@RequiredArgsConstructor
public class CustomerService {

    @Autowired
    private CustomerRepo customerRepo;

    public Customer delCustomer(String id) {
        try {
            Customer customer = getCustomer(id);
            customerRepo.deleteById(id);
            return customer;
        } catch (Exception exception) {
            throw new RuntimeException();
        }
    }

    public Customer getCustomer(String id) {
        return customerRepo.findById(id).orElseThrow(() -> new RuntimeException("Customer Not found"));
    }

    public Page<Customer> getAllCustomer(int page, int size) {
        return customerRepo.findAll(PageRequest.of(page, size, Sort.by("createdDate")));
    }

    // ✅ AJOUT : Récupérer tous les clients d'une structure spécifique
    public List<Customer> getCustomersByStructure(String codeStructure) {
        return customerRepo.findByCodeStructure(codeStructure);
    }

    public Customer updateCustomer(Customer newCustomer) {
        try {
            Optional<Customer> existingCustomerOpt = customerRepo.findById(newCustomer.getId());
            
            Customer customerToSave;
            if (existingCustomerOpt.isPresent()) {
                customerToSave = existingCustomerOpt.get();
                customerToSave.setCustomerName(newCustomer.getCustomerName());
                customerToSave.setNumCust(newCustomer.getNumCust());
                customerToSave.setCodePin(newCustomer.getCodePin());
                customerToSave.setVersion(newCustomer.getVersion());
                
                // ✅ Mise à jour du codeStructure si présent
                if (newCustomer.getCodeStructure() != null) {
                    customerToSave.setCodeStructure(newCustomer.getCodeStructure());
                }
            } else {
                customerToSave = newCustomer;
            }
            
            return customerRepo.save(customerToSave);
            
        } catch (Exception exception) {
            log.error("Erreur lors de la mise à jour/création du client : {}", exception.getMessage(), exception);
            throw new RuntimeException("Erreur updateCustomer: " + exception.getMessage(), exception);
        }
    }

    public Customer createCustomer(Customer customer) {
        Optional<Customer> existing = customerRepo.findById(customer.getId());
        
        if (existing.isPresent()) {
            Customer customerToUpdate = existing.get();
            customerToUpdate.setCustomerName(customer.getCustomerName());
            customerToUpdate.setNumCust(customer.getNumCust());
            customerToUpdate.setCodePin(customer.getCodePin());
            
            // ✅ Mise à jour du codeStructure si présent
            if (customer.getCodeStructure() != null) {
                customerToUpdate.setCodeStructure(customer.getCodeStructure());
            }
            return customerRepo.save(customerToUpdate);
        }
        
        customer.setVersion(null); 
        return customerRepo.save(customer);
    }

    public Optional<Customer> getCustomerByNumCust(String numCust) {
        return customerRepo.findByNumCust(numCust);
    }
}