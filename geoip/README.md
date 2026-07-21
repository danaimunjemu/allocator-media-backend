# GeoLite2 database

`GeoIpService` resolves subscriber IP addresses to a "City, Country" string
for `user_sessions.location` / `login_history.location`. It reads a local
MaxMind GeoLite2 City database file — MaxMind's license doesn't allow
redistributing it, so it isn't committed to the repo and must be downloaded
once per environment.

## Setup (free, ~2 minutes)

1. Create a free MaxMind account: https://www.maxmind.com/en/geolite2/signup
2. Once logged in, go to **Account > Manage License Keys** and generate a key.
3. Download `GeoLite2-City.mmdb` (either via the download link under
   **Account > Downloads**, or with the license key via MaxMind's `geoipupdate`
   tool).
4. Place the file at `services/allocator-backend/geoip/GeoLite2-City.mmdb`
   (this exact path is the default — override it with the `GEOIP_DB_PATH`
   env var or `app.geoip.database-path` if you'd rather store it elsewhere).

Until the file is present, `GeoIpService` logs a warning at startup and every
lookup returns `"Unknown"` — the app runs fine without it, location just
won't resolve.

## Updating

GeoLite2 databases are refreshed by MaxMind roughly weekly. For anything
beyond local dev, run MaxMind's `geoipupdate` on a schedule (cron / systemd
timer) pointed at this directory rather than re-downloading by hand.
