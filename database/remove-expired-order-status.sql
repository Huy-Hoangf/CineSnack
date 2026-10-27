-- Chạy một lần trên database CineSnack hiện có, sau khi sao lưu.
-- Giữ thời hạn giữ chỗ; đơn quá hạn được chuyển thành HUY.
USE [CineSnack];
SET ANSI_NULLS ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET QUOTED_IDENTIFIER ON;
SET NUMERIC_ROUNDABORT OFF;
SET XACT_ABORT ON;
BEGIN TRY
    BEGIN TRANSACTION;

    DECLARE @tableName sysname, @constraintName sysname, @sql nvarchar(max);
    DECLARE status_constraints CURSOR LOCAL FAST_FORWARD FOR
        SELECT OBJECT_NAME(parent_object_id), name
        FROM sys.check_constraints
        WHERE parent_object_id IN (
            OBJECT_ID('DON_HANG'), OBJECT_ID('VE'),
            OBJECT_ID('CHI_TIET_HOA_DON_BONG_NUOC')
        ) AND definition LIKE '%HET_HAN%';
    OPEN status_constraints;
    FETCH NEXT FROM status_constraints INTO @tableName, @constraintName;
    WHILE @@FETCH_STATUS = 0 BEGIN
        SET @sql = N'ALTER TABLE ' + QUOTENAME(@tableName)
            + N' DROP CONSTRAINT ' + QUOTENAME(@constraintName);
        EXEC sp_executesql @sql;
        FETCH NEXT FROM status_constraints INTO @tableName, @constraintName;
    END;
    CLOSE status_constraints;
    DEALLOCATE status_constraints;

    UPDATE DON_HANG SET trang_thai = 'HUY' WHERE trang_thai = 'HET_HAN';
    UPDATE VE SET trang_thai = 'HUY' WHERE trang_thai = 'HET_HAN';
    UPDATE CHI_TIET_HOA_DON_BONG_NUOC
        SET trang_thai_phuc_vu = 'HUY' WHERE trang_thai_phuc_vu = 'HET_HAN';

    IF NOT EXISTS (SELECT 1 FROM sys.check_constraints
        WHERE parent_object_id = OBJECT_ID('DON_HANG') AND name = 'CK_DON_HANG_TRANG_THAI')
        ALTER TABLE DON_HANG WITH CHECK ADD CONSTRAINT CK_DON_HANG_TRANG_THAI
            CHECK (trang_thai IN ('CHO_THANH_TOAN', 'DA_THANH_TOAN', 'HUY'));
    IF NOT EXISTS (SELECT 1 FROM sys.check_constraints
        WHERE parent_object_id = OBJECT_ID('VE') AND name = 'CK_VE_KHONG_HET_HAN')
        ALTER TABLE VE WITH CHECK ADD CONSTRAINT CK_VE_KHONG_HET_HAN
            CHECK (trang_thai IN ('CHO_THANH_TOAN', 'DA_THANH_TOAN', 'DA_SOAT', 'HUY'));
    IF NOT EXISTS (SELECT 1 FROM sys.check_constraints
        WHERE parent_object_id = OBJECT_ID('CHI_TIET_HOA_DON_BONG_NUOC')
          AND name = 'CK_BONG_NUOC_KHONG_HET_HAN')
        ALTER TABLE CHI_TIET_HOA_DON_BONG_NUOC WITH CHECK
            ADD CONSTRAINT CK_BONG_NUOC_KHONG_HET_HAN
            CHECK (trang_thai_phuc_vu IN ('CHO_THANH_TOAN', 'CHO_NHAN', 'DA_NHAN', 'HUY'));

    COMMIT;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK;
    THROW;
END CATCH;
