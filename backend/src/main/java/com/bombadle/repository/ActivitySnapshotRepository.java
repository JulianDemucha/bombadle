package com.bombadle.repository;

import com.bombadle.entity.ActivitySnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ActivitySnapshotRepository extends JpaRepository<ActivitySnapshot, Long> {
}
