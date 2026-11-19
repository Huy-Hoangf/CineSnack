# Đặc tả hoàn thiện nghiệp vụ CineSnack và bốn tài liệu Word

Ngày lập 09/11/2026. Trạng thái: bản đề xuất để người dùng duyệt, chưa phải mô tả chức năng đã triển khai.

## 1 Cơ sở và phạm vi đã xác nhận

Người dùng yêu cầu xử lý những mục còn thiếu trong lần review theo ảnh nhận xét và tạo bốn file Word giải thích rất chi tiết flow và code. Người dùng đã xác nhận dùng định dạng DOCX, yêu cầu admin có chỗ chỉnh giá và chính sách check-in, đồng thời yêu cầu chưa làm chuyển khoản tự động.

Quy trình của dự án yêu cầu trình bày code đề xuất để người dùng review trước khi sửa. Việc duyệt phạm vi không được diễn giải thành đã duyệt mọi thay đổi code chưa trình bày.

Bản này ghi lại các quyết định kỹ thuật đề xuất. Các lựa chọn chưa được người dùng nêu cụ thể đều được gọi là đề xuất, không coi là yêu cầu đã chốt.

### Bản đồ chức năng

| Module | Trách nhiệm | Phụ thuộc |
| --- | --- | --- |
| movie-details | Bổ sung đạo diễn và trailer trong quản trị và trang phim | account, catalog hiện có |
| theater-policy | Giá thường và VIP theo loại ngày và phòng; giới hạn check-in muộn | account, room management hiện có |
| showtime-timeline | Xem lịch theo ngày và phòng trên timeline | catalog, theater-policy khi tạo suất |
| payment-deadline | Đếm ngược và ẩn QR thanh toán khi hết hạn | booking, payment hiện có |
| ticket-qr | QR riêng cho từng vé và đọc QR tại cổng | booking, invoice, ticket check-in hiện có |
| learning-documents | Bốn tài liệu Word giải thích code sau sửa | Các module trên và các luồng hiện có |

Trình tự đề xuất: xác nhận đặc tả → phân tích code và knowledge graph → chuẩn bị code và test trong bản nháp → trình bày diff → áp dụng sau khi người dùng duyệt → kiểm chứng → hoàn thiện và render bốn tài liệu Word.

### Phần được giữ nguyên theo quyết định của người dùng

- Chưa tích hợp dịch vụ ngân hàng, SePay, payOS hoặc webhook tự xác nhận chuyển khoản.
- QR thanh toán vẫn là QR chuyển khoản hiện có, nhân viên đối chiếu và xác nhận đúng tổng tiền.
- Giữ kiến trúc Java 17, Spring Boot, Maven, SQL Server và HTML CSS JavaScript tĩnh.
- Giữ cơ chế tách hóa đơn vé và hóa đơn bỏng nước, ghi nhận thanh toán và chống soát vé lặp.
- Không tự chạy script reset hoặc nâng cấp database đang dùng.
- Không tự sửa những vấn đề trong các báo cáo cũ nằm ngoài review được xác nhận ở cuộc hội thoại này.

## 2 Hiện trạng được đối chiếu với code

