-- ============================================================
-- URL Shortener — Complete Database Schema
-- PostgreSQL 15+
-- ============================================================

-- ============================================================
-- EXTENSIONS
-- ============================================================
CREATE EXTENSION IF NOT EXISTS "pgcrypto";   -- gen_random_bytes(), crypt()
CREATE EXTENSION IF NOT EXISTS "pg_trgm";    -- trigram index trên search URL
CREATE EXTENSION IF NOT EXISTS "btree_gist"; -- GiST index cho exclusion constraint

-- ============================================================
-- TYPES
-- ============================================================
CREATE TYPE device_type_enum AS ENUM ('desktop', 'mobile', 'tablet', 'bot', 'unknown');
CREATE TYPE plan_type_enum   AS ENUM ('free', 'pro', 'team', 'enterprise');

-- ============================================================
-- 1. USERS
-- ============================================================
CREATE TABLE users (
                       id              BIGINT          PRIMARY KEY,          -- Snowflake ID (ứng dụng tạo)
                       email           VARCHAR(255)    NOT NULL UNIQUE,
                       password_hash   TEXT            NOT NULL,
                       display_name    VARCHAR(100),
                       plan_type       plan_type_enum  NOT NULL DEFAULT 'free',
                       is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
                       email_verified  BOOLEAN         NOT NULL DEFAULT FALSE,
                       created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
                       updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users(email);

-- ============================================================
-- 2. API KEYS
-- ============================================================
CREATE TABLE api_keys (
                          id                  BIGINT      PRIMARY KEY,
                          user_id             BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                          key_hash            TEXT        NOT NULL UNIQUE,  -- SHA-256 của raw key
                          name                VARCHAR(100),
                          rate_limit_per_hour BIGINT      NOT NULL DEFAULT 1000,
                          is_active           BOOLEAN     NOT NULL DEFAULT TRUE,
                          expires_at          TIMESTAMPTZ,
                          last_used_at        TIMESTAMPTZ,
                          created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_api_keys_user_id ON api_keys(user_id);
CREATE INDEX idx_api_keys_key_hash ON api_keys(key_hash);

-- ============================================================
-- 3. FOLDERS (Nhóm URL)
-- ============================================================
CREATE TABLE folders (
                         id          BIGINT      PRIMARY KEY,
                         user_id     BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                         name        VARCHAR(100) NOT NULL,
                         created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                         UNIQUE (user_id, name)
);

CREATE INDEX idx_folders_user_id ON folders(user_id);

-- ============================================================
-- 4. TAGS
-- ============================================================
CREATE TABLE tags (
                      id          BIGINT      PRIMARY KEY,
                      user_id     BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                      name        VARCHAR(64) NOT NULL,
                      color       VARCHAR(7)  DEFAULT '#6366f1',   -- hex color
                      created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                      UNIQUE (user_id, name)
);

CREATE INDEX idx_tags_user_id ON tags(user_id);

-- ============================================================
-- 5. SHORT_URLS
-- ============================================================
CREATE TABLE short_urls (
                            id                  BIGINT          PRIMARY KEY,   -- Snowflake ID
                            short_code          VARCHAR(16)     NOT NULL UNIQUE,
                            original_url        TEXT            NOT NULL,
                            user_id             BIGINT          REFERENCES users(id) ON DELETE SET NULL,
                            folder_id           BIGINT          REFERENCES folders(id) ON DELETE SET NULL,

                            title               VARCHAR(255),
                            preview_title       TEXT,
                            preview_image       TEXT,

                            custom_alias        BOOLEAN         NOT NULL DEFAULT FALSE,
                            is_active           BOOLEAN         NOT NULL DEFAULT TRUE,

    -- Password protection
                            password_protected  BOOLEAN         NOT NULL DEFAULT FALSE,
                            password_hash       TEXT,

    -- UTM mặc định tự động append
                            utm_source          VARCHAR(255),
                            utm_medium          VARCHAR(255),
                            utm_campaign        VARCHAR(255),

                            click_count         BIGINT          NOT NULL DEFAULT 0,  -- denormalized, cập nhật async

                            created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
                            updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
                            expires_at          TIMESTAMPTZ,

                            CONSTRAINT chk_password CHECK (
                                (password_protected = FALSE AND password_hash IS NULL) OR
                                (password_protected = TRUE  AND password_hash IS NOT NULL)
                                )
);

-- Index chính cho redirect (hot path)
CREATE INDEX idx_redirect_lookup ON short_urls(short_code)
    WHERE is_active = TRUE;

-- Index cho cleanup job (tìm link hết hạn)
CREATE INDEX idx_expires_at ON short_urls(expires_at)
    WHERE expires_at IS NOT NULL AND is_active = TRUE;

-- Index cho user dashboard
CREATE INDEX idx_short_urls_user_id  ON short_urls(user_id, created_at DESC);
CREATE INDEX idx_short_urls_folder   ON short_urls(folder_id) WHERE folder_id IS NOT NULL;

-- Full-text search trên URL + title
CREATE INDEX idx_short_urls_search ON short_urls
    USING gin((to_tsvector('english', COALESCE(title,'') || ' ' || original_url)));

-- ============================================================
-- 6. URL_TAGS (many-to-many)
-- ============================================================
CREATE TABLE url_tags (
                          short_url_id    BIGINT  NOT NULL REFERENCES short_urls(id) ON DELETE CASCADE,
                          tag_id          BIGINT  NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
                          PRIMARY KEY (short_url_id, tag_id)
);

CREATE INDEX idx_url_tags_tag_id ON url_tags(tag_id);

-- ============================================================
-- 7. CLICK_EVENTS (partitioned by month)
-- ============================================================
CREATE TABLE click_events (
                              id              BIGSERIAL,
                              clicked_at      TIMESTAMPTZ         NOT NULL DEFAULT NOW(),
                              short_url_id    BIGINT              NOT NULL,
                              session_id      UUID,               -- anonymous session tracking

    -- Network
                              ip_address      INET,
                              ip_country      VARCHAR(8),
                              ip_city         VARCHAR(100),
                              ip_latitude     DECIMAL(9,6),
                              ip_longitude    DECIMAL(9,6),

    -- HTTP headers
                              user_agent      TEXT,
                              referer         TEXT,
                              referer_domain  VARCHAR(255),        -- extracted từ referer

    -- Parsed từ user_agent
                              device_type     device_type_enum    NOT NULL DEFAULT 'unknown',
                              browser         VARCHAR(64),
                              browser_version VARCHAR(32),
                              os              VARCHAR(64),
                              os_version      VARCHAR(32),

                              PRIMARY KEY (id, clicked_at)
) PARTITION BY RANGE (clicked_at);

-- Tạo partition theo tháng (script automation hoặc pg_partman)
CREATE TABLE click_events_2025_01 PARTITION OF click_events
    FOR VALUES FROM ('2025-01-01') TO ('2025-02-01');

CREATE TABLE click_events_2025_02 PARTITION OF click_events
    FOR VALUES FROM ('2025-02-01') TO ('2025-03-01');

-- Index trên mỗi partition (tự động kế thừa từ bảng cha khi dùng pg_partman)
CREATE INDEX idx_click_events_url_id   ON click_events(short_url_id, clicked_at DESC);
CREATE INDEX idx_click_events_country  ON click_events(ip_country, clicked_at);
CREATE INDEX idx_click_events_device   ON click_events(device_type, clicked_at);

-- ============================================================
-- 8. DAILY_STATS (pre-aggregated, cập nhật bởi background job)
-- ============================================================
CREATE TABLE daily_stats (
                             short_url_id        BIGINT  NOT NULL REFERENCES short_urls(id) ON DELETE CASCADE,
                             stat_date           DATE    NOT NULL,

                             click_count         BIGINT  NOT NULL DEFAULT 0,
                             unique_ips          BIGINT  NOT NULL DEFAULT 0,

    -- Phân tách theo device
                             desktop_clicks      BIGINT  NOT NULL DEFAULT 0,
                             mobile_clicks       BIGINT  NOT NULL DEFAULT 0,
                             tablet_clicks       BIGINT  NOT NULL DEFAULT 0,

    -- Top country (JSON {"VN": 120, "US": 80})
                             country_breakdown   JSONB,
                             referer_breakdown   JSONB,

                             PRIMARY KEY (short_url_id, stat_date)
);

CREATE INDEX idx_daily_stats_date ON daily_stats(stat_date);

-- ============================================================
-- 9. RESERVED_ALIASES
-- ============================================================
CREATE TABLE reserved_aliases (
                                  alias       VARCHAR(64)     PRIMARY KEY,
                                  reason      VARCHAR(255),
                                  created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- Seed data mẫu
INSERT INTO reserved_aliases (alias, reason) VALUES
                                                 ('admin',    'System reserved'),
                                                 ('api',      'System reserved'),
                                                 ('app',      'System reserved'),
                                                 ('login',    'System reserved'),
                                                 ('logout',   'System reserved'),
                                                 ('signup',   'System reserved'),
                                                 ('dashboard','System reserved'),
                                                 ('help',     'System reserved'),
                                                 ('www',      'System reserved'),
                                                 ('mail',     'System reserved');

-- ============================================================
-- 10. BLOCKED_DOMAINS
-- ============================================================
CREATE TABLE blocked_domains (
                                 domain      VARCHAR(255)    PRIMARY KEY,
                                 reason      TEXT,
                                 created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_blocked_domains_created ON blocked_domains(created_at);

-- ============================================================
-- 11. RATE_LIMIT_COUNTERS (Redis preferred, fallback DB)
-- ============================================================
CREATE TABLE rate_limit_counters (
                                     key             VARCHAR(255)    PRIMARY KEY,   -- e.g. "ip:1.2.3.4:create" hoặc "user:123:create"
                                     count           BIGINT          NOT NULL DEFAULT 1,
                                     window_start    TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
                                     expires_at      TIMESTAMPTZ     NOT NULL
);

CREATE INDEX idx_rate_limit_expires ON rate_limit_counters(expires_at);

-- ============================================================
-- FUNCTIONS & TRIGGERS
-- ============================================================

-- Tự động cập nhật updated_at
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    NEW.updated_at = NOW();
RETURN NEW;
END;
$$;

CREATE TRIGGER trg_short_urls_updated_at
    BEFORE UPDATE ON short_urls
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- Validate không rút ngắn URL của blocked domain
CREATE OR REPLACE FUNCTION validate_url_domain()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE
v_domain TEXT;
BEGIN
    -- Extract domain từ URL
    v_domain := lower(
        regexp_replace(NEW.original_url, '^https?://([^/]+).*', '\1')
    );
    -- Bỏ www.
    v_domain := regexp_replace(v_domain, '^www\.', '');

    IF EXISTS (SELECT 1 FROM blocked_domains WHERE domain = v_domain) THEN
        RAISE EXCEPTION 'Domain % is blocked', v_domain;
END IF;

    -- Kiểm tra short_code không phải reserved alias
    IF EXISTS (SELECT 1 FROM reserved_aliases WHERE alias = NEW.short_code) THEN
        RAISE EXCEPTION 'Short code % is reserved', NEW.short_code;
END IF;

RETURN NEW;
END;
$$;

CREATE TRIGGER trg_validate_short_url
    BEFORE INSERT ON short_urls
    FOR EACH ROW EXECUTE FUNCTION validate_url_domain();

-- Hàm increment click_count bất đồng bộ (gọi từ background job)
CREATE OR REPLACE FUNCTION increment_click_count(p_short_url_id BIGINT, p_count BIGINT DEFAULT 1)
RETURNS VOID LANGUAGE plpgsql AS $$
BEGIN
UPDATE short_urls
SET click_count = click_count + p_count
WHERE id = p_short_url_id;
END;
$$;

-- ============================================================
-- VIEWS
-- ============================================================

-- View redirect nhanh (chỉ các cột cần thiết)
CREATE VIEW active_short_urls AS
SELECT
    short_code,
    original_url,
    password_protected,
    password_hash,
    expires_at,
    utm_source, utm_medium, utm_campaign
FROM short_urls
WHERE is_active = TRUE
  AND (expires_at IS NULL OR expires_at > NOW());

-- View thống kê tổng hợp của user
CREATE VIEW user_url_stats AS
SELECT
    su.user_id,
    COUNT(*)                                    AS total_links,
    SUM(su.click_count)                         AS total_clicks,
    COUNT(*) FILTER (WHERE su.is_active = TRUE) AS active_links,
        MAX(su.created_at)                          AS last_created_at
FROM short_urls su
WHERE su.user_id IS NOT NULL
GROUP BY su.user_id;

-- ============================================================
-- ROW LEVEL SECURITY (RLS)
-- ============================================================
ALTER TABLE short_urls    ENABLE ROW LEVEL SECURITY;
ALTER TABLE folders       ENABLE ROW LEVEL SECURITY;
ALTER TABLE tags          ENABLE ROW LEVEL SECURITY;
ALTER TABLE api_keys      ENABLE ROW LEVEL SECURITY;
ALTER TABLE daily_stats   ENABLE ROW LEVEL SECURITY;

-- Policy: user chỉ thấy link của mình
CREATE POLICY policy_short_urls_owner ON short_urls
    USING (user_id = current_setting('app.current_user_id', TRUE)::BIGINT);

CREATE POLICY policy_folders_owner ON folders
    USING (user_id = current_setting('app.current_user_id', TRUE)::BIGINT);

CREATE POLICY policy_tags_owner ON tags
    USING (user_id = current_setting('app.current_user_id', TRUE)::BIGINT);

CREATE POLICY policy_api_keys_owner ON api_keys
    USING (user_id = current_setting('app.current_user_id', TRUE)::BIGINT);

-- ============================================================
-- MAINTENANCE / HOUSEKEEPING
-- ============================================================

-- Job: xoá rate limit counters hết hạn (chạy mỗi 5 phút)
-- DELETE FROM rate_limit_counters WHERE expires_at < NOW();

-- Job: deactivate expired URLs (chạy mỗi giờ)
-- UPDATE short_urls SET is_active = FALSE
-- WHERE is_active = TRUE AND expires_at IS NOT NULL AND expires_at < NOW();

-- Job: aggregate daily_stats (chạy lúc 00:05 mỗi ngày)
-- INSERT INTO daily_stats (short_url_id, stat_date, click_count, unique_ips, ...)
-- SELECT short_url_id, DATE(clicked_at), COUNT(*), COUNT(DISTINCT ip_address), ...
-- FROM click_events
-- WHERE clicked_at >= CURRENT_DATE - INTERVAL '1 day'
--   AND clicked_at <  CURRENT_DATE
-- GROUP BY short_url_id, DATE(clicked_at)
-- ON CONFLICT (short_url_id, stat_date) DO UPDATE SET ...;

DROP TABLE IF EXISTS worker_node;

CREATE TABLE worker_node
(
    id BIGSERIAL PRIMARY KEY,

    host_name VARCHAR(64) NOT NULL,

    port VARCHAR(64) NOT NULL,

    type INT NOT NULL,

    launch_date DATE NOT NULL,

    modified TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);