package com.cinesnack.controller;

import com.cinesnack.account.controller.UserController;
import com.cinesnack.account.controller.RegistrationController;
import com.cinesnack.account.service.RegistrationService;
import com.cinesnack.common.controller.ApiExceptionHandler;
import com.cinesnack.common.controller.CsrfController;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.cinesnack.account.config.ApiSecurityConfig;
import com.cinesnack.account.entity.User;
import com.cinesnack.account.repository.UserRepository;
import com.cinesnack.account.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@SpringJUnitConfig(SessionSecurityTest.Config.class)
@WebAppConfiguration
class SessionSecurityTest {

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @Import({ ApiSecurityConfig.class, CsrfController.class, ApiExceptionHandler.class, UserController.class, RegistrationController.class, RegistrationService.class })
    static class Config {

        @Bean
        UserRepository khoNguoiDung() {
            return mock(UserRepository.class);
        }

        @Bean
        UserService dichVuNguoiDung() {
            return mock(UserService.class);
        }
    }

    @Autowired
    WebApplicationContext nguCanh;

    @Autowired
    UserRepository khoNguoiDung;

    @Autowired
    UserService dichVuNguoiDung;

    @Autowired
    PasswordEncoder boMaHoa;

    private MockMvc http;

    @Test
    void dangKyCongKhaiNhungCanCsrfVaDuLieuHopLe() throws Exception {
        String valid = "{\"hoTen\":\"An\",\"email\":\"an@example.com\",\"matKhau\":\"matkhau123\"}";
        http.perform(post("/api/auth/register").contentType("application/json").content(valid))
            .andExpect(status().isForbidden());
        http.perform(post("/api/auth/register").with(csrf()).contentType("application/json").content(valid))
            .andExpect(status().isCreated());
        verify(khoNguoiDung).taoKhachHang(eq("An"), eq("an@example.com"), anyString());
        http.perform(post("/api/auth/register").with(csrf()).contentType("application/json")
            .content("{\"hoTen\":\"An\",\"email\":\"bad\",\"matKhau\":\"123\"}"))
            .andExpect(status().isBadRequest());
    }

    @BeforeEach
    void chuanBi() {
        reset(khoNguoiDung, dichVuNguoiDung);
        var khach = new User(
            7,
            "customer@example.com",
            boMaHoa.encode("test-only-password"),
            "KHACH_HANG",
            "HOAT_DONG"
        );
        when(khoNguoiDung.timTheoThuDienTu(khach.thuDienTu())).thenReturn(khach);
        when(dichVuNguoiDung.layTaiKhoanHoatDong(khach.thuDienTu())).thenReturn(khach);
        http = org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(
            nguCanh
        )
            .apply(springSecurity())
            .build();
    }

    @Test
    void dangNhapLuuSessionVaTraThongTinKhongLoMatKhau() throws Exception {
        var ketQua = http
            .perform(
                post("/api/auth/login")
                    .with(csrf())
                    .param("username", "customer@example.com")
                    .param("password", "test-only-password")
            )
            .andExpect(status().isNoContent())
            .andReturn();
        var session = (MockHttpSession) ketQua.getRequest().getSession(false);
        assertNotNull(session, "Login must create a session usable by subsequent browser requests");

        http.perform(get("/api/auth/me").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(7))
            .andExpect(jsonPath("$.email").value("customer@example.com"))
            .andExpect(jsonPath("$.role").value("KHACH_HANG"))
            .andExpect(jsonPath("$.passwordHash").doesNotExist())
            .andExpect(jsonPath("$.matKhauHash").doesNotExist());
    }

    @Test
    void matKhauSaiKhongTaoSessionDangNhap() throws Exception {
        http.perform(
            post("/api/auth/login")
                .with(csrf())
                .param("username", "customer@example.com")
                .param("password", "wrong")
        ).andExpect(status().isUnauthorized());
    }

    @Test
    void dangNhapCanCsrf() throws Exception {
        http.perform(
            post("/api/auth/login")
                .param("username", "customer@example.com")
                .param("password", "test-only-password")
        ).andExpect(status().isForbidden());
    }

    @Test
    void tokenCsrfCuSauLoginKhongDungDuocVaTokenMoiDungDuoc() throws Exception {
        var truoc = http.perform(get("/api/csrf")).andExpect(status().isOk()).andReturn();
        var phienCu = (MockHttpSession) truoc.getRequest().getSession(false);
        String tokenCu = com.jayway.jsonpath.JsonPath.read(
            truoc.getResponse().getContentAsString(),
            "$.token"
        );
        var login = http
            .perform(
                post("/api/auth/login")
                    .session(phienCu)
                    .header("X-CSRF-TOKEN", tokenCu)
                    .param("username", "customer@example.com")
                    .param("password", "test-only-password")
            )
            .andExpect(status().isNoContent())
            .andReturn();
        var phienMoi = (MockHttpSession) login.getRequest().getSession(false);
        http.perform(
            post("/api/auth/logout").session(phienMoi).header("X-CSRF-TOKEN", tokenCu)
        ).andExpect(status().isForbidden());
        var sau = http
            .perform(get("/api/csrf").session(phienMoi))
            .andExpect(status().isOk())
            .andReturn();
        String tokenMoi = com.jayway.jsonpath.JsonPath.read(
            sau.getResponse().getContentAsString(),
            "$.token"
        );
        http.perform(
            post("/api/auth/logout").session(phienMoi).header("X-CSRF-TOKEN", tokenMoi)
        ).andExpect(status().isNoContent());
    }

    @Test
    void dangXuatHuySessionVaKhongConXemDuocTaiKhoan() throws Exception {
        var ketQua = http
            .perform(
                post("/api/auth/login")
                    .with(csrf())
                    .param("username", "customer@example.com")
                    .param("password", "test-only-password")
            )
            .andExpect(status().isNoContent())
            .andReturn();
        var session = (MockHttpSession) ketQua.getRequest().getSession(false);
        assertNotNull(session);

        http.perform(post("/api/auth/logout").session(session).with(csrf())).andExpect(
            status().isNoContent()
        );
        assertTrue(session.isInvalid(), "Logout must invalidate the authenticated session");
        http.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }
}
