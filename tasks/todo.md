- [x] Schema/repository: đơn chỉ vé/chỉ bắp nước/hỗn hợp dùng header đúng; cash có nha_cung_cap; thanh toán/hủy/hết hạn cập nhật đồng bộ.
- [x] Phiên/catalog: form login + CSRF, me không có hash, logout, public catalog và admin có phân quyền.
- [x] FE: ID/giá/ghế từ API, POST tạo đơn thật, giữ giỏ khi lỗi, xem/hủy đơn, nhân viên cash và hóa đơn tách.
- [x] Verification: regression/full Maven, browser của FE thật với fixture API, UI mobile, diff/merge simulation, báo cáo các giới hạn.


Giới hạn còn lại: giao dịch thanh toán trên SQL Server thật chưa được kiểm thử qua tài khoản; schema legacy không tự chuyển dữ liệu. Không stage/commit/merge repository gốc.

- [x] Khôi phục 6 phim/poster và 2 combo từ FE gốc vào SQL Server, không ghi đè bản ghi đã có.
- [x] Thay SQL reset bằng khởi tạo bổ sung và tắt SQL init khi startup; thử chạy hai lượt trên DB hiện tại và DB tạm mới.
- [x] Regression API thật (RED rồi GREEN), browser thật, giữ dữ liệu khi khởi động thêm; mvn test 45/45.
- [x] Đã tích hợp main: giải quyết conflict SQL bằng file cinesnack.sql đã gộp, giữ việc xóa script cũ. Không tự merge/stage/commit repository gốc.

- [x] Sửa trạng thái chọn ghế (đang tải/chưa có suất/chưa có ghế/lỗi), giữ nguyên thiết kế và luồng mua chỉ bắp nước.
- [x] Chỉ dùng CineSnack hiện có; seed phòng/60 ghế/4 suất và chạy lại kiểm tra giữ dữ liệu.
- [x] RED → GREEN: 3 browser regression mới và API suất/ghế thật; tổng 45 Java, 18 unit+browser, 3 live API.
- [x] Build và khởi động lại ứng dụng trên 8080, kiểm tra ghế/giá thật và mobile.
- [x] Chuẩn bị bản merge main đã giải quyết conflict trong clone Git riêng; nguyên trạng stage/index repository gốc.

- [x] Gộp 4 script thành database/cinesnack.sql, xóa 4 file cũ, cập nhật hướng dẫn và test.
- [x] Chạy chính file SQL gộp hai lần trên CineSnack; snapshot/ID được giữ, giá combo không bị ghi đè, không tạo database khác.

- [x] Sửa 4 P2 đã review; bổ sung regression cash/invoice/refresh, phim ngừng chiếu và endpoint movie theo ID.
- [x] Dùng file cinesnack.sql hiện tại: preflight + transaction toàn file; demo admin/user/staff BCrypt.
- [x] SQL atomicity/idempotency và 6 live API tests trên CineSnack; 48 Java đạt.
- [x] Lưu snapshot 100f017 và tích hợp main 07483623, giải quyết modify/delete SQL, giữ file SQL duy nhất.

- [x] Review chéo race cash: thêm generation/khóa thao tác, regression RED rồi GREEN.
- [x] Bản cuối: 48 Java, 21 FE/browser, 6 API thật; SQL atomicity và giữ dữ liệu khi chạy lại.
- [x] Tất cả thay đổi đã duyệt được lưu vào Git local theo yêu cầu; không có generated files/credentials SQL Server trong commit.
