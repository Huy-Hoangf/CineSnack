package com.cinesnack.controller;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.*;

import com.cinesnack.account.config.ApiSecurityConfig;
import com.cinesnack.account.repository.UserRepository;
import com.cinesnack.booking.controller.TicketCheckInController;
import com.cinesnack.booking.service.TicketCheckInService;
import com.cinesnack.catalog.controller.RoomManagementController;
import com.cinesnack.catalog.service.RoomManagementService;
import com.cinesnack.common.controller.ApiExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@SpringJUnitConfig(TheaterSecurityTest.Config.class)
@WebAppConfiguration
class TheaterSecurityTest {

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @Import({
        ApiSecurityConfig.class,
        RoomManagementController.class,
        TicketCheckInController.class,
        ApiExceptionHandler.class,
    })
    static class Config {

        @Bean
        UserRepository users() {
            return mock(UserRepository.class);
        }

        @Bean
        RoomManagementService rooms() {
            return mock(RoomManagementService.class);
        }

        @Bean
        TicketCheckInService checkIn() {
            return mock(TicketCheckInService.class);
        }
    }

    @Autowired
    WebApplicationContext context;

    MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void roomManagementIsAdminOnlyIncludingNestedRoutes() throws Exception {
        mvc.perform(get("/api/admin/rooms/management")).andExpect(
            status().isUnauthorized()
        );
        mvc.perform(
            get("/api/admin/rooms/1/seats").with(
                user("staff").roles("NHAN_VIEN")
            )
        ).andExpect(status().isForbidden());
        mvc.perform(
            get("/api/admin/rooms/management").with(
                user("admin").roles("QUAN_TRI")
            )
        ).andExpect(status().isOk());
    }

    @Test
    void setupRejectsOversizedLayoutsAndRequiresCsrf() throws Exception {
        mvc.perform(
            post("/api/admin/rooms/1/seats/setup")
                .with(user("admin").roles("QUAN_TRI"))
                .contentType("application/json")
                .content("{\"rows\":27,\"columns\":30,\"type\":\"THUONG\"}")
        ).andExpect(status().isForbidden());
        mvc.perform(
            post("/api/admin/rooms/1/seats/setup")
                .with(user("admin").roles("QUAN_TRI"))
                .with(csrf())
                .contentType("application/json")
                .content("{\"rows\":27,\"columns\":30,\"type\":\"THUONG\"}")
        ).andExpect(status().isBadRequest());
    }

    @Test
    void deleteRoomRequiresAdminAndCsrf() throws Exception {
        mvc.perform(
            delete("/api/admin/rooms/1")
                .with(user("staff").roles("NHAN_VIEN"))
                .with(csrf())
        ).andExpect(status().isForbidden());
        mvc.perform(
            delete("/api/admin/rooms/1").with(user("admin").roles("QUAN_TRI"))
        ).andExpect(status().isForbidden());
        mvc.perform(
            delete("/api/admin/rooms/1")
                .with(user("admin").roles("QUAN_TRI"))
                .with(csrf())
        ).andExpect(status().isNoContent());
    }

    @Test
    void checkInIsStaffOnlyAndRequiresCsrfAndValidCode() throws Exception {
        mvc.perform(
            post("/api/staff/tickets/scan")
                .with(user("customer").roles("KHACH_HANG"))
                .with(csrf())
                .contentType("application/json")
                .content("{\"code\":\"VE123\"}")
        ).andExpect(status().isForbidden());
        mvc.perform(
            post("/api/staff/tickets/scan")
                .with(user("staff").roles("NHAN_VIEN"))
                .contentType("application/json")
                .content("{\"code\":\"VE123\"}")
        ).andExpect(status().isForbidden());
        mvc.perform(
            post("/api/staff/tickets/scan")
                .with(user("staff").roles("NHAN_VIEN"))
                .with(csrf())
                .contentType("application/json")
                .content("{\"code\":\"VE123\"}")
        ).andExpect(status().isOk());
        mvc.perform(
            post("/api/staff/tickets/scan")
                .with(user("admin").roles("QUAN_TRI"))
                .with(csrf())
                .contentType("application/json")
                .content("{\"code\":\"<script>\"}")
        ).andExpect(status().isBadRequest());
    }

    @Test
    void issueRequiresStaffSessionAndCsrf() throws Exception {
        mvc.perform(
            post("/api/staff/tickets/issue")
                .with(user("customer").roles("KHACH_HANG"))
                .with(csrf())
                .contentType("application/json")
                .content("{\"code\":\"VE123\"}")
        ).andExpect(status().isForbidden());
        mvc.perform(
            post("/api/staff/tickets/issue")
                .with(user("staff").roles("NHAN_VIEN"))
                .contentType("application/json")
                .content("{\"code\":\"VE123\"}")
        ).andExpect(status().isForbidden());
        mvc.perform(
            post("/api/staff/tickets/issue")
                .with(user("staff").roles("NHAN_VIEN"))
                .with(csrf())
                .contentType("application/json")
                .content("{\"code\":\"VE123\"}")
        ).andExpect(status().isOk());
    }
}
