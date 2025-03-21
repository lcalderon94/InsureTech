package com.insurtech.claim.repository;

import com.insurtech.claim.model.entity.Claim;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ClaimRepositoryCustom {
    Page<Claim> search(String searchTerm, Pageable pageable);
}