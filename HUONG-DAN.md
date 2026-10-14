# Chạy và kiểm tra CineSnack

Java 17, Maven, SQL Server; frontend HTML/CSS/JS tại src/main/resources/UI.

Đặt CINESNACK_DB_URL, CINESNACK_DB_USER, CINESNACK_DB_PASSWORD trong môi trường. Dùng tài khoản đang hoạt động có mật khẩu BCrypt. Chạy mvn spring-boot:run rồi mở http://localhost:8080/html/index.html.

Giao diện dùng session và CSRF, catalog/API thật, giỏ cinesnack-gio-v2, ID phim/suất/ghế/sản phẩm từ database. booking.html?phim=<ID> dùng ID số. Checkout tạo đơn; nhân viên thu tiền mặt trong admin; khách xem đơn và hóa đơn trong orders.

Kiểm thử Java: mvn test. Kiểm thử FE: node --test tests/frontend/unit/cinema.test.cjs tests/frontend/unit/cart.test.cjs. Browser: node --test tests/frontend/browser/authentication.test.cjs tests/frontend/browser/site.test.cjs; cần Playwright và Edge hoặc biến TRINH_DUYET. Browser test tự khởi động API fixture RAM và phục vụ giao diện thật; không truy cập SQL Server.

Xem docs/payment-invoice.md để đọc luồng nghiệp vụ và giới hạn kiểm chứng. Chỉ còn một script: database/cinesnack.sql. Khởi động ứng dụng không chạy SQL tự động (spring.sql.init.mode=never).

## Khởi tạo và khôi phục danh mục

Mở database/cinesnack.sql trong SSMS, kết nối đúng instance chứa database CineSnack đã tồn tại và chạy toàn bộ file. File gồm cấu trúc bảng/ràng buộc/procedure, bổ sung metadata hóa đơn, phim/combo và phòng/ghế/suất chiếu. Không tạo/xóa database hoặc xóa dữ liệu; các mục đã có được bỏ qua. Phần bổ sung hóa đơn chỉ hỗ trợ schema hóa đơn tách, không tự chuyển schema cũ.

Database hiện đã nhập 6 phim và 2 combo gốc từ frontend. Mở lại trang hoặc tải lại để thấy dữ liệu. Đã có Phòng 01, 60 ghế A1–F10 và 4 suất cho ngày 15/10/2026. Đã có 3 tài khoản demo đang hoạt động, mật khẩu BCrypt; xem bảng tài khoản bên dưới.

Nếu chạy ứng dụng bằng JAR, dừng bằng Ctrl+C trước mvn package để tránh Windows khóa file JAR. Sau BUILD SUCCESS, khởi động lại bằng cấu hình kết nối đã dùng. Nếu chỉ cập nhật danh mục trong database thì giao diện lấy được dữ liệu mới mà không cần build lại.

## Phòng, ghế và suất chiếu

Toàn bộ dùng database CineSnack hiện có trên cùng SQL Server. Không tự tạo database khác, kể cả khi kiểm thử. Script khởi tạo yêu cầu CineSnack đã tồn tại, không CREATE/DROP DATABASE.

Phần 4 của database/cinesnack.sql chỉ thêm Phòng 01/ghế còn thiếu và lịch ngày mai theo giờ Việt Nam: Dune 10:00, Những mảnh ghép cảm xúc 2 13:15, Kung Fu Panda 4 16:30, Godzilla x Kong 19:45. Giá 75.000đ lấy theo suất, không có phụ thu VIP. Script chạy thủ công: chạy lại cùng ngày không trùng; chạy vào ngày khác có thể bổ sung lịch cho ngày mai, không chuyển/xóa lịch cũ. Khi lịch cũ đã qua cần tạo suất mới để đặt vé.

FE hiện thông báo ngay tại phần ghế khi chưa có suất, chưa cấu hình ghế hoặc API lỗi; không hiện sơ đồ trống. Bản JAR đã build và khởi động lại. Mở /html/booking.html?phim=1 để chọn ngày 15/10/2026 và xem sơ đồ thật.

Xem docs/git-integration.md để tích hợp main; chưa stage/commit/merge ở repository gốc.

## Kiểm tra script SQL gộp

Chạy ./tests/database/catalog-idempotency.ps1 -Server '<máy SQL Server>' trong PowerShell sau khi đã nhập dữ liệu. Test thực thi chính cinesnack.sql hai lượt, so sánh snapshot/ID, thử giá combo trong transaction rồi rollback. Chỉ kết nối database CineSnack, không tạo database kiểm thử khác. Bản gộp đã được kiểm tra trên SQLEXPRESS ngày 14/10/2026.

## Tài khoản demo cố định

Tất cả dùng database CineSnack và cơ chế đăng nhập/session hiện có. Mật khẩu chung: CineSnack@123.

| Email | Vai trò |
|---|---|
| admin@cinesnack.local | QUAN_TRI |
| user@cinesnack.local | KHACH_HANG |
| staff@cinesnack.local | NHAN_VIEN |

Các tài khoản được seed trong chính database/cinesnack.sql bằng BCrypt cost 12. Chạy lại không đổi mật khẩu/vai trò nếu email đã tồn tại. Tài khoản demo không thay thế cấu hình tài khoản SQL Server: kết nối DB vẫn dùng biến môi trường hoặc Windows Authentication.

Script SQL đã có preflight trước DDL, một transaction bao toàn file và không có GO; dùng EXEC cho các khối để procedure/trigger và cột mới được biên dịch đúng. Nếu lỗi, script rollback; không tự tạo database khác.

Đã sửa: thu tiền hiển thị thành công/tiền thừa ngay, lỗi tải hóa đơn tách riêng; phản hồi refresh cũ không ghi đè biên nhận; phim ngừng chiếu không báo ghế khả dụng; phim theo ID mở đúng tab dù không nằm trang đầu. GET /api/movies/{id} công khai và trả 404 nếu phim không còn hiển thị.

Kiểm thử thêm: ./tests/database/p2-atomicity.ps1 -Server '<máy SQL Server>'. Live login/role: đặt CINESNACK_LIVE_URL=http://localhost:8080 rồi chạy node --test tests/frontend/integration/accounts.test.cjs tests/frontend/integration/catalog.test.cjs.
