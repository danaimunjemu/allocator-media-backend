package com.allocator.authservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GeoIpService")
class GeoIpServiceTest {

    private GeoIpService service;

    @BeforeEach
    void setUp() {
        service = new GeoIpService();
        ReflectionTestUtils.setField(service, "databasePath", "geoip/GeoLite2-City.mmdb");
        ReflectionTestUtils.invokeMethod(service, "init");
    }

    @Test
    @DisplayName("resolves a known public IP to a real city/country")
    void resolvesPublicIp() {
        // Google Public DNS — historically resolves to Mountain View, US in GeoLite2.
        assertThat(service.resolveLocation("8.8.8.8")).isNotEqualTo("Unknown");
    }

    @Test
    @DisplayName("returns Unknown for loopback addresses instead of attempting a lookup")
    void loopbackIsUnknown() {
        assertThat(service.resolveLocation("127.0.0.1")).isEqualTo("Unknown");
        assertThat(service.resolveLocation("0:0:0:0:0:0:0:1")).isEqualTo("Unknown");
    }

    @Test
    @DisplayName("returns Unknown for blank input")
    void blankIsUnknown() {
        assertThat(service.resolveLocation("")).isEqualTo("Unknown");
        assertThat(service.resolveLocation(null)).isEqualTo("Unknown");
    }
}
