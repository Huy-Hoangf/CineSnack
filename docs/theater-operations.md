# Phòng, ghế, hóa đơn và soát vé

Phạm vi: quản trị quản lý phòng và ghế; nhân viên cấp/in vé một lần; nhân viên hoặc quản trị soát vé. Khách vẫn xem được mã vé điện tử và hóa đơn của đơn được phép truy cập.

## Sử dụng

- Trong không gian làm việc chọn **Phòng & ghế** để thêm/sửa phòng, chọn **Sơ đồ ghế** để thiết lập hoặc sửa ghế. Thiết lập 1–26 hàng (A–Z), 1–30 ghế mỗi hàng, loại thường hoặc VIP. Chỉ tạo sơ đồ cho phòng chưa có ghế; không xóa sơ đồ đang dùng.
- Phòng còn suất chiếu chưa kết thúc không được ngừng hoạt động hoặc sửa sơ đồ. Phòng đã có vé không được đổi tên/loại. Ghế đã có lịch sử vé không được đổi mã, loại hoặc trạng thái. Đổi trạng thái phòng/ghế không xóa lịch sử.
- Đơn đã thanh toán có **In hóa đơn vé**, **In hóa đơn bỏng nước** tương ứng với loại hàng đã mua. Hóa đơn chỉ có vé hoặc chỉ có bỏng nước thì chỉ xuất loại tương ứng; không tạo hóa đơn rỗng.
- Nhân viên mở **Nhận và in vé**, nhập mã vé đã thanh toán để cấp một lượt in. Mỗi vé chỉ được in một lần và không được in sau khi soát; lượt in được ghi trước khi mở hộp thoại in, nên hủy hộp thoại hoặc lỗi máy in vẫn tiêu thụ lượt in. Mã vạch CODE128 tạo tại máy, không gửi mã vé tới dịch vụ bên ngoài. Mẫu in khổ 80 × 180 mm.
- **Soát vé vào rạp** là bước riêng. Nhập mã hoặc dùng máy quét mã vạch dạng bàn phím và Enter. Vé điện tử có thể soát mà chưa cần in giấy. Chỉ soát vé đã thanh toán, thuộc đơn và hóa đơn đã thanh toán, trong khoảng từ 30 phút trước giờ bắt đầu đến trước giờ kết thúc (giờ Việt Nam). Một lệnh cập nhật có điều kiện bảo đảm vé chỉ được soát một lần, đồng thời ghi nhân viên và thời gian.

## Cấu trúc và kiểm tra

- `RoomManagementController/Service/Repository`: quản lý phòng và ghế. `/api/admin/rooms/management` trả danh sách có cả phòng ngừng hoạt động, phân trang; endpoint `/api/admin/rooms` hiện tại tiếp tục trả phòng hoạt động cho form lịch chiếu.
- `TicketCheckInController/Service/Repository`: `/api/staff/tickets/issue` chỉ nhân viên; `/api/staff/tickets/scan` cho nhân viên/quản trị. Cả hai giữ CSRF và kiểm tra lại vai trò tài khoản đang hoạt động tại service.
- `theater.html`, `theater.js`, `theater.css`: giao diện vận hành. `ticket-print.js` dùng chung cho trang đơn và quản trị.
- Schema bổ sung `in_ve_luc`, `nhan_vien_in_id` để lưu lượt in, cùng `soat_ve_luc`, `nhan_vien_soat_id` cho lượt soát. Chạy migration trong `database/cinesnack.sql` trước khi triển khai API mới; không chạy reset dữ liệu.
- Thư viện JsBarcode 3.11.6 (MIT) được lưu tại `assets/vendor`, nguồn: https://github.com/lindell/JsBarcode; không tải script từ CDN khi sử dụng.

Kiểm tra: `mvn -Dtest=RoomManagementServiceTest,TicketCheckInRepositoryTest,TicketCheckInServiceTest,TheaterSecurityTest test`, `mvn test`, `node --check src/main/resources/UI/assets/js/theater.js`, `node --check src/main/resources/UI/assets/js/ticket-print.js`.

Kiểm tra giao diện bằng dữ liệu giả: chạy `node src/test/js/theater-fixture-server.cjs`, mở `http://localhost:8091/html/theater.html` và `http://localhost:8091/html/orders.html?guest=1&code=DEMO1`. Fixture chỉ dùng bộ nhớ, không kết nối database, không phải server dùng thật. Dừng bằng Ctrl+C sau khi kiểm tra. Chưa kiểm tra bản mới trên máy in giấy hoặc máy quét vật lý.

Tiêu chí nghiệm thu: nhân viên không quản lý được ghế; tạo 2×3 ra đúng A1–B3; không ghi đè sơ đồ; phòng còn lịch tương lai không được xóa; hai loại hóa đơn in độc lập; mỗi vé chỉ được nhân viên cấp một lượt in; vé đã soát không được in; vé đã soát hoặc ngoài giờ bị từ chối lần soát tiếp theo; soát thành công ghi nhận nhân viên và thời gian.
