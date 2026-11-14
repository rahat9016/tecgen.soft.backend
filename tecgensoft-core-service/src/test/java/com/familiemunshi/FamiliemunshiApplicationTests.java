package com.familiemunshi;

import static org.junit.jupiter.api.Assertions.*;

import com.familiemunshi.common.utils.JwtUtil;
import com.familiemunshi.service.SaasService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class FamiliemunshiApplicationTests {
  @Test
  void tokenPurposesStaySeparate() {
    var jwt = new JwtUtil();
    ReflectionTestUtils.setField(jwt, "secret", "test-only-secret-at-least-thirty-two-characters");
    ReflectionTestUtils.setField(jwt, "tokenExpirationMs", 60000L);
    ReflectionTestUtils.setField(jwt, "refreshExpirationMs", 120000L);
    jwt.init();
    var id = UUID.randomUUID();
    var access = jwt.generateUserToken(id, "a@test.invalid");
    var refresh = jwt.generateUserRefreshToken(id, "a@test.invalid");
    assertTrue(jwt.validateUserToken(access));
    assertFalse(jwt.validateUserToken(refresh));
    assertTrue(jwt.validateUserRefreshToken(refresh));
    assertFalse(jwt.validateUserRefreshToken(access));
    assertFalse(jwt.validateUserToken("invalid"));
    assertEquals(id, jwt.extractUserId(access));
  }

  @Test
  void hostnameNormalization() {
    assertEquals("example.com", SaasService.normalizeHost(" EXAMPLE.COM. "));
    assertThrows(Exception.class, () -> SaasService.normalizeHost("https://example.com"));
    assertThrows(Exception.class, () -> SaasService.normalizeHost("a..com"));
  }
}
