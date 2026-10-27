# Đơn, thanh toán QR và hóa đơn

Đơn mới lưu vé và bắp nước theo `don_hang_id`. Hai bảng hóa đơn chỉ nhận bản ghi khi thanh toán thành công. Đơn giữ ghế tối đa 15 phút (không vượt giờ chiếu); quá thời gian này, đơn chuyển sang `HUY`, giải phóng ghế và không tạo hóa đơn.

## Nâng cấp SQL Server hiện có

1. Sao lưu database `CineSnack`.
2. Chạy `database/order-invoice-migration.sql` trên database đó. Script chuyển chi tiết cũ sang tham chiếu đơn, giữ hóa đơn đã thanh toán, xóa các bản ghi hóa đơn nháp chưa phát hành và thêm khóa ngoại mới. Script dùng transaction và rollback khi lỗi.
3. Sau khi nâng cấp, có thể chạy `database/cinesnack.sql` để cập nhật phần schema/dữ liệu danh mục còn thiếu. Không dùng script reset để nâng cấp.

Database mới dùng `database/cinesnack.sql`, đã chứa cấu trúc đơn và hóa đơn mới. Không cần chạy migration trên database mới.

## Cấu hình QR

Thiết lập các biến môi trường trên máy chạy Spring Boot:

| Biến | Nội dung |
|---|---|
| `CINESNACK_QR_BANK_ID` | Mã ngân hàng dùng cho VietQR (ví dụ BIN ngân hàng) |
| `CINESNACK_QR_ACCOUNT_NO` | Số tài khoản nhận tiền |
| `CINESNACK_QR_ACCOUNT_NAME` | Tên chủ tài khoản hiển thị |

Nếu chưa cấu hình ngân hàng hoặc số tài khoản, API QR trả 503 và màn hình đơn báo chưa cấu hình. QR lấy số tiền và mã đơn từ database. Mã QR được hiển thị qua dịch vụ ảnh VietQR; CineSnack chưa kết nối ngân hàng hoặc webhook nên việc quét/chuyển tiền không tự xác nhận đơn.

Staff vào **Tạo đơn tại quầy** để bán vé và bắp nước. Mục **Theo dõi thanh toán** cập nhật đơn chờ mỗi 10 giây. Sau khi kiểm tra giao dịch thực tế, staff nhập mã giao dịch và số tiền thực nhận bằng đúng tổng tiền đơn để xác nhận chuyển khoản. Xác nhận tiền mặt vẫn hỗ trợ tiền thừa cho khách mua trực tiếp.