- `catalog/dto/AdminCatalog.java` có `actors`, chưa có `director` hoặc `trailerUrl`.
- `catalog/dto/Catalog.java` và các repository đọc phim cũng chưa có hai trường đó.
- `catalog/service/AdminCatalogService.java` đã bảo vệ suất có vé và chặn lịch trùng phòng.
- `UI/assets/js/admin.js` dựng suất chiếu thành bảng, chưa có timeline theo ngày và phòng.
- `SUAT_CHIEU` có `gia_co_ban` và `phu_thu_vip`. Chưa có chính sách giá ngày thường và cuối tuần được cấu hình riêng.
- `booking/service/OrderService.java` đã lấy giá từ suất, cộng phụ thu VIP ở backend và lưu đơn giá từng vé.
- `UI/assets/js/checkout.js` chưa dùng `expiresAt` của đơn để tắt QR, chưa có đếm ngược.
- `UI/assets/js/orders.js` hiện có mốc hết hạn tĩnh và timeout ẩn QR, chưa có đồng hồ đếm ngược.
- `payment/service/PaymentService.java` từ chối xác nhận đơn hết hạn và chỉ cho nhân viên xác nhận chuyển khoản.
- `booking/repository/TicketCheckInRepository.java` dùng một câu UPDATE có điều kiện để soát vé một lần, trong khoảng từ 30 phút trước giờ chiếu đến trước giờ kết thúc.
- `UI/assets/js/ticket-print.js` in CODE128; `theater.js` dùng `BrowserMultiFormatOneDReader`; trang đơn của khách hiển thị chuỗi mã vé.
- Thư viện ZXing hiện được lưu trong `assets/vendor`; cần kiểm tra API tạo và đọc QR của bản đang có trước khi quyết định thêm bất kỳ thư viện nào.

Kết quả kiểm tra ở lượt review trước là 142 test Maven và 10 test JavaScript đạt. Đây là bằng chứng cho code tại thời điểm review, không phải kết quả kiểm thử các thay đổi chưa triển khai. Test trình duyệt ở lượt đó chưa chạy được do môi trường chưa resolve được `playwright`.

## 3 Phim và trailer

### Hành vi đề xuất

1. Form quản trị phim có trường đạo diễn và đường dẫn trailer, cùng với diễn viên đã có.
2. API tạo và sửa phim nhận hai trường mới. API công khai trả cùng thông tin để trang chi tiết phim hiển thị.
3. Hai trường được phép trống, để phim hiện có tiếp tục đọc được và không phải nhập dữ liệu giả.
4. Đạo diễn tối đa 200 ký tự; URL trailer tối đa 500 ký tự. URL phải là HTTPS hợp lệ, có host và không có thông tin đăng nhập trong URL.
5. Đề xuất dùng nút mở trailer trong tab mới, `rel="noopener noreferrer"`, không chèn HTML hoặc iframe tùy ý từ dữ liệu admin.
6. Nếu trường trống, ẩn riêng phần hiển thị tương ứng.
7. Cho phép bổ sung metadata như mô tả, diễn viên, đạo diễn và trailer cho phim đã có vé. Tiếp tục khóa các thuộc tính ảnh hưởng vé/lịch sử như tên và thời lượng theo chính sách hiện có. Phần thay đổi điều kiện khóa sẽ được nêu rõ trong diff để duyệt.

### Dữ liệu

Thêm cột nullable `PHIM.dao_dien NVARCHAR(200)` và `PHIM.trailer_url NVARCHAR(500)`. Cập nhật SQL tạo mới và migration nâng cấp có kiểm tra cột đã tồn tại. Không xóa phim hoặc tạo lại bảng.

### Kiểm chứng

- Lưu và đọc lại đạo diễn/trailer qua API quản trị và công khai.
- Dữ liệu cũ chưa có hai trường vẫn đọc và hiển thị bình thường sau migration.
- Từ chối URL `javascript:`, HTTP, URL có credentials và URL quá dài.
- Nội dung có ký tự HTML được hiển thị như văn bản.
- Khách và nhân viên không sửa được phim.
- Cho phép bổ sung metadata phim có vé, nhưng không đổi được các trường được bảo vệ.

## 4 Giá do admin cấu hình

### Lựa chọn kỹ thuật đề xuất

Đề xuất lưu bảng giá theo từng phòng, gồm giá ghế thường và giá ghế VIP cho ngày thường/cuối tuần. Cách này cho phép phòng khác nhau có giá khác nhau và không buộc mọi suất hiện có đổi sang một mức giá chung.

