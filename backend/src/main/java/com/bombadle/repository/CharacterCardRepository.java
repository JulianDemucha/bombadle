package com.bombadle.repository;

import com.bombadle.entity.CharacterCard;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CharacterCardRepository extends JpaRepository<CharacterCard, Long> {
    @Query(value = "SELECT * FROM character_card ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
    CharacterCard findRandomCard();

    @Query(value = "SELECT * FROM character_card WHERE id NOT IN (:excludedIds) ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
    CharacterCard findRandomCardExcluding(@Param("excludedIds") List<Long> excludedIds);

    @EntityGraph(attributePaths = {"colors", "affiliations", "quotes"})
    Optional<CharacterCard> findById(Long id);

    boolean existsByName(String name);

}
