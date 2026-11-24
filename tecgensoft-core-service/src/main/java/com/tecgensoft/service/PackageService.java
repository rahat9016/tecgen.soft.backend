package com.tecgensoft.service;

import com.tecgensoft.http.dtos.requests.UpdatePackageRequest;
import com.tecgensoft.jpa.daos.PackageDao;
import com.tecgensoft.jpa.repositories.PackageRepository;
import com.tecgensoft.mapper.PackageMapper;
import com.tecgensoft.service.entities.SubscriptionPlan;
import com.tecgensoft.service.enums.PackageFeatureCode;
import com.tecgensoft.service.enums.ProductCode;
import com.tecgensoft.service.exceptions.PackageNotFoundException;
import com.tecgensoft.service.exceptions.UnsupportedPackageFeatureException;
import com.tecgensoft.service.exceptions.UnsupportedPackageLimitException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@Transactional
@RequiredArgsConstructor
public class PackageService {
    private static final Pattern FEATURE_CODE = Pattern.compile("[A-Z][A-Z0-9_]{1,63}");
    private static final Pattern LIMIT_CODE = Pattern.compile("[A-Z][A-Z0-9_]{1,63}");

    private final PackageRepository packageRepository;

    public SubscriptionPlan create(SubscriptionPlan plan) {
        if (plan.features().stream().anyMatch(value -> !FEATURE_CODE.matcher(value).matches()
                || !PackageFeatureCode.supports(plan.product(), value))) {
            throw new UnsupportedPackageFeatureException();
        }
        if (plan.limits().entrySet().stream().anyMatch(entry ->
                !LIMIT_CODE.matcher(entry.getKey()).matches()
                        || entry.getValue() < -1
                        || !isLimitSupported(plan.product(), entry.getKey()))) {
            throw new UnsupportedPackageLimitException();
        }

        int version = packageRepository.findLatestVersion(plan.code()) + 1;
        PackageDao saved = packageRepository.saveAndFlush(PackageMapper.toDao(plan, version));
        return PackageMapper.toEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<SubscriptionPlan> getAll(ProductCode product) {
        List<PackageDao> packages = product == null
                ? packageRepository.findAllByOrderByProductAscCodeAscVersionDesc()
                : packageRepository.findAllByProductOrderByCodeAscVersionDesc(product);
        return packages.stream().map(PackageMapper::toEntity).toList();
    }


    @Transactional(readOnly=true)
    public SubscriptionPlan get(UUID packageId) {
        return packageRepository.findById(packageId).map(PackageMapper::toEntity)
                .orElseThrow(PackageNotFoundException::new);
    }

    public SubscriptionPlan update(UUID packageId, UpdatePackageRequest request) {
        PackageDao dao=packageRepository.findById(packageId)
                .orElseThrow(PackageNotFoundException::new);
        dao.setDisplayName(request.displayName().trim());
        dao.setDescription(request.description());
        if(request.billingPeriod()!=null) dao.setBillingPeriod(request.billingPeriod());
        dao.setCurrency(request.currency().toUpperCase(Locale.ROOT));
        dao.setPrice(request.price().setScale(2,RoundingMode.UNNECESSARY));
        if(request.active()!=null) dao.setActive(request.active());
        if(request.availableForSale()!=null) dao.setAvailableForSale(request.availableForSale());
        return PackageMapper.toEntity(packageRepository.saveAndFlush(dao));
    }

    private boolean isLimitSupported(ProductCode product, String code) {
        return switch (product) {
            case TURF -> "MAX_COURTS".equals(code) || "MAX_BOOKINGS".equals(code);
            case HOTEL -> "MAX_ROOMS".equals(code);
        };
    }
}