Ngày thường là thứ Hai đến thứ Sáu. Cuối tuần là thứ Bảy và Chủ nhật. Xác định loại ngày theo giờ Việt Nam tại thời điểm bắt đầu suất. Chưa thêm giá ngày lễ do chưa có yêu cầu và lịch ngày lễ riêng.

Admin có thể lựa chọn khi tạo suất:

- `MANUAL`: giữ cách nhập giá cơ bản và phụ thu VIP hiện tại.
- `POLICY`: lấy bảng giá của phòng theo ngày bắt đầu suất và chụp giá vào chính suất đó.

Nếu phòng chưa có bảng giá hợp lệ, không cho dùng `POLICY` và trả thông báo yêu cầu cấu hình. Không dùng số tiền mẫu hoặc âm thầm thay bằng 0.

### Quy tắc dữ liệu

- Đề xuất bảng `BANG_GIA_PHONG`, một bản ghi cho mỗi phòng, khóa ngoại tới `PHONG_CHIEU`.
- Bốn giá được nhập bằng VND nguyên, không âm và không vượt giới hạn cột/DTO đang dùng.
- Giá VIP không thấp hơn giá thường cùng loại ngày, để có thể lưu phụ thu không âm theo mô hình hiện tại.
- Khi lấy chính sách: `gia_co_ban = gia_thuong`; `phu_thu_vip = gia_vip - gia_thuong`.
- Lưu chế độ giá của suất để quản trị biết suất đã được nhập tay hay lấy từ bảng giá.
- Sửa bảng giá không tự cập nhật giá của suất đã tạo, vé đã giữ hoặc đã bán.
- Khi admin yêu cầu lấy lại bảng giá cho suất chưa có vé, backend kiểm tra lại phòng, ngày và giá trong transaction hiện có.
- Tính tổng tiền đặt vé tiếp tục dựa trên giá lưu ở suất và giá từng vé, không tin giá gửi từ trình duyệt.

### API đề xuất

`GET /api/admin/rooms/{id}/pricing` đọc bảng giá phòng.

`PUT /api/admin/rooms/{id}/pricing` cập nhật bốn giá của phòng. Kiểm tra role ở security và tài khoản hoạt động ở service, giữ CSRF.

`ShowRequest` có lựa chọn chế độ giá rõ ràng. Request cũ không có lựa chọn vẫn dùng `MANUAL`, giữ khả năng hoạt động của các client hiện có.

### Kiểm chứng

- Thứ Sáu và thứ Bảy chọn đúng hai nhóm giá.
- Thời điểm UTC sát nửa đêm được quy đổi sang ngày Việt Nam trước khi chọn nhóm giá.
- Giá thường, VIP và tổng đơn khớp giữa API ghế, backend tạo đơn và hóa đơn.
- Sửa bảng giá không đổi suất/vé/đơn cũ.
- Chặn giá lẻ, âm, quá lớn và VIP thấp hơn thường.
- Chặn staff hoặc khách sửa bảng giá, kể cả phiên admin cũ sau khi tài khoản bị đổi quyền.

## 5 Giới hạn check-in muộn do admin quyết định

Đề xuất thêm cấu hình vận hành toàn rạp với trường `lateCheckInMinutes` có thể để trống.

- Để trống: giữ hành vi hiện tại, cho soát đến trước khi suất kết thúc.
- Có giá trị từ 0 đến 600: cho soát đến mốc bắt đầu cộng số phút đó, đồng thời luôn phải trước giờ kết thúc suất.
- Mở cổng vẫn từ 30 phút trước giờ bắt đầu.
- Mốc kết thúc suất luôn là biên loại trừ. Với giới hạn muộn theo phút, đề xuất cho phép đúng mốc giới hạn và từ chối sau mốc đó; test phải thể hiện rõ ranh giới này.
- Cấu hình mới áp dụng cho các lần soát tiếp theo. Giao diện phải nói rõ phạm vi áp dụng để admin không tưởng đây là cấu hình riêng từng suất.
- Việc kiểm tra thời hạn phải nằm trong cùng câu UPDATE có điều kiện soát vé, không tách kiểm tra và ghi nhận thành hai thao tác có thể đua nhau.
- Giữ kiểm tra vé, đơn và hóa đơn đã thanh toán, trạng thái suất hợp lệ, role hiện hành, nhân viên và thời gian soát.

