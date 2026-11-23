package com.tecgensoft.service;

import com.tecgensoft.common.exceptions.BadRequestException;
import com.tecgensoft.common.component.StorageFileSupport;
import com.tecgensoft.common.service.FileStorageService;
import com.tecgensoft.jpa.daos.OrganizationDao;
import com.tecgensoft.jpa.repositories.OrganizationRepository;
import com.tecgensoft.mapper.OrganizationMapper;
import com.tecgensoft.service.entities.Organization;
import com.tecgensoft.service.exceptions.OrganizationNameAlreadyExistsException;
import com.tecgensoft.service.exceptions.OrganizationNotFoundException;
import com.tecgensoft.service.entities.OrganizationAdminCredentials;
import com.tecgensoft.service.entities.CreatedOrganization;
import com.tecgensoft.http.validations.ValidationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class OrganizationService {
    private final OrganizationRepository organizationRepository;
    private final OrganizationAdminProvisioner organizationAdminProvisioner;
    private final FileStorageService fileStorageService;
    private final SubscriptionService subscriptionService;

    public CreatedOrganization createOrganization(
            Organization requestedOrganization,
            MultipartFile logo,
            String adminEmail,
            String adminPassword,
            com.tecgensoft.service.entities.SubscriptionSetup subscriptionSetup
    ) {
        String normalizedName = requestedOrganization.name().trim();
        if (organizationRepository.existsByName(normalizedName)) {
            throw new OrganizationNameAlreadyExistsException(normalizedName);
        }
        Organization organizationToCreate = requestedOrganization
                .withName(normalizedName)
                .withCode(generateOrganizationCode());

        // OPTIONAL LOGO
        if (logo != null && !logo.isEmpty()) {
            ValidationUtil.validateMediaType(logo);
            requireActiveTransaction();

            String filename = fileStorageService.uploadPublicFile(
                    logo,
                    StorageFileSupport.ORGANIZATION_LOGOS
            );

            registerUploadedFileCleanup(filename);
            organizationToCreate = organizationToCreate.withLogoUrl(filename);
        }

        // SAVE ORGANIZATION
        OrganizationDao organizationDao = OrganizationMapper.toDao(organizationToCreate);
        OrganizationDao saved = organizationRepository.saveAndFlush(organizationDao);

        // CREATE ADMIN
        Organization organization = OrganizationMapper.toEntity(saved);
        OrganizationAdminCredentials admin = organizationAdminProvisioner.provision(saved, adminEmail, adminPassword);

        com.tecgensoft.service.entities.Subscription subscription =
                subscriptionService.provisionInitialSubscription(saved, subscriptionSetup);

        return new CreatedOrganization(organization, admin, subscription);
    }

    @Transactional(readOnly = true)
    public List<Organization> getAllOrganizations() {
        return organizationRepository.findAllByOrderByNameAsc().stream()
                .map(OrganizationMapper::toEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public Organization getOrganizationById(UUID organizationId) {
        return organizationRepository.findById(organizationId)
                .map(OrganizationMapper::toEntity)
                .orElseThrow(() -> new OrganizationNotFoundException(organizationId));
    }

    public Organization updateOrganization(
            UUID organizationId,
            Organization requestedOrganization,
            MultipartFile newLogo
    ) {
        // 1. FIND EXISTING ORGANIZATION
        OrganizationDao organizationDao = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new OrganizationNotFoundException(organizationId));
        // 2. CONVERT DAO TO DOMAIN
        Organization existingOrganization = OrganizationMapper.toEntity(organizationDao);

        // 3. VALIDATE AND APPLY PARTIAL UPDATE
        Organization updatedOrganization = existingOrganization;
        if (requestedOrganization.name() != null) {
            updatedOrganization = updatedOrganization.withName(requestedOrganization.name());
        }
        if (requestedOrganization.address() != null) {
            updatedOrganization = updatedOrganization.withAddress(requestedOrganization.address());
        }
        if (requestedOrganization.phone() != null) {
            updatedOrganization = updatedOrganization.withPhone(requestedOrganization.phone());
        }
        if (requestedOrganization.email() != null) {
            updatedOrganization = updatedOrganization.withEmail(requestedOrganization.email());
        }
        if (requestedOrganization.isActive() != null) {
            updatedOrganization = updatedOrganization.withIsActive(requestedOrganization.isActive());
        }

        if (requestedOrganization.name() != null) {
            String normalizedName = updatedOrganization.name().trim();
            if (normalizedName.isEmpty()) {
                throw new BadRequestException("organizationNameCannotBeBlank");
            }
            updatedOrganization = updatedOrganization.withName(normalizedName);
        }

        // 4. VALIDATE UNIQUE NAME
        if (!existingOrganization.name().equals(updatedOrganization.name())
                && organizationRepository.existsByNameAndIdNot(updatedOrganization.name(), organizationId)) {
            throw new OrganizationNameAlreadyExistsException(updatedOrganization.name());
        }

        // 5. HANDLE LOGO REPLACEMENT
        if (newLogo != null && !newLogo.isEmpty()) {
            ValidationUtil.validateMediaType(newLogo);
            if (!TransactionSynchronizationManager.isSynchronizationActive()) {
                throw new IllegalStateException("Active transaction synchronization is required");
            }

            String previousLogo = organizationDao.getLogoUrl();
            String newFilename = fileStorageService.uploadPublicFile(
                    newLogo,
                    StorageFileSupport.ORGANIZATION_LOGOS
            );

            updatedOrganization = updatedOrganization.withLogoUrl(newFilename);
            registerOrganizationLogoReplacementCleanup(previousLogo, newFilename);
        }

        // 6. APPLY UPDATED FIELDS TO MANAGED DAO
        organizationDao.setName(updatedOrganization.name());
        organizationDao.setAddress(updatedOrganization.address());
        organizationDao.setPhone(updatedOrganization.phone());
        organizationDao.setEmail(updatedOrganization.email());
        organizationDao.setActive(updatedOrganization.isActive());
        organizationDao.setLogoUrl(updatedOrganization.logoUrl());

        // 7. SAVE DATABASE CHANGES
        OrganizationDao savedOrganization = organizationRepository.saveAndFlush(organizationDao);

        // 8. RETURN DOMAIN
        return OrganizationMapper.toEntity(savedOrganization);
    }

    private String generateOrganizationCode() {
        String code;
        do {
            code = "ORG_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        } while (organizationRepository.existsByCode(code));
        return code;
    }

    private void registerUploadedFileCleanup(String filename) {
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        if (status == TransactionSynchronization.STATUS_COMMITTED) {
                            return;
                        }

                        try {
                            fileStorageService.deleteFile(filename);
                        } catch (Exception exception) {
                            log.error("Failed to cleanup organization logo: {}", filename, exception);
                        }
                    }
                }
        );
    }

    private void registerOrganizationLogoReplacementCleanup(String previousLogo, String newFilename) {
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        if (previousLogo == null || previousLogo.isBlank() || previousLogo.equals(newFilename)) {
                            return;
                        }
                        try {
                            fileStorageService.deleteFile(previousLogo);
                            log.info("Previous organization logo deleted: {}", previousLogo);
                        } catch (Exception exception) {
                            log.error("Failed to delete previous organization logo: {}", previousLogo, exception);
                        }
                    }

                    @Override
                    public void afterCompletion(int status) {
                        if (status == TransactionSynchronization.STATUS_COMMITTED) {
                            return;
                        }
                        try {
                            fileStorageService.deleteFile(newFilename);
                            log.info("Rolled-back organization logo deleted: {}", newFilename);
                        } catch (Exception exception) {
                            log.error("Failed to clean up uploaded organization logo: {}", newFilename, exception);
                        }
                    }
                }
        );
    }

    private void requireActiveTransaction() {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("Organization logo upload requires an active transaction");
        }
    }
}
