-- Non-destructive, repeatable upgrade. Review and run manually on a backed-up database.
USE [CineSnack];
SET XACT_ABORT ON;
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;

IF OBJECT_ID(N'dbo.PHIM', N'U') IS NULL
   OR OBJECT_ID(N'dbo.PHONG_CHIEU', N'U') IS NULL
   OR OBJECT_ID(N'dbo.SUAT_CHIEU', N'U') IS NULL
    THROW 50010, N'Không tìm thấy schema CineSnack cần nâng cấp; chưa thay đổi dữ liệu.', 1;

BEGIN TRY
    BEGIN TRANSACTION;
    IF COL_LENGTH('dbo.PHIM', 'dao_dien') IS NULL
        ALTER TABLE dbo.PHIM ADD dao_dien NVARCHAR(200) NULL;
    IF COL_LENGTH('dbo.PHIM', 'trailer_url') IS NULL
        ALTER TABLE dbo.PHIM ADD trailer_url NVARCHAR(500) NULL;
    IF COL_LENGTH('dbo.SUAT_CHIEU', 'che_do_gia') IS NULL
        ALTER TABLE dbo.SUAT_CHIEU ADD che_do_gia VARCHAR(10) NOT NULL
            CONSTRAINT DF_SUAT_CHIEU_CHE_DO_GIA DEFAULT 'MANUAL' WITH VALUES;
    -- Compile only after ALTER has made the column available.
    IF NOT EXISTS (SELECT 1 FROM sys.check_constraints
        WHERE parent_object_id = OBJECT_ID(N'dbo.SUAT_CHIEU') AND name = N'CK_SUAT_CHIEU_CHE_DO_GIA')
        EXEC(N'ALTER TABLE dbo.SUAT_CHIEU ADD CONSTRAINT CK_SUAT_CHIEU_CHE_DO_GIA
            CHECK (che_do_gia IN (''MANUAL'', ''POLICY''));');

    IF OBJECT_ID(N'dbo.BANG_GIA_PHONG', N'U') IS NULL
    BEGIN
        CREATE TABLE dbo.BANG_GIA_PHONG (
            phong_id BIGINT NOT NULL CONSTRAINT PK_BANG_GIA_PHONG PRIMARY KEY,
            gia_thuong_ngay_thuong DECIMAL(12,2) NOT NULL,
            gia_vip_ngay_thuong DECIMAL(12,2) NOT NULL,
            gia_thuong_cuoi_tuan DECIMAL(12,2) NOT NULL,
            gia_vip_cuoi_tuan DECIMAL(12,2) NOT NULL,
            CONSTRAINT FK_BANG_GIA_PHONG_PHONG FOREIGN KEY (phong_id) REFERENCES dbo.PHONG_CHIEU(phong_id) ON DELETE CASCADE,
            CONSTRAINT CK_BANG_GIA_PHONG_GIA CHECK (
                gia_thuong_ngay_thuong >= 0 AND gia_vip_ngay_thuong >= gia_thuong_ngay_thuong
                AND gia_thuong_cuoi_tuan >= 0 AND gia_vip_cuoi_tuan >= gia_thuong_cuoi_tuan
                AND gia_thuong_ngay_thuong = FLOOR(gia_thuong_ngay_thuong)
                AND gia_vip_ngay_thuong = FLOOR(gia_vip_ngay_thuong)
                AND gia_thuong_cuoi_tuan = FLOOR(gia_thuong_cuoi_tuan)
                AND gia_vip_cuoi_tuan = FLOOR(gia_vip_cuoi_tuan))
        );
    END;
    IF OBJECT_ID(N'dbo.CHINH_SACH_RAP', N'U') IS NULL
    BEGIN
        CREATE TABLE dbo.CHINH_SACH_RAP (
            chinh_sach_id INT NOT NULL CONSTRAINT PK_CHINH_SACH_RAP PRIMARY KEY,
            gioi_han_soat_muon_phut INT NULL,
            CONSTRAINT CK_CHINH_SACH_RAP_DON CHECK (chinh_sach_id = 1),
            CONSTRAINT CK_CHINH_SACH_RAP_MUON CHECK (gioi_han_soat_muon_phut BETWEEN 0 AND 600)
        );
    END;
    IF NOT EXISTS (SELECT 1 FROM dbo.CHINH_SACH_RAP WITH (UPDLOCK, HOLDLOCK) WHERE chinh_sach_id = 1)
        INSERT INTO dbo.CHINH_SACH_RAP(chinh_sach_id, gioi_han_soat_muon_phut) VALUES (1, NULL);
    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