API đề xuất: `GET` và `PUT /api/admin/theater-policy`. Chỉ admin đọc/sửa cấu hình quản trị. Service soát vé đọc chính sách ở server.

Kiểm chứng: mặc định cũ; đúng thời điểm mở; quá sớm; trước/đúng/sau giới hạn muộn; đúng giờ kết thúc; hai máy quét cùng vé; vé chưa thanh toán; suất hủy; role không hợp lệ.

## 6 Timeline lịch chiếu

### Hành vi đề xuất

- Thêm chế độ xem timeline trong mục lịch chiếu hiện có, bên cạnh bảng danh sách.
- Chọn ngày, chuyển ngày trước/sau, lọc phòng và trạng thái.
- Mỗi hàng là một phòng; trục ngang là thời gian. Mỗi khối có tên phim, thời gian bắt đầu/kết thúc và trạng thái.
- Bấm khối mở thông tin hoặc form sửa hiện có. Không thêm kéo thả ở lượt này.
- Suất qua nửa đêm được hiển thị ở cả hai ngày có giao nhau và cắt khối theo biên ngày đang xem.
- Giữ một cách xem dạng bảng có thể thao tác bằng bàn phím; không dùng màu là dấu hiệu duy nhất để phân biệt trạng thái.
- Trên màn hình hẹp, timeline có cuộn ngang, không bóp khối đến mức không đọc được.

### Dữ liệu

API đề xuất `GET /api/admin/showtimes/timeline?date=YYYY-MM-DD&roomId=...&status=...`.

Truy vấn theo điều kiện giao khoảng `bat_dau < ngay_sau AND ket_thuc > dau_ngay`, có thứ tự ổn định theo phòng và thời gian. Không lấy duy nhất trang 100 suất đầu của danh sách hiện tại rồi gọi đó là lịch đầy đủ.

Giới hạn phạm vi một ngày. Nếu cần giới hạn số bản ghi, phải báo rõ hoặc có phân trang đầy đủ; không âm thầm cắt bỏ suất. Kiểm tra role giống các API lịch chiếu khác.

Kiểm chứng: nhiều phòng, ngày trống, lọc phòng/trạng thái, qua nửa đêm, số suất vượt một trang danh sách, timezone, thao tác bàn phím và màn hình hẹp. Giữ các test chống trùng giờ hiện có.

## 7 Đếm ngược và hết hạn QR thanh toán

### Hành vi

- Trang checkout và trang chi tiết đơn hiển thị thời gian giữ chỗ còn lại.
- Sử dụng `expiresAt` do backend trả, không tự cộng cố định 15 phút; hạn có thể ngắn hơn khi sát giờ chiếu.
- Hết hạn: ẩn QR, vô hiệu hóa tải ảnh và sao chép thông tin thanh toán, thu hồi object URL, báo không chuyển tiền cho đơn này và giữ đường dẫn xem trạng thái.
- Không chỉ giảm biến đếm mỗi giây; tính lại chênh lệch với mốc hết hạn để không sai khi tab ngủ.
- Chặn phản hồi QR hoặc kết quả tạo PNG về muộn làm mã xuất hiện lại sau khi hết hạn.
- Khi trang/đơn thay đổi, hủy timer và không để callback cũ ghi đè đơn đang xem.
- Mở lại popup hoặc quay lại tab phải kiểm tra hạn ngay.
- Khi đơn đã thanh toán hoặc đã hủy theo dữ liệu mới, dừng timer và bỏ các thao tác chuyển tiền.
- Đề xuất bổ sung thời gian server trong phản hồi QR hoặc response đơn để giảm lệch đồng hồ máy khách; chi tiết thay đổi API sẽ được trình bày trong diff.
- Backend tiếp tục là nơi quyết định đơn có được thanh toán hay không; UI không tự gán đơn thành đã thanh toán hoặc tự tạo hóa đơn.

