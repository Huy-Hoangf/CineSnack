-- CANH BAO: moi lan chay se XOA TOAN BO du lieu cu trong database CineSnack.

USE master;
GO

IF DB_ID(N'CineSnack') IS NOT NULL
BEGIN
    ALTER DATABASE [CineSnack] SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
    DROP DATABASE [CineSnack];
END;
GO

CREATE DATABASE [CineSnack];
GO

USE [CineSnack];
GO

-- 1. Moi phong co nhieu ghe va nhieu suat chieu.
CREATE TABLE PHONG_CHIEU (
    phong_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    ten_phong NVARCHAR(50) NOT NULL UNIQUE,
    loai_phong NVARCHAR(30) NOT NULL,
    trang_thai VARCHAR(20) NOT NULL
);

-- 2. Mot phim co the co nhieu suat chieu.
CREATE TABLE PHIM (
    phim_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    ten_phim NVARCHAR(200) NOT NULL,
    the_loai NVARCHAR(100) NOT NULL,
    thoi_luong_phut INT NOT NULL,
    phan_loai_do_tuoi VARCHAR(10) NOT NULL,
    poster_url NVARCHAR(500) NULL,
    trang_thai VARCHAR(20) NOT NULL,
    CHECK (thoi_luong_phut > 0)
);

-- 3. Tai khoan khach hang, nhan vien va quan tri.
CREATE TABLE NGUOI_DUNG (
    nguoi_dung_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    ho_ten NVARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    mat_khau_hash VARCHAR(255) NOT NULL,
    vai_tro VARCHAR(20) NOT NULL,
    trang_thai VARCHAR(20) NOT NULL
);

-- 4. San pham ban le: bong, nuoc, do an vat.
CREATE TABLE SAN_PHAM (
    san_pham_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    ten_san_pham NVARCHAR(100) NOT NULL,
    loai_san_pham VARCHAR(20) NOT NULL,
    gia_ban DECIMAL(12,2) NOT NULL,
    trang_thai VARCHAR(20) NOT NULL,
    CHECK (gia_ban >= 0)
);

-- 5. Combo co gia ban rieng.
CREATE TABLE COMBO (
    combo_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    ten_combo NVARCHAR(100) NOT NULL,
    gia_ban DECIMAL(12,2) NOT NULL,
    trang_thai VARCHAR(20) NOT NULL,
    CHECK (gia_ban >= 0)
);

-- 6. Ma ghe chi can duy nhat trong cung mot phong.
CREATE TABLE GHE (
    ghe_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    phong_id BIGINT NOT NULL,
    ma_ghe VARCHAR(10) NOT NULL,
    loai_ghe VARCHAR(20) NOT NULL,
    trang_thai VARCHAR(20) NOT NULL,
    FOREIGN KEY (phong_id) REFERENCES PHONG_CHIEU(phong_id),
    UNIQUE (phong_id, ma_ghe)
);

-- 7. Mot suat chieu chieu mot phim tai mot phong.
CREATE TABLE SUAT_CHIEU (
    suat_chieu_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    phim_id BIGINT NOT NULL,
    phong_id BIGINT NOT NULL,
    bat_dau DATETIME2(0) NOT NULL,
    ket_thuc DATETIME2(0) NOT NULL,
    gia_co_ban DECIMAL(12,2) NOT NULL,
    trang_thai VARCHAR(20) NOT NULL,
    FOREIGN KEY (phim_id) REFERENCES PHIM(phim_id),
    FOREIGN KEY (phong_id) REFERENCES PHONG_CHIEU(phong_id),
    CHECK (ket_thuc > bat_dau),
    CHECK (gia_co_ban >= 0)
);

-- 8. Nguoi dung co the NULL neu cho phep khach mua khong dang nhap.
CREATE TABLE DON_HANG (
    don_hang_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    nguoi_dung_id BIGINT NULL,
    ma_don_hang VARCHAR(30) NOT NULL UNIQUE,
    email_nhan_ve VARCHAR(150) NOT NULL,
    tong_tien DECIMAL(12,2) NOT NULL,
    trang_thai VARCHAR(20) NOT NULL,
    het_han_luc DATETIME2(0) NULL,
    tao_luc DATETIME2(0) NOT NULL,
    FOREIGN KEY (nguoi_dung_id) REFERENCES NGUOI_DUNG(nguoi_dung_id),
    CHECK (tong_tien >= 0)
);

