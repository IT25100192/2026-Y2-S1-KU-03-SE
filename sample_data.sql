-- ==========================================================
-- StarVoice Lanka - Sample Data (Part C: Insert Sample Data)
-- Target Database: Microsoft SQL Server
-- Run this AFTER schema.sql has created all tables.
-- Inserted in FK-safe order, grouped by module to match schema.sql.
-- ==========================================================

USE starvoice_lanka;
GO

-- ==========================================================
-- MODULE 1: USER MANAGEMENT
-- ==========================================================
-- 1. USERS
INSERT INTO users (email, full_name, mobile, nic, password_hash, role, status, verified_at) VALUES
('admin@starvoicelanka.lk', 'Sanduni Perera', '0771234567', '199512345678', 'HASHED_PW_1', 'ADMIN', 'ACTIVE', SYSUTCDATETIME()),
('sponsor.manager@starvoicelanka.lk', 'Ruwan Fernando', '0772345678', '198834567891', 'HASHED_PW_2', 'SPONSOR_MANAGER', 'ACTIVE', SYSUTCDATETIME()),
('kasun.silva@gmail.com', 'Kasun Silva', '0773456789', '199245671234', 'HASHED_PW_3', 'VOTER', 'ACTIVE', SYSUTCDATETIME()),
('nimasha.jay@gmail.com', 'Nimasha Jayawardena', '0774567890', '199667891234', 'HASHED_PW_4', 'VOTER', 'ACTIVE', SYSUTCDATETIME()),
('tharindu.perera@gmail.com', 'Tharindu Perera', '0775678901', '199778912345', 'HASHED_PW_5', 'VOTER', 'PENDING', NULL),
('dilani.wickrama@gmail.com', 'Dilani Wickramasinghe', '0776789012', '199889123456', 'HASHED_PW_6', 'VOTER', 'ACTIVE', SYSUTCDATETIME());
GO

SELECT * FROM users;
GO
-- 2. AUDIT LOGS
INSERT INTO audit_logs (action, actor_id, actor_email, target_user_id, target_email, state_before, state_after, reason, ip_address) VALUES
('STATUS_CHANGED', 1, 'admin@starvoicelanka.lk', 5, 'tharindu.perera@gmail.com', 'PENDING', 'ACTIVE', 'Email verification completed', '192.168.1.10'),
('ROLE_CHANGED', 1, 'admin@starvoicelanka.lk', 2, 'sponsor.manager@starvoicelanka.lk', 'VOTER', 'SPONSOR_MANAGER', 'Promoted to manage sponsor accounts', '192.168.1.10'),
('STATUS_CHANGED', 1, 'admin@starvoicelanka.lk', 3, 'kasun.silva@gmail.com', 'PENDING', 'ACTIVE', 'Email verification completed', '192.168.1.10'),
('STATUS_CHANGED', 1, 'admin@starvoicelanka.lk', 4, 'nimasha.jay@gmail.com', 'PENDING', 'ACTIVE', 'Email verification completed', '192.168.1.11'),
('STATUS_CHANGED', 1, 'admin@starvoicelanka.lk', 6, 'dilani.wickrama@gmail.com', 'PENDING', 'ACTIVE', 'Email verification completed', '192.168.1.11');
GO

SELECT * FROM audit_logs;
GO

-- ==========================================================
-- MODULE 2: CONTESTANT & ROUND MANAGEMENT
-- ==========================================================

-- 3. SEASONS
INSERT INTO seasons (name, season_year, is_current) VALUES
('StarVoice Lanka Season 1', 2025, 0),
('StarVoice Lanka Season 2', 2026, 1),
('StarVoice Lanka Pilot Season', 2022, 0),
('StarVoice Lanka Season 0', 2023, 0),
('StarVoice Lanka Junior Edition', 2024, 0);
GO

SELECT * FROM seasons;
GO

-- 4. CONTESTANTS (season_id = 2 -> Season 2)
INSERT INTO contestants (season_id, full_name, stage_name, age, district, bio, photo_url, status, total_votes) VALUES
(2, 'Sachini Madushani', 'Sachi', 21, 'Colombo', 'Pop vocalist with a background in church choir singing.', '/uploads/contestants/sachini.jpg', 'ACTIVE', 0),
(2, 'Eshan Bandara', 'Eshan B', 24, 'Kandy', 'Acoustic guitarist and singer-songwriter from the hill country.', '/uploads/contestants/eshan.jpg', 'ACTIVE', 0),
(2, 'Hiruni Gunasekara', 'Hiru', 19, 'Galle', 'Classically trained vocalist specialising in Sinhala ballads.', '/uploads/contestants/hiruni.jpg', 'ACTIVE', 0),
(2, 'Malith Rajapaksha', 'Malith R', 26, 'Kurunegala', 'Baila and folk fusion performer.', '/uploads/contestants/malith.jpg', 'ELIMINATED', 0),
(2, 'Ruwangi Costa', 'Ruwangi', 22, 'Negombo', 'R&B and soul singer.', '/uploads/contestants/ruwangi.jpg', 'ACTIVE', 0);
GO

