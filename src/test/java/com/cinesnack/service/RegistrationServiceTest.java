package com.cinesnack.service;

import com.cinesnack.account.dto.RegisterRequest;
import com.cinesnack.account.repository.UserRepository;
import com.cinesnack.account.service.RegistrationService;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RegistrationServiceTest {
    private final UserRepository repository = mock(UserRepository.class);
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final RegistrationService service = new RegistrationService(repository, encoder);

    @Test void storesEncodedPassword() {
        service.dangKy(new RegisterRequest(" An ", " an@example.com ", "matkhau123"));
        verify(repository).taoKhachHang(eq("An"), eq("an@example.com"), argThat(hash -> encoder.matches("matkhau123", hash)));
    }

    @Test void rejectsDuplicateEmail() {
        doThrow(new DuplicateKeyException("duplicate")).when(repository).taoKhachHang(any(), any(), any());
        var error = assertThrows(ResponseStatusException.class, () -> service.dangKy(new RegisterRequest("An", "an@example.com", "matkhau123")));
        assertEquals(409, error.getStatusCode().value());
    }

    @Test void rejectsPasswordLongerThanBcryptByteLimit() {
        var error = assertThrows(ResponseStatusException.class, () -> service.dangKy(new RegisterRequest("An", "an@example.com", "á".repeat(37))));
        assertEquals(400, error.getStatusCode().value());
        verifyNoInteractions(repository);
    }
}
