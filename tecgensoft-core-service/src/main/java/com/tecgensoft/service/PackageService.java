package com.tecgensoft.service;

import com.tecgensoft.http.dtos.requests.CreatePackageRequest;
import com.tecgensoft.http.dtos.requests.UpdatePackageAvailabilityRequest;
import com.tecgensoft.http.dtos.requests.UpdatePackageRequest;
import com.tecgensoft.jpa.daos.PackageDao;
import com.tecgensoft.jpa.repositories.PackageRepository;
import com.tecgensoft.mapper.PackageMapper;
import com.tecgensoft.service.entities.SoftwarePackage;
import com.tecgensoft.service.enums.PackageFeatureCode;
import com.tecgensoft.service.enums.ProductCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;

@Service
@Transactional
@RequiredArgsConstructor
public class PackageService {
    private static final Pattern FEATURE_CODE = Pattern.compile("[A-Z][A-Z0-9_]{1,63}");
    private static final Pattern LIMIT_CODE = Pattern.compile("[A-Z][A-Z0-9_]{1,63}");

    private final PackageRepository packageRepository;
    @Autowired private JdbcTemplate jdbc;

    public SoftwarePackage create(CreatePackageRequest request) {
        String code = request.code().trim().toUpperCase(Locale.ROOT);
        Set<String> features = request.features().stream()
                .map(value -> value.trim().toUpperCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        Map<String, Integer> limits = request.limits().entrySet().stream()
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        entry -> entry.getKey().trim().toUpperCase(Locale.ROOT),
                        Map.Entry::getValue
                ));

        if (features.stream().anyMatch(value -> !FEATURE_CODE.matcher(value).matches()
                || !PackageFeatureCode.supports(request.product(), value))) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Package features must be supported by the selected product"
            );
        }
        if (limits.entrySet().stream().anyMatch(entry ->
                !LIMIT_CODE.matcher(entry.getKey()).matches()
                        || entry.getValue() < -1
                        || !isLimitSupported(request.product(), entry.getKey()))) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Package limits must be supported by the selected product and nonnegative"
            );
        }

        PackageDao dao = new PackageDao();
        dao.setCode(code);
        dao.setVersion(packageRepository.findLatestVersion(code) + 1);
        dao.setDisplayName(request.displayName().trim());
        dao.setProduct(request.product());
        dao.setBillingPeriod(request.billingPeriod()==null?com.tecgensoft.service.enums.BillingPeriod.MONTHLY:request.billingPeriod());
        dao.setDescription(request.description() == null ? null : request.description().trim());
        dao.setCurrency(request.currency().trim().toUpperCase(Locale.ROOT));
        dao.setPrice(request.price().setScale(2, RoundingMode.UNNECESSARY));
        dao.setFeatures(features);
        dao.setLimits(limits);
        dao.setActive(true);
        dao.setAvailableForSale(true);

        PackageDao saved=packageRepository.saveAndFlush(dao);
        for(String feature:features) jdbc.update("insert into package_features(id,package_id,feature_id,feature_value) select ?,?,id,'true'::jsonb from features where product_code=? and code=? on conflict(package_id,feature_id) do nothing",
                UUID.randomUUID(),saved.getId(),saved.getProduct().name(),feature);
        for(var limit:limits.entrySet()) jdbc.update("insert into package_features(id,package_id,feature_id,feature_value) select ?,?,id,to_jsonb(?::integer) from features where product_code=? and code=? on conflict(package_id,feature_id) do nothing",
                UUID.randomUUID(),saved.getId(),limit.getValue(),saved.getProduct().name(),limit.getKey());
        return PackageMapper.toEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<SoftwarePackage> getAll(ProductCode product) {
        List<PackageDao> packages = product == null
                ? packageRepository.findAllByOrderByProductAscCodeAscVersionDesc()
                : packageRepository.findAllByProductOrderByCodeAscVersionDesc(product);
        return packages.stream().map(PackageMapper::toEntity).toList();
    }

    public SoftwarePackage updateAvailability(UUID packageId, UpdatePackageAvailabilityRequest request) {
        PackageDao dao = packageRepository.findById(packageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Package not found"));
        dao.setActive(request.active());
        dao.setAvailableForSale(request.availableForSale());
        return PackageMapper.toEntity(packageRepository.saveAndFlush(dao));
    }

    @Transactional(readOnly=true)
    public SoftwarePackage get(UUID packageId) {
        return packageRepository.findById(packageId).map(PackageMapper::toEntity)
                .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Package not found"));
    }

    public SoftwarePackage update(UUID packageId,UpdatePackageRequest request) {
        PackageDao dao=packageRepository.findById(packageId)
                .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Package not found"));
        dao.setDisplayName(request.displayName().trim());dao.setDescription(request.description());
        if(request.billingPeriod()!=null) dao.setBillingPeriod(request.billingPeriod());
        dao.setCurrency(request.currency().toUpperCase(Locale.ROOT));dao.setPrice(request.price().setScale(2,RoundingMode.UNNECESSARY));
        return PackageMapper.toEntity(packageRepository.saveAndFlush(dao));
    }

    private boolean isLimitSupported(ProductCode product, String code) {
        return switch (product) {
            case TURF -> "MAX_COURTS".equals(code) || "MAX_BOOKINGS".equals(code);
            case HOTEL -> "MAX_ROOMS".equals(code);
        };
    }
}
