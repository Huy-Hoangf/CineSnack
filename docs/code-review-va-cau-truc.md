# Review mã nguồn và cấu trúc CineSnack

Ngày rà soát: 28/10/2026. Phạm vi: mã Java trong `src/main/java`, JavaScript giao diện trong `src/main/resources/UI/assets/js`, các bài test Maven và luồng tài khoản, đặt vé, bắp nước, thanh toán, hóa đơn. Đây là review tĩnh kết hợp chạy test; chưa kiểm thử thủ công với SQL Server và trình duyệt thật.

## Cấu trúc sau khi sắp xếp

| Nhóm | Nhiệm vụ |
| --- | --- |
| `account` | Tài khoản, xác thực, phân quyền URL |
| `catalog` | Phim, suất, ghế, sản phẩm, combo và quản trị danh mục |
| `booking` | Tạo, xem, hủy, hết hạn đơn; giữ ghế và quyền truy cập đơn guest |
| `payment` | QR, tiền mặt, chuyển khoản và bản ghi thanh toán |
| `invoice` | Phát hành và đọc hóa đơn |
| `common` | CSRF, xử lý lỗi API, hỗ trợ SQL, mã và cấu hình web chung |
| `config` | Kết nối SQL Server theo môi trường máy chạy |

Các lớp Java có nội dung đã chuyển vào nhóm nghiệp vụ tương ứng. Đã bỏ 22 tệp `.java` rỗng trong các thư mục lớp cũ. API và tên bảng SQL không đổi.

## Kết quả review

Không tìm thấy lỗi P1/P2 mới do thay đổi package trong các luồng được bài test bao phủ. Cần lưu ý ba điểm bảo trì sau:

1. **P3 — `admin.js` gộp hai nghiệp vụ.** Tệp này vừa theo dõi đơn (`loadOrders`, `showOrder`), vừa quản lý phim và suất (`loadMovies`, `loadShows`, `editMovie`, `editShow`). Nên tách thành mã monitor và mã quản trị danh mục khi có test giao diện cho cả hai role. Các biến dùng chung và đăng ký sự kiện cần được chuyển có kiểm soát; không chỉ cắt tệp theo dòng.
2. **P3 — `OrderService` dài và gồm cả ghi lẫn đọc.** `taoDonHang` giữ giao dịch tạo đơn, trong khi các hàm `layChiTiet`, `layDanhSach`, `huyDonHang` là các nhánh khác. Nên giữ nguyên hàm tạo đơn trong một service giao dịch; khi refactor tiếp, tách đọc danh sách/chi tiết thành service truy vấn và giữ test về quyền truy cập guest, staff, khách hàng.
3. **P3 — `orders.js` có một hàm dựng chi tiết rất dài.** `hienChiTiet` xử lý nhiều loại thông tin đơn và thanh toán. Có thể tách hàm dựng vé, bắp nước, QR, hóa đơn thành các hàm nhỏ trong cùng tệp trước khi tạo module mới.

Điểm cần giữ nguyên: security kiểm tra quyền ở server, `OrderService` kiểm tra lại ghế và giá, `PaymentService` xác nhận đúng số tiền chuyển khoản và phát hành hóa đơn trong transaction. Thống kê trong `admin.js` chỉ đếm 20 đơn của trang hiện tại; giao diện đang ghi rõ “Đơn trên trang”, vì vậy không nên gọi đây là báo cáo toàn rạp.

## Kiểm chứng

`mvn test`: 86 test chạy, 0 lỗi, 0 thất bại. Chưa chạy kiểm thử tích hợp SQL Server (`*IT`) hoặc kiểm thử thao tác giao diện trên trình duyệt.