-- 9. Bang trung gian: mot combo gom nhieu san pham.
CREATE TABLE COMBO_SAN_PHAM (
    combo_id BIGINT NOT NULL,
    san_pham_id BIGINT NOT NULL,
    so_luong INT NOT NULL,
    PRIMARY KEY (combo_id, san_pham_id),
    FOREIGN KEY (combo_id) REFERENCES COMBO(combo_id),
    FOREIGN KEY (san_pham_id) REFERENCES SAN_PHAM(san_pham_id),
    CHECK (so_luong > 0)
);

-- BAN DE XUAT CHO REVIEW. Khong chay tren database co du lieu can giu.
-- DON_HANG lien ket mot lan mua, co the chi mua ve hoac chi mua bong nuoc.
-- Thanh toan chung theo DON_HANG. Moi loai co toi da mot hoa don / don hang.
ALTER TABLE GHE ADD CONSTRAINT UQ_GHE_PHONG UNIQUE (ghe_id, phong_id);
ALTER TABLE SUAT_CHIEU ADD CONSTRAINT UQ_SUAT_CHIEU_PHONG
    UNIQUE (suat_chieu_id, phong_id);
ALTER TABLE SUAT_CHIEU ADD CONSTRAINT CK_SUAT_CHIEU_TRANG_THAI
    CHECK (trang_thai IN ('SAP_CHIEU', 'DANG_CHIEU', 'DA_CHIEU', 'HUY'));
ALTER TABLE DON_HANG ADD CONSTRAINT CK_DON_HANG_TRANG_THAI
    CHECK (trang_thai IN ('CHO_THANH_TOAN', 'DA_THANH_TOAN', 'HUY', 'HET_HAN'));

CREATE TABLE HOA_DON_VE (
    hoa_don_ve_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    don_hang_id BIGINT NOT NULL UNIQUE,
    ma_hoa_don VARCHAR(30) NOT NULL UNIQUE,
    tong_tien DECIMAL(12,2) NOT NULL CHECK (tong_tien >= 0),
    trang_thai VARCHAR(20) NOT NULL DEFAULT 'CHO_THANH_TOAN'
        CHECK (trang_thai IN ('CHO_THANH_TOAN', 'DA_THANH_TOAN', 'HUY', 'HET_HAN')),
    tao_luc DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
    FOREIGN KEY (don_hang_id) REFERENCES DON_HANG(don_hang_id)
);

CREATE TABLE HOA_DON_BONG_NUOC (
    hoa_don_bong_nuoc_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    don_hang_id BIGINT NOT NULL UNIQUE,
    ma_hoa_don VARCHAR(30) NOT NULL UNIQUE,
    tong_tien DECIMAL(12,2) NOT NULL CHECK (tong_tien >= 0),
    trang_thai VARCHAR(20) NOT NULL DEFAULT 'CHO_THANH_TOAN'
        CHECK (trang_thai IN ('CHO_THANH_TOAN', 'DA_THANH_TOAN', 'HUY', 'HET_HAN')),
    tao_luc DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
    FOREIGN KEY (don_hang_id) REFERENCES DON_HANG(don_hang_id)
);

CREATE TABLE VE (
    ve_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    hoa_don_ve_id BIGINT NOT NULL,
    suat_chieu_id BIGINT NOT NULL,
    ghe_id BIGINT NOT NULL,
    phong_id BIGINT NOT NULL,
    ma_ve VARCHAR(30) NOT NULL UNIQUE,
    don_gia DECIMAL(12,2) NOT NULL CHECK (don_gia >= 0),
    trang_thai VARCHAR(20) NOT NULL DEFAULT 'CHO_THANH_TOAN'
        CHECK (trang_thai IN ('CHO_THANH_TOAN', 'DA_THANH_TOAN', 'DA_SOAT', 'HUY', 'HET_HAN')),
    soat_ve_luc DATETIME2(0) NULL,
    nhan_vien_soat_id BIGINT NULL,
    FOREIGN KEY (hoa_don_ve_id) REFERENCES HOA_DON_VE(hoa_don_ve_id),
    FOREIGN KEY (suat_chieu_id, phong_id)
        REFERENCES SUAT_CHIEU(suat_chieu_id, phong_id),
    FOREIGN KEY (ghe_id, phong_id) REFERENCES GHE(ghe_id, phong_id),
    FOREIGN KEY (nhan_vien_soat_id) REFERENCES NGUOI_DUNG(nguoi_dung_id),
    CONSTRAINT CK_VE_SOAT CHECK (
        (trang_thai = 'DA_SOAT' AND soat_ve_luc IS NOT NULL AND nhan_vien_soat_id IS NOT NULL)
        OR (trang_thai <> 'DA_SOAT' AND soat_ve_luc IS NULL AND nhan_vien_soat_id IS NULL)
    )
);

