package com.tecgensoft.jpa.repositories;

import com.tecgensoft.jpa.daos.PackageDao;
import com.tecgensoft.service.enums.ProductCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PackageRepository extends JpaRepository<PackageDao, UUID> {
    @Query("select coalesce(max(p.version), 0) from PackageDao p where p.code = :code")
    int findLatestVersion(@Param("code") String code);

    List<PackageDao> findAllByOrderByProductAscCodeAscVersionDesc();

    List<PackageDao> findAllByProductOrderByCodeAscVersionDesc(ProductCode product);

    Optional<PackageDao> findByIdAndActiveTrueAndAvailableForSaleTrue(UUID id);
}
