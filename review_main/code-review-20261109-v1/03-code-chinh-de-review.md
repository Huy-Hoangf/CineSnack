# Code chính để review

Các khối dưới đây là trích đoạn từ code đã kiểm thử, không phải file độc lập để chạy. Xem file patch cho toàn bộ thay đổi hoặc thư mục files cho nguyên văn từng file. Source gốc chưa được áp dụng bản sửa.

## Đạo diễn và trailer

Nguồn: [src/main/java/com/cinesnack/catalog/dto/AdminCatalog.java](files/src/main/java/com/cinesnack/catalog/dto/AdminCatalog.java) dòng 25.

```java
        String status,
        @Size(max = 200) String director,
        @Size(max = 500) String trailerUrl
    ) {
        public MovieRequest(String name, String genre, String description, String actors,
            int duration, String ageRating, String posterUrl, String status) {
            this(name, genre, description, actors, duration, ageRating, posterUrl, status, null, null);
        }

        @AssertTrue(message = "Trailer phải là URL HTTPS có máy chủ và không chứa tài khoản.")
        @JsonIgnore
        public boolean isTrailerUrlValid() {
            if (trailerUrl == null || trailerUrl.isEmpty()) return true;
            try {
                URI uri = URI.create(trailerUrl);
                return "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null
                    && uri.getRawUserInfo() == null;
            } catch (IllegalArgumentException ex) {
                return false;
            }
        }
    }

    public record ShowRequest(
```

## Chọn giá theo ngày Việt Nam

Nguồn: [src/main/java/com/cinesnack/catalog/service/TheaterPolicyService.java](files/src/main/java/com/cinesnack/catalog/service/TheaterPolicyService.java) dòng 39.

```java
    public TheaterPolicy.ShowPrices pricesForShow(long roomId, OffsetDateTime startsAt) {
        var p = requirePricing(roomId);
        var day = startsAt.atZoneSameInstant(ZoneId.of("Asia/Ho_Chi_Minh")).getDayOfWeek();
        boolean weekend = day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
        return new TheaterPolicy.ShowPrices(weekend ? p.weekendRegular() : p.weekdayRegular(), weekend ? p.weekendVip() : p.weekdayVip());
    }
    private TheaterPolicy.RoomPricing requirePricing(long roomId) {
        var p=repo.pricing(roomId);
```

## Chụp bảng giá vào suất

Nguồn: [src/main/java/com/cinesnack/catalog/service/AdminCatalogService.java](files/src/main/java/com/cinesnack/catalog/service/AdminCatalogService.java) dòng 138.

```java
        if ("POLICY".equals(request.pricingMode())) {
            var prices = policy.pricesForShow(request.roomId(), request.startsAt());
            request = new AdminCatalog.ShowRequest(request.movieId(),request.roomId(),request.startsAt(),
                prices.regular(),prices.vip().subtract(prices.regular()),request.status(),"POLICY");
        }
        if (id == null) id = repo.insertShow(request, start, end);
        else repo.updateShow(id, request, start, end);
        return repo.show(id);
    }

```

## Soát vé trong một UPDATE có giới hạn muộn

Nguồn: [src/main/java/com/cinesnack/booking/repository/TicketCheckInRepository.java](files/src/main/java/com/cinesnack/booking/repository/TicketCheckInRepository.java) dòng 21.

```java
    public int checkIn(String code, long staff, LocalDateTime now, Integer lateCheckInMinutes) {
        return db.update(
            """
            UPDATE VE SET trang_thai='DA_SOAT',soat_ve_luc=?,nhan_vien_soat_id=?
            WHERE UPPER(ma_ve)=UPPER(?) AND trang_thai='DA_THANH_TOAN'
            AND EXISTS (SELECT 1 FROM HOA_DON_VE h JOIN DON_HANG d ON d.don_hang_id=h.don_hang_id
                WHERE h.hoa_don_ve_id=VE.hoa_don_ve_id AND d.don_hang_id=VE.don_hang_id
                AND h.trang_thai='DA_THANH_TOAN' AND d.trang_thai='DA_THANH_TOAN')
            AND EXISTS (SELECT 1 FROM SUAT_CHIEU s WHERE s.suat_chieu_id=VE.suat_chieu_id
                AND s.trang_thai IN ('SAP_CHIEU','DANG_CHIEU') AND s.bat_dau<=? AND s.ket_thuc>?
                AND (?=1 OR s.bat_dau>=?))
            """,
            now,
            staff,
            code,
            now.plusMinutes(30),
            now,
            lateCheckInMinutes == null ? 1 : 0,
            now.minusMinutes(lateCheckInMinutes == null ? 0 : lateCheckInMinutes)
        );
    }

```

