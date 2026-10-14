# Tích hợp main sau khi gộp SQL

Database chỉ còn một file: database/cinesnack.sql. File khởi tạo cũ đã được gộp và xóa khỏi working tree; các phần nâng cấp/catalog/booking cũng nằm trong cùng file. Không dùng lại script khởi tạo xóa dữ liệu của main.

Commit local không tự gây conflict. Khi merge/rebase main, Git có thể báo conflict rename/delete hoặc nội dung vì main còn dùng tên SQL cũ. Các lần mô phỏng trước khi gộp đã giải quyết được conflict trong bản sao Git; chưa có merge/commit ở repository gốc. Không coi kết quả cũ là bảo đảm merge sạch cho phiên bản gộp hoặc main thay đổi sau đó.

Trước khi merge, review và commit những thay đổi bạn muốn lưu, gồm file SQL mới và việc xóa file cũ; giữ các thay đổi đã stage từ trước theo ý bạn. Sau đó git fetch origin và git merge origin/main. Nếu conflict ở SQL, đối chiếu thay đổi schema mới của main với cinesnack.sql, bổ sung phần cần thiết vào file duy nhất và giữ việc xóa các script cũ. Không chọn toàn bộ ours/theirs nếu chưa kiểm tra schema. Khi đã giải quyết, git add các đường dẫn liên quan, git diff --cached --check, mvn test rồi git merge --continue.

Merge Git không yêu cầu chạy SQL hoặc thao tác database. Script cinesnack.sql chỉ chạy thủ công trong CineSnack hiện có và không xóa dữ liệu. Để giảm conflict sau lần đồng bộ main, thống nhất cùng tên file SQL với nhóm và hạn chế nhiều người sửa cùng phần trong file.
