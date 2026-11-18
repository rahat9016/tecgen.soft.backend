package com.tecgensoft.http.controllers;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/system/organizations/{organizationId}/domains")
@Tag(name="Organization Domains",description="Register and verify hostnames for tenant resolution")
public class SystemOrganizationDomainController {
  private static final Pattern HOST = Pattern.compile("(?=^.{1,253}$)^[a-z0-9](?:[a-z0-9.-]*[a-z0-9])?$");
  private final JdbcTemplate jdbc;
  public record CreateDomainRequest(@NotBlank String hostname) {}
  public record VerifyDomainRequest(boolean verified) {}
  public record Domain(UUID id, UUID organizationId, String hostname, boolean verified) {}

  @GetMapping
  @PreAuthorize("@permissionAuthorizationService.hasPermission(authentication, 'ORGANIZATION_READ')")
  public List<Domain> list(@PathVariable UUID organizationId) {
    return jdbc.query("select id,organization_id,hostname,verified from organization_domains where organization_id=? order by hostname",
        (rs,n) -> new Domain(rs.getObject(1,UUID.class),rs.getObject(2,UUID.class),rs.getString(3),rs.getBoolean(4)),organizationId);
  }

  @PostMapping
  @PreAuthorize("@permissionAuthorizationService.hasPermission(authentication, 'ORGANIZATION_UPDATE')")
  public Domain create(@PathVariable UUID organizationId, @Valid @RequestBody CreateDomainRequest request) {
    String host = request.hostname().trim().toLowerCase(Locale.ROOT);
    if (!HOST.matcher(host).matches() || host.contains(".."))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid hostname");
    UUID id=UUID.randomUUID();
    try {
      jdbc.update("insert into organization_domains(id,organization_id,hostname,verified) values (?,?,?,false)",id,organizationId,host);
    } catch (org.springframework.dao.DuplicateKeyException ex) {
      throw new ResponseStatusException(HttpStatus.CONFLICT,"Hostname is already registered");
    }
    return new Domain(id,organizationId,host,false);
  }

  @PatchMapping("/{domainId}/verification")
  @PreAuthorize("@permissionAuthorizationService.hasPermission(authentication, 'ORGANIZATION_UPDATE')")
  public Domain verify(@PathVariable UUID organizationId,@PathVariable UUID domainId,
                       @Valid @RequestBody VerifyDomainRequest request) {
    int count=jdbc.update("update organization_domains set verified=? where id=? and organization_id=?",
        request.verified(),domainId,organizationId);
    if (count==0) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Domain not found");
    return jdbc.queryForObject("select id,organization_id,hostname,verified from organization_domains where id=?",
        (rs,n)->new Domain(rs.getObject(1,UUID.class),rs.getObject(2,UUID.class),rs.getString(3),rs.getBoolean(4)),domainId);
  }
}