Kiểm chứng bằng test fake clock và delayed promise: hết hạn chính xác, sát giờ chiếu, tab ngủ, mở lại popup, đổi đơn, QR trả muộn, PNG trả muộn, QR tải lỗi, timer được dọn, không tự tạo lại đơn khi lỗi mạng.

## 8 QR riêng cho từng vé

- Tạo QR trong trình duyệt, không gửi mã vé tới website tạo QR bên ngoài.
- Payload của mỗi QR là `VE.ma_ve` duy nhất của vé đó, không phải mã đơn, mã hóa đơn hay token truy cập guest.
- Trang đơn khách hiển thị QR từng ghế sau thanh toán; mã văn bản vẫn có để nhập tay.
- Vé đã soát có nhãn đã sử dụng và không trình bày QR như một vé còn hiệu lực.
- Mẫu vé in có QR riêng của vé. Giữ CODE128 hiện có nếu còn cần cho thiết bị cũ; cùng trỏ về một mã vé và cùng quy tắc soát một lần.
- Camera tại trang vận hành đọc cả QR và CODE128. Đọc mã chỉ điền vào ô, vẫn giữ bước nhân viên xác nhận; không tự tiêu thụ lượt in hoặc soát khi camera vừa nhận được mã.
- Thử khả năng tạo/đọc QR của thư viện ZXing đã có. Chỉ thêm phụ thuộc nếu không thể dùng thư viện hiện tại và phải nêu lý do trong bản review.
- Quy tắc một lượt in và một lượt soát tiếp tục do backend bảo vệ. QR không thay thế kiểm tra thanh toán, quyền và thời gian.

Kiểm chứng: đơn nhiều ghế có QR khác nhau; encode/decode khớp mã; khách chưa thanh toán không thấy QR vé khả dụng; vé đã soát có trạng thái rõ; camera đọc hai định dạng; scanner gõ bàn phím vẫn hoạt động; hai lần soát không cùng thành công; không có request mã vé tới dịch vụ tạo ảnh ngoài.

## 9 Bốn tài liệu Word rất chi tiết

Thư mục đầu ra dự kiến `docs/huong-dan-4-nguoi/`. Bốn tài liệu là file DOCX, không thay bằng Markdown hoặc PDF.

1. `01-nghiem-tuan-tai-khoan-va-quyen-truy-cap.docx`.
2. `02-huy-anh-phim-phong-va-lich-chieu.docx`.
3. `03-viet-anh-bap-nuoc-thanh-toan-va-bao-cao.docx`.
4. `04-dat-ve-don-hang-va-hoa-don.docx`.

### Cấu trúc bắt buộc của mỗi tài liệu

- Mục tiêu module, người phụ trách, phạm vi, dữ liệu vào/ra và phần việc liên quan nhóm khác.
- Mục lục, thuật ngữ và thứ tự đọc code cho người mới.
- Sơ đồ kiến trúc và sơ đồ flow; phân biệt luồng thành công, thất bại và trường hợp đồng thời.
- Hướng dẫn thao tác trên từng màn hình cho từng vai trò liên quan.
- Bản đồ file thực tế, tên class/hàm và vị trí code sau khi sửa.
- API: method, URL, quyền, CSRF, DTO, validation, ví dụ request/response và mã lỗi.
- Flow từng bước từ sự kiện giao diện, hàm JavaScript, API, controller, service, repository, SQL và phản hồi ngược về DOM.
- Trích code thực tế theo từng khối vừa đọc được, giải thích tham số, từng điều kiện quan trọng, biến trạng thái, câu SQL và lý do dùng transaction/khóa.
- Bảng dữ liệu, khóa chính/ngoại, trạng thái và quy tắc chuyển trạng thái.
- Phân quyền ở giao diện và server; phiên tài khoản/guest; các dữ liệu client không được tự quyết định.
- Test liên quan và cách chạy, dữ liệu đầu vào, kết quả mong đợi, cách tái hiện lỗi và giới hạn kiểm chứng.
- Tình huống mẫu xuyên module, ví dụ đặt hai ghế kèm combo rồi thanh toán, phát hành hóa đơn và soát từng vé.
- Câu hỏi giải thích/bảo vệ đồ án và cách lần từ hành vi tới code để trả lời.
- Giới hạn thực tế: chuyển khoản còn thủ công; cấu hình và kiểm thử SQL Server/thiết bị nào chưa thực hiện phải ghi đúng.

