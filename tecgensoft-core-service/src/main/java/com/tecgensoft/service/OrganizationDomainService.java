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

    @Transactional(readOnly = true)
    public List<String> hostnames(UUID organizationId) {
        return jdbc.query(
                "select hostname from organization_domains where organization_id=? order by hostname",
                (rs, n) -> rs.getString(1), organizationId);
    }
}
