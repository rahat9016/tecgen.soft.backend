package com.tecgensoft.service;

import com.tecgensoft.common.exceptions.BadRequestException;
import com.tecgensoft.common.exceptions.ConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class OrganizationDomainService {
    private static final Pattern HOST =
            Pattern.compile("(?=^.{1,253}$)^[a-z0-9](?:[a-z0-9.-]*[a-z0-9])?$");

    private final JdbcTemplate jdbc;

    @Transactional
    public UUID register(UUID organizationId, String hostname, boolean verified) {
        String host = hostname.trim().toLowerCase(Locale.ROOT);
        if (!HOST.matcher(host).matches() || host.contains("..")) {
            throw new BadRequestException("invalidHostname");
        }
        UUID id = UUID.randomUUID();
        try {
            jdbc.update(
                    "insert into organization_domains(id,organization_id,hostname,verified) values (?,?,?,?)",
                    id, organizationId, host, verified);
        } catch (DuplicateKeyException ex) {
            throw new ConflictException("hostnameAlreadyRegistered", new Object[]{host});
        }
        return id;
    }

    /** The organization and product a verified hostname belongs to. */
    public record DomainTenant(UUID organizationId, String product, boolean active, String name, String logoUrl) {
    }

    @Transactional(readOnly = true)
    public Optional<DomainTenant> findVerified(String hostname) {
        return jdbc.query(
                "select d.organization_id,d.product,o.is_active,o.name,o.logo_url from organization_domains d "
                        + "join organizations o on o.id=d.organization_id "
                        + "where d.hostname=? and d.verified=true",
                (rs, n) -> new DomainTenant(rs.getObject(1, UUID.class), rs.getString(2), rs.getBoolean(3),
                        rs.getString(4), rs.getString(5)),
                hostname.trim().toLowerCase(Locale.ROOT)).stream().findFirst();
    }

    @Transactional(readOnly = true)
    public List<String> hostnames(UUID organizationId) {
        return jdbc.query(
                "select hostname from organization_domains where organization_id=? order by hostname",
                (rs, n) -> rs.getString(1), organizationId);
    }
}
