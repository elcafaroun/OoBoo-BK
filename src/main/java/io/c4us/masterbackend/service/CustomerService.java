package io.c4us.masterbackend.service;

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

    public Customer updateCustomer(Customer newCustomer) {
        try {
            Customer customer = getCustomer(newCustomer.getId());
          //  customer.setId(newCustomer.getId());
            customerRepo.save(customer);
            return customer;
        } catch (Exception exception) {
            throw new RuntimeException();
        }
    }

    public Customer createCustomer(Customer customer) {
 
    //private Passwor passwordEncoder; 
        return customerRepo.save(customer);
    }


    
    
}
