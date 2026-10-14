# Trạng thái tích hợp main

Đã lưu các thay đổi của dự án cùng bản sửa P2 vào commit 100f017, sau đó tích hợp origin/main tại 07483623c57af3257da4bfd7d3e30b81d557090c trên nhánh hiện tại. Conflict modify/delete duy nhất là đường dẫn SQL cũ: đã giải quyết bằng việc giữ file cinesnack.sql có schema hóa đơn tách, ràng buộc/index/trigger/procedure của main và các kiểm tra an toàn, đồng thời giữ việc xóa file SQL cũ.

Database vẫn chỉ có một file SQL trong dự án và tất cả dùng chung CineSnack. Giải quyết Git không chạy SQL hay reset database. Các commit được lưu ở local.

Sau commit merge, main tại commit đã tích hợp là tổ tiên của nhánh. Các thay đổi main đã tích hợp sẽ không tạo lại conflict cũ; những thay đổi mới sau đó vẫn cần được đối chiếu khi merge. Để giảm conflict tương lai, thống nhất cùng tên cinesnack.sql với nhóm và phối hợp khi chỉnh cùng phần trong file.

Trước các lần merge sau: lưu thay đổi đã review, git fetch origin, kiểm tra diff schema, rồi git merge origin/main. Nếu conflict mới, đối chiếu thay đổi cần thiết vào file SQL duy nhất. Không chọn toàn bộ ours/theirs nếu chưa kiểm tra schema; không chạy SQL để giải quyết Git conflict.
