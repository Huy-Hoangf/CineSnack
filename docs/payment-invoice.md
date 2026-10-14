# Luồng đặt vé, đơn hàng và hóa đơn

FE dùng API cùng origin và session cookie. Đăng nhập tài khoản có sẵn qua POST /api/auth/login; GET /api/auth/me trả ID/email/role; POST /api/auth/logout kết thúc phiên. API thay đổi dữ liệu cần CSRF lấy từ GET /api/csrf. Không lưu mật khẩu ở browser.

Catalog công khai: GET /api/movies, /api/showtimes, /api/showtimes/{id}/seats, /api/snacks. Booking sử dụng ID và dữ liệu hiện hành; không fallback dữ liệu demo. Giỏ v2 lưu ID/số lượng, lấy lại giá khi chỉnh sửa. Tối đa 10 ghế, mỗi sản phẩm/combo 1–10.

Checkout gửi email, showTimeId, seatIds, snacks và combos tới POST /api/orders. BE kiểm tra tài khoản, suất/phòng/phim/ghế và giá trong database, tạo đơn giữ chỗ tối đa 15 phút và không quá giờ bắt đầu. Có thể mua chỉ vé, chỉ bắp nước hoặc cả hai. Client không quyết định tổng tiền.

Khách xem và hủy đơn của mình trên orders.html. Nhân viên/quản trị tra cứu và xác nhận tiền mặt trên admin.html. Thanh toán, phát hành hóa đơn và cập nhật vé/bắp nước nằm trong một transaction. Gửi lại thanh toán không tạo thêm giao dịch thành công. Hủy/hết hạn giải phóng ghế; đơn đã thanh toán không được hủy qua API này.

Một đơn có hóa đơn vé và/hoặc hóa đơn bắp nước theo loại đã mua. API hóa đơn trả danh sách. Vé chuyển DA_THANH_TOAN, bắp nước CHO_NHAN. Nội dung vé được chụp khi thanh toán; chi tiết bắp nước giữ tên/giá lúc mua. Trang quản trị phân trang; thống kê chỉ mô tả trang đang xem.

## Database và vận hành

Java 17, SQL Server. Đặt CINESNACK_DB_URL, CINESNACK_DB_USER, CINESNACK_DB_PASSWORD trong môi trường tiến trình; tài khoản NGUOI_DUNG phải HOAT_DONG và mật khẩu BCrypt. Chạy mvn spring-boot:run, mở http://localhost:8080/html/index.html.

Chỉ dùng database CineSnack hiện có. Mở database/cinesnack.sql trong SSMS và chạy toàn bộ file khi chủ động khởi tạo/bổ sung dữ liệu. File SQL duy nhất gồm 4 phần: cấu trúc bảng/ràng buộc/index/procedure; metadata hóa đơn; phim/combo; phòng/ghế/suất chiếu. Toàn file chọn rõ CineSnack, preflight trước mọi DDL, dùng một transaction bao ngoài và EXEC các khối thay vì GO, không CREATE/DROP DATABASE, không DELETE/TRUNCATE và không tự chạy khi ứng dụng khởi động (spring.sql.init.mode=never).

Các đối tượng/bản ghi đã có được bỏ qua, giữ giá và trạng thái hiện tại. Schema hóa đơn chung cũ cần phương án chuyển đổi riêng, script không tự chuyển dữ liệu legacy. Danh mục gốc gồm 6 phim/poster và 2 combo. Phòng 01 có 60 ghế A1–F10, 4 suất cho ngày 15/10/2026 với giá 75.000đ. Phần 4 tạo lịch ngày mai theo giờ Việt Nam: chạy lại cùng ngày không trùng; chủ động chạy ngày khác có thể bổ sung lịch ngày mai mà không xóa lịch cũ. Có seed ba tài khoản demo theo yêu cầu; không tự tạo đơn hàng.

## Kiểm chứng

- Kiểm tra trước khi gộp: 45 Java, 8 FE unit, 10 browser fixture và 3 live API test đạt. Browser thật đọc Java/SQL Server thấy đủ 60 ghế, chọn A1 và tổng 75.000đ, không lỗi JS hoặc tràn ngang mobile 375px.
- Sau khi gộp ngày 14/10/2026: tests/database/catalog-idempotency.ps1 thực thi chính file cinesnack.sql hai lượt trên database CineSnack, giữ nguyên snapshot dữ liệu/ID và xác nhận giá đã chỉnh không bị ghi đè; thử giá được rollback. Không tạo database khác.
- Live API test: đặt CINESNACK_LIVE_URL=http://localhost:8080 rồi chạy node --test tests/frontend/integration/catalog.test.cjs. Test chỉ đọc và kỳ vọng danh mục/giá gốc; sửa kỳ vọng nếu bạn chủ động thay đổi dữ liệu.
- Maven test sau gộp: 45/45 đạt, không có lỗi hoặc bỏ qua. Đã kiểm tra đăng nhập/session/vai trò của ba tài khoản trên SQL Server thật; luồng thu tiền trong browser được kiểm thử bằng fixture và transaction backend bằng unit/H2, chưa chạy giao dịch thanh toán live trên dữ liệu người dùng. Fixture không thay thế kiểm chứng giao dịch thật.

Không thuộc phạm vi: đăng ký tài khoản, QR/ví/cổng thanh toán, hoàn tiền, email/PDF hóa đơn. Xem docs/git-integration.md để tích hợp main; đã lưu các thay đổi đã duyệt và tích hợp main theo yêu cầu khử conflict; các commit ở local.

## Sửa các P2 sau review

Cash receipt được giữ ngay sau POST thành công; lỗi GET invoices không để form thu tiền trở lại. Mỗi lần tải chi tiết có generation riêng và khi bắt đầu cash sẽ vô hiệu hóa refresh trước đó; callback của đơn đã bỏ chọn không đổi generation của đơn mới. Regression đã tái hiện lỗi trước sửa rồi qua sau sửa.

SQL: preflight schema trước tất cả EXEC/DDL, toàn file là một batch có transaction bao ngoài. Test trên chính CineSnack đã kích hoạt nhánh preflight (không thay đổi dữ liệu) và chèn lỗi ngay trước commit bao ngoài (mọi seed/DDL rollback). Chạy lại SQL giữ snapshot dữ liệu/ID và giá combo đã chỉnh. Không tạo database khác.

Catalog: ghế JOIN phim và kiểm tra DANG_CHIEU. Phim theo ID có endpoint riêng, 404 khi không tìm thấy; frontend chọn đúng tab, không giới hạn trong ds trang đầu.

48 kiểm thử Java đạt. 6 live API tests đạt, gồm 3 tài khoản đăng nhập/vai trò/phân quyền/logout và 3 catalog tests. Tài khoản admin@cinesnack.local, user@cinesnack.local, staff@cinesnack.local có mật khẩu demo CineSnack@123 lưu dưới dạng BCrypt. Không ghi đè account cùng email đã có.

Git đã được tích hợp main 07483623 trên nhánh hiện tại, giải quyết modify/delete bằng việc giữ cinesnack.sql duy nhất. 21 FE unit/browser và 6 live API tests của bản cuối đều đạt. Việc merge không thay đổi Java/FE đã kiểm thử hoặc dữ liệu SQL Server.
