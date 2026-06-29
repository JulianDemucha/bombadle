package com.bombadle.repository;

import com.bombadle.entity.ActivitySnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ActivitySnapshotRepository extends JpaRepository<ActivitySnapshot, Long> {

    List<ActivitySnapshot> findAllByOrderByTimestampAsc();

    List<ActivitySnapshot> findByTimestampAfterOrderByTimestampAsc(Instant from);
}
