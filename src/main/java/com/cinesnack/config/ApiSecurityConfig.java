package com.cinesnack.config;

import com.cinesnack.repository.UserRepository;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class ApiSecurityConfig {

    @Bean
    public PasswordEncoder boMaHoaMatKhau() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public UserDetailsService dichVuChiTietNguoiDung(UserRepository khoNguoiDung) {
        return thuDienTu -> {
            var taiKhoan = khoNguoiDung.timTheoThuDienTu(thuDienTu);
            if (
                taiKhoan == null ||
                !Set.of("KHACH_HANG", "NHAN_VIEN", "QUAN_TRI").contains(taiKhoan.vaiTro())
            ) {
                throw new UsernameNotFoundException("Tài khoản không hợp lệ");
            }
            return User.withUsername(taiKhoan.thuDienTu())
                .password(taiKhoan.matKhauHash())
                .roles(taiKhoan.vaiTro())
                .disabled(!"HOAT_DONG".equals(taiKhoan.trangThai()))
                .build();
        };
    }

    @Bean
    public SecurityFilterChain baoMatApi(HttpSecurity baoMatHttp) throws Exception {
        return (
            baoMatHttp
                .securityMatcher("/api/**")
                .authorizeHttpRequests(quyTac ->
                    quyTac
                        .requestMatchers("/api/csrf", "/api/auth/login")
                        .permitAll()
                        .requestMatchers(
                            HttpMethod.GET,
                            "/api/movies",
                            "/api/movies/*",
                            "/api/showtimes",
                            "/api/showtimes/*/seats",
                            "/api/snacks"
                        )
                        .permitAll()
                        .requestMatchers(
                            "/api/admin/movies",
                            "/api/admin/movies/**",
                            "/api/admin/showtimes",
                            "/api/admin/showtimes/**",
                            "/api/admin/rooms"
                        )
                        .hasRole("QUAN_TRI")
                        .requestMatchers("/api/guest/orders", "/api/guest/orders/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/admin/orders")
                        .hasRole("NHAN_VIEN")
                        .requestMatchers("/api/admin/**")
                        .hasAnyRole("NHAN_VIEN", "QUAN_TRI")
                        .requestMatchers(HttpMethod.POST, "/api/orders/*/payments/cash")
                        .hasAnyRole("NHAN_VIEN", "QUAN_TRI")
                        .requestMatchers(HttpMethod.POST, "/api/orders/*/payments/transfer")
                        .hasAnyRole("NHAN_VIEN", "QUAN_TRI")
                        .anyRequest()
                        .authenticated()
                )
                .formLogin(dangNhap ->
                    dangNhap
                        .loginProcessingUrl("/api/auth/login")
                        .successHandler((req, res, auth) -> res.setStatus(204))
                        .failureHandler((req, res, error) -> res.sendError(401))
                )
                .logout(dangXuat ->
                    dangXuat
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler((req, res, auth) -> res.setStatus(204))
                )
                .exceptionHandling(loi ->
                    loi.authenticationEntryPoint((req, res, error) -> res.sendError(401))
                )
                .httpBasic(Customizer.withDefaults())
                // Giữ CSRF: client lấy token tại /api/csrf và gửi lại khi POST.
                .build()
        );
    }
}
