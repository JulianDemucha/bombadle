package com.bombadle.repository;

import com.bombadle.entity.DeletedAccount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeletedAccountRepository extends JpaRepository<DeletedAccount, Long> {
}