SELECT * FROM contestants;
GO

-- 5. ROUNDS (season_id = 2)
INSERT INTO rounds (season_id, name, sequence, status, opens_at, closes_at, advance_count) VALUES
(2, 'Auditions', 1, 'RESULTS_PUBLISHED', '2026-01-05T09:00:00', '2026-01-12T23:59:59', 20),
(2, 'Top 20 - Live Round 1', 2, 'CLOSED', '2026-02-01T18:00:00', '2026-02-08T23:59:59', 10),
(2, 'Top 10 - Live Round 2', 3, 'OPEN', '2026-03-01T18:00:00', '2026-03-08T23:59:59', 5),
(2, 'Top 5 - Semi Final', 4, 'DRAFT', '2026-03-15T18:00:00', '2026-03-22T23:59:59', 3),
(2, 'Grand Finale', 5, 'DRAFT', '2026-04-01T18:00:00', '2026-04-05T23:59:59', 1);
GO

SELECT * FROM rounds;
GO

-- 6. ROUND ENTRIES (round_id = 2 -> Top 20 Live Round 1)
INSERT INTO round_entries (round_id, contestant_id, votes_in_round, position, outcome, performance_title, media_url, media_mime, media_size_bytes, media_original_name, media_uploaded_at) VALUES
(2, 1, 4520, 1, 'ADVANCED', 'Manike Mage Hithe (Cover)', '/uploads/performances/sachini_r2.mp4', 'video/mp4', 52428800, 'sachini_r2.mp4', '2026-02-02T10:00:00'),
(2, 2, 3980, 2, 'ADVANCED', 'Original: Kandy Mountain Song', '/uploads/performances/eshan_r2.mp4', 'video/mp4', 48234880, 'eshan_r2.mp4', '2026-02-02T11:15:00'),
(2, 3, 3750, 3, 'ADVANCED', 'Sanda Ra Landewi (Cover)', '/uploads/performances/hiruni_r2.mp4', 'video/mp4', 51200000, 'hiruni_r2.mp4', '2026-02-02T12:30:00'),
(2, 4, 1200, 5, 'ELIMINATED', 'Baila Medley', '/uploads/performances/malith_r2.mp4', 'video/mp4', 44040192, 'malith_r2.mp4', '2026-02-02T13:45:00'),
(2, 5, 2890, 4, 'ADVANCED', 'Original: Negombo Nights', '/uploads/performances/ruwangi_r2.mp4', 'video/mp4', 47185920, 'ruwangi_r2.mp4', '2026-02-02T15:00:00');
GO

SELECT * FROM round_entries;
GO

-- ==========================================================
-- MODULE 3: VOTING MANAGEMENT
-- ==========================================================

