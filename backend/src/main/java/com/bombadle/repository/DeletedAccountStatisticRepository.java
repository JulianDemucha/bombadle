package com.bombadle.repository;

import com.bombadle.entity.DeletedAccountStatistic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DeletedAccountStatisticRepository extends JpaRepository<DeletedAccountStatistic, Long> {
    Optional<DeletedAccountStatistic> findByDeletedAccountId(Long deletedAccountId);
}
