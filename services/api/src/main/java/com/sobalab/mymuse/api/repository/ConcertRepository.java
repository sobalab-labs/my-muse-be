package com.sobalab.mymuse.api.repository;

import com.sobalab.mymuse.api.model.Concert;
import com.sobalab.mymuse.api.model.enums.Region;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ConcertRepository extends JpaRepository<Concert, UUID> {

    @Query("SELECT c FROM Concert c JOIN c.venue v WHERE (:q IS NULL OR LOWER(c.title) LIKE LOWER(CONCAT('%', :q, '%'))) " +
           "AND (:region IS NULL OR v.region = :region)")
    List<Concert> searchConcerts(@Param("q") String q, @Param("region") Region region, Pageable pageable);

    @Query("SELECT c FROM Concert c JOIN c.venue v WHERE " +
           "(:region IS NULL OR v.region = :region) " +
           "AND (:startDate IS NULL OR c.date >= :startDate) " +
           "AND (:endDate IS NULL OR c.date < :endDate)")
    List<Concert> findTrendingFeed(@Param("region") Region region,
                                   @Param("startDate") LocalDateTime startDate,
                                   @Param("endDate") LocalDateTime endDate,
                                   Pageable pageable);
}
