package com.cinesnack.account.service;

import com.cinesnack.account.dto.RegisterRequest;
import com.cinesnack.account.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RegistrationService {
    private final UserRepository repository;
    private final PasswordEncoder encoder;

    public RegistrationService(UserRepository repository, PasswordEncoder encoder) {
        this.repository = repository;
        this.encoder = encoder;
    }

    public void dangKy(RegisterRequest request) {
        String hoTen = request.hoTen().trim();
        String email = request.email().trim();
        if (hoTen.isEmpty() || request.matKhau().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Họ tên hoặc mật khẩu không hợp lệ");
        }
        try {
            repository.taoKhachHang(hoTen, email, encoder.encode(request.matKhau()));
        } catch (DuplicateKeyException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email đã được sử dụng");
        }
    }
}