-- Ve da soat van chiem ghe; ve huy/het han duoc phep ban lai ghe.
CREATE UNIQUE INDEX UX_VE_GHE_CON_HIEU_LUC
    ON VE(suat_chieu_id, ghe_id)
    WHERE trang_thai IN ('CHO_THANH_TOAN', 'DA_THANH_TOAN', 'DA_SOAT');
CREATE INDEX IX_VE_HOA_DON ON VE(hoa_don_ve_id);

CREATE TABLE CHI_TIET_HOA_DON_BONG_NUOC (
    chi_tiet_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    hoa_don_bong_nuoc_id BIGINT NOT NULL,
    san_pham_id BIGINT NULL,
    combo_id BIGINT NULL,
    ten_luc_mua NVARCHAR(100) NOT NULL,
    so_luong INT NOT NULL CHECK (so_luong > 0),
    don_gia DECIMAL(12,2) NOT NULL CHECK (don_gia >= 0),
    trang_thai_phuc_vu VARCHAR(20) NOT NULL DEFAULT 'CHO_THANH_TOAN'
        CHECK (trang_thai_phuc_vu IN ('CHO_THANH_TOAN', 'CHO_NHAN', 'DA_NHAN', 'HUY', 'HET_HAN')),
    nhan_luc DATETIME2(0) NULL,
    nhan_vien_giao_id BIGINT NULL,
    FOREIGN KEY (hoa_don_bong_nuoc_id)
        REFERENCES HOA_DON_BONG_NUOC(hoa_don_bong_nuoc_id),
    FOREIGN KEY (san_pham_id) REFERENCES SAN_PHAM(san_pham_id),
    FOREIGN KEY (combo_id) REFERENCES COMBO(combo_id),
    FOREIGN KEY (nhan_vien_giao_id) REFERENCES NGUOI_DUNG(nguoi_dung_id),
    CHECK (
        (san_pham_id IS NOT NULL AND combo_id IS NULL)
        OR (san_pham_id IS NULL AND combo_id IS NOT NULL)
    ),
    CONSTRAINT CK_BONG_NUOC_NHAN CHECK (
        (trang_thai_phuc_vu = 'DA_NHAN' AND nhan_luc IS NOT NULL AND nhan_vien_giao_id IS NOT NULL)
        OR (trang_thai_phuc_vu <> 'DA_NHAN' AND nhan_luc IS NULL AND nhan_vien_giao_id IS NULL)
    )
);
CREATE INDEX IX_BONG_NUOC_HOA_DON
    ON CHI_TIET_HOA_DON_BONG_NUOC(hoa_don_bong_nuoc_id);

CREATE TABLE THANH_TOAN (
    thanh_toan_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    don_hang_id BIGINT NOT NULL,
    nha_cung_cap VARCHAR(30) NOT NULL,
    ma_giao_dich VARCHAR(100) NULL,
    phuong_thuc VARCHAR(30) NOT NULL,
    so_tien DECIMAL(12,2) NOT NULL CHECK (so_tien >= 0),
    trang_thai VARCHAR(20) NOT NULL DEFAULT 'CHO_XU_LY'
        CHECK (trang_thai IN ('CHO_XU_LY', 'THANH_CONG', 'THAT_BAI')),
    thanh_toan_luc DATETIME2(0) NULL,
    FOREIGN KEY (don_hang_id) REFERENCES DON_HANG(don_hang_id),
    CHECK (ma_giao_dich IS NULL OR LEN(LTRIM(RTRIM(ma_giao_dich))) > 0),
    CHECK (trang_thai <> 'THANH_CONG' OR thanh_toan_luc IS NOT NULL)
);
CREATE UNIQUE INDEX UX_THANH_TOAN_GIAO_DICH
    ON THANH_TOAN(nha_cung_cap, ma_giao_dich)
    WHERE ma_giao_dich IS NOT NULL;
