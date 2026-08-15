package io.c4us.masterbackend.ressource;

import java.net.URI;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.c4us.masterbackend.domain.Customer;
import io.c4us.masterbackend.service.CustomerService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/customer")
@RequiredArgsConstructor
public class CustomerRessource {

    private final CustomerService customerService;

    @PostMapping
    public ResponseEntity<Customer> createCustomer(@RequestBody Customer customer) {
        try {
            Customer created = customerService.createCustomer(customer);
            return ResponseEntity.created(URI.create("/customer/" + created.getId()))
                    .body(created);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/update")
    public ResponseEntity<Customer> updateCustomer(@RequestBody Customer customer) {
        try {
            Customer updated = customerService.updateCustomer(customer);
            return ResponseEntity.ok(updated); // 200 OK pour une mise à jour
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping
    public ResponseEntity<Page<Customer>> getCustomer(@RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        return ResponseEntity.ok().body(customerService.getAllCustomer(page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Customer> getCustomer(@PathVariable(value = "id") String id) {
        return ResponseEntity.ok().body(customerService.getCustomer(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Customer> delCustomer(@PathVariable(value = "id") String id) {
        return ResponseEntity.ok().body(customerService.delCustomer(id));
    }

    @GetMapping("/number/{numCust}")
    public ResponseEntity<Customer> getCustomerByNumCust(@PathVariable String numCust) {
        return customerService.getCustomerByNumCust(numCust)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    @GetMapping("/structure/{codeStructure}")
    public ResponseEntity<List<Customer>> getCustomersByStructure(@PathVariable(value = "codeStructure") String codeStructure) {
        return ResponseEntity.ok().body(customerService.getCustomersByStructure(codeStructure));
    }
}