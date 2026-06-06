package com.bombadle.repository;

import com.bombadle.entity.CurrentCardState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CurrentCardStateRepository extends JpaRepository<CurrentCardState, Integer> {
}
