package com.allocator.authservice.service;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.AddressNotFoundException;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CityResponse;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Resolves an IP address to a "City, Country" string using a local MaxMind
 * GeoLite2 City database. The .mmdb file is not shipped with the repo (it's a
 * MaxMind-licensed binary) — see geoip/README.md for how to obtain it. Until
 * it's present, lookups degrade to "Unknown" rather than failing startup.
 */
@Service
@Slf4j
public class GeoIpService {

    @Value("${app.geoip.database-path:geoip/GeoLite2-City.mmdb}")
    private String databasePath;

    private DatabaseReader reader;

    @PostConstruct
    void init() {
        File dbFile = new File(databasePath);
        if (!dbFile.exists()) {
            log.warn("GeoLite2 database not found at '{}' — subscriber location will show as 'Unknown' " +
                    "until it's downloaded from MaxMind and placed there (see geoip/README.md), " +
                    "or app.geoip.database-path / GEOIP_DB_PATH is set to point at it.", databasePath);
            return;
        }
        try {
            reader = new DatabaseReader.Builder(dbFile).build();
            log.info("GeoLite2 database loaded from '{}'", databasePath);
        } catch (IOException e) {
            log.warn("Failed to load GeoLite2 database from '{}': {}", databasePath, e.getMessage());
        }
    }

    /**
     * Best-effort IP -> "City, Country" lookup. Returns "Unknown" for null/blank
     * input, private/loopback addresses, an unloaded database, or any lookup
     * failure — this must never throw, since it sits in the login/session path.
     */
    public String resolveLocation(String ip) {
        if (reader == null || ip == null || ip.isBlank()) {
            return "Unknown";
        }
        try {
            InetAddress address = InetAddress.getByName(ip);
            if (address.isLoopbackAddress() || address.isSiteLocalAddress() || address.isAnyLocalAddress()) {
                return "Unknown";
            }
            CityResponse response = reader.city(address);
            String city = response.getCity().getName();
            String country = response.getCountry().getName();
            if (city != null && country != null) {
                return city + ", " + country;
            }
            return country != null ? country : "Unknown";
        } catch (AddressNotFoundException e) {
            return "Unknown";
        } catch (UnknownHostException | GeoIp2Exception e) {
            log.debug("GeoIP lookup failed for {}: {}", ip, e.getMessage());
            return "Unknown";
        } catch (IOException e) {
            log.debug("GeoIP database read failed for {}: {}", ip, e.getMessage());
            return "Unknown";
        }
    }
}
