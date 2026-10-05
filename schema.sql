-- ==========================================================
-- StarVoice Lanka - Database Schema DDL (Microsoft SQL Server)
-- Project Code: 2026-Y2-S1-KU-03 (SLIIT SE2030)
-- Target Database: Microsoft SQL Server 2019 / 2022 / Azure SQL
-- ==========================================================

USE master;
GO

IF NOT EXISTS (SELECT name FROM sys.databases WHERE name = 'starvoice_lanka')
BEGIN
    CREATE DATABASE starvoice_lanka;
END
GO

USE starvoice_lanka;
GO

-- ==========================================================
-- MODULE 1: USER MANAGEMENT (UM01 - UM06)
-- ==========================================================

-- 1. USERS TABLE
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='users' AND xtype='U')
BEGIN
    CREATE TABLE users (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        email VARCHAR(150) NOT NULL UNIQUE,
        full_name VARCHAR(120) NOT NULL,
        mobile VARCHAR(30) NOT NULL,
        nic VARCHAR(30) NULL,
        password_hash VARCHAR(255) NOT NULL,
        role VARCHAR(30) NOT NULL DEFAULT 'VOTER', -- VOTER, ADMIN, SPONSOR_MANAGER
        status VARCHAR(30) NOT NULL DEFAULT 'PENDING', -- PENDING, ACTIVE, SUSPENDED
        verification_code VARCHAR(10) NULL,
        verification_expires_at DATETIME2 NULL,
        verified_at DATETIME2 NULL,
        reset_code VARCHAR(10) NULL,
        reset_expires_at DATETIME2 NULL,
        last_login_at DATETIME2 NULL,
        closed_at DATETIME2 NULL,
        anonymised_at DATETIME2 NULL,
        created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
        updated_at DATETIME2 NULL
    );
END
GO

-- 2. AUDIT LOGS TABLE (UM06)
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='audit_logs' AND xtype='U')
BEGIN
    CREATE TABLE audit_logs (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        action VARCHAR(50) NOT NULL, -- STATUS_CHANGED, ROLE_CHANGED, ACCOUNT_CLOSED
        actor_id BIGINT NULL,
        actor_email VARCHAR(150) NOT NULL,
        target_user_id BIGINT NULL,
        target_email VARCHAR(150) NULL,
        state_before VARCHAR(255) NULL,
        state_after VARCHAR(255) NULL,
        reason NVARCHAR(500) NULL,
        ip_address VARCHAR(50) NULL,
        created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
        updated_at DATETIME2 NULL,
        CONSTRAINT fk_audit_actor FOREIGN KEY (actor_id) REFERENCES users(id) ON DELETE NO ACTION,
        CONSTRAINT fk_audit_target FOREIGN KEY (target_user_id) REFERENCES users(id) ON DELETE NO ACTION
    );
END
GO

-- ==========================================================
-- MODULE 2: CONTESTANT & ROUND MANAGEMENT (CM01 - CM06)
-- ==========================================================

-- 3. SEASONS TABLE
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='seasons' AND xtype='U')
BEGIN
    CREATE TABLE seasons (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        name VARCHAR(120) NOT NULL,
        season_year INT NOT NULL,
        is_current BIT NOT NULL DEFAULT 0,
        created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
        updated_at DATETIME2 NULL
    );
END
GO

-- 4. CONTESTANTS TABLE (CM01, CM02, CM06)
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='contestants' AND xtype='U')
BEGIN
    CREATE TABLE contestants (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        season_id BIGINT NOT NULL,
        full_name VARCHAR(120) NOT NULL,
        stage_name VARCHAR(120) NULL,
        age INT NULL,
        district VARCHAR(80) NOT NULL,
        bio NVARCHAR(MAX) NULL,
        photo_url VARCHAR(500) NULL,
        status VARCHAR(30) NOT NULL DEFAULT 'REGISTERED', -- REGISTERED, ACTIVE, ELIMINATED, WITHDRAWN
        total_votes INT NOT NULL DEFAULT 0,
        created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
        updated_at DATETIME2 NULL,
        CONSTRAINT fk_contestant_season FOREIGN KEY (season_id) REFERENCES seasons(id) ON DELETE CASCADE
    );
END
GO

-- 5. ROUNDS TABLE (CM03, CM04, CM05)
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='rounds' AND xtype='U')
BEGIN
    CREATE TABLE rounds (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        season_id BIGINT NOT NULL,
        name VARCHAR(120) NOT NULL,
        sequence INT NOT NULL,
        status VARCHAR(30) NOT NULL DEFAULT 'DRAFT', -- DRAFT, OPEN, CLOSED, RESULTS_PUBLISHED
        opens_at DATETIME2 NULL,
        closes_at DATETIME2 NULL,
        advance_count INT NOT NULL DEFAULT 0,
        created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
        updated_at DATETIME2 NULL,
        CONSTRAINT fk_round_season FOREIGN KEY (season_id) REFERENCES seasons(id) ON DELETE CASCADE
    );
