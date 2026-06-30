package com.bombadle.repository;

import com.bombadle.entity.DeletedAccountStatistic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeletedAccountStatisticRepository extends JpaRepository<DeletedAccountStatistic, Long> {
    Optional<DeletedAccountStatistic> findByDeletedAccountId(Long deletedAccountId);

    @Modifying
    @Query("DELETE FROM DeletedAccountStatistic d WHERE d.deletedAccountId IN :deletedAccountIds")
    void deleteAllByDeletedAccountIdIn(@Param("deletedAccountIds") List<Long> deletedAccountIds);
}