### Phân chia và liên kết

Tài liệu Nghiêm Tuấn giải thích đăng ký, BCrypt, login/logout, session, CSRF, role, quản trị nhân viên và quyền xem đơn. Tài liệu khác dẫn chiếu phần này và giải thích lại điều kiện quyền tại điểm giao nghiệp vụ của mình.

Tài liệu Huy Anh gồm phim, đạo diễn/trailer, phòng, trạng thái phòng, sơ đồ ghế, thường/VIP, bảng giá theo loại ngày, tạo/sửa suất và timeline, chống trùng lịch và bảo vệ lịch sử vé.

Tài liệu Việt Anh gồm sản phẩm/combo, giá/trạng thái bán, giao nhận bắp nước, QR chuyển khoản, thu tiền mặt/tiền thừa, xác nhận thủ công, idempotency, transaction thanh toán, email sau commit và báo cáo theo ngày.

Tài liệu đặt vé gồm chọn phim/suất/ghế, sessionStorage và giỏ, đơn guest/tài khoản/tại quầy, giữ ghế, hết hạn, hủy, tính giá server, chi tiết đơn, hai loại hóa đơn, QR vé, in và soát vé. Khi giải thích thanh toán phải dẫn tới tài liệu Việt Anh, không mô tả hai cách xác nhận thanh toán khác nhau.

### Chất lượng Word

Văn bản tiếng Việt, heading và mục lục rõ ràng, font đọc được, code monospace có xuống dòng phù hợp, sơ đồ và bảng không bị cắt, header/footer và số trang nhất quán. Không kéo dài số trang bằng cách lặp lại code hoặc diễn giải chung chung.

Mỗi file phải được render bằng quy trình `documents` và kiểm tra ảnh từng trang trước khi giao. Ảnh/PDF trung gian chỉ phục vụ QA, không thay bốn file DOCX được yêu cầu.

## 10 Phân tích code bằng understand

Plugin được tìm thấy tại đường dẫn người dùng chỉ định, core đã build và runtime Node được bundle sẵn có thể dùng.

Chưa có knowledge graph chuẩn trong `.ua/` hoặc `.understand-anything/`. Báo cáo `review_main` chứa graph phục vụ một lần review cũ, không coi là graph ký hiệu hiện tại.

Đã tạo `.ua/.understandignore` dạng starter theo script của plugin. Chưa chạy phân tích. Đề xuất giữ test trong phạm vi để liên kết test với code; loại `target`, upload, cấu hình máy, credential, log, vendor minified và các file render QA. Không đưa mật khẩu kết nối hoặc các file secrets vào graph.

Ngôn ngữ nội dung graph đề xuất là tiếng Việt, cùng ngôn ngữ bốn tài liệu. Khoảng 174 file text trong các nhóm source/test/database/docs trước khi áp bộ lọc của plugin; phân tích đầy đủ có thể mất thời gian. Chỉ chạy subagent scanner/analyzer theo quy trình skill sau khi bộ lọc được duyệt.

## 11 Cấu trúc và quy tắc thay đổi

