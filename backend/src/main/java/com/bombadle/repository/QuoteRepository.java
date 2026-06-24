package com.bombadle.repository;
import com.bombadle.entity.Quote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface QuoteRepository extends JpaRepository<Quote, Long> {
    @Query(value = "SELECT * FROM quote ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
    Quote findRandomQuote();
}
