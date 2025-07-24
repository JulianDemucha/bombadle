package com.bombadle.repository;

import com.bombadle.entity.CharacterCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface CharacterCardRepository extends JpaRepository<CharacterCard, Long> {
    @Query(value =
            "SELECT * FROM character_card ORDER BY RAND() LIMIT 1"
            , nativeQuery = true)
    CharacterCard findRandomCard();

}
