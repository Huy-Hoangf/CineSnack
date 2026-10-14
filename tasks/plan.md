# Nối FE–BE và đồng bộ main

Phạm vi/đặc tả và code preview đã được duyệt qua yêu cầu áp dụng sau kiểm thử mô phỏng. Không tự stage/commit, merge hoặc chạy SQL. Giữ nguyên thiết kế và Maven. Các phần ngoài phạm vi: đăng ký, cổng thanh toán, hoàn tiền, email/PDF.

1. Đồng bộ schema và repository theo main, giữ hóa đơn tách và bảo vệ soát vé/giao hàng.
2. Regression test giờ chờ khóa, phiên/CSRF; thêm API catalog và danh sách đơn cho nhân viên.
3. Nối giao diện catalog/booking/auth/checkout/orders/admin với API thật, bỏ fallback demo.
4. Chạy test Java, browser với API fixture, review và mô phỏng merge; ghi rõ SQL Server chưa được xác minh nếu thiếu môi trường.

Kiểm thử chính: mvn test; node --test tests/frontend/unit/*.test.cjs; browser test và git diff --check. Kiểm thử SQL Server chỉ khi có CINESNACK_TEST_DB_URL; không tự chọn DB thật hay chạy reset.
