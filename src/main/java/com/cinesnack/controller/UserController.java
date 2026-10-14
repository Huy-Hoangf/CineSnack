package com.cinesnack.controller;
import com.cinesnack.service.UserService;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.Map;
@RestController
public class UserController {
    private final UserService nguoiDung;
    public UserController(UserService nguoiDung) { this.nguoiDung=nguoiDung; }
    @GetMapping("/api/auth/me")
    public Map<String,Object> layTaiKhoan(Principal principal) {
        var u=nguoiDung.layTaiKhoanHoatDong(principal.getName());
        return Map.of("id",u.maSo(),"email",u.thuDienTu(),"role",u.vaiTro());
    }
}
