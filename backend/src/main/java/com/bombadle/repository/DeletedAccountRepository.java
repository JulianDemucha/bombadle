package com.bombadle.repository;

import com.bombadle.entity.DeletedAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DeletedAccountRepository extends JpaRepository<DeletedAccount, Long> {
    Optional<DeletedAccount> findByEmail(String email);
}

