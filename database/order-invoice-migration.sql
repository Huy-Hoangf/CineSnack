-- Chạy một lần trên database CineSnack hiện có, sau khi sao lưu.
-- Giữ hóa đơn đã thanh toán; chuyển vé/bắp nước chờ thanh toán về đơn.
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

    IF COL_LENGTH('VE', 'don_hang_id') IS NULL
        ALTER TABLE VE ADD don_hang_id BIGINT NULL;
    IF COL_LENGTH('CHI_TIET_HOA_DON_BONG_NUOC', 'don_hang_id') IS NULL
        ALTER TABLE CHI_TIET_HOA_DON_BONG_NUOC ADD don_hang_id BIGINT NULL;

    EXEC sp_executesql N'UPDATE v SET don_hang_id=h.don_hang_id
        FROM VE v JOIN HOA_DON_VE h ON h.hoa_don_ve_id=v.hoa_don_ve_id
        WHERE v.don_hang_id IS NULL';
    EXEC sp_executesql N'UPDATE c SET don_hang_id=h.don_hang_id
        FROM CHI_TIET_HOA_DON_BONG_NUOC c
        JOIN HOA_DON_BONG_NUOC h ON h.hoa_don_bong_nuoc_id=c.hoa_don_bong_nuoc_id
        WHERE c.don_hang_id IS NULL';
    EXEC sp_executesql N'IF EXISTS (SELECT 1 FROM VE WHERE don_hang_id IS NULL) OR
        EXISTS (SELECT 1 FROM CHI_TIET_HOA_DON_BONG_NUOC WHERE don_hang_id IS NULL)
        THROW 50020, N''Chi tiết không có đơn hàng; không thể nâng cấp.'', 1';

    -- Cột hóa đơn trở thành liên kết tùy chọn trước khi thanh toán.
    DECLARE @fk sysname, @sql nvarchar(max);
    DECLARE fk_ticket_cursor CURSOR LOCAL FAST_FORWARD FOR
        SELECT fk.name
        FROM sys.foreign_keys fk
        JOIN sys.foreign_key_columns fkc ON fkc.constraint_object_id=fk.object_id
        WHERE fk.parent_object_id=OBJECT_ID('VE')
          AND COL_NAME(fkc.parent_object_id,fkc.parent_column_id)='hoa_don_ve_id';
    OPEN fk_ticket_cursor;
    FETCH NEXT FROM fk_ticket_cursor INTO @fk;
    WHILE @@FETCH_STATUS=0 BEGIN
        SET @sql=N'ALTER TABLE VE DROP CONSTRAINT '+QUOTENAME(@fk);
        EXEC sp_executesql @sql;
        FETCH NEXT FROM fk_ticket_cursor INTO @fk;
    END;
    CLOSE fk_ticket_cursor;
    DEALLOCATE fk_ticket_cursor;

    DECLARE fk_food_cursor CURSOR LOCAL FAST_FORWARD FOR
        SELECT fk.name
        FROM sys.foreign_keys fk
        JOIN sys.foreign_key_columns fkc ON fkc.constraint_object_id=fk.object_id
        WHERE fk.parent_object_id=OBJECT_ID('CHI_TIET_HOA_DON_BONG_NUOC')
          AND COL_NAME(fkc.parent_object_id,fkc.parent_column_id)='hoa_don_bong_nuoc_id';
    OPEN fk_food_cursor;
    FETCH NEXT FROM fk_food_cursor INTO @fk;
    WHILE @@FETCH_STATUS=0 BEGIN
        SET @sql=N'ALTER TABLE CHI_TIET_HOA_DON_BONG_NUOC DROP CONSTRAINT '+QUOTENAME(@fk);
        EXEC sp_executesql @sql;
        FETCH NEXT FROM fk_food_cursor INTO @fk;
    END;
    CLOSE fk_food_cursor;
    DEALLOCATE fk_food_cursor;

    IF EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID('VE') AND name='IX_VE_HOA_DON')
        DROP INDEX IX_VE_HOA_DON ON VE;
    IF EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID('CHI_TIET_HOA_DON_BONG_NUOC') AND name='IX_BONG_NUOC_HOA_DON')
        DROP INDEX IX_BONG_NUOC_HOA_DON ON CHI_TIET_HOA_DON_BONG_NUOC;

    IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id=OBJECT_ID('VE') AND name='hoa_don_ve_id' AND is_nullable=0)
        ALTER TABLE VE ALTER COLUMN hoa_don_ve_id BIGINT NULL;
    IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id=OBJECT_ID('CHI_TIET_HOA_DON_BONG_NUOC') AND name='hoa_don_bong_nuoc_id' AND is_nullable=0)
        ALTER TABLE CHI_TIET_HOA_DON_BONG_NUOC ALTER COLUMN hoa_don_bong_nuoc_id BIGINT NULL;

    UPDATE v SET hoa_don_ve_id=NULL FROM VE v
    JOIN HOA_DON_VE h ON h.hoa_don_ve_id=v.hoa_don_ve_id
    WHERE h.trang_thai<>'DA_THANH_TOAN';
    UPDATE c SET hoa_don_bong_nuoc_id=NULL FROM CHI_TIET_HOA_DON_BONG_NUOC c
    JOIN HOA_DON_BONG_NUOC h ON h.hoa_don_bong_nuoc_id=c.hoa_don_bong_nuoc_id
    WHERE h.trang_thai<>'DA_THANH_TOAN';
    DELETE FROM HOA_DON_VE WHERE trang_thai<>'DA_THANH_TOAN';
    DELETE FROM HOA_DON_BONG_NUOC WHERE trang_thai<>'DA_THANH_TOAN';

    DECLARE @bang sysname, @kiemTra sysname;
    DECLARE check_cursor CURSOR LOCAL FAST_FORWARD FOR
        SELECT OBJECT_NAME(parent_object_id), name
        FROM sys.check_constraints
        WHERE parent_object_id IN (OBJECT_ID('HOA_DON_VE'),OBJECT_ID('HOA_DON_BONG_NUOC'))
          AND definition LIKE '%trang_thai%';
    OPEN check_cursor;
    FETCH NEXT FROM check_cursor INTO @bang,@kiemTra;
    WHILE @@FETCH_STATUS=0 BEGIN
        SET @sql=N'ALTER TABLE '+QUOTENAME(@bang)+N' DROP CONSTRAINT '+QUOTENAME(@kiemTra);
        EXEC sp_executesql @sql;
        FETCH NEXT FROM check_cursor INTO @bang,@kiemTra;
    END;
    CLOSE check_cursor;
    DEALLOCATE check_cursor;
    DECLARE default_cursor CURSOR LOCAL FAST_FORWARD FOR
        SELECT OBJECT_NAME(dc.parent_object_id), dc.name
        FROM sys.default_constraints dc
        JOIN sys.columns c ON c.object_id=dc.parent_object_id AND c.column_id=dc.parent_column_id
        WHERE dc.parent_object_id IN (OBJECT_ID('HOA_DON_VE'),OBJECT_ID('HOA_DON_BONG_NUOC'))
          AND c.name='trang_thai';
    OPEN default_cursor;
    FETCH NEXT FROM default_cursor INTO @bang,@kiemTra;
    WHILE @@FETCH_STATUS=0 BEGIN
        SET @sql=N'ALTER TABLE '+QUOTENAME(@bang)+N' DROP CONSTRAINT '+QUOTENAME(@kiemTra);
        EXEC sp_executesql @sql;
        FETCH NEXT FROM default_cursor INTO @bang,@kiemTra;
    END;
    CLOSE default_cursor;
    DEALLOCATE default_cursor;
    ALTER TABLE HOA_DON_VE ADD CONSTRAINT DF_HOA_DON_VE_DA_THANH_TOAN DEFAULT 'DA_THANH_TOAN' FOR trang_thai;
    ALTER TABLE HOA_DON_BONG_NUOC ADD CONSTRAINT DF_HOA_DON_BONG_DA_THANH_TOAN DEFAULT 'DA_THANH_TOAN' FOR trang_thai;
    IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name='CK_HOA_DON_VE_CHI_DA_THANH_TOAN')
        ALTER TABLE HOA_DON_VE ADD CONSTRAINT CK_HOA_DON_VE_CHI_DA_THANH_TOAN CHECK(trang_thai='DA_THANH_TOAN');
    IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name='CK_HOA_DON_BONG_CHI_DA_THANH_TOAN')
        ALTER TABLE HOA_DON_BONG_NUOC ADD CONSTRAINT CK_HOA_DON_BONG_CHI_DA_THANH_TOAN CHECK(trang_thai='DA_THANH_TOAN');

    EXEC sp_executesql N'ALTER TABLE VE ALTER COLUMN don_hang_id BIGINT NOT NULL';
    EXEC sp_executesql N'ALTER TABLE CHI_TIET_HOA_DON_BONG_NUOC ALTER COLUMN don_hang_id BIGINT NOT NULL';
    IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name='FK_VE_DON_HANG')
        EXEC sp_executesql N'ALTER TABLE VE ADD CONSTRAINT FK_VE_DON_HANG FOREIGN KEY(don_hang_id) REFERENCES DON_HANG(don_hang_id)';
    IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name='FK_BONG_NUOC_DON_HANG')
        EXEC sp_executesql N'ALTER TABLE CHI_TIET_HOA_DON_BONG_NUOC ADD CONSTRAINT FK_BONG_NUOC_DON_HANG FOREIGN KEY(don_hang_id) REFERENCES DON_HANG(don_hang_id)';
    IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name='FK_VE_HOA_DON_VE')
        ALTER TABLE VE ADD CONSTRAINT FK_VE_HOA_DON_VE FOREIGN KEY(hoa_don_ve_id) REFERENCES HOA_DON_VE(hoa_don_ve_id);
    IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name='FK_BONG_NUOC_HOA_DON')
        ALTER TABLE CHI_TIET_HOA_DON_BONG_NUOC ADD CONSTRAINT FK_BONG_NUOC_HOA_DON FOREIGN KEY(hoa_don_bong_nuoc_id) REFERENCES HOA_DON_BONG_NUOC(hoa_don_bong_nuoc_id);
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID('VE') AND name='IX_VE_HOA_DON')
        CREATE INDEX IX_VE_HOA_DON ON VE(hoa_don_ve_id);
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID('CHI_TIET_HOA_DON_BONG_NUOC') AND name='IX_BONG_NUOC_HOA_DON')
        CREATE INDEX IX_BONG_NUOC_HOA_DON ON CHI_TIET_HOA_DON_BONG_NUOC(hoa_don_bong_nuoc_id);

    COMMIT;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT>0 ROLLBACK;
    THROW;
END CATCH;
