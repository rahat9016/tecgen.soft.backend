package com.tecgensoft.service;

import com.tecgensoft.http.dtos.requests.AssignSubscriptionRequest;
import com.tecgensoft.http.dtos.requests.UpdateSubscriptionStatusRequest;
import com.tecgensoft.jpa.daos.OrganizationDao;
import com.tecgensoft.jpa.daos.PackageDao;
import com.tecgensoft.jpa.daos.SubscriptionDao;
import com.tecgensoft.jpa.repositories.OrganizationRepository;
import com.tecgensoft.jpa.repositories.PackageRepository;
import com.tecgensoft.jpa.repositories.SubscriptionRepository;
import com.tecgensoft.jpa.repositories.SubscriptionPaymentRepository;
import com.tecgensoft.jpa.daos.SubscriptionPaymentDao;
import com.tecgensoft.mapper.SubscriptionMapper;
import com.tecgensoft.service.entities.Subscription;
import com.tecgensoft.service.enums.ProductCode;
import com.tecgensoft.service.enums.SubscriptionStatus;
import com.tecgensoft.service.exceptions.PackageNotFoundException;
import com.tecgensoft.service.exceptions.SubscriptionPaymentNotFoundException;
import com.tecgensoft.service.exceptions.InvalidSubscriptionPeriodException;
import com.tecgensoft.service.exceptions.UnsupportedPaymentMethodException;
import com.tecgensoft.service.exceptions.UnsupportedPaymentStatusException;
import com.tecgensoft.service.exceptions.InvoiceNotPayableException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;

import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@Transactional
@RequiredArgsConstructor
public class SubscriptionService {
    private final OrganizationRepository organizationRepository;
    private final PackageRepository packageRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPaymentRepository paymentRepository;
    private final OrganizationPermissionGranter permissionGranter;
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    @Value("${subscriptions.turf-basic-trial-days:30}")
    private int turfBasicTrialDays;

    private Instant periodEnd(Instant start,com.tecgensoft.service.enums.BillingPeriod period) {
        var utc=start.atZone(java.time.ZoneOffset.UTC);
        return (period==com.tecgensoft.service.enums.BillingPeriod.YEARLY?utc.plusYears(1):utc.plusMonths(1)).toInstant();
    }

    @Scheduled(fixedDelayString = "${billing.cycle-scan-ms:3600000}")
    public void rollBillingCycles() {
        Instant now = Instant.now();

        // 1. ROLL SUBSCRIPTIONS PAST PERIOD END (never expire / never stop)
        var toRoll = subscriptionRepository.findAllByStatusInAndEndsAtBefore(
                List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.TRIALING), now);
        for (SubscriptionDao subscription : toRoll) {
            Instant newStart = subscription.getEndsAt();
            Instant newEnd = periodEnd(newStart, subscription.getBillingPeriod());
            subscription.setStartsAt(newStart);
            subscription.setEndsAt(newEnd);
            if (subscription.isTrial()) {
                subscription.setTrial(false);
                subscription.setTrialStartDate(null);
                subscription.setTrialEndDate(null);
                subscription.setAgreedPrice(subscription.getPackageDao().getPrice());
            }
            subscription.setStatus(SubscriptionStatus.ACTIVE);
            subscriptionRepository.save(subscription);
            createInvoice(subscription, subscription.getPackageDao().getPrice(),
                    "DUE", null, null, newStart, newEnd);
        }