CREATE INDEX IX_THANH_TOAN_DON_HANG ON THANH_TOAN(don_hang_id);

CREATE INDEX IX_SUAT_CHIEU_PHONG_THOI_GIAN
    ON SUAT_CHIEU(phong_id, bat_dau) INCLUDE (ket_thuc, trang_thai);
GO

-- Bao ve khi INSERT/UPDATE, ke ca cap nhat nhieu suat chieu trong mot lenh.
-- UPDLOCK + HOLDLOCK giu khoa den cuoi transaction; backend retry khi deadlock.
-- Hai khoang tiep giap (ket_thuc = bat_dau) duoc phep.
CREATE TRIGGER TR_SUAT_CHIEU_KHONG_TRUNG_GIO
ON SUAT_CHIEU
AFTER INSERT, UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS (
        SELECT 1
        FROM inserted AS i
        JOIN SUAT_CHIEU AS s WITH (UPDLOCK, HOLDLOCK, INDEX(IX_SUAT_CHIEU_PHONG_THOI_GIAN))
          ON s.phong_id = i.phong_id
         AND s.suat_chieu_id <> i.suat_chieu_id
         AND s.bat_dau < i.ket_thuc
         AND s.ket_thuc > i.bat_dau
        WHERE i.trang_thai <> 'HUY' AND s.trang_thai <> 'HUY'
    )
    BEGIN
        THROW 50001, N'Suat chieu bi trung gio trong cung phong.', 1;
    END;
END;
GO

-- @nhan_vien_id phai lay tu danh tinh dang nhap, KHONG lay tu request cua khach.
-- Backend phai kiem tra quyen; SQL kiem tra them vai tro va trang thai tai khoan.
-- Gia dinh soat ve tu 30 phut truoc gio chieu den truoc gio ket thuc.
CREATE PROCEDURE SOAT_VE
    @ma_ve VARCHAR(30),
    @nhan_vien_id BIGINT
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;
    DECLARE @luc_soat DATETIME2(0) = SYSDATETIME();
    IF NOT EXISTS (
        SELECT 1 FROM NGUOI_DUNG
        WHERE nguoi_dung_id = @nhan_vien_id
          AND vai_tro IN ('NHAN_VIEN', 'QUAN_TRI')
          AND trang_thai = 'HOAT_DONG'
    )
    BEGIN
        THROW 50002, N'Tai khoan khong co quyen soat ve.', 1;
    END;

    UPDATE v
    SET trang_thai = 'DA_SOAT', soat_ve_luc = @luc_soat,
        nhan_vien_soat_id = @nhan_vien_id
    FROM VE AS v
    JOIN HOA_DON_VE AS h WITH (UPDLOCK, HOLDLOCK)
      ON h.hoa_don_ve_id = v.hoa_don_ve_id
    JOIN DON_HANG AS d WITH (UPDLOCK, HOLDLOCK)
      ON d.don_hang_id = h.don_hang_id
    JOIN SUAT_CHIEU AS s WITH (UPDLOCK, HOLDLOCK)
      ON s.suat_chieu_id = v.suat_chieu_id
    WHERE v.ma_ve = @ma_ve AND v.trang_thai = 'DA_THANH_TOAN'
      AND h.trang_thai = 'DA_THANH_TOAN'
      AND d.trang_thai = 'DA_THANH_TOAN'
      AND s.trang_thai IN ('SAP_CHIEU', 'DANG_CHIEU')
      AND @luc_soat >= DATEADD(MINUTE, -30, s.bat_dau)
      AND @luc_soat < s.ket_thuc;

    IF @@ROWCOUNT = 0
    BEGIN
        THROW 50003, N'Ve khong hop le, da soat, chua thanh toan hoac ngoai gio soat.', 1;
    END;
    SELECT ve_id, ma_ve, trang_thai, soat_ve_luc
    FROM VE WHERE ma_ve = @ma_ve;
