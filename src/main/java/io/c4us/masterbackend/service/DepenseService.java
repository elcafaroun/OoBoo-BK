package io.c4us.masterbackend.service;

import io.c4us.masterbackend.domain.Depense;
import io.c4us.masterbackend.repo.DepenseRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DepenseService {

    private final DepenseRepo depenseRepository;

    public Depense saveDepense(Depense depense) {
        return depenseRepository.save(depense);
    }

    public void deleteDepense(Long id) {
        depenseRepository.deleteById(id);
    }

    public List<Depense> getAllByStructure(String codeStructure) {
        return depenseRepository.findByCodeStructure(codeStructure);
    }

    public Double getSumByDate(Date date, String code) {
        Double sum = depenseRepository.sumByDate(date, code);
        return sum != null ? sum : 0.0;
    }

    public Double getSumBetween(Date start, Date end, String code) {
        Double sum = depenseRepository.sumBetweenDates(start, end, code);
        return sum != null ? sum : 0.0;
    }
}