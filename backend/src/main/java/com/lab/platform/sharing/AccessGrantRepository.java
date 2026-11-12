package com.lab.platform.sharing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccessGrantRepository extends JpaRepository<AccessGrant, Long> {

    Optional<AccessGrant> findByUserIdAndEquipmentIdAndActiveTrue(Long userId, Long equipmentId);

    List<AccessGrant> findByUserIdAndActiveTrue(Long userId);

    List<AccessGrant> findByEquipmentId(Long equipmentId);

    List<AccessGrant> findByAgreementId(Long agreementId);
}
