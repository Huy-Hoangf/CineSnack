-- Lịch mẫu cho ngày mai tại Phòng 01; chạy lại không tạo suất trùng giờ.
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

    DECLARE @phong BIGINT = (
        SELECT phong_id FROM PHONG_CHIEU
        WHERE ten_phong = N'Phòng 01' AND trang_thai = 'HOAT_DONG'
    );
    IF @phong IS NULL
        THROW 50030, N'Không tìm thấy Phòng 01 đang hoạt động.', 1;

    DECLARE @ngay DATETIME2(0) = DATEADD(DAY, 1,
        CONVERT(DATE, SYSDATETIMEOFFSET() AT TIME ZONE 'SE Asia Standard Time'));
    DECLARE @lich TABLE (ten_phim NVARCHAR(200), phut INT);
    INSERT INTO @lich VALUES
        (N'Dune: Hành tinh cát – Phần hai', 600),
        (N'Những mảnh ghép cảm xúc 2', 795),
        (N'Kung Fu Panda 4', 990),
        (N'Godzilla x Kong: Đế chế mới', 1185);

    IF EXISTS (SELECT 1 FROM @lich l WHERE NOT EXISTS (
        SELECT 1 FROM PHIM p
        WHERE p.ten_phim = l.ten_phim AND p.trang_thai = 'DANG_CHIEU'))
        THROW 50031, N'Thiếu phim đang chiếu trong danh mục.', 1;

    INSERT INTO SUAT_CHIEU (phim_id, phong_id, bat_dau, ket_thuc, gia_co_ban, trang_thai)
    SELECT p.phim_id, @phong, t.bat_dau,
        DATEADD(MINUTE, p.thoi_luong_phut, t.bat_dau), 75000, 'SAP_CHIEU'
    FROM @lich l
    JOIN PHIM p ON p.ten_phim = l.ten_phim AND p.trang_thai = 'DANG_CHIEU'
    CROSS APPLY (VALUES (DATEADD(MINUTE, l.phut, @ngay))) t(bat_dau)
    WHERE NOT EXISTS (
        SELECT 1 FROM SUAT_CHIEU s WITH (UPDLOCK, HOLDLOCK)
        WHERE s.phong_id = @phong AND s.trang_thai <> 'HUY'
          AND s.bat_dau < DATEADD(MINUTE, p.thoi_luong_phut, t.bat_dau)
          AND s.ket_thuc > t.bat_dau
    );

    COMMIT;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK;
    THROW;
END CATCH;
