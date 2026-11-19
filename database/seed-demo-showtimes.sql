-- Lịch mẫu đến hết 31/12/2026; chạy cinesnack.sql trước để có Phòng 01-08.
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
    DECLARE @phong TABLE (id BIGINT PRIMARY KEY);
    INSERT INTO @phong
    SELECT phong_id FROM PHONG_CHIEU
    WHERE ten_phong IN (N'Phòng 01', N'Phòng 02', N'Phòng 03', N'Phòng 04',
                       N'Phòng 05', N'Phòng 06', N'Phòng 07', N'Phòng 08')
      AND trang_thai = 'HOAT_DONG';
    IF (SELECT COUNT(*) FROM @phong) <> 8
        THROW 50030, N'Cần đủ 8 phòng đang hoạt động.', 1;

    DECLARE @hienTai DATETIME2(0) = CONVERT(DATETIME2(0), SYSDATETIMEOFFSET() AT TIME ZONE 'SE Asia Standard Time');
    DECLARE @homNay DATE = CONVERT(DATE, @hienTai);
    -- Phút kể từ 00:00: 600 = 10:00, 900 = 15:00, 1200 = 20:00.
    DECLARE @gio TABLE (phut INT);
    INSERT INTO @gio VALUES (600), (900), (1200);

    DECLARE @id BIGINT, @phim BIGINT, @phongChon BIGINT;
    DECLARE @batDau DATETIME2(0), @ketThuc DATETIME2(0);
    -- Suất đã có vé giữ nguyên phòng; đánh dấu để lần chạy sau không phân lại.
    IF NOT EXISTS (
        SELECT 1
        FROM sys.extended_properties
        WHERE class = 0 AND name = N'CineSnack_RoomsRandomizedV1'
    )
    BEGIN
        DECLARE suatCu CURSOR LOCAL FAST_FORWARD FOR
            SELECT s.suat_chieu_id, s.bat_dau, s.ket_thuc FROM SUAT_CHIEU s
            WHERE s.bat_dau > @hienTai AND s.trang_thai = 'SAP_CHIEU'
              AND NOT EXISTS (SELECT 1 FROM VE v WHERE v.suat_chieu_id = s.suat_chieu_id);
        OPEN suatCu;
        FETCH NEXT FROM suatCu INTO @id, @batDau, @ketThuc;
        WHILE @@FETCH_STATUS = 0
        BEGIN
            SET @phongChon = NULL;
            SELECT TOP (1) @phongChon = r.id FROM @phong r
            WHERE NOT EXISTS (
                SELECT 1 FROM SUAT_CHIEU s WITH (UPDLOCK, HOLDLOCK)
                WHERE s.phong_id = r.id AND s.suat_chieu_id <> @id AND s.trang_thai <> 'HUY'
                  AND s.bat_dau < @ketThuc AND s.ket_thuc > @batDau)
            ORDER BY NEWID();
            IF @phongChon IS NOT NULL
                UPDATE SUAT_CHIEU SET phong_id = @phongChon WHERE suat_chieu_id = @id;
            FETCH NEXT FROM suatCu INTO @id, @batDau, @ketThuc;
        END;
        CLOSE suatCu;
        DEALLOCATE suatCu;
        EXEC sys.sp_addextendedproperty @name=N'CineSnack_RoomsRandomizedV1', @value=N'completed';
    END;

    IF @homNay <= '2026-12-31'
    BEGIN
        DECLARE @canTao TABLE (phim_id BIGINT, bat_dau DATETIME2(0), ket_thuc DATETIME2(0));
        ;WITH ngay AS (
            SELECT @homNay AS ngay_chieu
            UNION ALL
            SELECT DATEADD(DAY, 1, ngay_chieu) FROM ngay WHERE ngay_chieu < '2026-12-31'
        )
        INSERT INTO @canTao
        SELECT p.phim_id, t.bat_dau, DATEADD(MINUTE, p.thoi_luong_phut, t.bat_dau)
        FROM ngay n CROSS JOIN PHIM p CROSS JOIN @gio g
        CROSS APPLY (VALUES (DATEADD(MINUTE, g.phut, CONVERT(DATETIME2(0), n.ngay_chieu)))) t(bat_dau)
        WHERE p.trang_thai = 'DANG_CHIEU'
          AND t.bat_dau > @hienTai
        OPTION (MAXRECURSION 0);

        DECLARE suatMoi CURSOR LOCAL FAST_FORWARD FOR
            SELECT phim_id, bat_dau, ket_thuc FROM @canTao ORDER BY bat_dau, phim_id;
        OPEN suatMoi;
        FETCH NEXT FROM suatMoi INTO @phim, @batDau, @ketThuc;
        WHILE @@FETCH_STATUS = 0
        BEGIN
            IF NOT EXISTS (
                SELECT 1
                FROM SUAT_CHIEU WITH (UPDLOCK, HOLDLOCK)
                WHERE phim_id = @phim AND bat_dau = @batDau AND trang_thai <> 'HUY'
            )
            BEGIN
                SET @phongChon = NULL;
                SELECT TOP (1) @phongChon = r.id FROM @phong r
                WHERE NOT EXISTS (
                    SELECT 1 FROM SUAT_CHIEU s WITH (UPDLOCK, HOLDLOCK)
                    WHERE s.phong_id = r.id AND s.trang_thai <> 'HUY'
                      AND s.bat_dau < @ketThuc AND s.ket_thuc > @batDau)
                ORDER BY NEWID();
                IF @phongChon IS NOT NULL
                    INSERT INTO SUAT_CHIEU (phim_id, phong_id, bat_dau, ket_thuc, gia_co_ban, trang_thai)
                    VALUES (@phim, @phongChon, @batDau, @ketThuc, 75000, 'SAP_CHIEU');
            END;
            FETCH NEXT FROM suatMoi INTO @phim, @batDau, @ketThuc;
        END;
        CLOSE suatMoi;
        DEALLOCATE suatMoi;
    END;
    COMMIT;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK;
    THROW;
END CATCH;
