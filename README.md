# CineSnack

Đồ án website bán vé xem phim và bỏng nước cho một rạp.

## Công nghệ

- Java 17, Spring Boot, Maven
- HTML, CSS, JavaScript
- SQL Server

## Chạy dự án

Mở thư mục `C:\DATN-CineSnack` bằng VS Code rồi chạy:

```powershell
mvn spring-boot:run
```

Truy cập `http://localhost:8080`.

## Ghi chú

- `CineSnack.sql`: tạo cơ sở dữ liệu trong SQL Server. Chạy lại file này sẽ xóa dữ liệu cũ.
- `src/main/java/com/cinesnack`: mã Java của backend.
- `src/main/resources/static`: các trang HTML và tài nguyên giao diện.
- Hiện mới có cấu trúc thư mục và các trang cơ bản; chưa kết nối cơ sở dữ liệu.