END;
GO

-- Xac nhan TOAN BO so_luong cua mot dong; cac dong khac co the nhan sau.
-- QR hoa don giup backend liet ke dong hang; backend xac minh dong thuoc QR.
CREATE PROCEDURE NHAN_BONG_NUOC
    @ma_hoa_don VARCHAR(30),
    @chi_tiet_id BIGINT,
    @nhan_vien_id BIGINT
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;
    DECLARE @luc_nhan DATETIME2(0) = SYSDATETIME();
    IF NOT EXISTS (
        SELECT 1 FROM NGUOI_DUNG
        WHERE nguoi_dung_id = @nhan_vien_id
          AND vai_tro IN ('NHAN_VIEN', 'QUAN_TRI')
          AND trang_thai = 'HOAT_DONG'
    )
    BEGIN
        THROW 50004, N'Tai khoan khong co quyen giao hang.', 1;
    END;

    UPDATE ct
    SET trang_thai_phuc_vu = 'DA_NHAN', nhan_luc = @luc_nhan,
        nhan_vien_giao_id = @nhan_vien_id
    FROM CHI_TIET_HOA_DON_BONG_NUOC AS ct
    JOIN HOA_DON_BONG_NUOC AS h WITH (UPDLOCK, HOLDLOCK)
      ON h.hoa_don_bong_nuoc_id = ct.hoa_don_bong_nuoc_id
    JOIN DON_HANG AS d WITH (UPDLOCK, HOLDLOCK)
      ON d.don_hang_id = h.don_hang_id
    WHERE ct.chi_tiet_id = @chi_tiet_id
      AND h.ma_hoa_don = @ma_hoa_don
      AND ct.trang_thai_phuc_vu = 'CHO_NHAN'
      AND h.trang_thai = 'DA_THANH_TOAN'
      AND d.trang_thai = 'DA_THANH_TOAN';

    IF @@ROWCOUNT = 0
    BEGIN
        THROW 50005, N'Dong hang khong hop le, da nhan, chua thanh toan hoac da huy.', 1;
    END;
    SELECT chi_tiet_id, ten_luc_mua, so_luong, trang_thai_phuc_vu, nhan_luc
    FROM CHI_TIET_HOA_DON_BONG_NUOC WHERE chi_tiet_id = @chi_tiet_id;
END;
GO

-- CAN XU LY TRONG BACKEND, TRONG CUNG TRANSACTION:
-- 1. Tinh tong_tien tung hoa don tu chi tiet; DON_HANG = tong hai hoa don hien co.
--    Khong nhan tong tien/gia tu frontend; khong sua gia/chi tiet sau thanh toan.
-- 2. Khi thanh toan thanh cong: doi DON_HANG, hoa don va VE sang DA_THANH_TOAN;
--    doi dong bong nuoc sang CHO_NHAN. Xac minh chu ky, so tien, ma don cua cong.
--    Xu ly callback lap theo nha_cung_cap + ma_giao_dich; unique index chi chan ghi trung.
-- 3. Job het han doi DON_HANG, hoa don, VE va dong bong nuoc CHO_THANH_TOAN
--    sang HET_HAN. Su dung khoa tren DON_HANG de tranh xung dot voi thanh toan.
--    Het_han_luc khong tu dong giai phong ghe; phai cap nhat trang thai VE.
-- 4. Tao ve chi cho suat/ghe dang mo ban; huy suat can cap nhat don/hoa don/ve lien quan.
-- 5. Soat ve/giao hang qua procedure, tham so nhan vien tu session dang nhap.
--    QR nen chua token kho doan, khong su dung ID tuan tu de xac thuc.
-- 6. Vai tro nhan vien: NHAN_VIEN/QUAN_TRI; tai khoan hoat dong: HOAT_DONG.
-- 7. Hoan tien/huy sau DA_SOAT hoac DA_NHAN chua thuoc pham vi ban de xuat nay.
-- 8. Khong chay script reset de nang cap database co du lieu; can migration rieng.