END
GO

-- 6. ROUND ENTRIES TABLE (CM03 - Line-up & CM06 - Performance Media)
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='round_entries' AND xtype='U')
BEGIN
    CREATE TABLE round_entries (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        round_id BIGINT NOT NULL,
        contestant_id BIGINT NOT NULL,
        votes_in_round INT NOT NULL DEFAULT 0,
        position INT NULL,
        outcome VARCHAR(30) NOT NULL DEFAULT 'PENDING', -- PENDING, ADVANCED, ELIMINATED
        performance_title NVARCHAR(200) NULL,
        media_url VARCHAR(500) NULL,
        media_mime VARCHAR(100) NULL,
        media_size_bytes BIGINT NULL,
        media_original_name NVARCHAR(255) NULL,
        media_uploaded_at DATETIME2 NULL,
        created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
        updated_at DATETIME2 NULL,
        CONSTRAINT uq_round_contestant UNIQUE (round_id, contestant_id),
        CONSTRAINT fk_entry_round FOREIGN KEY (round_id) REFERENCES rounds(id) ON DELETE CASCADE,
        CONSTRAINT fk_entry_contestant FOREIGN KEY (contestant_id) REFERENCES contestants(id) ON DELETE NO ACTION
    );
END
GO

-- ==========================================================
-- MODULE 3: VOTING MANAGEMENT (VM01 - VM06)
-- ==========================================================

-- 7. VOTES TABLE (VM01 - Cast, VM05 - Ledger & Void, VM06 - Fraud Detection)
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='votes' AND xtype='U')
BEGIN
    CREATE TABLE votes (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        voter_id BIGINT NOT NULL,
        round_id BIGINT NOT NULL,
        contestant_id BIGINT NOT NULL,
        type VARCHAR(20) NOT NULL, -- FREE, PAID
        count INT NOT NULL DEFAULT 1,
        credits_deducted INT NOT NULL DEFAULT 0,
        is_void BIT NOT NULL DEFAULT 0,
        ip_address VARCHAR(50) NULL,
        user_agent VARCHAR(500) NULL,
        created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
        updated_at DATETIME2 NULL,
        CONSTRAINT fk_vote_voter FOREIGN KEY (voter_id) REFERENCES users(id) ON DELETE NO ACTION,
        CONSTRAINT fk_vote_round FOREIGN KEY (round_id) REFERENCES rounds(id) ON DELETE NO ACTION,
        CONSTRAINT fk_vote_contestant FOREIGN KEY (contestant_id) REFERENCES contestants(id) ON DELETE NO ACTION
    );
END
GO

-- 8. VOTE QUOTAS TABLE (VM02 - Free / Paid Split)
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='vote_quotas' AND xtype='U')
BEGIN
    CREATE TABLE vote_quotas (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        user_id BIGINT NOT NULL,
        round_id BIGINT NOT NULL,
        free_votes_used INT NOT NULL DEFAULT 0,
        paid_votes_used INT NOT NULL DEFAULT 0,
        created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
        updated_at DATETIME2 NULL,
        CONSTRAINT uq_quota_user_round UNIQUE (user_id, round_id),
        CONSTRAINT fk_quota_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
        CONSTRAINT fk_quota_round FOREIGN KEY (round_id) REFERENCES rounds(id) ON DELETE CASCADE
    );
END
GO

-- ==========================================================
-- MODULE 4: NOTIFICATION MANAGEMENT (NM01 - NM06)
-- ==========================================================

-- 9. NOTIFICATIONS TABLE (NM01 - Dispatch, NM03 - In-App, NM06 - Retry Engine)
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='notifications' AND xtype='U')
BEGIN
    CREATE TABLE notifications (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        user_id BIGINT NULL,
        template VARCHAR(50) NOT NULL, -- ACCOUNT_VERIFICATION, PASSWORD_RESET, VOTE_CONFIRMATION, ROUND_OPENED, ELIMINATION_NOTICE, RECEIPT_ISSUED, FRAUD_ALERT, SPONSOR_INVOICE, AGREEMENT_RENEWED
        channel VARCHAR(20) NOT NULL, -- EMAIL, SMS, IN_APP
        subject NVARCHAR(255) NOT NULL,
        body NVARCHAR(MAX) NOT NULL,
        is_broadcast BIT NOT NULL DEFAULT 0,
        status VARCHAR(20) NOT NULL DEFAULT 'QUEUED', -- QUEUED, SENT, FAILED
        attempts INT NOT NULL DEFAULT 0,
        next_attempt_at DATETIME2 NULL,
        sent_at DATETIME2 NULL,
        read_at DATETIME2 NULL,
        abandoned_at DATETIME2 NULL,
        last_error NVARCHAR(MAX) NULL,
        created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
        updated_at DATETIME2 NULL,
        CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
    );