- Dùng package nghiệp vụ hiện có. DTO rõ ràng, controller mỏng, service giữ luật, repository giữ SQL.
- Chính sách phòng và giá thuộc catalog; booking sử dụng giá đã chụp ở suất và dịch vụ chính sách ở server, không đặt logic giá vào tiện ích JavaScript chung.
- Nếu cần tách timeline hoặc đồng hồ thành file mới, chỉ tách khi giúp tránh phình `admin.js`/`orders.js` và có ranh giới rõ. Không tạo lớp chỉ chuyển tiếp không có trách nhiệm riêng.
- Không ghi đè các thay đổi chưa commit của người dùng. Ghi nhận baseline trước khi tạo bản nháp/diff.
- Không commit, push, triển khai hoặc chạy migration trên database thật trong phạm vi này.
- Không sửa hoặc xóa ba tài liệu Word cũ ở `docs/huong-dan-3-nguoi/`.

## 12 Kiểm thử và điều kiện nghiệm thu

1. Viết test lỗi/luật mới trước, xác nhận RED, sau đó thay đổi tối thiểu và xác nhận GREEN.
2. Test backend tập trung: validation, giá theo ngày/phòng, bảo vệ giá cũ, quyền cấu hình, giới hạn soát, metadata phim, khoảng thời gian timeline.
3. Test frontend: countdown với fake clock, phản hồi QR/PNG muộn, đọc/ghi QR, hiển thị timeline và form admin. Không chỉ kiểm tra chuỗi source code hoặc tên hàm tồn tại.
4. Chạy full Maven test suite sau các test tập trung. Chạy unit JavaScript và kiểm tra cú pháp các file thay đổi.
5. Kiểm thử trình duyệt bằng fixture RAM khi có runtime Playwright; không tự dùng database thật để thay cho fixture. Nếu không chạy được phải ghi lỗi môi trường cụ thể.
6. Migration kiểm tra chạy lại không lỗi và giữ dữ liệu trên database thử nghiệm riêng khi có điều kiện. Không suy kết quả H2 thành đã kiểm chứng SQL Server.
7. Review theo correctness, readability, architecture, security và performance. Chỉ xác nhận đạt khi có bằng chứng.
8. Mỗi chức năng phải có diff cụ thể để người dùng duyệt trước khi áp dụng vào source đang làm việc.
9. Bốn tài liệu giải thích đúng code cuối cùng, đủ flow và code đã nêu, đã render và kiểm tra từng trang.

Các lệnh kiểm chứng dự kiến, phải điều chỉnh danh sách test theo code thực tế:

```powershell
mvn test
node --test tests/frontend/unit/cart.test.cjs tests/frontend/unit/cinema.test.cjs
node --test src/test/js/authentication.test.cjs
node --check src/main/resources/UI/assets/js/checkout.js
node --check src/main/resources/UI/assets/js/orders.js
node --check src/main/resources/UI/assets/js/theater.js
node --check src/main/resources/UI/assets/js/ticket-print.js
node --check src/main/resources/UI/assets/js/admin.js
```

## 13 Nội dung chờ duyệt

- Các lựa chọn kỹ thuật đề xuất: giá theo phòng; thứ Bảy/Chủ nhật là cuối tuần; giá chụp vào suất; giới hạn check-in toàn rạp để trống giữ hành vi cũ; trailer mở bằng liên kết HTTPS; timeline không kéo thả; QR từng vé và giữ CODE128 khi cần.
- Phạm vi lọc file trong `.ua/.understandignore` trước khi plugin bắt đầu phân tích.
- Cho phép chuyển từ đặc tả sang chuẩn bị code/diff để người dùng review. Chưa coi đây là duyệt áp dụng code chưa được trình bày.

Sau khi duyệt, ghi lại kế hoạch và task theo các lát cắt có kiểm thử. Không tự thêm tích hợp chuyển khoản tự động trong quá trình thực hiện.
