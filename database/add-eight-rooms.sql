-- Nâng cấp database hiện có; chạy trước seed-demo-showtimes.sql.
USE [CineSnack];
SET XACT_ABORT ON;
BEGIN TRY
    BEGIN TRANSACTION;
    DECLARE @soPhong INT = 1;
    DECLARE @phong BIGINT;

    WHILE @soPhong <= 8
    BEGIN
        DECLARE @tenPhong NVARCHAR(50) =
            N'Phòng ' + RIGHT(N'0' + CONVERT(NVARCHAR(2), @soPhong), 2);

        IF NOT EXISTS (
            SELECT 1
            FROM PHONG_CHIEU WITH (UPDLOCK, HOLDLOCK)
            WHERE ten_phong = @tenPhong
        )
            INSERT INTO PHONG_CHIEU (ten_phong, loai_phong, trang_thai)
            VALUES (@tenPhong, N'2D', 'HOAT_DONG');

        SET @phong = NULL;
        SELECT @phong = phong_id
        FROM PHONG_CHIEU
        WHERE ten_phong = @tenPhong AND trang_thai = 'HOAT_DONG';

        IF @phong IS NULL
            THROW 50011, N'Phòng chiếu đã ngừng hoạt động.', 1;

        INSERT INTO GHE (phong_id, ma_ghe, loai_ghe, trang_thai)
        SELECT
            @phong,
            CONCAT(h.hang, s.so),
            CASE WHEN h.hang IN ('D', 'E', 'F') THEN 'VIP' ELSE 'THUONG' END,
            'HOAT_DONG'
        FROM (VALUES ('A'), ('B'), ('C'), ('D'), ('E'), ('F')) h(hang)
        CROSS JOIN (VALUES (1), (2), (3), (4), (5), (6), (7), (8), (9), (10)) s(so)
        WHERE NOT EXISTS (
            SELECT 1
            FROM GHE WITH (UPDLOCK, HOLDLOCK)
            WHERE phong_id = @phong AND ma_ghe = CONCAT(h.hang, s.so)
        );

        SET @soPhong += 1;
    END;
    COMMIT;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK;
    THROW;
END CATCH;