## Timeline theo khoảng giao ngày

Nguồn: [src/main/java/com/cinesnack/catalog/repository/AdminCatalogRepository.java](files/src/main/java/com/cinesnack/catalog/repository/AdminCatalogRepository.java) dòng 125.

```java
    public List<Catalog.Show> timeline(LocalDateTime start, LocalDateTime end, Long roomId, String status) {
        return jdbc.query(
            SHOW_SELECT + "WHERE s.bat_dau<? AND s.ket_thuc>? AND (? IS NULL OR s.phong_id=?) " +
                "AND (?='' OR s.trang_thai=?) ORDER BY s.phong_id,s.bat_dau,s.suat_chieu_id",
            (r, i) -> show(r), end, start, roomId, roomId, status, status
        );
    }

    public Catalog.Show show(long id) {
```

## Đếm ngược không cộng thêm thời gian khi mạng chậm

Nguồn: [src/main/resources/UI/assets/js/payment-deadline.js](files/src/main/resources/UI/assets/js/payment-deadline.js) dòng 3.

```javascript
  function start({ expiresAt, serverTime, requestedAt, onTick = () => {}, onExpire = () => {},
    now = () => Date.now(), schedule = (fn) => setInterval(fn, 1000),
    cancel = (id) => clearInterval(id) }) {
    const deadline = Date.parse(expiresAt);
    const server = serverTime == null ? null : Date.parse(serverTime);
    if (!Number.isFinite(deadline) || (server !== null && !Number.isFinite(server)))
      throw new Error("Không xác định được thời hạn giữ chỗ.");
    const receivedAt = now();
    // Charge the full round trip conservatively; a delayed response must not
    // grant extra hold time when synchronizing with the server clock.
    const latency = requestedAt == null ? 0 : Math.max(0, receivedAt - requestedAt);
    const offset = server === null ? 0 : server + latency - receivedAt;
    let timer, stopped = false;
    function stop() {
      stopped = true;
      if (timer !== undefined) cancel(timer);
    }
    function refresh() {
      if (stopped) return false;
      const seconds = Math.max(0, Math.ceil((deadline - now() - offset) / 1000));
      onTick(`${String(Math.floor(seconds / 60)).padStart(2, "0")}:${String(seconds % 60).padStart(2, "0")}`);
      if (!seconds) {
        stop();
        onExpire();
        return false;
      }
      return true;
    }
    if (refresh()) timer = schedule(refresh);
    return { refresh, isActive: refresh, stop };
  }
  const api = { start };
  if (typeof module !== "undefined" && module.exports) module.exports = api;
```

## Chặn PNG hoàn thành sau hết hạn

Nguồn: [src/main/resources/UI/assets/js/checkout.js](files/src/main/resources/UI/assets/js/checkout.js) dòng 162.

```javascript
          const png = await CineSnack.qrPngUrl(image);
          if (!deadline.isActive() || currentQr !== qr) {
            URL.revokeObjectURL(png);
            return;
          }
          currentQrPngUrl = png;
          downloadQr.href = currentQrPngUrl;
          downloadQr.download = `CineSnack-${ma}.png`;
          downloadQr.hidden = false;
        } catch {
```

## Bỏ QR ngay khi hủy thành công

Nguồn: [src/main/resources/UI/assets/js/orders.js](files/src/main/resources/UI/assets/js/orders.js) dòng 264.

```javascript
        donHang.status = "HUY";
        paymentQr?.dispose();
        paymentQr?.node.remove();
        root.querySelector("#trang-thai-don").textContent = nhan.HUY;
        root.querySelectorAll(".cash-form").forEach((form) => form.hidden = true);
        await hienChiTiet(box, code, user, thayDoi, hopLe, guestMode);
        await thayDoi();
      }),
```

## Tạo QR vé cục bộ

Nguồn: [src/main/resources/UI/assets/js/ticket-code.js](files/src/main/resources/UI/assets/js/ticket-code.js) dòng 3.

```javascript
  function create(code) {
    if (typeof code !== "string" || !/^[A-Za-z0-9]{1,30}$/.test(code))
      throw new Error("Mã vé không hợp lệ.");
    if (!window.ZXingBrowser?.BrowserQRCodeSvgWriter)
      throw new Error("Không tải được bộ tạo QR vé. Bạn vẫn có thể dùng mã văn bản.");
    const svg = new ZXingBrowser.BrowserQRCodeSvgWriter().write(code, 160, 160);
    svg.classList.add("ticket-qr");
    svg.setAttribute("role", "img");
    svg.setAttribute("aria-label", "QR vé " + code);
    svg.style.background = "#fff";
    return svg;
  }
  return { create };
```

