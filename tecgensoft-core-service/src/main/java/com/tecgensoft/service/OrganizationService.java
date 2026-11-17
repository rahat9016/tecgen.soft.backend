package com.tecgensoft.service;

import com.tecgensoft.http.dtos.requests.CreateOrganizationRequest;
import com.tecgensoft.http.dtos.requests.UpdateOrganizationRequest;
import com.tecgensoft.common.exceptions.BadRequestException;
import com.tecgensoft.jpa.daos.OrganizationDao;
import com.tecgensoft.jpa.repositories.OrganizationRepository;
import com.tecgensoft.mapper.OrganizationMapper;
import com.tecgensoft.service.entities.Organization;
import com.tecgensoft.service.exceptions.OrganizationNameAlreadyExistsException;
import com.tecgensoft.service.exceptions.OrganizationNotFoundException;
import com.tecgensoft.service.entities.OrganizationAdminCredentials;
import com.tecgensoft.service.entities.CreatedOrganizationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class OrganizationService {
    private final OrganizationRepository organizationRepository;
    private final OrganizationAdminProvisioner organizationAdminProvisioner;

    public CreatedOrganizationResult createOrganization(CreateOrganizationRequest request) {
        String name = request.name().trim();
        if (organizationRepository.existsByName(name)) {
            throw new OrganizationNameAlreadyExistsException(name);
        }
        String code = generateOrganizationCode();
        OrganizationDao saved = organizationRepository.saveAndFlush(new OrganizationDao(
                name, code, request.address(), request.phone(), request.email()
        ));
        Organization organization = OrganizationMapper.toEntity(saved);
        OrganizationAdminCredentials admin = organizationAdminProvisioner.provision(saved);
        return new CreatedOrganizationResult(organization, admin);
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

    public Organization updateOrganization(UUID organizationId, UpdateOrganizationRequest request) {
        OrganizationDao organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new OrganizationNotFoundException(organizationId));

        if (request.name() != null) {
            String name = request.name().trim();
            if (name.isEmpty()) {
                throw new BadRequestException("organizationNameCannotBeBlank");
            }
            if (organizationRepository.existsByNameAndIdNot(name, organizationId)) {
                throw new OrganizationNameAlreadyExistsException(name);
            }
            organization.setName(name);
        }
        if (request.address() != null) organization.setAddress(request.address());
        if (request.phone() != null) organization.setPhone(request.phone());
        if (request.email() != null) organization.setEmail(request.email());
        if (request.isActive() != null) organization.setActive(request.isActive());

        return OrganizationMapper.toEntity(organizationRepository.saveAndFlush(organization));
    }

    private String generateOrganizationCode() {
        String code;
        do {
            code = "ORG_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        } while (organizationRepository.existsByCode(code));
        return code;
    }
}
