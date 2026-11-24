package com.tecgensoft.turf.common;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Finds the site hostname a public request was made for. The gateway replaces any client supplied
 * X-Tenant-Host with the value it verified; direct calls fall back to the request host. The result
 * only selects an organization and never grants access to protected data.
 */
@Component
public class TenantHostResolver {
  public static final String HEADER = "X-Tenant-Host";
  private static final Pattern HOST =
      Pattern.compile("(?=^.{1,253}$)^[a-z0-9](?:[a-z0-9.-]*[a-z0-9])?$");

  public String resolve(HttpServletRequest request) {
    String header = request.getHeader(HEADER);
    String host = header == null || header.isBlank() ? request.getServerName() : header;
    host = host.strip().toLowerCase(Locale.ROOT);
    int port = host.indexOf(':');
    if (port >= 0) host = host.substring(0, port);
    if (!HOST.matcher(host).matches() || host.contains(".."))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid tenant host");
    return host;
  }
}
