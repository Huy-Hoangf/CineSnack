package com.cinesnack.service;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.cinesnack.account.entity.User;
import com.cinesnack.account.service.UserService;
import com.cinesnack.catalog.dto.TheaterPolicy;
import com.cinesnack.catalog.repository.TheaterPolicyRepository;
import com.cinesnack.catalog.service.TheaterPolicyService;
import jakarta.validation.Validation;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
class TheaterPolicyServiceTest {
 final TheaterPolicyRepository repo=mock(TheaterPolicyRepository.class);
 final UserService users=mock(UserService.class);
 final TheaterPolicyService service=new TheaterPolicyService(repo,users);
 final TheaterPolicy.RoomPricing prices=new TheaterPolicy.RoomPricing(new BigDecimal("70000"),new BigDecimal("90000"),new BigDecimal("100000"),new BigDecimal("130000"));
 @Test void selectsVietnamFridayAndSaturdayAcrossUtcDateBoundary() {
  when(repo.pricing(2)).thenReturn(prices);
  assertEquals(new BigDecimal("70000"),service.pricesForShow(2,OffsetDateTime.parse("2026-11-12T18:00:00Z")).regular());
  assertEquals(new BigDecimal("100000"),service.pricesForShow(2,OffsetDateTime.parse("2026-11-13T18:00:00Z")).regular());
  assertEquals(new BigDecimal("130000"),service.pricesForShow(2,OffsetDateTime.parse("2026-11-13T18:00:00Z")).vip());
 }
 @Test void absentPolicyIsConflictRatherThanZeroPrice() {
  assertEquals(409,assertThrows(ResponseStatusException.class,()->service.pricesForShow(2,OffsetDateTime.now())).getStatusCode().value());
 }
 @Test void currentStaffCannotReadOrWriteAdminPolicy() {
  when(users.layTaiKhoanHoatDong("oldAdmin")).thenReturn(new User(1,"oldAdmin","","NHAN_VIEN","HOAT_DONG"));
  assertEquals(403,assertThrows(ResponseStatusException.class,()->service.savePricing(2,prices,"oldAdmin")).getStatusCode().value());
  assertEquals(403,assertThrows(ResponseStatusException.class,()->service.policy("oldAdmin")).getStatusCode().value());
  verifyNoInteractions(repo);
 }
 @Test void rejectsInvalidPricesAndLateBounds() {
  try(var factory=Validation.buildDefaultValidatorFactory()) {
   var v=factory.getValidator();
   for(String amount:new String[]{"1.5","-1","10000000000"})
    assertFalse(v.validate(new TheaterPolicy.RoomPricing(new BigDecimal(amount),new BigDecimal("9999999999"),BigDecimal.ZERO,BigDecimal.ZERO)).isEmpty());
   assertFalse(v.validate(new TheaterPolicy.RoomPricing(BigDecimal.TEN,BigDecimal.ONE,BigDecimal.ZERO,BigDecimal.ZERO)).isEmpty());
   assertFalse(v.validate(new TheaterPolicy.Settings(-1)).isEmpty());
   assertFalse(v.validate(new TheaterPolicy.Settings(601)).isEmpty());
   assertTrue(v.validate(new TheaterPolicy.Settings(null)).isEmpty());
  }
 }
}
