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

-- 10. Ve gan voi mot don hang, mot suat chieu va mot ghe.
CREATE TABLE VE (
    ve_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    don_hang_id BIGINT NOT NULL,
    suat_chieu_id BIGINT NOT NULL,
    ghe_id BIGINT NOT NULL,
    ma_ve VARCHAR(30) NOT NULL UNIQUE,
    don_gia DECIMAL(12,2) NOT NULL,
    trang_thai VARCHAR(20) NOT NULL,
    soat_ve_luc DATETIME2(0) NULL,
    FOREIGN KEY (don_hang_id) REFERENCES DON_HANG(don_hang_id),
    FOREIGN KEY (suat_chieu_id) REFERENCES SUAT_CHIEU(suat_chieu_id),
    FOREIGN KEY (ghe_id) REFERENCES GHE(ghe_id),
    CHECK (don_gia >= 0)
);

-- 11. Mot don hang co the co nhieu lan thu thanh toan.
CREATE TABLE THANH_TOAN (
    thanh_toan_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    don_hang_id BIGINT NOT NULL,
    ma_giao_dich VARCHAR(100) NULL,
    phuong_thuc VARCHAR(30) NOT NULL,
    so_tien DECIMAL(12,2) NOT NULL,
    trang_thai VARCHAR(20) NOT NULL,
    thanh_toan_luc DATETIME2(0) NULL,
    FOREIGN KEY (don_hang_id) REFERENCES DON_HANG(don_hang_id),
    CHECK (so_tien >= 0)
);

-- 12. Moi dong don hang chon MOT san pham le HOAC MOT combo.
CREATE TABLE CHI_TIET_DON_HANG (
    chi_tiet_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    don_hang_id BIGINT NOT NULL,
    san_pham_id BIGINT NULL,
    combo_id BIGINT NULL,
    ten_luc_mua NVARCHAR(100) NOT NULL,
    so_luong INT NOT NULL,
    don_gia DECIMAL(12,2) NOT NULL,
    trang_thai_phuc_vu VARCHAR(20) NOT NULL,
    FOREIGN KEY (don_hang_id) REFERENCES DON_HANG(don_hang_id),
    FOREIGN KEY (san_pham_id) REFERENCES SAN_PHAM(san_pham_id),
    FOREIGN KEY (combo_id) REFERENCES COMBO(combo_id),
    CHECK (so_luong > 0),
    CHECK (don_gia >= 0),
    CHECK (
        (san_pham_id IS NOT NULL AND combo_id IS NULL)
        OR (san_pham_id IS NULL AND combo_id IS NOT NULL)
    )
);

-- CAN XU LY TRONG CODE UNG DUNG:
-- 1. Ghe tren VE phai thuoc cung phong voi SUAT_CHIEU.
-- 2. Mot ghe khong duoc ban trung trong cung suat chieu khi ve con hieu luc.
--    Kiem tra truoc khi ghi VE. Neu co nhieu nguoi dat dong thoi, can them co che
--    khoa/kiem soat dong thoi; transaction thong thuong mot minh chua du.
-- 3. Hai suat chieu khong duoc trung gio trong cung phong.
-- 4. Het han giu cho: doi trang thai ve/don hang va mo ban lai ghe.
-- 5. tong_tien = tong gia ve + tong(so_luong * don_gia) cua bong nuoc.
-- 6. Luu mat khau da hash, khong luu mat khau goc.



