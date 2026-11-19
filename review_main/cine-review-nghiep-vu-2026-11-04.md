# Review nghiệp vụ và trải nghiệm CineSnack — 04/11/2026

## Phạm vi và cách kiểm tra

Đọc luồng khách chọn phim, ghế, bắp nước, tạo và theo dõi đơn; luồng admin quản lý phim, lịch chiếu, phòng, ghế; luồng staff tạo đơn tại quầy, tra cứu, thu tiền và soát vé. Đối chiếu controller, service, repository, SQL và giao diện tĩnh. Không review API quét QR thanh toán như yêu cầu. Không sửa code dự án.

Đã chạy `mvn -q test` (130 test, 0 lỗi) và `node --test src/test/js/authentication.test.cjs` (2 test đạt). Chưa kiểm tra trực tiếp trên SQL Server, trình duyệt thật hoặc thiết bị quầy. Sơ đồ tĩnh 159 file, 330 liên kết được lưu tại `C:\DATN-CineSnack-backup\cine-review-knowledge-graph.json`; đây là sơ đồ file/import/tài nguyên phục vụ review, không phải đồ thị ký hiệu đầy đủ của plugin.

## Phát hiện cần ưu tiên

### P1 — Bắp nước đã thanh toán không có bước giao nhận trong ứng dụng

`OrderRepository.xacNhanChiTietDaThanhToan` chuyển dòng bắp nước sang `CHO_NHAN`, nhưng không có controller/service nhận bắp nước. SQL có thủ tục `NHAN_BONG_NUOC` nhưng Java không gọi. `orders.js` chỉ liệt kê tên và giá, không hiển thị trạng thái phục vụ. Khách không biết bắp nước đã sẵn sàng hay nhận ở đâu; staff không thể ghi nhận đã giao và dễ giao trùng.

Đề xuất: màn hình staff liệt kê đơn bắp nước đã thanh toán, mã nhận, các món và trạng thái; thao tác nhận/giao có kiểm tra đơn và cập nhật một lần; màn hình khách hiển thị trạng thái và chỉ dẫn nhận hàng.

Chứng cứ: `OrderRepository.java:253-272`, `database/cinesnack.sql:502-538`, `orders.js:95-103`, danh sách controller tại `src/main/java/com/cinesnack`.

### P1 — Thiếu quy trình khi phải hủy suất đã bán vé

`AdminCatalogService.saveShow` và `deleteShow` chặn mọi thay đổi/xóa khi đã phát sinh vé; `OrderService.huyDonDaKhoa` chỉ cho hủy đơn chờ thanh toán. Vì vậy khi rạp mất điện, phòng hỏng hoặc thay lịch sau bán vé, admin không có cách ghi nhận hủy suất, xử lý vé và hướng dẫn khách trong hệ thống. Đây là khoảng trống vận hành, không phải lỗi giao dịch QR.

Đề xuất: quy trình hủy suất có lý do, khóa bán tiếp, danh sách khách/đơn bị ảnh hưởng và trạng thái xử lý đổi suất hoặc hoàn tiền. Xác nhận chính sách hoàn/đổi của rạp trước khi triển khai.

Chứng cứ: `AdminCatalogService.java:77-114`, `OrderService.java:269-284`, `admin.js:168-185`.

### P2 — Admin chưa quản lý được thực đơn bắp nước

Danh mục sản phẩm/combo được đọc từ `SAN_PHAM` và `COMBO`; sidebar admin chỉ có đơn, phim, lịch chiếu và phòng/ghế. Không có API hoặc biểu mẫu để đổi giá, ngừng bán hay thêm món. Khi hết hàng hoặc thay giá, rạp phải sửa SQL trực tiếp; website vẫn có thể mời khách đặt món đang hết.

Đề xuất: admin quản lý tên, giá, trạng thái bán và thành phần combo; staff có ít nhất khả năng xem tình trạng món để tư vấn khách. Với rạp một địa điểm, có thể bắt đầu bằng bật/tắt bán và đổi giá.

Chứng cứ: `admin.html:57-100`, `CatalogRepository.java:117-136`, `BookingCatalogRepository.java:80-92`, không có controller sản phẩm/combo.

### P2 — Mô tả phim và diễn viên được lưu nhưng khách không thấy

API phim đã trả `description` và `actors`, nhưng hộp chi tiết phim chỉ hiển thị tên, thể loại, thời lượng và phân loại tuổi. Khách thiếu thông tin để quyết định mua vé; hai trường admin vừa nhập không tạo giá trị trên giao diện công khai.

Đề xuất: hiển thị nội dung và diễn viên trong chi tiết phim; nếu trống, ẩn riêng nhãn thay vì hiển thị khoảng trắng.

Chứng cứ: `Catalog.java:12-22`, `movies.js:21-39`, `admin.html:230-231`.

### P2 — Phòng đã “xóa” vẫn giữ tên duy nhất

