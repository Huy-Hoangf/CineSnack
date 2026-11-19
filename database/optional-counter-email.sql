-- Chạy một lần trên database hiện có để đơn tại quầy có thể không có email.
USE [CineSnack];
SET XACT_ABORT ON;
BEGIN TRY
    BEGIN TRANSACTION;
    IF EXISTS (
        SELECT 1
        FROM sys.columns
        WHERE object_id = OBJECT_ID(N'DON_HANG')
          AND name = N'email_nhan_ve'
          AND is_nullable = 0
    )
        ALTER TABLE DON_HANG ALTER COLUMN email_nhan_ve VARCHAR(150) NULL;

    COMMIT;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK;
    THROW;
END CATCH;
