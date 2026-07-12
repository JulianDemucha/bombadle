package com.bombadle.repository;
import com.bombadle.entity.Quote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuoteRepository extends JpaRepository<Quote, Long> {
    @Query(value = "SELECT * FROM quote ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
    Quote findRandomQuote();

    @Query(value = "SELECT * FROM quote WHERE id NOT IN (:excludedIds) ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
    Quote findRandomQuoteExcluding(@Param("excludedIds") List<Long> excludedIds);
}
