-- RMS local database: (re)create the empty database.
--
-- DESTRUCTIVE: drops rms_local and everything in it. Local or throwaway MySQL only.
-- Never run this against a shared or production server.
--
-- The setup scripts run this file first, then ../schema.sql and ../test-seed.sql.

DROP DATABASE IF EXISTS rms_local;
CREATE DATABASE rms_local CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE rms_local;