END
GO

-- 10. NOTIFICATION PREFERENCES TABLE (NM04)
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='notification_preferences' AND xtype='U')
BEGIN
    CREATE TABLE notification_preferences (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        user_id BIGINT NOT NULL UNIQUE,
        email BIT NOT NULL DEFAULT 1,
        sms BIT NOT NULL DEFAULT 1,
        in_app BIT NOT NULL DEFAULT 1,
        announcements BIT NOT NULL DEFAULT 1,
        created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
        updated_at DATETIME2 NULL,
        CONSTRAINT fk_pref_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    );
END
GO

-- ==========================================================
-- MODULE 5: PAYMENT & VOTE CREDITS (PM01 - PM06)
-- ==========================================================

-- 11. VOTE BUNDLES TABLE (PM01)
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='vote_bundles' AND xtype='U')
BEGIN
    CREATE TABLE vote_bundles (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        code VARCHAR(50) NOT NULL UNIQUE, -- STARTER, FAN, SUPER_FAN
        name VARCHAR(100) NOT NULL,
        credits INT NOT NULL,
        price_lkr DECIMAL(12,2) NOT NULL,
        is_active BIT NOT NULL DEFAULT 1,
        created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
        updated_at DATETIME2 NULL
    );
END
GO

-- 12. CREDIT ACCOUNTS (WALLET) TABLE (PM03)
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='credit_accounts' AND xtype='U')
BEGIN
    CREATE TABLE credit_accounts (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        user_id BIGINT NOT NULL UNIQUE,
        balance INT NOT NULL DEFAULT 0,
        lifetime_purchased INT NOT NULL DEFAULT 0,
        lifetime_spent INT NOT NULL DEFAULT 0,
        created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
        updated_at DATETIME2 NULL,
        CONSTRAINT fk_wallet_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    );
END
GO

-- 13. PAYMENTS & TRANSACTIONS TABLE (PM02, PM04 - Refund, PM05 - Receipt, PM06 - Reconciliation)
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='payments' AND xtype='U')
BEGIN
    CREATE TABLE payments (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        user_id BIGINT NOT NULL,
        bundle_id BIGINT NOT NULL,
        receipt_no VARCHAR(50) NOT NULL UNIQUE,
        reference VARCHAR(100) NOT NULL UNIQUE,
        amount_lkr DECIMAL(12,2) NOT NULL,
        vote_credits INT NOT NULL,
        method VARCHAR(30) NOT NULL DEFAULT 'CARD', -- CARD, ONLINE_WALLET, BANK_TRANSFER
        status VARCHAR(30) NOT NULL DEFAULT 'PENDING', -- PENDING, SUCCESS, FAILED, REFUNDED
        paid_at DATETIME2 NULL,
        refunded_at DATETIME2 NULL,
        refund_reason NVARCHAR(255) NULL,
        failure_reason NVARCHAR(255) NULL,
        created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
        updated_at DATETIME2 NULL,
        CONSTRAINT fk_payment_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE NO ACTION,
        CONSTRAINT fk_payment_bundle FOREIGN KEY (bundle_id) REFERENCES vote_bundles(id) ON DELETE NO ACTION
    );
END
GO

-- ==========================================================
-- MODULE 6: SPONSOR & CAMPAIGN MANAGEMENT (SM01 - SM06)
-- ==========================================================

-- 14. SPONSORS TABLE (SM01)
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='sponsors' AND xtype='U')
BEGIN
    CREATE TABLE sponsors (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        company_name NVARCHAR(150) NOT NULL,
        contact_name NVARCHAR(100) NOT NULL,
        contact_email VARCHAR(150) NOT NULL,
        contact_phone VARCHAR(50) NOT NULL,
        logo_url VARCHAR(500) NULL,
        website_url VARCHAR(500) NULL,
        status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, INACTIVE
        created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
        updated_at DATETIME2 NULL
    );
END
GO

-- 15. SPONSORSHIP PACKAGES TABLE (SM02)
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='sponsorship_packages' AND xtype='U')
BEGIN
    CREATE TABLE sponsorship_packages (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        name NVARCHAR(100) NOT NULL,
        tier VARCHAR(30) NOT NULL, -- TITLE, POWERED_BY, ASSOCIATE, SEGMENT
        price_lkr DECIMAL(12,2) NOT NULL,
        max_impressions INT NOT NULL DEFAULT 100000,
        banner_placement VARCHAR(30) NOT NULL DEFAULT 'LEADERBOARD', -- TOP, SIDEBAR, LEADERBOARD, FOOTER
        is_active BIT NOT NULL DEFAULT 1,
        created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
        updated_at DATETIME2 NULL
    );
