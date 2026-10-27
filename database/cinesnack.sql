USE [CineSnack];
SET XACT_ABORT ON;
SET ANSI_NULLS ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET QUOTED_IDENTIFIER ON;
SET NUMERIC_ROUNDABORT OFF;
IF OBJECT_ID(N'VE',N'U') IS NOT NULL AND
   (COL_LENGTH('VE','hoa_don_ve_id') IS NULL OR COL_LENGTH('VE','phong_id') IS NULL
    OR OBJECT_ID(N'HOA_DON_VE',N'U') IS NULL OR OBJECT_ID(N'HOA_DON_BONG_NUOC',N'U') IS NULL)
    THROW 50010, N'Schema hiện có chưa tương thích hóa đơn tách; chưa thay đổi dữ liệu.', 1;
BEGIN TRY
    BEGIN TRANSACTION;

    EXEC(N'

USE [CineSnack];

SET ANSI_NULLS ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET QUOTED_IDENTIFIER ON;
SET NUMERIC_ROUNDABORT OFF;

IF DB_ID(N''CineSnack'') IS NULL THROW 50010,
N''Không tìm thấy database CineSnack. Chọn đúng SQL Server đã cấu hình.'',
1;
USE [CineSnack];
');

    EXEC(N'USE [CineSnack];

IF OBJECT_ID(N''PHONG_CHIEU'', N''U'') IS NULL BEGIN
CREATE TABLE PHONG_CHIEU (
    phong_id BIGINT IDENTITY(1, 1) PRIMARY KEY,
    ten_phong NVARCHAR(50) NOT NULL UNIQUE,
    loai_phong NVARCHAR(30) NOT NULL,
    trang_thai VARCHAR(20) NOT NULL
);
END;

IF OBJECT_ID(N''PHIM'', N''U'') IS NULL BEGIN
CREATE TABLE PHIM (
    phim_id BIGINT IDENTITY(1, 1) PRIMARY KEY,
    ten_phim NVARCHAR(200) NOT NULL,
    the_loai NVARCHAR(100) NOT NULL,
    thoi_luong_phut INT NOT NULL,
    phan_loai_do_tuoi VARCHAR(10) NOT NULL,
    poster_url NVARCHAR(500) NULL,
    trang_thai VARCHAR(20) NOT NULL,
    CHECK (thoi_luong_phut > 0)
);
END;

IF OBJECT_ID(N''NGUOI_DUNG'', N''U'') IS NULL BEGIN
CREATE TABLE NGUOI_DUNG (
    nguoi_dung_id BIGINT IDENTITY(1, 1) PRIMARY KEY,
    ho_ten NVARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    mat_khau_hash VARCHAR(255) NOT NULL,
    vai_tro VARCHAR(20) NOT NULL,
    trang_thai VARCHAR(20) NOT NULL
);
END;

IF OBJECT_ID(N''SAN_PHAM'', N''U'') IS NULL BEGIN
CREATE TABLE SAN_PHAM (
    san_pham_id BIGINT IDENTITY(1, 1) PRIMARY KEY,
    ten_san_pham NVARCHAR(100) NOT NULL,
    loai_san_pham VARCHAR(20) NOT NULL,
    gia_ban DECIMAL(12, 2) NOT NULL,
    trang_thai VARCHAR(20) NOT NULL,
    CHECK (gia_ban >= 0)
);
END;

IF OBJECT_ID(N''COMBO'', N''U'') IS NULL BEGIN
CREATE TABLE COMBO (
    combo_id BIGINT IDENTITY(1, 1) PRIMARY KEY,
    ten_combo NVARCHAR(100) NOT NULL,
    gia_ban DECIMAL(12, 2) NOT NULL,
    trang_thai VARCHAR(20) NOT NULL,
    CHECK (gia_ban >= 0)
);
END;

IF OBJECT_ID(N''GHE'', N''U'') IS NULL BEGIN
CREATE TABLE GHE (
    ghe_id BIGINT IDENTITY(1, 1) PRIMARY KEY,
    phong_id BIGINT NOT NULL,
    ma_ghe VARCHAR(10) NOT NULL,
    loai_ghe VARCHAR(20) NOT NULL,
    trang_thai VARCHAR(20) NOT NULL,
    FOREIGN KEY (phong_id) REFERENCES PHONG_CHIEU (phong_id),
    UNIQUE (phong_id, ma_ghe)
);
END;

IF OBJECT_ID(N''SUAT_CHIEU'', N''U'') IS NULL BEGIN
CREATE TABLE SUAT_CHIEU (
    suat_chieu_id BIGINT IDENTITY(1, 1) PRIMARY KEY,
    phim_id BIGINT NOT NULL,
    phong_id BIGINT NOT NULL,
    bat_dau DATETIME2(0) NOT NULL,
    ket_thuc DATETIME2(0) NOT NULL,
    gia_co_ban DECIMAL(12, 2) NOT NULL,
    trang_thai VARCHAR(20) NOT NULL,
    FOREIGN KEY (phim_id) REFERENCES PHIM (phim_id),
    FOREIGN KEY (phong_id) REFERENCES PHONG_CHIEU (phong_id),
    CHECK (ket_thuc > bat_dau),
    CHECK (gia_co_ban >= 0)
);
END;

IF OBJECT_ID(N''DON_HANG'', N''U'') IS NULL BEGIN
CREATE TABLE DON_HANG (
    don_hang_id BIGINT IDENTITY(1, 1) PRIMARY KEY,
    nguoi_dung_id BIGINT NULL,
    ma_don_hang VARCHAR(30) NOT NULL UNIQUE,
    email_nhan_ve VARCHAR(150) NOT NULL,
    tong_tien DECIMAL(12, 2) NOT NULL,
    trang_thai VARCHAR(20) NOT NULL,
    het_han_luc DATETIME2(0) NULL,
    tao_luc DATETIME2(0) NOT NULL,
    FOREIGN KEY (nguoi_dung_id) REFERENCES NGUOI_DUNG (nguoi_dung_id),
    CHECK (tong_tien >= 0)
);
END;

IF OBJECT_ID(N''COMBO_SAN_PHAM'', N''U'') IS NULL BEGIN
CREATE TABLE COMBO_SAN_PHAM (
    combo_id BIGINT NOT NULL,
    san_pham_id BIGINT NOT NULL,
    so_luong INT NOT NULL,
    PRIMARY KEY (combo_id, san_pham_id),
    FOREIGN KEY (combo_id) REFERENCES COMBO (combo_id),
    FOREIGN KEY (san_pham_id) REFERENCES SAN_PHAM (san_pham_id),
    CHECK (so_luong > 0)
);
END;

IF NOT EXISTS (
    SELECT
        1
    FROM
        sys.objects
    WHERE
        name = N''UQ_GHE_PHONG''
        AND parent_object_id = OBJECT_ID(N''GHE'')
)
ALTER TABLE GHE
ADD CONSTRAINT UQ_GHE_PHONG UNIQUE (ghe_id, phong_id);
IF NOT EXISTS (
    SELECT
        1
    FROM
        sys.objects
    WHERE
        name = N''UQ_SUAT_CHIEU_PHONG''
        AND parent_object_id = OBJECT_ID(N''SUAT_CHIEU'')
)
ALTER TABLE SUAT_CHIEU
ADD CONSTRAINT UQ_SUAT_CHIEU_PHONG UNIQUE (suat_chieu_id, phong_id);
IF NOT EXISTS (
    SELECT
        1
    FROM
        sys.objects
    WHERE
        name = N''CK_SUAT_CHIEU_TRANG_THAI''
        AND parent_object_id = OBJECT_ID(N''SUAT_CHIEU'')
)
ALTER TABLE SUAT_CHIEU
ADD CONSTRAINT CK_SUAT_CHIEU_TRANG_THAI CHECK (
    trang_thai IN (''SAP_CHIEU'', ''DANG_CHIEU'', ''DA_CHIEU'', ''HUY'')
);
IF NOT EXISTS (
    SELECT
        1
    FROM
        sys.objects
    WHERE
        name = N''CK_DON_HANG_TRANG_THAI''
        AND parent_object_id = OBJECT_ID(N''DON_HANG'')
)
ALTER TABLE DON_HANG
ADD CONSTRAINT CK_DON_HANG_TRANG_THAI CHECK (
    trang_thai IN (
        ''CHO_THANH_TOAN'',
        ''DA_THANH_TOAN'',
        ''HUY''
    )
);
IF OBJECT_ID(N''HOA_DON_VE'', N''U'') IS NULL BEGIN
CREATE TABLE HOA_DON_VE (
    hoa_don_ve_id BIGINT IDENTITY(1, 1) PRIMARY KEY,
    don_hang_id BIGINT NOT NULL UNIQUE,
    ma_hoa_don VARCHAR(30) NOT NULL UNIQUE,
    tong_tien DECIMAL(12, 2) NOT NULL CHECK (tong_tien >= 0),
    trang_thai VARCHAR(20) NOT NULL DEFAULT ''DA_THANH_TOAN'' CHECK (trang_thai = ''DA_THANH_TOAN''),
    tao_luc DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
    FOREIGN KEY (don_hang_id) REFERENCES DON_HANG (don_hang_id)
);
END;
IF OBJECT_ID(N''HOA_DON_BONG_NUOC'', N''U'') IS NULL BEGIN
CREATE TABLE HOA_DON_BONG_NUOC (
    hoa_don_bong_nuoc_id BIGINT IDENTITY(1, 1) PRIMARY KEY,
    don_hang_id BIGINT NOT NULL UNIQUE,
    ma_hoa_don VARCHAR(30) NOT NULL UNIQUE,
    tong_tien DECIMAL(12, 2) NOT NULL CHECK (tong_tien >= 0),
    trang_thai VARCHAR(20) NOT NULL DEFAULT ''DA_THANH_TOAN'' CHECK (trang_thai = ''DA_THANH_TOAN''),
    tao_luc DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
    FOREIGN KEY (don_hang_id) REFERENCES DON_HANG (don_hang_id)
);
END;
IF OBJECT_ID(N''VE'', N''U'') IS NULL BEGIN
CREATE TABLE VE (
    ve_id BIGINT IDENTITY(1, 1) PRIMARY KEY,
    hoa_don_ve_id BIGINT NULL,
    don_hang_id BIGINT NOT NULL,
    suat_chieu_id BIGINT NOT NULL,
    ghe_id BIGINT NOT NULL,
    phong_id BIGINT NOT NULL,
    ma_ve VARCHAR(30) NOT NULL UNIQUE,
    don_gia DECIMAL(12, 2) NOT NULL CHECK (don_gia >= 0),
    trang_thai VARCHAR(20) NOT NULL DEFAULT ''CHO_THANH_TOAN'' CHECK (
        trang_thai IN (
            ''CHO_THANH_TOAN'',
            ''DA_THANH_TOAN'',
            ''DA_SOAT'',
            ''HUY''
        )
    ),
    soat_ve_luc DATETIME2(0) NULL,
    nhan_vien_soat_id BIGINT NULL,
    FOREIGN KEY (hoa_don_ve_id) REFERENCES HOA_DON_VE (hoa_don_ve_id),
    FOREIGN KEY (don_hang_id) REFERENCES DON_HANG (don_hang_id),
    FOREIGN KEY (suat_chieu_id, phong_id) REFERENCES SUAT_CHIEU (suat_chieu_id, phong_id),
    FOREIGN KEY (ghe_id, phong_id) REFERENCES GHE (ghe_id, phong_id),
    FOREIGN KEY (nhan_vien_soat_id) REFERENCES NGUOI_DUNG (nguoi_dung_id),
    CONSTRAINT CK_VE_SOAT CHECK (
        (
            trang_thai = ''DA_SOAT''
            AND soat_ve_luc IS NOT NULL
            AND nhan_vien_soat_id IS NOT NULL
        )
        OR (
            trang_thai <> ''DA_SOAT''
            AND soat_ve_luc IS NULL
            AND nhan_vien_soat_id IS NULL
        )
    )
);
END;

IF NOT EXISTS (
    SELECT
        1
    FROM
        sys.indexes
    WHERE
        name = N''UX_VE_GHE_CON_HIEU_LUC''
        AND object_id = OBJECT_ID(N''VE'')
)
CREATE UNIQUE INDEX UX_VE_GHE_CON_HIEU_LUC ON VE (suat_chieu_id, ghe_id)
WHERE
    trang_thai IN (''CHO_THANH_TOAN'', ''DA_THANH_TOAN'', ''DA_SOAT'');
IF NOT EXISTS (
    SELECT
        1
    FROM
        sys.indexes
    WHERE
        name = N''IX_VE_HOA_DON''
        AND object_id = OBJECT_ID(N''VE'')
)
CREATE INDEX IX_VE_HOA_DON ON VE (hoa_don_ve_id);
IF OBJECT_ID(N''CHI_TIET_HOA_DON_BONG_NUOC'', N''U'') IS NULL BEGIN
CREATE TABLE CHI_TIET_HOA_DON_BONG_NUOC (
    chi_tiet_id BIGINT IDENTITY(1, 1) PRIMARY KEY,
    hoa_don_bong_nuoc_id BIGINT NULL,
    don_hang_id BIGINT NOT NULL,
    san_pham_id BIGINT NULL,
    combo_id BIGINT NULL,
    ten_luc_mua NVARCHAR(100) NOT NULL,
    so_luong INT NOT NULL CHECK (so_luong > 0),
    don_gia DECIMAL(12, 2) NOT NULL CHECK (don_gia >= 0),
    trang_thai_phuc_vu VARCHAR(20) NOT NULL DEFAULT ''CHO_THANH_TOAN'' CHECK (
        trang_thai_phuc_vu IN (
            ''CHO_THANH_TOAN'',
            ''CHO_NHAN'',
            ''DA_NHAN'',
            ''HUY''
        )
    ),
    nhan_luc DATETIME2(0) NULL,
    nhan_vien_giao_id BIGINT NULL,
    FOREIGN KEY (hoa_don_bong_nuoc_id) REFERENCES HOA_DON_BONG_NUOC (hoa_don_bong_nuoc_id),
    FOREIGN KEY (don_hang_id) REFERENCES DON_HANG (don_hang_id),
    FOREIGN KEY (san_pham_id) REFERENCES SAN_PHAM (san_pham_id),
    FOREIGN KEY (combo_id) REFERENCES COMBO (combo_id),
    FOREIGN KEY (nhan_vien_giao_id) REFERENCES NGUOI_DUNG (nguoi_dung_id),
    CHECK (
        (
            san_pham_id IS NOT NULL
            AND combo_id IS NULL
        )
        OR (
            san_pham_id IS NULL
            AND combo_id IS NOT NULL
        )
    ),
    CONSTRAINT CK_BONG_NUOC_NHAN CHECK (
        (
            trang_thai_phuc_vu = ''DA_NHAN''
            AND nhan_luc IS NOT NULL
            AND nhan_vien_giao_id IS NOT NULL
        )
        OR (
            trang_thai_phuc_vu <> ''DA_NHAN''
            AND nhan_luc IS NULL
            AND nhan_vien_giao_id IS NULL
        )
    )
);
END;
IF NOT EXISTS (
    SELECT
        1
    FROM
        sys.indexes
    WHERE
        name = N''IX_BONG_NUOC_HOA_DON''
        AND object_id = OBJECT_ID(N''CHI_TIET_HOA_DON_BONG_NUOC'')
)
CREATE INDEX IX_BONG_NUOC_HOA_DON ON CHI_TIET_HOA_DON_BONG_NUOC (hoa_don_bong_nuoc_id);
IF OBJECT_ID(N''THANH_TOAN'', N''U'') IS NULL BEGIN
CREATE TABLE THANH_TOAN (
    thanh_toan_id BIGINT IDENTITY(1, 1) PRIMARY KEY,
    don_hang_id BIGINT NOT NULL,
    nha_cung_cap VARCHAR(30) NOT NULL,
    ma_giao_dich VARCHAR(100) NULL,
    phuong_thuc VARCHAR(30) NOT NULL,
    so_tien DECIMAL(12, 2) NOT NULL CHECK (so_tien >= 0),
    trang_thai VARCHAR(20) NOT NULL DEFAULT ''CHO_XU_LY'' CHECK (
        trang_thai IN (''CHO_XU_LY'', ''THANH_CONG'', ''THAT_BAI'')
    ),
    thanh_toan_luc DATETIME2(0) NULL,
    FOREIGN KEY (don_hang_id) REFERENCES DON_HANG (don_hang_id),
    CHECK (
        ma_giao_dich IS NULL
        OR LEN(LTRIM(RTRIM(ma_giao_dich))) > 0
    ),
    CHECK (
        trang_thai <> ''THANH_CONG''
        OR thanh_toan_luc IS NOT NULL
    )
);
END;
IF NOT EXISTS (
    SELECT
        1
    FROM
        sys.indexes
    WHERE
        name = N''UX_THANH_TOAN_GIAO_DICH''
        AND object_id = OBJECT_ID(N''THANH_TOAN'')
)
CREATE UNIQUE INDEX UX_THANH_TOAN_GIAO_DICH ON THANH_TOAN (nha_cung_cap, ma_giao_dich)
WHERE
    ma_giao_dich IS NOT NULL;
IF NOT EXISTS (
    SELECT
        1
    FROM
        sys.indexes
    WHERE
        name = N''IX_THANH_TOAN_DON_HANG''
        AND object_id = OBJECT_ID(N''THANH_TOAN'')
)
CREATE INDEX IX_THANH_TOAN_DON_HANG ON THANH_TOAN (don_hang_id);
IF NOT EXISTS (
    SELECT
        1
    FROM
        sys.indexes
    WHERE
        name = N''IX_SUAT_CHIEU_PHONG_THOI_GIAN''
        AND object_id = OBJECT_ID(N''SUAT_CHIEU'')
)
CREATE INDEX IX_SUAT_CHIEU_PHONG_THOI_GIAN ON SUAT_CHIEU (phong_id, bat_dau) INCLUDE (ket_thuc, trang_thai);
');

    EXEC(N'USE [CineSnack];
IF OBJECT_ID(N''TR_SUAT_CHIEU_KHONG_TRUNG_GIO'') IS NULL EXEC(N''

CREATE
TRIGGER TR_SUAT_CHIEU_KHONG_TRUNG_GIO ON SUAT_CHIEU AFTER
INSERT
,
UPDATE AS BEGIN
SET NOCOUNT ON;
IF EXISTS (
    SELECT
        1
    FROM
        inserted AS i
        JOIN SUAT_CHIEU AS s
    WITH
        (
            UPDLOCK,
            HOLDLOCK,
            INDEX (IX_SUAT_CHIEU_PHONG_THOI_GIAN)
        ) ON s.phong_id = i.phong_id
        AND s.suat_chieu_id <> i.suat_chieu_id
        AND s.bat_dau < i.ket_thuc
        AND s.ket_thuc > i.bat_dau
    WHERE
        i.trang_thai <> ''''HUY''''
        AND s.trang_thai <> ''''HUY''''
) BEGIN THROW 50001,
N''''Suat chieu bi trung gio trong cung phong.'''',
1;
END;
END;
'');
');

    EXEC(N'USE [CineSnack];
IF OBJECT_ID(N''SOAT_VE'') IS NULL EXEC(N''

CREATE PROCEDURE SOAT_VE @ma_ve VARCHAR(30),
@nhan_vien_id BIGINT AS BEGIN
SET NOCOUNT ON;
SET XACT_ABORT ON;
DECLARE @luc_soat DATETIME2(0) = SYSDATETIME();
IF NOT EXISTS (
    SELECT
        1
    FROM
        NGUOI_DUNG
    WHERE
        nguoi_dung_id = @nhan_vien_id
        AND vai_tro IN (''''NHAN_VIEN'''', ''''QUAN_TRI'''')
        AND trang_thai = ''''HOAT_DONG''''
) BEGIN THROW 50002,
N''''Tai khoan khong co quyen soat ve.'''',
1;
END;
UPDATE v
SET trang_thai = ''''DA_SOAT'''',
    soat_ve_luc = @luc_soat,
    nhan_vien_soat_id = @nhan_vien_id
FROM
    VE AS v
    JOIN HOA_DON_VE AS h
WITH
    (UPDLOCK, HOLDLOCK) ON h.hoa_don_ve_id = v.hoa_don_ve_id
    JOIN DON_HANG AS d
WITH
    (UPDLOCK, HOLDLOCK) ON d.don_hang_id = h.don_hang_id
    JOIN SUAT_CHIEU AS s
WITH
    (UPDLOCK, HOLDLOCK) ON s.suat_chieu_id = v.suat_chieu_id
WHERE
    v.ma_ve = @ma_ve
    AND v.trang_thai = ''''DA_THANH_TOAN''''
    AND h.trang_thai = ''''DA_THANH_TOAN''''
    AND d.trang_thai = ''''DA_THANH_TOAN''''
    AND s.trang_thai IN (''''SAP_CHIEU'''', ''''DANG_CHIEU'''')
    AND @luc_soat >= DATEADD(MINUTE, -30, s.bat_dau)
    AND @luc_soat < s.ket_thuc;
IF @@ROWCOUNT = 0 BEGIN THROW 50003,
N''''Ve khong hop le, da soat, chua thanh toan hoac ngoai gio soat.'''',
1;
END;
SELECT
    ve_id,
    ma_ve,
    trang_thai,
    soat_ve_luc
FROM
    VE
WHERE
    ma_ve = @ma_ve;
END;
'');
');

    EXEC(N'USE [CineSnack];
IF OBJECT_ID(N''NHAN_BONG_NUOC'') IS NULL EXEC(N''

CREATE PROCEDURE NHAN_BONG_NUOC @ma_hoa_don VARCHAR(30),
@chi_tiet_id BIGINT,
@nhan_vien_id BIGINT AS BEGIN
SET NOCOUNT ON;
SET XACT_ABORT ON;
DECLARE @luc_nhan DATETIME2(0) = SYSDATETIME();
IF NOT EXISTS (
    SELECT
        1
    FROM
        NGUOI_DUNG
    WHERE
        nguoi_dung_id = @nhan_vien_id
        AND vai_tro IN (''''NHAN_VIEN'''', ''''QUAN_TRI'''')
        AND trang_thai = ''''HOAT_DONG''''
) BEGIN THROW 50004,
N''''Tai khoan khong co quyen giao hang.'''',
1;
END;
UPDATE ct
SET trang_thai_phuc_vu = ''''DA_NHAN'''',
    nhan_luc = @luc_nhan,
    nhan_vien_giao_id = @nhan_vien_id
FROM
    CHI_TIET_HOA_DON_BONG_NUOC AS ct
    JOIN HOA_DON_BONG_NUOC AS h
WITH
    (UPDLOCK, HOLDLOCK) ON h.hoa_don_bong_nuoc_id = ct.hoa_don_bong_nuoc_id
    JOIN DON_HANG AS d
WITH
    (UPDLOCK, HOLDLOCK) ON d.don_hang_id = h.don_hang_id
WHERE
    ct.chi_tiet_id = @chi_tiet_id
    AND h.ma_hoa_don = @ma_hoa_don
    AND ct.trang_thai_phuc_vu = ''''CHO_NHAN''''
    AND h.trang_thai = ''''DA_THANH_TOAN''''
    AND d.trang_thai = ''''DA_THANH_TOAN'''';
IF @@ROWCOUNT = 0 BEGIN THROW 50005,
N''''Dong hang khong hop le, da nhan, chua thanh toan hoac da huy.'''',
1;
END;
SELECT
    chi_tiet_id,
    ten_luc_mua,
    so_luong,
    trang_thai_phuc_vu,
    nhan_luc
FROM
    CHI_TIET_HOA_DON_BONG_NUOC
WHERE
    chi_tiet_id = @chi_tiet_id;
END;
'');
');

    EXEC(N'USE [CineSnack];

');

    EXEC(N'
USE [CineSnack];

SET XACT_ABORT ON;
SET ANSI_NULLS ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET QUOTED_IDENTIFIER ON;
SET NUMERIC_ROUNDABORT OFF;
IF OBJECT_ID(''HOA_DON_VE'', ''U'') IS NULL
OR OBJECT_ID(''HOA_DON_BONG_NUOC'', ''U'') IS NULL
OR COL_LENGTH(''VE'', ''hoa_don_ve_id'') IS NULL THROW 50010,
N''Schema chưa theo main; không tự chuyển hoặc xóa dữ liệu. Xem docs/payment-invoice.md.'',
1;
BEGIN TRY
    BEGIN TRANSACTION;
IF COL_LENGTH(''HOA_DON_VE'', ''email_nhan'') IS NULL
ALTER TABLE HOA_DON_VE
ADD email_nhan VARCHAR(150) NULL;
IF COL_LENGTH(''HOA_DON_VE'', ''phat_hanh_luc'') IS NULL
ALTER TABLE HOA_DON_VE
ADD phat_hanh_luc DATETIME2(0) NULL;
IF COL_LENGTH(''HOA_DON_BONG_NUOC'', ''email_nhan'') IS NULL
ALTER TABLE HOA_DON_BONG_NUOC
ADD email_nhan VARCHAR(150) NULL;
IF COL_LENGTH(''HOA_DON_BONG_NUOC'', ''phat_hanh_luc'') IS NULL
ALTER TABLE HOA_DON_BONG_NUOC
ADD phat_hanh_luc DATETIME2(0) NULL;
IF COL_LENGTH(''VE'', ''mo_ta_luc_mua'') IS NULL
ALTER TABLE VE
ADD mo_ta_luc_mua NVARCHAR(400) NULL;

EXEC(N''UPDATE h
SET email_nhan = d.email_nhan_ve,
    phat_hanh_luc = t.luc
FROM
    HOA_DON_VE h
    JOIN DON_HANG d ON d.don_hang_id = h.don_hang_id
    OUTER APPLY (
        SELECT
            MAX(thanh_toan_luc) luc
        FROM
            THANH_TOAN
        WHERE
            don_hang_id = d.don_hang_id
            AND trang_thai = ''''THANH_CONG''''
    ) t
WHERE
    h.email_nhan IS NULL
'');
EXEC(N''UPDATE h
SET email_nhan = d.email_nhan_ve,
    phat_hanh_luc = t.luc
FROM
    HOA_DON_BONG_NUOC h
    JOIN DON_HANG d ON d.don_hang_id = h.don_hang_id
    OUTER APPLY (
        SELECT
            MAX(thanh_toan_luc) luc
        FROM
            THANH_TOAN
        WHERE
            don_hang_id = d.don_hang_id
            AND trang_thai = ''''THANH_CONG''''
    ) t
WHERE
    h.email_nhan IS NULL
'');
EXEC(N''UPDATE v
SET mo_ta_luc_mua = CONCAT(
        p.ten_phim,
        N'''' | '''',
        r.ten_phong,
        N'''' | '''',
        g.ma_ghe,
        N'''' | '''',
        CONVERT(VARCHAR(19), s.bat_dau, 120)
    )
FROM
    VE v
    JOIN SUAT_CHIEU s ON s.suat_chieu_id = v.suat_chieu_id
    JOIN PHIM p ON p.phim_id = s.phim_id
    JOIN PHONG_CHIEU r ON r.phong_id = s.phong_id
    JOIN GHE g ON g.ghe_id = v.ghe_id
WHERE
    v.mo_ta_luc_mua IS NULL
'');
IF NOT EXISTS (
    SELECT
        1
    FROM
        sys.indexes
    WHERE
        object_id = OBJECT_ID(''THANH_TOAN'')
        AND name = ''UX_THANH_TOAN_MOT_LAN_THANH_CONG''
)
CREATE UNIQUE INDEX UX_THANH_TOAN_MOT_LAN_THANH_CONG ON THANH_TOAN (don_hang_id)
WHERE
    trang_thai = ''THANH_CONG'';
IF NOT EXISTS (
    SELECT
        1
    FROM
        sys.indexes
    WHERE
        object_id = OBJECT_ID(''DON_HANG'')
        AND name = ''IX_DON_HANG_HET_HAN''
)
CREATE INDEX IX_DON_HANG_HET_HAN ON DON_HANG (trang_thai, het_han_luc) INCLUDE (ma_don_hang);
IF NOT EXISTS (
    SELECT
        1
    FROM
        sys.indexes
    WHERE
        object_id = OBJECT_ID(''DON_HANG'')
        AND name = ''IX_DON_HANG_NGUOI_DUNG''
)
CREATE INDEX IX_DON_HANG_NGUOI_DUNG ON DON_HANG (nguoi_dung_id, don_hang_id DESC);
COMMIT;
END TRY BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK;
THROW;
END CATCH;
');

    EXEC(N'
USE [CineSnack];

USE [CineSnack];
SET XACT_ABORT ON;
BEGIN TRY
    BEGIN TRANSACTION;
IF NOT EXISTS (
    SELECT
        1
    FROM
        PHIM
    WITH
        (UPDLOCK, HOLDLOCK)
    WHERE
        ten_phim = N''Dune: Hành tinh cát – Phần hai''
)
INSERT INTO
    PHIM (
        ten_phim,
        the_loai,
        thoi_luong_phut,
        phan_loai_do_tuoi,
        poster_url,
        trang_thai
    )
VALUES
    (
        N''Dune: Hành tinh cát – Phần hai'',
        N''Khoa học viễn tưởng, Phiêu lưu'',
        166,
        N''T16'',
        N''/assets/images/movies/dune.jpg'',
        N''DANG_CHIEU''
    );
IF NOT EXISTS (
    SELECT
        1
    FROM
        PHIM
    WITH
        (UPDLOCK, HOLDLOCK)
    WHERE
        ten_phim = N''Những mảnh ghép cảm xúc 2''
)
INSERT INTO
    PHIM (
        ten_phim,
        the_loai,
        thoi_luong_phut,
        phan_loai_do_tuoi,
        poster_url,
        trang_thai
    )
VALUES
    (
        N''Những mảnh ghép cảm xúc 2'',
        N''Hoạt hình, Gia đình'',
        96,
        N''P'',
        N''/assets/images/movies/inside-out.jpg'',
        N''DANG_CHIEU''
    );
IF NOT EXISTS (
    SELECT
        1
    FROM
        PHIM
    WITH
        (UPDLOCK, HOLDLOCK)
    WHERE
        ten_phim = N''Kung Fu Panda 4''
)
INSERT INTO
    PHIM (
        ten_phim,
        the_loai,
        thoi_luong_phut,
        phan_loai_do_tuoi,
        poster_url,
        trang_thai
    )
VALUES
    (
        N''Kung Fu Panda 4'',
        N''Hoạt hình, Hành động'',
        94,
        N''P'',
        N''/assets/images/movies/kung-fu-panda.jpg'',
        N''DANG_CHIEU''
    );
IF NOT EXISTS (
    SELECT
        1
    FROM
        PHIM
    WITH
        (UPDLOCK, HOLDLOCK)
    WHERE
        ten_phim = N''Godzilla x Kong: Đế chế mới''
)
INSERT INTO
    PHIM (
        ten_phim,
        the_loai,
        thoi_luong_phut,
        phan_loai_do_tuoi,
        poster_url,
        trang_thai
    )
VALUES
    (
        N''Godzilla x Kong: Đế chế mới'',
        N''Hành động, Phiêu lưu'',
        115,
        N''T13'',
        N''/assets/images/movies/godzilla.jpg'',
        N''DANG_CHIEU''
    );
IF NOT EXISTS (
    SELECT
        1
    FROM
        PHIM
    WITH
        (UPDLOCK, HOLDLOCK)
    WHERE
        ten_phim = N''Người Nhện: Du hành vũ trụ nhện''
)
INSERT INTO
    PHIM (
        ten_phim,
        the_loai,
        thoi_luong_phut,
        phan_loai_do_tuoi,
        poster_url,
        trang_thai
    )
VALUES
    (
        N''Người Nhện: Du hành vũ trụ nhện'',
        N''Hoạt hình, Phiêu lưu'',
        140,
        N''T13'',
        N''/assets/images/movies/spider-man.jpg'',
        N''SAP_CHIEU''
    );
IF NOT EXISTS (
    SELECT
        1
    FROM
        PHIM
    WITH
        (UPDLOCK, HOLDLOCK)
    WHERE
        ten_phim = N''Interstellar: Hố đen tử thần''
)
INSERT INTO
    PHIM (
        ten_phim,
        the_loai,
        thoi_luong_phut,
        phan_loai_do_tuoi,
        poster_url,
        trang_thai
    )
VALUES
    (
        N''Interstellar: Hố đen tử thần'',
        N''Khoa học viễn tưởng'',
        169,
        N''T13'',
        N''/assets/images/movies/interstellar.jpg'',
        N''SAP_CHIEU''
    );
IF NOT EXISTS (
    SELECT
        1
    FROM
        COMBO
    WITH
        (UPDLOCK, HOLDLOCK)
    WHERE
        ten_combo = N''Combo Một Mình Vui''
)
INSERT INTO
    COMBO (ten_combo, gia_ban, trang_thai)
VALUES
    (N''Combo Một Mình Vui'', 59000, ''HOAT_DONG'');
IF NOT EXISTS (
    SELECT
        1
    FROM
        COMBO
    WITH
        (UPDLOCK, HOLDLOCK)
    WHERE
        ten_combo = N''Combo Hẹn Hò''
)
INSERT INTO
    COMBO (ten_combo, gia_ban, trang_thai)
VALUES
    (N''Combo Hẹn Hò'', 99000, ''HOAT_DONG'');
COMMIT;
END TRY BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK;
THROW;
END CATCH;
');

    EXEC(N'
USE [CineSnack];

USE [CineSnack];
SET XACT_ABORT ON;
SET ANSI_NULLS ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET QUOTED_IDENTIFIER ON;
SET NUMERIC_ROUNDABORT OFF;
BEGIN TRY
    BEGIN TRANSACTION;
IF NOT EXISTS (
    SELECT
        1
    FROM
        PHONG_CHIEU
    WITH
        (UPDLOCK, HOLDLOCK)
    WHERE
        ten_phong = N''Phòng 01''
)
INSERT INTO
    PHONG_CHIEU (ten_phong, loai_phong, trang_thai)
VALUES
    (N''Phòng 01'', N''2D'', ''HOAT_DONG'');
DECLARE @phong BIGINT = (
    SELECT
        phong_id
    FROM
        PHONG_CHIEU
    WITH
        (UPDLOCK, HOLDLOCK)
    WHERE
        ten_phong = N''Phòng 01''
);
IF NOT EXISTS (
    SELECT
        1
    FROM
        PHONG_CHIEU
    WHERE
        phong_id = @phong
        AND trang_thai = ''HOAT_DONG''
) THROW 50011,
N''Phòng 01 đã ngừng hoạt động; không tự thay đổi trạng thái.'',
1;
INSERT INTO
    GHE (phong_id, ma_ghe, loai_ghe, trang_thai)
SELECT
    @phong,
    CONCAT(h.hang, s.so),
    CASE
        WHEN h.hang IN (''D'', ''E'', ''F'') THEN ''VIP''
        ELSE ''THUONG''
    END,
    ''HOAT_DONG''
FROM
    (
        VALUES
            (''A''),
            (''B''),
            (''C''),
            (''D''),
            (''E''),
            (''F'')
    ) h (hang)
    CROSS JOIN (
        VALUES
            (1),
            (2),
            (3),
            (4),
            (5),
            (6),
            (7),
            (8),
            (9),
            (10)
    ) s (so)
WHERE
    NOT EXISTS (
        SELECT
            1
        FROM
            GHE
        WITH
            (UPDLOCK, HOLDLOCK)
        WHERE
            phong_id = @phong
            AND ma_ghe = CONCAT(h.hang, s.so)
    );
DECLARE @ngay DATETIME2(0) = DATEADD(
    DAY,
    1,
    CONVERT(
        DATE,
        SYSDATETIMEOFFSET() AT TIME ZONE ''SE Asia Standard Time''
    )
);
DECLARE @lich
TABLE (ten_phim NVARCHAR(200), phut INT);
INSERT INTO
    @lich
VALUES
    (N''Dune: Hành tinh cát – Phần hai'', 600),
    (N''Những mảnh ghép cảm xúc 2'', 795),
    (N''Kung Fu Panda 4'', 990),
    (N''Godzilla x Kong: Đế chế mới'', 1185);
IF EXISTS (
    SELECT
        1
    FROM
        @lich l
    WHERE
        NOT EXISTS (
            SELECT
                1
            FROM
                PHIM p
            WHERE
                p.ten_phim = l.ten_phim
                AND p.trang_thai = ''DANG_CHIEU''
        )
) THROW 50012,
N''Thiếu phim đang chiếu; kiểm tra danh mục trước khi bổ sung suất.'',
1;
INSERT INTO
    SUAT_CHIEU (
        phim_id,
        phong_id,
        bat_dau,
        ket_thuc,
        gia_co_ban,
        trang_thai
    )
SELECT
    p.phim_id,
    @phong,
    t.bat_dau,
    DATEADD(MINUTE, p.thoi_luong_phut, t.bat_dau),
    75000,
    ''SAP_CHIEU''
FROM
    @lich l
    JOIN PHIM p ON p.ten_phim = l.ten_phim
    AND p.trang_thai = ''DANG_CHIEU''
    CROSS APPLY (
        SELECT
            DATEADD(MINUTE, l.phut, @ngay) bat_dau
    ) t
WHERE
    NOT EXISTS (
        SELECT
            1
        FROM
            SUAT_CHIEU
        WITH
            (UPDLOCK, HOLDLOCK)
        WHERE
            phong_id = @phong
            AND bat_dau = t.bat_dau
    );

COMMIT;
END TRY BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK;
THROW;
END CATCH;
');

    EXEC(N'
INSERT INTO
    NGUOI_DUNG (ho_ten, email, mat_khau_hash, vai_tro, trang_thai)
SELECT
    x.ten,
    x.email,
    ''$2a$12$kn3UMtBfNnb0Fkv70s2Pkurd999oOUnnR9Fygd75VGMFhN2zCSrj6'',
    x.vai_tro,
    ''HOAT_DONG''
FROM
    (
        VALUES
            (
                N''Admin CineSnack'',
                ''admin@cinesnack.local'',
                ''QUAN_TRI''
            ),
            (
                N''User CineSnack'',
                ''user@cinesnack.local'',
                ''KHACH_HANG''
            ),
            (
                N''Staff CineSnack'',
                ''staff@cinesnack.local'',
                ''NHAN_VIEN''
            )
    ) x (ten, email, vai_tro)
WHERE
    NOT EXISTS (
        SELECT
            1
        FROM
            NGUOI_DUNG
        WITH
            (UPDLOCK, HOLDLOCK)
        WHERE
            email = x.email
    );
');
    COMMIT;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK;
    THROW;
END CATCH;