        // 2. FLIP DUE INVOICES PAST THEIR GRACE WINDOW TO OVERDUE (label only)
        markOverdue(now);
    }

    private void markOverdue(Instant now) {
        var dueInvoices = paymentRepository.findAllByPaymentStatus("DUE");
        java.util.List<SubscriptionPaymentDao> overdue = new java.util.ArrayList<>();
        for (SubscriptionPaymentDao invoice : dueInvoices) {
            int grace = invoice.getSubscription().getGraceDays();
            Instant cutoff = invoice.getCreatedAt().plusSeconds((long) grace * 86400);
            if (now.isAfter(cutoff)) {
                invoice.setPaymentStatus("OVERDUE");
                overdue.add(invoice);
            }
        }
        if (!overdue.isEmpty()) {
            paymentRepository.saveAll(overdue);
        }
    }

    private SubscriptionPaymentDao createInvoice(
            SubscriptionDao subscription, java.math.BigDecimal amount, String status,
            String method, String transactionId, Instant periodStart, Instant periodEnd) {
        SubscriptionPaymentDao payment = new SubscriptionPaymentDao();
        payment.setSubscription(subscription);
        payment.setAmount(amount.setScale(2, RoundingMode.HALF_UP));
        payment.setCurrency(subscription.getPackageDao().getCurrency());
        payment.setPaymentMethod(method == null ? "N/A" : method);
        payment.setPaymentStatus(status);
        payment.setTransactionId(transactionId);
        if ("PAID".equals(status)) {
            payment.setPaymentDate(Instant.now());
        }
        payment.setPeriodStart(periodStart);
        payment.setPeriodEnd(periodEnd);
        return paymentRepository.saveAndFlush(payment);
    }

    @Transactional
    public Subscription provisionInitialSubscription(
            OrganizationDao organization,
            UUID packageId,
            boolean isFreeTrial,
            Instant startsAt,
            Instant endsAt,
            java.math.BigDecimal amountPaid,
            int graceDays
    ) {
        // 1. VALIDATE PERIOD
        if (!endsAt.isAfter(startsAt)) {
            throw new InvalidSubscriptionPeriodException();
        }

        // 2. FIND SELLABLE PACKAGE
        PackageDao packageDao = packageRepository
                .findByIdAndActiveTrueAndAvailableForSaleTrue(packageId)
                .orElseThrow(PackageNotFoundException::new);

        // 3. BUILD SUBSCRIPTION
        SubscriptionDao subscription = new SubscriptionDao();
        subscription.setOrganization(organization);
        subscription.setPackageDao(packageDao);
        subscription.setProduct(packageDao.getProduct());
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setBillingPeriod(packageDao.getBillingPeriod());
        subscription.setStartsAt(startsAt);
        subscription.setEndsAt(endsAt);
        subscription.setTrial(isFreeTrial);
        subscription.setGraceDays(graceDays);
        subscription.setActivatedAt(Instant.now());

        // 4. TRIAL VS PAID PRICING
        if (isFreeTrial) {
            subscription.setTrialStartDate(startsAt);
            subscription.setTrialEndDate(endsAt);
            subscription.setAgreedPrice(java.math.BigDecimal.ZERO);
        } else {
            subscription.setAgreedPrice(packageDao.getPrice());
        }

        SubscriptionDao saved = subscriptionRepository.saveAndFlush(subscription);

        // 5. GRANT PRODUCT PERMISSIONS
        permissionGranter.grantProductPermissions(organization.getId(), packageDao.getProduct());

        // 6. CREATE FIRST INVOICE
        if (isFreeTrial) {
            createInvoice(saved, java.math.BigDecimal.ZERO, "PAID", "FREE_TRIAL", null, startsAt, endsAt);
        } else {
            boolean fullyPaid = amountPaid != null
                    && amountPaid.compareTo(packageDao.getPrice()) >= 0;
            createInvoice(saved, packageDao.getPrice(),
                    fullyPaid ? "PAID" : "DUE", fullyPaid ? "CASH" : null, null, startsAt, endsAt);
        }

        // 7. RETURN DOMAIN
        return SubscriptionMapper.toEntity(saved);
    }

    public SubscriptionPaymentDao payInvoice(
            UUID organizationId, UUID subscriptionId, UUID paymentId,
            String method, String transactionId
    ) {
        // 1. FIND INVOICE WITHIN ORGANIZATION
        SubscriptionPaymentDao invoice = paymentRepository
                .findByIdAndSubscription_Id(paymentId, subscriptionId)
                .filter(p -> p.getSubscription().getOrganization().getId().equals(organizationId))
                .orElseThrow(SubscriptionPaymentNotFoundException::new);

        // 2. ONLY DUE / OVERDUE ARE PAYABLE
        if (!List.of("DUE", "OVERDUE").contains(invoice.getPaymentStatus())) {
            throw new InvoiceNotPayableException();
        }

        // 3. VALIDATE METHOD
        if (!List.of("BKASH", "NAGAD", "SSL_COMMERZ", "BANK", "CASH").contains(method)) {
            throw new UnsupportedPaymentMethodException(method);
        }

        // 4. SETTLE
        invoice.setPaymentStatus("PAID");
        invoice.setPaymentMethod(method);
        invoice.setTransactionId(transactionId);
        invoice.setPaymentDate(Instant.now());
        return paymentRepository.saveAndFlush(invoice);
    }

    public Subscription assign(UUID organizationId, AssignSubscriptionRequest request) {
        if (request.endsAt() != null && !request.endsAt().isAfter(request.startsAt())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "endsAt must be later than startsAt");
        }

        OrganizationDao organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found"));
        PackageDao packageDao = packageRepository.findByIdAndActiveTrueAndAvailableForSaleTrue(request.packageId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Available package not found"));
        if(packageDao.getBillingPeriod()!=request.billingPeriod())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Billing period must match selected package");

        Instant now = Instant.now();
        boolean firstTurfBasicTrial = packageDao.getProduct() == ProductCode.TURF
                && "TURF_BASIC".equals(packageDao.getCode())
                && subscriptionRepository.findTopByOrganization_IdAndProductOrderByCreatedAtDesc(
                        organizationId, packageDao.getProduct()).isEmpty();
        var currentActive = subscriptionRepository.findTopByOrganization_IdAndProductOrderByCreatedAtDesc(
                organizationId, packageDao.getProduct())
                .filter(existing -> existing.getStatus() == SubscriptionStatus.ACTIVE
                        || existing.getStatus() == SubscriptionStatus.TRIALING);
        if (request.startsAt().isAfter(now) && currentActive.isPresent()) {
            SubscriptionDao current = currentActive.get();
            boolean currentIsEffective = !current.getStartsAt().isAfter(now)
                    && (current.getEndsAt() == null || current.getEndsAt().isAfter(now));
            if (currentIsEffective) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "A future subscription cannot replace a currently active subscription"
                );
            }
        }
        if (!firstTurfBasicTrial) currentActive.ifPresent(previous -> {
            previous.setStatus(SubscriptionStatus.REPLACED);
            subscriptionRepository.saveAndFlush(previous);
        });

        SubscriptionDao dao = new SubscriptionDao();
        dao.setOrganization(organization);
        dao.setPackageDao(packageDao);
        dao.setProduct(packageDao.getProduct());
        dao.setStatus(firstTurfBasicTrial ? SubscriptionStatus.TRIALING : SubscriptionStatus.ACTIVE);
        dao.setBillingPeriod(request.billingPeriod());
        dao.setStartsAt(firstTurfBasicTrial ? now : request.startsAt());
        dao.setEndsAt(firstTurfBasicTrial ? now.plusSeconds((long) turfBasicTrialDays * 86400) : request.endsAt());
        dao.setAgreedPrice(firstTurfBasicTrial ? java.math.BigDecimal.ZERO : request.agreedPrice().setScale(2, RoundingMode.UNNECESSARY));
        dao.setTrial(firstTurfBasicTrial);
        if (firstTurfBasicTrial) {
            dao.setTrialStartDate(now);
            dao.setTrialEndDate(dao.getEndsAt());
        } else if (dao.getStatus() == SubscriptionStatus.ACTIVE) dao.setActivatedAt(now);

        SubscriptionDao saved = subscriptionRepository.saveAndFlush(dao);
        permissionGranter.grantProductPermissions(organizationId, packageDao.getProduct());
        return SubscriptionMapper.toEntity(saved);
    }

    public Subscription updateStatus(
            UUID organizationId,
            UUID subscriptionId,
            UpdateSubscriptionStatusRequest request
    ) {
        if (request.status() == SubscriptionStatus.REPLACED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "REPLACED is managed by the system");
        }
        if(!List.of(SubscriptionStatus.ACTIVE,SubscriptionStatus.SUSPENDED,SubscriptionStatus.CANCELLED).contains(request.status()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Status must be ACTIVE, SUSPENDED, or CANCELLED");
        SubscriptionDao dao = subscriptionRepository.findByIdAndOrganization_Id(subscriptionId, organizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Subscription not found"));

        if (request.status() == SubscriptionStatus.ACTIVE) {
            subscriptionRepository.findTopByOrganization_IdAndProductOrderByCreatedAtDesc(organizationId,dao.getProduct())
                    .filter(other -> !other.getId().equals(subscriptionId)
                            && (other.getStatus()==SubscriptionStatus.ACTIVE || other.getStatus()==SubscriptionStatus.TRIALING))
                    .ifPresent(other -> { throw new ResponseStatusException(HttpStatus.CONFLICT,"An active subscription already exists for this product"); });
        }

        dao.setStatus(request.status());
        if (request.status() == SubscriptionStatus.ACTIVE) {
            Instant now=Instant.now();dao.setActivatedAt(now);dao.setCancelledAt(null);dao.setTrial(false);
            dao.setTrialStartDate(null);dao.setTrialEndDate(null);dao.setAgreedPrice(dao.getPackageDao().getPrice());
            dao.setStartsAt(now);dao.setEndsAt(periodEnd(now,dao.getBillingPeriod()));
        }
        if (request.status() == SubscriptionStatus.CANCELLED) dao.setCancelledAt(Instant.now());
        return SubscriptionMapper.toEntity(subscriptionRepository.saveAndFlush(dao));
    }

    public Subscription renew(UUID organizationId,UUID subscriptionId) {
        var dao=subscriptionRepository.findByIdAndOrganization_Id(subscriptionId,organizationId)
                .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Subscription not found"));
        Instant now=Instant.now();
        var start=dao.getEndsAt()!=null && dao.getEndsAt().isAfter(now)?dao.getEndsAt():now;
        dao.setStartsAt(start);
        dao.setEndsAt(periodEnd(start,dao.getBillingPeriod()));
        dao.setStatus(SubscriptionStatus.ACTIVE); dao.setTrial(false); dao.setTrialStartDate(null); dao.setTrialEndDate(null); dao.setActivatedAt(now);
        dao.setAgreedPrice(dao.getPackageDao().getPrice());dao.setCancelledAt(null);
        return SubscriptionMapper.toEntity(subscriptionRepository.saveAndFlush(dao));
    }

    public Subscription extendTrial(UUID organizationId,UUID subscriptionId,int days) {
        if(days<1 || days>365) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Trial extension must be 1 to 365 days");
        var dao=subscriptionRepository.findByIdAndOrganization_Id(subscriptionId,organizationId)
                .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Subscription not found"));
        if(!dao.isTrial() || dao.getStatus()!=SubscriptionStatus.TRIALING)
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Subscription is not trialing");
        Instant end=(dao.getTrialEndDate()==null?Instant.now():dao.getTrialEndDate()).plusSeconds((long)days*86400);
        dao.setTrialEndDate(end);dao.setEndsAt(end);
        return SubscriptionMapper.toEntity(subscriptionRepository.saveAndFlush(dao));
    }

    public List<SubscriptionPaymentDao> payments(UUID organizationId,UUID subscriptionId) {
        if(!subscriptionRepository.existsByIdAndOrganization_Id(subscriptionId,organizationId))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Subscription not found");
        return paymentRepository.findAllBySubscription_IdOrderByCreatedAtDesc(subscriptionId);
    }

    public SubscriptionPaymentDao createPayment(UUID organizationId,UUID subscriptionId,SubscriptionPaymentDao payment) {
        var subscription=subscriptionRepository.findByIdAndOrganization_Id(subscriptionId,organizationId)
                .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Subscription not found"));
        payment.setSubscription(subscription);
        if(!List.of("BKASH","NAGAD","SSL_COMMERZ","BANK","CASH").contains(payment.getPaymentMethod()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Unsupported payment method");
        if(!List.of("PAID","DUE","OVERDUE","SUCCESS","PENDING","FAILED","REFUNDED").contains(payment.getPaymentStatus()))
            throw new UnsupportedPaymentStatusException();
        if("SUCCESS".equals(payment.getPaymentStatus()) || "PAID".equals(payment.getPaymentStatus())) {
            Instant now=payment.getPaymentDate()==null?Instant.now():payment.getPaymentDate();
            subscription.setStatus(SubscriptionStatus.ACTIVE);subscription.setTrial(false);
            subscription.setTrialStartDate(null);subscription.setTrialEndDate(null);subscription.setActivatedAt(now);
            subscription.setCancelledAt(null);subscription.setAgreedPrice(subscription.getPackageDao().getPrice());
            if(payment.getPeriodStart()!=null) subscription.setStartsAt(payment.getPeriodStart());
            if(payment.getPeriodEnd()!=null) subscription.setEndsAt(payment.getPeriodEnd());
            else if(subscription.getEndsAt()==null || !subscription.getEndsAt().isAfter(now))
                subscription.setEndsAt(periodEnd(now,subscription.getBillingPeriod()));
            subscriptionRepository.save(subscription);
        }
        return paymentRepository.saveAndFlush(payment);
    }

    public SubscriptionPaymentDao refundPayment(UUID organizationId,UUID subscriptionId,UUID paymentId) {
        var payment=paymentRepository.findByIdAndSubscription_Id(paymentId,subscriptionId)
                .filter(row->row.getSubscription().getOrganization().getId().equals(organizationId))
                .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Subscription payment not found"));
        if(!"SUCCESS".equals(payment.getPaymentStatus())) throw new ResponseStatusException(HttpStatus.CONFLICT,"Only successful payments can be refunded");
        payment.setPaymentStatus("REFUNDED");
        return paymentRepository.saveAndFlush(payment);
    }

    @Transactional(readOnly = true)
    public List<Subscription> getHistory(UUID organizationId) {
        if (!organizationRepository.existsById(organizationId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found");
        }
        return subscriptionRepository.findAllByOrganization_IdOrderByCreatedAtDesc(organizationId)
                .stream().map(SubscriptionMapper::toEntity).toList();
    }

    @Transactional(readOnly=true)
    public Subscription get(UUID organizationId,UUID subscriptionId) {
        return subscriptionRepository.findByIdAndOrganization_Id(subscriptionId,organizationId)
                .map(SubscriptionMapper::toEntity)
                .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Subscription not found"));
    }

    public EntitlementSnapshot getEntitlements(UUID organizationId, ProductCode product) {
        OrganizationDao organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found"));
        SubscriptionDao subscription = subscriptionRepository
                .findTopByOrganization_IdAndProductOrderByCreatedAtDesc(organizationId, product)
                .orElse(null);

        if (subscription == null) {
            return EntitlementSnapshot.inactive(organizationId, product, "INACTIVE");
        }

        Instant now = Instant.now();
        String status = subscription.getStatus().name();
        boolean active = organization.isActive()
                && subscription.getStatus() != SubscriptionStatus.CANCELLED
                && subscription.getStatus() != SubscriptionStatus.SUSPENDED;
        if (!organization.isActive()) {
            status = "INACTIVE";
        } else if (active && subscription.getStartsAt().isAfter(now)) {
            status = "NOT_STARTED";
            active = false;
        }
        // NOTE: endsAt past এলেও active থাকে — billing আলাদা, access বন্ধ হয় না

        PackageDao packageDao = subscription.getPackageDao();
        var featureData=featureData(packageDao);
        long trialDays = subscription.isTrial() && subscription.getTrialEndDate() != null
                ? Math.max(0, java.time.Duration.between(now, subscription.getTrialEndDate()).toDays() + 1) : 0;
        return new EntitlementSnapshot(
                organizationId,
                packageDao.getId(),
                packageDao.getCode(),
                packageDao.getVersion(),
                product,
                status,
                active,
                subscription.isTrial(),
                active ? trialDays : 0,
                active ? featureData.features() : java.util.Map.of(),
                active ? featureData.limits() : java.util.Map.of()
        );
    }

    public record EntitlementSnapshot(
            UUID organizationId,
            UUID packageId,
            String packageCode,
            int version,
            ProductCode product,
            String subscriptionStatus,
            boolean active,
            boolean isTrial,
            long trialDaysRemaining,
            java.util.Map<String,Object> features,
            java.util.Map<String, Integer> limits
    ) {
        private static EntitlementSnapshot inactive(UUID organizationId, ProductCode product, String status) {
            return new EntitlementSnapshot(
                    organizationId, null, null, 0, product, status, false, false, 0,
                    java.util.Map.of(), java.util.Map.of()
            );
        }
    }

    private FeatureData featureData(PackageDao packageDao) {
        var features=new java.util.LinkedHashMap<String,Object>();
        var limits=new java.util.LinkedHashMap<String,Integer>(packageDao.getLimits());
        packageDao.getFeatures().forEach(code->features.put(code,true));
        jdbc.query("select f.code,f.feature_type,pf.feature_value::text from package_features pf join features f on f.id=pf.feature_id where pf.package_id=? and f.is_active",
                rs->{while(rs.next()) {
                    try {
                        Object value=objectMapper.readValue(rs.getString(3),Object.class);
                        if("NUMBER".equals(rs.getString(2))) limits.put(rs.getString(1),((Number)value).intValue());
                        else features.put(rs.getString(1),value);
                    } catch(java.io.IOException ex){throw new IllegalStateException("Invalid package feature JSON",ex);}
                } return null;},packageDao.getId());
        return new FeatureData(java.util.Map.copyOf(features),java.util.Map.copyOf(limits));
    }
    private record FeatureData(java.util.Map<String,Object> features,java.util.Map<String,Integer> limits) {}
}
