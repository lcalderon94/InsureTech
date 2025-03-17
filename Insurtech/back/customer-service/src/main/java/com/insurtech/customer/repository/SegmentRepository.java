package com.insurtech.customer.repository;

import com.insurtech.customer.model.entity.Segment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SegmentRepository extends JpaRepository<Segment, Long> {

    Optional<Segment> findByName(String name);

    List<Segment> findByIsActiveTrue();

    List<Segment> findBySegmentType(Segment.SegmentType segmentType);

    @Query("SELECT s FROM Segment s JOIN s.customers c WHERE c.id = :customerId")
    List<Segment> findByCustomerId(@Param("customerId") Long customerId);

    @Query("SELECT s FROM Segment s WHERE s.isActive = true AND SIZE(s.customers) > :minSize")
    List<Segment> findActiveSegmentsWithMinimumCustomers(@Param("minSize") int minSize);
}