-- 7. VOTES (round_id = 2)
INSERT INTO votes (voter_id, round_id, contestant_id, type, count, credits_deducted, is_void, ip_address, user_agent) VALUES
(3, 2, 1, 'FREE', 1, 0, 0, '112.134.10.5', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'),
(4, 2, 2, 'FREE', 1, 0, 0, '112.134.10.9', 'Mozilla/5.0 (iPhone; CPU iPhone OS 17_0)'),
(6, 2, 1, 'PAID', 10, 10, 0, '112.134.11.2', 'Mozilla/5.0 (Android 14; Mobile)'),
(3, 2, 3, 'PAID', 5, 5, 0, '112.134.10.5', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'),
(6, 2, 5, 'FREE', 1, 0, 0, '112.134.11.2', 'Mozilla/5.0 (Android 14; Mobile)');
GO

SELECT * FROM votes;
GO

-- 8. VOTE QUOTAS
INSERT INTO vote_quotas (user_id, round_id, free_votes_used, paid_votes_used) VALUES
(3, 2, 1, 5),
(4, 2, 1, 0),
(6, 2, 1, 10),
(3, 3, 0, 0),
(4, 3, 0, 0);
GO

SELECT * FROM vote_quotas;
GO



-- ==========================================================
-- MODULE 4: NOTIFICATION MANAGEMENT
-- ==========================================================

-- 9. NOTIFICATIONS
INSERT INTO notifications (user_id, template, channel, subject, body, is_broadcast, status, attempts, sent_at) VALUES
(5, 'ACCOUNT_VERIFICATION', 'EMAIL', 'Verify your StarVoice Lanka account', 'Please use code 483920 to verify your account.', 0, 'SENT', 1, '2026-01-20T09:05:00'),
(3, 'VOTE_CONFIRMATION', 'IN_APP', 'Your vote was recorded', 'You voted for Sachini Madushani in Top 20 - Live Round 1.', 0, 'SENT', 1, '2026-02-02T10:05:00'),
(NULL, 'ROUND_OPENED', 'EMAIL', 'Top 10 Live Round is now open!', 'Voting for Top 10 - Live Round 2 has started. Cast your votes now!', 1, 'SENT', 1, '2026-03-01T18:00:05'),
(4, 'ELIMINATION_NOTICE', 'SMS', 'Round Results', 'Malith Rajapaksha has been eliminated in Top 20 - Live Round 1.', 0, 'FAILED', 2, NULL),
(6, 'RECEIPT_ISSUED', 'EMAIL', 'Your payment receipt', 'Your payment of LKR 1000.00 was successful. Receipt RCPT-2026-0002.', 0, 'SENT', 1, '2026-02-28T20:16:00');
GO

SELECT * FROM notifications;
GO

-- 10. NOTIFICATION PREFERENCES
INSERT INTO notification_preferences (user_id, email, sms, in_app, announcements) VALUES
(3, 1, 1, 1, 1),
(4, 1, 0, 1, 1),
(6, 0, 1, 1, 0),
(1, 1, 0, 1, 1),
(2, 1, 1, 1, 0);
GO

SELECT * FROM notification_preferences;
GO

-- ==========================================================
-- MODULE 5: PAYMENT & VOTE CREDITS
-- ==========================================================

-- 11. VOTE BUNDLES
INSERT INTO vote_bundles (code, name, credits, price_lkr, is_active) VALUES
('STARTER', 'Starter Pack', 10, 100.00, 1),
('FAN', 'Fan Pack', 60, 500.00, 1),
('SUPER_FAN', 'Super Fan Pack', 150, 1000.00, 1),
('MINI', 'Mini Pack', 5, 50.00, 1),
('MEGA_FAN', 'Mega Fan Pack', 350, 2000.00, 1);
GO

SELECT * FROM vote_bundles;
GO

-- 12. CREDIT ACCOUNTS (WALLET)
INSERT INTO credit_accounts (user_id, balance, lifetime_purchased, lifetime_spent) VALUES
(3, 45, 60, 15),
(4, 0, 0, 0),
(6, 140, 150, 10),
(5, 10, 10, 0),
(1, 0, 0, 0);
GO

SELECT * FROM credit_accounts;
GO

-- 13. PAYMENTS & TRANSACTIONS (bundle_id: 1=STARTER, 2=FAN, 3=SUPER_FAN)
INSERT INTO payments (user_id, bundle_id, receipt_no, reference, amount_lkr, vote_credits, method, status, paid_at) VALUES
(3, 2, 'RCPT-2026-0001', 'TXN-88213A', 500.00, 60, 'CARD', 'SUCCESS', '2026-02-01T08:30:00'),
(6, 3, 'RCPT-2026-0002', 'TXN-88214B', 1000.00, 150, 'ONLINE_WALLET', 'SUCCESS', '2026-02-28T20:15:00'),
(4, 1, 'RCPT-2026-0003', 'TXN-88215C', 100.00, 10, 'CARD', 'FAILED', NULL),
(4, 2, 'RCPT-2026-0004', 'TXN-88216D', 500.00, 60, 'BANK_TRANSFER', 'PENDING', NULL),
(5, 1, 'RCPT-2026-0005', 'TXN-88217E', 100.00, 10, 'ONLINE_WALLET', 'SUCCESS', '2026-03-02T09:45:00');
GO

SELECT * FROM payments;
GO

-- ==========================================================
-- MODULE 6: SPONSOR & CAMPAIGN MANAGEMENT
-- ==========================================================

-- 14. SPONSORS
INSERT INTO sponsors (company_name, contact_name, contact_email, contact_phone, logo_url, website_url, status) VALUES
('Dialog Axiata PLC', 'Chamara Wijesinghe', 'chamara.w@dialog.lk', '0112345678', '/uploads/sponsors/dialog.png', 'https://www.dialog.lk', 'ACTIVE'),
('Munchee Biscuits', 'Anusha Perera', 'anusha.p@munchee.lk', '0112349876', '/uploads/sponsors/munchee.png', 'https://www.munchee.lk', 'ACTIVE'),
('Commercial Bank of Ceylon PLC', 'Nadeesha Karunaratne', 'nadeesha.k@combank.lk', '0112345111', '/uploads/sponsors/combank.png', 'https://www.combank.lk', 'ACTIVE'),
('Elephant House', 'Ishara Gunawardena', 'ishara.g@elephanthouse.lk', '0112345222', '/uploads/sponsors/elephanthouse.png', 'https://www.elephanthouse.lk', 'ACTIVE'),
('Singer Sri Lanka PLC', 'Tharaka Bandara', 'tharaka.b@singer.lk', '0112345333', '/uploads/sponsors/singer.png', 'https://www.singersl.com', 'INACTIVE');
GO

SELECT * FROM sponsors;
GO

-- 15. SPONSORSHIP PACKAGES
INSERT INTO sponsorship_packages (name, tier, price_lkr, max_impressions, banner_placement, is_active) VALUES
('Title Sponsor Package', 'TITLE', 2500000.00, 500000, 'TOP', 1),
('Segment Sponsor Package', 'SEGMENT', 750000.00, 150000, 'SIDEBAR', 1),
('Powered By Package', 'POWERED_BY', 1500000.00, 300000, 'LEADERBOARD', 1),
('Associate Sponsor Package', 'ASSOCIATE', 400000.00, 100000, 'FOOTER', 1),
('Associate Plus Package', 'ASSOCIATE', 250000.00, 60000, 'SIDEBAR', 1);
GO

SELECT * FROM sponsorship_packages;
GO

-- 16. SPONSORSHIP AGREEMENTS (round_id 3 -> Top 10 Live Round 2)
INSERT INTO sponsorship_agreements (sponsor_id, package_id, season_id, round_id, banner_url, click_url, impressions_delivered, clicks_recorded, status, start_date, end_date) VALUES
(1, 1, 2, NULL, '/uploads/banners/dialog_top.png', 'https://www.dialog.lk/promo', 128500, 640, 'ACTIVE', '2026-01-01', '2026-06-30'),
(2, 2, 2, 3, '/uploads/banners/munchee_sidebar.png', 'https://www.munchee.lk/promo', 32000, 210, 'ACTIVE', '2026-03-01', '2026-03-31'),
(3, 3, 2, NULL, '/uploads/banners/combank_leaderboard.png', 'https://www.combank.lk/promo', 45000, 180, 'ACTIVE', '2026-02-01', '2026-05-31'),
(4, 4, 2, 2, '/uploads/banners/elephanthouse_footer.png', 'https://www.elephanthouse.lk/promo', 18000, 95, 'EXPIRED', '2026-02-01', '2026-02-28'),
(5, 5, 2, 3, '/uploads/banners/singer_sidebar.png', 'https://www.singersl.com/promo', 0, 0, 'DRAFT', '2026-03-15', '2026-04-15');
GO

SELECT * FROM sponsorship_agreements;
GO

-- 17. SPONSOR IMPRESSIONS
INSERT INTO sponsor_impressions (agreement_id, round_id, placement, ip_address, user_agent) VALUES
(1, 2, 'TOP', '112.134.10.5', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'),
(1, 3, 'TOP', '112.134.10.9', 'Mozilla/5.0 (iPhone; CPU iPhone OS 17_0)'),
(2, 3, 'SIDEBAR', '112.134.11.2', 'Mozilla/5.0 (Android 14; Mobile)'),
(3, 2, 'LEADERBOARD', '112.134.10.5', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'),
(4, 2, 'FOOTER', '112.134.10.9', 'Mozilla/5.0 (iPhone; CPU iPhone OS 17_0)');
GO

SELECT * FROM sponsor_impressions;
GO

-- 18. SPONSOR INVOICES
INSERT INTO sponsor_invoices (agreement_id, invoice_number, amount_lkr, status, issued_at, due_date, paid_at, payment_reference) VALUES
(1, 'INV-2026-001', 1250000.00, 'PAID', '2026-01-02T09:00:00', '2026-01-16', '2026-01-10T14:20:00', 'BANKTXN-55231'),
(2, 'INV-2026-002', 375000.00, 'ISSUED', '2026-03-02T09:00:00', '2026-03-16', NULL, NULL),
(3, 'INV-2026-003', 750000.00, 'PAID', '2026-02-02T09:00:00', '2026-02-16', '2026-02-12T11:30:00', 'BANKTXN-55987'),
(4, 'INV-2026-004', 200000.00, 'OVERDUE', '2026-02-03T09:00:00', '2026-02-17', NULL, NULL),
(5, 'INV-2026-005', 125000.00, 'ISSUED', '2026-03-16T09:00:00', '2026-03-30', NULL, NULL);
GO

SELECT * FROM sponsor_invoices;
GO

PRINT 'StarVoice Lanka sample data inserted successfully.';
GO

-- ==========================================================
-- StarVoice Lanka - Part D: SQL Queries & Outputs
-- ==========================================================

-- Query 1: Simple SELECT
-- Show all open/current voting rounds.
SELECT id, name, sequence, status, opens_at, closes_at
FROM rounds
WHERE status IN ('OPEN', 'CLOSED', 'RESULTS_PUBLISHED');
GO

-- Query 2: JOIN
-- Show each vote together with the voter and the contestant they voted for.
SELECT
    v.id AS vote_id,
    u.full_name AS voter_name,
    c.full_name AS contestant_name,
    r.name AS round_name,
    v.type,
    v.count,
    v.created_at
FROM votes v
JOIN users u ON u.id = v.voter_id
JOIN contestants c ON c.id = v.contestant_id
JOIN rounds r ON r.id = v.round_id;
GO

-- Query 3: Aggregation
-- Count the number of (non-void) votes cast in the system.
SELECT COUNT(*) AS total_votes
FROM votes
WHERE is_void = 0;
GO

-- Query 4: GROUP BY / HAVING
-- Find rounds that have at least one vote cast.
SELECT
    r.id,
    r.name,
    COUNT(v.id) AS vote_count
FROM rounds r
JOIN votes v ON v.round_id = r.id
GROUP BY r.id, r.name
HAVING COUNT(v.id) >= 1;
GO

-- Query 5: Subquery
-- Find contestants whose vote count in a round is above the average vote count
-- across all round entries.
SELECT
    re.round_id,
    re.contestant_id,
    c.full_name,
    re.votes_in_round
FROM round_entries re
JOIN contestants c ON c.id = re.contestant_id
WHERE re.votes_in_round > (SELECT AVG(votes_in_round) FROM round_entries);
GO

-- ==========================================================
-- StarVoice Lanka - Part E: Stored Procedure
-- usp_GetRoundResults
--
-- Returns the ranked leaderboard for a single voting round. Given a
-- @RoundId, it returns every contestant entered in that round with
-- their vote count and outcome, ranked by vote count.
-- ==========================================================

CREATE PROCEDURE dbo.usp_GetRoundResults
    (@RoundId BIGINT)
AS
BEGIN
    SET NOCOUNT ON;

    IF NOT EXISTS (SELECT 1 FROM dbo.rounds WHERE id = @RoundId)
    BEGIN
        RAISERROR('Round not found.', 16, 1);
        RETURN;
    END;

    SELECT
        RANK() OVER (ORDER BY re.votes_in_round DESC) AS rank_position,
        c.id AS contestant_id,
        c.full_name,
        c.stage_name,
        c.district,
        re.votes_in_round,
        re.outcome
    FROM dbo.round_entries re
    JOIN dbo.contestants c ON c.id = re.contestant_id
    WHERE re.round_id = @RoundId
    ORDER BY rank_position, c.full_name;
END;
GO

-- ----------------------------------------------------------
-- Procedure Execution
-- ----------------------------------------------------------

-- A1. Success case: an existing round with entries
DECLARE @RoundId BIGINT = 2; -- Top 20 - Live Round 1

SELECT id AS round_id, name AS round_name
FROM dbo.rounds WHERE id = @RoundId;

EXEC dbo.usp_GetRoundResults @RoundId = @RoundId;
GO

-- A2. Error case: round that does not exist
EXEC dbo.usp_GetRoundResults @RoundId = 999999;
GO

-- ==========================================================
-- StarVoice Lanka - Part F: Trigger
-- trg_votes_validate_entry
--
-- This trigger fires after every INSERT or UPDATE on the votes table.
-- It validates that the (round_id, contestant_id) pair on a vote
-- actually exists in round_entries - i.e. that the contestant being
-- voted for is genuinely entered in that round.
--
-- This check exists because votes.round_id and votes.contestant_id
-- are stored directly on the votes row (rather than via a single
-- round_entry_id foreign key), purely so the voting/quota logic can
-- query votes by round or by contestant without an extra join. No
-- standard constraint (CHECK, FOREIGN KEY) can validate that this
-- pair is a genuine round_entries combination, because that
-- comparison spans two tables. If a mismatch is found, the trigger
-- rolls back the transaction and raises an error, so a vote for a
-- contestant who isn't actually in that round is never stored.
-- ==========================================================

USE starvoice_lanka;
GO

CREATE TRIGGER dbo.trg_votes_validate_entry
ON dbo.votes
AFTER INSERT, UPDATE
AS
BEGIN
    SET NOCOUNT ON;

    -- For UPDATEs that touch neither column involved, nothing to re-check.
    IF EXISTS (SELECT 1 FROM deleted)
       AND NOT (UPDATE(round_id) OR UPDATE(contestant_id))
        RETURN;

    IF EXISTS (
        SELECT 1
        FROM inserted i
        WHERE NOT EXISTS (
            SELECT 1 FROM dbo.round_entries re
            WHERE re.round_id = i.round_id AND re.contestant_id = i.contestant_id
        )
    )
    BEGIN
        IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
        RAISERROR('Vote rejected: contestant is not entered in this round.', 16, 1);
        RETURN;
    END;
END;
GO

-- ----------------------------------------------------------
-- Trigger Execution
-- ----------------------------------------------------------

-- Pick real IDs from existing data
DECLARE @wrongRound BIGINT = 3; -- Top 10 - Live Round 2 (no round_entries yet)
DECLARE @rightRound BIGINT = 2; -- Top 20 - Live Round 1 (Sachini is entered here)
DECLARE @contestant BIGINT = (SELECT id FROM dbo.contestants WHERE full_name = 'Sachini Madushani');
DECLARE @voter BIGINT = (SELECT id FROM dbo.users WHERE email = 'dilani.wickrama@gmail.com'); 

PRINT 'wrongRound=' + CAST(@wrongRound AS VARCHAR) + ', rightRound=' + CAST(@rightRound AS VARCHAR)
    + ', contestant=' + CAST(@contestant AS VARCHAR) + ', voter=' + CAST(@voter AS VARCHAR);
GO

-- TEST 1: insert a vote for a round the contestant is NOT entered in -> trigger should block it
DECLARE @wrongRound BIGINT = 3;
DECLARE @contestant BIGINT = (SELECT id FROM dbo.contestants WHERE full_name = 'Sachini Madushani');
DECLARE @voter BIGINT = (SELECT id FROM dbo.users WHERE email = 'dilani.wickrama@gmail.com');

BEGIN TRY
    INSERT INTO dbo.votes (voter_id, round_id, contestant_id, type, count, credits_deducted, is_void)
    VALUES (@voter, @wrongRound, @contestant, 'FREE', 1, 0, 0);
    PRINT 'TEST 1: vote inserted (unexpected --- trigger did not fire)';
END TRY
BEGIN CATCH
    PRINT 'TEST 1 blocked by trigger: ' + ERROR_MESSAGE();
END CATCH;
GO

-- TEST 2: insert a vote for a round/contestant pair that IS a real round_entries row -> should succeed
DECLARE @rightRound BIGINT = 2;
DECLARE @contestant BIGINT = (SELECT id FROM dbo.contestants WHERE full_name = 'Sachini Madushani');
DECLARE @voter BIGINT = (SELECT id FROM dbo.users WHERE email = 'dilani.wickrama@gmail.com');

BEGIN TRY
    INSERT INTO dbo.votes (voter_id, round_id, contestant_id, type, count, credits_deducted, is_void)
    VALUES (@voter, @rightRound, @contestant, 'FREE', 1, 0, 0);
    PRINT 'TEST 2: vote inserted successfully';
END TRY
BEGIN CATCH
    PRINT 'TEST 2 unexpectedly blocked: ' + ERROR_MESSAGE();
END CATCH;
GO

-- Show the vote that actually got stored
SELECT
    v.id,
    u.full_name AS voter,
    c.full_name AS contestant,
    r.name AS round_name
FROM dbo.votes v
JOIN dbo.users u ON u.id = v.voter_id
JOIN dbo.contestants c ON c.id = v.contestant_id
JOIN dbo.rounds r ON r.id = v.round_id
WHERE v.voter_id = (SELECT id FROM dbo.users WHERE email = 'dilani.wickrama@gmail.com');
GO
