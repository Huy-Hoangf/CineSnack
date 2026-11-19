# Bản sửa CineSnack để duyệt trước khi áp dụng

## Trạng thái

Đã chuẩn bị và kiểm thử toàn bộ bản đề xuất. Source gốc C:/DATN-CineSnack/src chưa được chỉnh; chưa chạy SQL trên database thật; chưa tạo4 file Word theo thứ tự người dùng yêu cầu sửa code trước. Kiểm tra SHA256 xác nhận các file source gốc vẫn giống lúc khảo sát.

## Cách review

1. Mở03-code-chinh-de-review.md để đọc những đoạn chính.
2. Mở01-toan-bo-thay-doi.patch để xem mọi dòng thay đổi so với working copy của bạn lúc khảo sát.
3. Thư mục files chứa nguyên văn54 file đề xuất, gồm source,test vàSQL; đây không phải bản sao đầy đủ để chạy riêng.
4. Xem04-kiem-thu.md và05-review-ky-thuat.md.

## Phạm vi

- Đạo diễn và trailer HTTPS,bao gồm cập nhật metadata phim đã có vé nhưng không đổi thông tin được bảo vệ.
- Timeline ngày/phòng,truy vấn đủ suất trong ngày và xử lý qua nửa đêm.
- Admin chỉnh4giá từng phòng;giá ngày thường/cuối tuần,thường/VIP;chọn MANUAL/POLICY và chụp giá vào suất.
- Admin chỉnh giới hạn check-in muộn;kiểm tra thanh toán,vai trò vàsoát1lần vẫn do server.
- Countdown giữ ghế,ẩnQR khi hết hạn,chặn phản hồi muộn và revalidate trang cache.
- QR riêng mỗi vé tại máy khách,camera đọcQR/CODE128 và giữ bước xác nhận.
- Chuyển khoản tiếp tục xác nhận thủ công.

## Sau khi duyệt

Áp dụng đúng patch sau khi kiểm tra source không đổi;chạy lại kiểm thử ởworkspace chính. Migration database/theater-review-migration.sql phải được review và chạy riêng trênSQLServer đã sao lưu trước khi dùngAPI mới;script không reset dữ liệu. Sau khi code được áp dụng, tạo4DOCX rất chi tiết theo phân công trong ảnh và kiểm tra render từng trang.
