
package com.tecgensoft.common.utils;

import java.text.Normalizer;
import java.util.Locale;

public final class OrganizationAdminGenerator {

    private OrganizationAdminGenerator() {
        throw new IllegalStateException(
                "Utility class cannot be instantiated"
        );
    }

    public static String generateEmail(
            String organizationName,
            String organizationCode,
            String emailDomain
    ) {

        if (organizationName == null || organizationName.isBlank()) {
            throw new IllegalArgumentException(
                    "Organization name is required"
            );
        }

        if (organizationCode == null || organizationCode.isBlank()) {
            throw new IllegalArgumentException(
                    "Organization code is required"
            );
        }

        if (emailDomain == null || emailDomain.isBlank()) {
            throw new IllegalArgumentException(
                    "Email domain is required"
            );
        }

        String slug = Normalizer.normalize(
                        organizationName.trim(),
                        Normalizer.Form.NFKD
                )
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");

        if (slug.isBlank()) {
            slug = "organization";
        }

        if (slug.length() > 45) {
            slug = slug.substring(0, 45)
                    .replaceAll("-+$", "");
        }

        String normalizedCode = organizationCode.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9-]", "");

        String domain = emailDomain.trim()
                .toLowerCase(Locale.ROOT)
                .replaceFirst("^@", "");

        String email = slug + "." + normalizedCode + "@" + domain;

        if (email.length() > 150) {
            throw new IllegalArgumentException(
                    "Generated email exceeds 150 characters"
            );
        }

        return email;
    }
}
