package io.c4us.masterbackend.repo;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import io.c4us.masterbackend.domain.SegmentRule;

public interface SegmentRuleRepo extends JpaRepository<SegmentRule, String> {
    Optional<SegmentRule> findBySegmentNameAndCodeStructure(String segmentName, String codeStructure);
    Optional<SegmentRule> findBySegmentName(String segmentName);
}