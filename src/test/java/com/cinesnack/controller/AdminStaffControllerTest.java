package com.cinesnack.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.cinesnack.account.controller.AdminStaffController;
import com.cinesnack.account.entity.User;
import com.cinesnack.account.repository.AdminStaffRepository;
import com.cinesnack.account.service.UserService;
import java.security.Principal;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

class AdminStaffControllerTest {

    final AdminStaffRepository repository = mock(AdminStaffRepository.class);
    final UserService users = mock(UserService.class);
    final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
    final AdminStaffController controller = new AdminStaffController(
        repository,
        users,
        encoder
    );
    final Principal admin = () -> "admin@example.com";

    AdminStaffControllerTest() {
        when(users.layTaiKhoanHoatDong(admin.getName())).thenReturn(
            new User(1, admin.getName(), "", "QUAN_TRI", "HOAT_DONG")
        );
    }

    @Test
    void rejectsPasswordsOver72Utf8BytesBeforeEncodingOrSaving() {
        for (String password : new String[] {
            "x".repeat(73),
            "ế".repeat(25),
        }) {
            var request = new AdminStaffController.Create(
                "Nhân viên",
                "staff@example.com",
                password,
                "NHAN_VIEN"
            );
            var error = assertThrows(ResponseStatusException.class, () ->
                controller.create(request, admin)
            );
            assertEquals(400, error.getStatusCode().value());
        }
        verifyNoInteractions(repository);
    }

    @Test
    void acceptsExactly72BytesAndStoresOnlyTheHash() {
        String password = "x".repeat(72);
        controller.create(
            new AdminStaffController.Create(
                "Nhân viên",
                "STAFF@example.com",
                password,
                "NHAN_VIEN"
            ),
            admin
        );
        var hash = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(repository).create(
            eq("Nhân viên"),
            eq("staff@example.com"),
            hash.capture(),
            eq("NHAN_VIEN")
        );
        assertTrue(encoder.matches(password, hash.getValue()));
    }
}
