package com.lab.platform.sharing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SharingAgreementRepository extends JpaRepository<SharingAgreement, Long> {

    Optional<SharingAgreement> findByAgreementNumber(String agreementNumber);

    Optional<SharingAgreement> findByProviderInstitutionIdAndConsumerInstitutionIdAndStatus(
            Long providerInstitutionId, Long consumerInstitutionId, SharingAgreementStatus status);

    @Query("SELECT a FROM SharingAgreement a WHERE a.providerInstitution.id = :institutionId OR a.consumerInstitution.id = :institutionId ORDER BY a.createdAt DESC")
    List<SharingAgreement> findByInstitutionId(@Param("institutionId") Long institutionId);

    List<SharingAgreement> findByStatus(SharingAgreementStatus status);
}