Phòng có lịch cũ được đổi sang `DA_XOA` và ẩn khỏi danh sách, nhưng `PHONG_CHIEU.ten_phong` vẫn `UNIQUE`. Admin không thể tạo phòng mới cùng tên, dù giao diện đã báo xóa. Trường hợp sửa chữa rồi đưa phòng 01 vào hoạt động lại sẽ gây lỗi trùng tên khó hiểu.

Đề xuất: có chức năng khôi phục phòng lưu trữ, hoặc tách tên hiển thị hiện hành khỏi định danh lịch sử. Trước mắt, giao diện cần nói rõ phòng được lưu trữ và tên vẫn được giữ.

Chứng cứ: `RoomManagementService.java:42-53`, `RoomManagementRepository.java:15-55`, `database/cinesnack.sql:38-45`.

### P2 có điều kiện — VIP hiện chỉ khác kiểu ghế, không khác giá

`CatalogRepository.layGhe` trả cùng `gia_co_ban` cho mọi ghế; `OrderService` nhân cùng giá suất chiếu với số ghế. Nếu “VIP” của rạp là hạng ghế tính giá cao hơn, cả hiển thị và tính tiền đều sai kỳ vọng. Nếu VIP chỉ là loại ghế/định vị nhưng cùng giá, cần ghi rõ để khách không hiểu nhầm.

Đề xuất: xác nhận chính sách giá; nếu có phụ thu, lưu giá theo hạng và tính lại ở backend, đồng thời hiển thị từng giá tại sơ đồ ghế và tóm tắt.

Chứng cứ: `CatalogRepository.java:96-111`, `OrderService.java:95-139`, `booking.js:95-110`.

### P3 — Admin chưa có báo cáo vận hành toàn rạp

Ba ô thống kê hiện chỉ đếm 20 đơn trên trang, và giao diện đã ghi rõ “trên trang”; không nên xem đó là doanh thu hay kết quả bán vé. Admin chưa có màn hình doanh thu theo ngày/suất/phim, số ghế bán, bắp nước và hoàn/hủy. Đây là thiếu chức năng quản trị, không phải lỗi tính toán của ba ô hiện tại.

Đề xuất: báo cáo ngày trước, sau đó thêm bộ lọc phim/suất và đối soát theo phương thức thanh toán.

Chứng cứ: `admin.js:68-116`, `admin.html:114-186`.

## Ma trận vai trò

| Nghiệp vụ | Khách | Staff | Admin | Nhận xét |
| --- | --- | --- | --- | --- |
| Xem phim, suất, ghế | Có | Có | Có | Nên hiển thị mô tả/diễn viên cho khách |
| Tạo đơn online | Có | — | — | Giữ ghế 15 phút; backend kiểm tra lại giá và ghế |
| Tạo đơn tại quầy | — | Có | Không | Đúng phân vai đang thiết kế |
| Tra cứu đơn toàn rạp | — | Có | Có | Có tìm kiếm và trạng thái |
| Thu tiền mặt/chuyển khoản | — | Có | Có | Cần quy trình đối soát/hoàn khi hủy suất |
| Soát vé | — | Có | Có | Có giới hạn thời gian và chống soát lặp |
| Giao bắp nước | Xem đơn | Thiếu | Thiếu | Ưu tiên cao |
| Phim, lịch chiếu | — | Xem gián tiếp | Có | Thiếu xử lý suất đã bán vé bị hủy |
| Phòng, ghế | — | Không | Có | Phòng lưu trữ cần cách khôi phục/tên rõ ràng |
| Sản phẩm, combo, tồn bán | Xem danh mục | Không | Thiếu | Đang phụ thuộc SQL |
| Tài khoản và phân quyền staff | — | — | Thiếu | Nên có nếu rạp tự vận hành nhân sự |
| Báo cáo doanh thu/vận hành | — | — | Thiếu | Ưu tiên sau luồng giao hàng và hủy suất |

## Điểm đang làm tốt

- Backend kiểm tra lại ghế, suất chiếu, giá và trạng thái trước khi tạo đơn.
- Có luồng guest; màn hình đặt vé cho phép mua bắp nước riêng.
- Staff chỉ tạo đơn tại quầy, admin giữ quyền quản lý danh mục; quyền được kiểm tra ở server.
- Soát vé dùng cập nhật có điều kiện để tránh nhận cùng vé hai lần.
- Giao diện có thông báo trạng thái, vùng lỗi và nút thao tác rõ ràng ở nhiều bước.

## Thứ tự xử lý đề nghị

1. Hoàn thiện giao/nhận bắp nước sau thanh toán.
2. Chốt chính sách hủy suất đã bán vé, đổi suất và hoàn tiền rồi mới xây quy trình.
3. Cho admin quản lý tình trạng bán và giá bắp nước.
4. Hiển thị thông tin phim đã nhập; làm rõ trạng thái phòng lưu trữ.
5. Xác nhận quy tắc giá ghế VIP, sau đó làm báo cáo và quản lý nhân sự.
