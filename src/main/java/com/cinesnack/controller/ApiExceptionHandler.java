package com.cinesnack.controller;

import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.*;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

@RestControllerAdvice(basePackages = "com.cinesnack.controller")
public class ApiExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> xuLyLoiNghiepVu(ResponseStatusException ngoaiLe) {
        return ResponseEntity.status(ngoaiLe.getStatusCode())
                .body(Map.of("message", ngoaiLe.getReason() == null ? "Yêu cầu không hợp lệ" : ngoaiLe.getReason()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class,
            HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            HandlerMethodValidationException.class})
    public ResponseEntity<Map<String, String>> xuLyDuLieuKhongHopLe(Exception ngoaiLe) {
        return taoPhanHoi(HttpStatus.BAD_REQUEST, "Dữ liệu gửi lên không hợp lệ");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> loiXungDot(DataIntegrityViolationException ngoaiLe) {
        return taoPhanHoi(HttpStatus.CONFLICT, "Dữ liệu đã thay đổi hoặc bị trùng, vui lòng kiểm tra lại đơn hàng");
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, String>> xuLyLoiCoSoDuLieu(DataAccessException ngoaiLe) {
        return taoPhanHoi(HttpStatus.SERVICE_UNAVAILABLE, "Không thể xử lý dữ liệu lúc này, vui lòng thử lại");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> xuLyLoiKhongDuKien(Exception ngoaiLe) {
        return taoPhanHoi(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể xử lý yêu cầu lúc này");
    }

    private ResponseEntity<Map<String, String>> taoPhanHoi(HttpStatus trangThai, String thongBao) {
        return ResponseEntity.status(trangThai).body(Map.of("message", thongBao));
    }
}