END
GO

-- 16. SPONSORSHIP AGREEMENTS TABLE (SM03 - Agreement, SM06 - Expiry / Renewal)
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='sponsorship_agreements' AND xtype='U')
BEGIN
    CREATE TABLE sponsorship_agreements (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        sponsor_id BIGINT NOT NULL,
        package_id BIGINT NOT NULL,
        season_id BIGINT NULL,
        round_id BIGINT NULL,
        banner_url VARCHAR(500) NOT NULL,
        click_url VARCHAR(500) NOT NULL,
        impressions_delivered INT NOT NULL DEFAULT 0,
        clicks_recorded INT NOT NULL DEFAULT 0,
        status VARCHAR(30) NOT NULL DEFAULT 'DRAFT', -- DRAFT, ACTIVE, EXPIRED, TERMINATED
        start_date DATE NOT NULL,
        end_date DATE NOT NULL,
        created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
        updated_at DATETIME2 NULL,
        CONSTRAINT fk_agreement_sponsor FOREIGN KEY (sponsor_id) REFERENCES sponsors(id) ON DELETE CASCADE,
        CONSTRAINT fk_agreement_package FOREIGN KEY (package_id) REFERENCES sponsorship_packages(id) ON DELETE CASCADE,
        CONSTRAINT fk_agreement_season FOREIGN KEY (season_id) REFERENCES seasons(id) ON DELETE NO ACTION,
        CONSTRAINT fk_agreement_round FOREIGN KEY (round_id) REFERENCES rounds(id) ON DELETE NO ACTION
    );
END
GO

-- 17. SPONSOR IMPRESSIONS TABLE (SM04 - Delivery & Click Tracking)
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='sponsor_impressions' AND xtype='U')
BEGIN
    CREATE TABLE sponsor_impressions (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        agreement_id BIGINT NOT NULL,
        round_id BIGINT NULL,
        placement VARCHAR(30) NOT NULL, -- TOP, SIDEBAR, LEADERBOARD, FOOTER
        ip_address VARCHAR(50) NULL,
        user_agent VARCHAR(500) NULL,
        created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
        updated_at DATETIME2 NULL,
        CONSTRAINT fk_impression_agreement FOREIGN KEY (agreement_id) REFERENCES sponsorship_agreements(id) ON DELETE CASCADE,
        CONSTRAINT fk_impression_round FOREIGN KEY (round_id) REFERENCES rounds(id) ON DELETE NO ACTION
    );
END
GO

-- 18. SPONSOR INVOICES TABLE (SM05)
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='sponsor_invoices' AND xtype='U')
BEGIN
    CREATE TABLE sponsor_invoices (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        agreement_id BIGINT NOT NULL,
        invoice_number VARCHAR(50) NOT NULL UNIQUE,
        amount_lkr DECIMAL(12,2) NOT NULL,
        status VARCHAR(30) NOT NULL DEFAULT 'ISSUED', -- ISSUED, PAID, CANCELLED, OVERDUE
        issued_at DATETIME2 NOT NULL,
        due_date DATE NOT NULL,
        paid_at DATETIME2 NULL,
        payment_reference NVARCHAR(100) NULL,
        created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
        updated_at DATETIME2 NULL,
        CONSTRAINT fk_invoice_agreement FOREIGN KEY (agreement_id) REFERENCES sponsorship_agreements(id) ON DELETE CASCADE
    );
END
GO

-- ==========================================================
-- INDEXES FOR HIGH-TRAFFIC VOTING & LEADERBOARDS
-- ==========================================================
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_votes_round' AND object_id = OBJECT_ID('votes'))
BEGIN
    CREATE NONCLUSTERED INDEX idx_votes_round ON votes (round_id, contestant_id, is_void);
END
GO

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_round_entries_round' AND object_id = OBJECT_ID('round_entries'))
BEGIN
    CREATE NONCLUSTERED INDEX idx_round_entries_round ON round_entries (round_id, votes_in_round DESC);
END
GO

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_notifications_retry' AND object_id = OBJECT_ID('notifications'))
BEGIN
    CREATE NONCLUSTERED INDEX idx_notifications_retry ON notifications (status, next_attempt_at) WHERE status = 'FAILED';
END
GO

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_sponsor_impressions_agreement' AND object_id = OBJECT_ID('sponsor_impressions'))
BEGIN
    CREATE NONCLUSTERED INDEX idx_sponsor_impressions_agreement ON sponsor_impressions (agreement_id);
END
GO

PRINT 'StarVoice Lanka database schema created successfully.';
GO

