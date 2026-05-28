package com.bombadle.repository;

import com.bombadle.dto.GuessListDto;
import com.bombadle.entity.AnonymousGuessList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface AnonymousGuessListRepository extends JpaRepository<AnonymousGuessList, Long> {
    @Modifying
    @Transactional
    @Query(value = "TRUNCATE TABLE anonymous_guess_list CASCADE ", nativeQuery = true)
    void truncateTable();

}
