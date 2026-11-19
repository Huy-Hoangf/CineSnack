"use strict";
(async () => {
  const layPhanTu = (id) => document.getElementById(id);
  const field = (form, name) => form.elements.namedItem(name);
  const node = (tag, text, cls) => {
    const n = document.createElement(tag);
    if (text !== undefined) n.textContent = text;
    if (cls) n.className = cls;
    return n;
  };
  let user,
    roomPage = 0,
    selectedRoom,
    editedRoom,
    editedSeat,
    seatGeneration = 0,
    roomGeneration = 0;
  let cameraControls = null,
    cameraSession = 0;
  const api = (path, options) =>
    CineSnackApi.goi("/api/admin/rooms" + path, options);
  function error(err) {
    layPhanTu("theater-error").textContent = err.message;
    layPhanTu("theater-error").focus();
  }
  function stopCamera(message = "") {
    cameraSession++;
    cameraControls?.stop();
    cameraControls = null;
    layPhanTu("ticket-camera").hidden = true;
    layPhanTu("ticket-camera-status").textContent = message;
  }
  async function startCamera(inputId) {
    stopCamera();
    if (!navigator.mediaDevices?.getUserMedia || !window.ZXingBrowser) {
      layPhanTu("ticket-camera-status").textContent =
        "Trình duyệt không mở được camera. Bạn vẫn có thể nhập mã vé.";
      return;
    }
    const session = cameraSession;
    const video = layPhanTu("ticket-camera-video");
    layPhanTu("ticket-camera").hidden = false;
    layPhanTu("ticket-camera-status").textContent =
      "Đưa mã QR hoặc mã vạch trên vé vào khung hình.";
    try {
      const reader = new ZXingBrowser.BrowserMultiFormatReader();
      reader.possibleFormats = [ZXingBrowser.BarcodeFormat.QR_CODE, ZXingBrowser.BarcodeFormat.CODE_128];
      const controls = await reader.decodeFromVideoDevice(
        undefined,
        video,
        (result, _error, controls) => {
          if (!result || session !== cameraSession) return;
          const code = result.getText().trim();
          if (!/^[A-Za-z0-9]{1,30}$/.test(code)) return;
          controls.stop();
          stopCamera("Đã đọc mã " + code + ". Kiểm tra rồi bấm nút xác nhận.");
          const input = layPhanTu(inputId);
          input.value = code;
          input.focus();
        },
      );
      if (session === cameraSession) cameraControls = controls;
      else controls.stop();
    } catch {
      if (session === cameraSession) {
        stopCamera(
          "Không thể mở camera. Kiểm tra quyền truy cập hoặc nhập mã vé.",
        );
      }
    }
  }
  async function busy(button, action, errorBox = null) {
    if (button.disabled) return;
    button.disabled = true;
    button.setAttribute("aria-busy", "true");
    layPhanTu("theater-error").textContent = "";
    if (errorBox) errorBox.textContent = "";
    try {
      await action();
    } catch (err) {
      if (errorBox) errorBox.textContent = err.message;
      else error(err);
    } finally {
      button.disabled = false;
      button.removeAttribute("aria-busy");
    }
  }
  function action(label, handler, className = "account-button") {
    const b = node("button", label, className);
    b.type = "button";
    b.addEventListener("click", () => busy(b, handler));
    return b;
  }
  function editRoom(room) {
    editedRoom = room;
    const f = layPhanTu("room-form");
    f.reset();
    if (room)
      for (const name of ["name", "type", "status"])
        field(f, name).value = room[name];
    layPhanTu("room-title").textContent = room
      ? "Sửa " + room.name
      : "Thêm phòng";
    layPhanTu("room-error").textContent = "";
    layPhanTu("room-dialog").showModal();
  }
  async function loadRooms() {
    const generation = ++roomGeneration;
    const rooms = await api(`/management?page=${roomPage}&size=20`);
    if (generation !== roomGeneration) return;
    layPhanTu("rooms-list").replaceChildren();
    for (const room of rooms) {
      const row = node("tr");
      for (const [label, value] of [
        ["Phòng", room.name],
        ["Loại", room.type],
        ["Số ghế", room.seatCount],
        [
          "Trạng thái",
          room.status === "HOAT_DONG" ? "Hoạt động" : "Ngừng hoạt động",
        ],
      ]) {
        const cell = node("td", value);
        cell.dataset.label = label;
        row.append(cell);
      }
      const actions = node("td");
      actions.append(
        action("Sơ đồ ghế", () => openSeats(room)),
        action("Bảng giá", () => {
          seatGeneration++;
          selectedRoom = null;
          layPhanTu("seats-panel").hidden = true;
          return CineSnackTheaterSettings.selectRoom(room);
        }),
        action("Sửa phòng", () => editRoom(room)),
        action("Xóa phòng", async () => {
          if (!confirm(`Xóa phòng “${room.name}”?`)) return;
          await api(`/${room.id}`, { method: "DELETE" });
          if (selectedRoom?.id === room.id) {
            selectedRoom = null;
            layPhanTu("seats-panel").hidden = true;
          }
          CineSnackTheaterSettings.clearRoom();
          await loadRooms();
          layPhanTu("theater-status").textContent = "Đã xóa phòng chiếu.";
        }, "account-button ops-danger"),
      );
      row.append(actions);
      layPhanTu("rooms-list").append(row);
    }
    layPhanTu("rooms-prev").disabled = roomPage === 0;
    layPhanTu("rooms-next").disabled = rooms.length < 20;
    layPhanTu("rooms-page").textContent = "Trang " + (roomPage + 1);
    layPhanTu("rooms-empty").hidden = rooms.length > 0;
  }
  async function loadArchived() {
    const rooms = await api("/archived?size=100");
    const list = layPhanTu("archived-rooms");
    list.replaceChildren();
    if (!rooms.length) list.append(node("p", "Chưa có phòng lưu trữ."));
    for (const room of rooms) {
      const row = node(
        "p",
        `${room.name} · ${room.type} · ${room.seatCount} ghế · Đã lưu trữ`,
      );
      row.append(
        " ",
        action("Khôi phục", async () => {
          await api(`/${room.id}/restore`, { method: "POST" });
          await Promise.all([loadRooms(), loadArchived()]);
          layPhanTu("theater-status").textContent =
            "Đã khôi phục phòng về trạng thái ngừng hoạt động.";
        }),
      );
      list.append(row);
    }
  }
  function editSeat(seat) {
    editedSeat = { ...seat, roomId: selectedRoom.id };
    const f = layPhanTu("seat-form");
    for (const name of ["code", "type", "status"])
      field(f, name).value = seat[name];
    layPhanTu("seat-title").textContent =
      selectedRoom.name + " · Ghế " + seat.code;
    layPhanTu("seat-error").textContent = "";
    layPhanTu("seat-dialog").showModal();
  }
  async function openSeats(room) {
    CineSnackTheaterSettings.clearRoom();
    const generation = ++seatGeneration;
    layPhanTu("seats-panel").hidden = true;
    const seats = await api(`/${room.id}/seats`);
    if (generation !== seatGeneration) return;
    selectedRoom = room;
    layPhanTu("seats-panel").hidden = false;
    layPhanTu("seats-title").textContent = "Sơ đồ ghế · " + room.name;
    layPhanTu("seat-setup").hidden =
      seats.length > 0 || room.status !== "HOAT_DONG";
    layPhanTu("seat-summary").textContent =
      seats.length +
      " ghế · " +
      seats.filter((s) => s.status === "HOAT_DONG").length +
      " ghế hoạt động";
    const map = layPhanTu("seat-map");
    map.replaceChildren();
    const parsed = seats.map((seat) => ({
      seat,
      match: /^([A-Z])(\d+)$/.exec(seat.code),
    }));
    const columns = Math.max(
      1,
      ...parsed.map((x) => (x.match ? Number(x.match[2]) : 1)),
    );
    map.style.gridTemplateColumns = `repeat(${Math.min(columns, 100)}, 44px)`;
    for (const { seat, match } of parsed) {
      const b = action(seat.code, () => editSeat(seat));
      b.className =
        "theater-seat" +
        (seat.type === "VIP" ? " vip" : "") +
        (seat.status !== "HOAT_DONG" ? " locked" : "");
      b.setAttribute(
        "aria-label",
        `Ghế ${seat.code}, ${seat.type === "VIP" ? "VIP" : "thường"}, ${seat.status === "HOAT_DONG" ? "hoạt động" : "đang khóa"}`,
      );
      if (match) {
        b.style.gridRow = String(match[1].charCodeAt(0) - 64);
        b.style.gridColumn = String(Number(match[2]));
      }
      map.append(b);
    }
    layPhanTu("seats-title").focus();
  }
  async function showPanel(name) {
    stopCamera();
    if (name === "rooms" && user?.role !== "QUAN_TRI") name = "checkin";
    layPhanTu("rooms-panel").hidden = name !== "rooms";
    layPhanTu("checkin-panel").hidden = name !== "checkin";
    layPhanTu("tab-rooms").setAttribute(
      "aria-pressed",
      String(name === "rooms"),
    );
    layPhanTu("tab-checkin").setAttribute(
      "aria-pressed",
      String(name === "checkin"),
    );
    if (name === "rooms") await loadRooms();
    else layPhanTu(layPhanTu("issue-panel").hidden ? "ticket-code" : "issue-code").focus();
  }
  function chooseTicketMode(issue) {
    stopCamera();
    layPhanTu("issue-panel").hidden = !issue;
    layPhanTu("scan-panel").hidden = issue;
    layPhanTu("choose-issue").setAttribute("aria-pressed", String(issue));
    layPhanTu("choose-scan").setAttribute("aria-pressed", String(!issue));
    layPhanTu(issue ? "issue-code" : "ticket-code").focus();
  }
  layPhanTu("choose-issue").addEventListener("click", () => chooseTicketMode(true));
  layPhanTu("choose-scan").addEventListener("click", () => chooseTicketMode(false));
  layPhanTu("tab-rooms").addEventListener("click", () =>
    showPanel("rooms").catch(error),
  );
  layPhanTu("tab-checkin").addEventListener("click", () =>
    showPanel("checkin").catch(error),
  );
  layPhanTu("issue-camera-start").addEventListener("click", () =>
    startCamera("issue-code"),
  );
  layPhanTu("checkin-camera-start").addEventListener("click", () =>
    startCamera("ticket-code"),
  );
  layPhanTu("ticket-camera-stop").addEventListener("click", () =>
    stopCamera("Camera đã tắt."),
  );
  document.addEventListener("visibilitychange", () => {
    if (document.hidden) stopCamera();
  });
  window.addEventListener("beforeunload", () => stopCamera());
  layPhanTu("room-add").addEventListener("click", () => editRoom(null));
  layPhanTu("archived-refresh").addEventListener("click", () =>
    loadArchived().catch(error),
  );
  document
    .querySelectorAll("[data-close]")
    .forEach((b) =>
      b.addEventListener("click", () => layPhanTu(b.dataset.close).close()),
    );
  for (const [id, delta] of [
    ["rooms-prev", -1],
    ["rooms-next", 1],
  ])
    layPhanTu(id).addEventListener("click", () => {
      if (layPhanTu(id).disabled) return;
      roomPage = Math.max(0, roomPage + delta);
      layPhanTu("rooms-prev").disabled = true;
      layPhanTu("rooms-next").disabled = true;
      loadRooms().catch(error);
    });
  layPhanTu("room-form").addEventListener("submit", (event) => {
    event.preventDefault();
    const f = event.currentTarget;
    busy(
      f.querySelector('[type="submit"]'),
      async () => {
        const body = Object.fromEntries(
          ["name", "type", "status"].map((k) => [k, field(f, k).value.trim()]),
        );
        const result = await api(editedRoom ? "/" + editedRoom.id : "", {
          method: editedRoom ? "PUT" : "POST",
          body,
        });
        layPhanTu("room-dialog").close();
        roomPage = 0;
        await loadRooms();
        if (selectedRoom?.id === result.id)
          await openSeats({ ...selectedRoom, ...body });
        layPhanTu("theater-status").textContent = "Đã lưu phòng chiếu.";
      },
      layPhanTu("room-error"),
    );
  });
  layPhanTu("seat-setup").addEventListener("submit", (event) => {
    event.preventDefault();
    const f = event.currentTarget;
    const room = selectedRoom;
    busy(f.querySelector('[type="submit"]'), async () => {
      await api(`/${room.id}/seats/setup`, {
        method: "POST",
        body: {
          rows: Number(field(f, "rows").value),
          columns: Number(field(f, "columns").value),
          type: field(f, "type").value,
        },
      });
      await loadRooms();
      if (selectedRoom?.id === room.id) await openSeats(room);
      layPhanTu("theater-status").textContent = "Đã tạo sơ đồ ghế.";
    });
  });
  layPhanTu("seat-form").addEventListener("submit", (event) => {
    event.preventDefault();
    const f = event.currentTarget;
    const seat = editedSeat;
    busy(
      f.querySelector('[type="submit"]'),
      async () => {
        const body = Object.fromEntries(
          ["code", "type", "status"].map((k) => [k, field(f, k).value.trim()]),
        );
        await api(`/${seat.roomId}/seats/${seat.id}`, { method: "PUT", body });
        layPhanTu("seat-dialog").close();
        if (selectedRoom?.id === seat.roomId) await openSeats(selectedRoom);
        layPhanTu("theater-status").textContent = "Đã lưu ghế.";
      },
      layPhanTu("seat-error"),
    );
  });
  layPhanTu("checkin-form").addEventListener("submit", (event) => {
    event.preventDefault();
    const f = event.currentTarget;
    busy(f.querySelector('[type="submit"]'), async () => {
      layPhanTu("checkin-result").replaceChildren();
      const result = await CineSnackApi.goi("/api/staff/tickets/scan", {
        method: "POST",
        body: { code: field(f, "code").value.trim() },
      });
      const t = result.ticket;
      layPhanTu("checkin-result").append(
        node("strong", "Đã soát vé · Ghế " + t.seatCode),
        node("h3", t.movieName),
        node("p", t.roomName + " · " + CineSnack.dinhDangSuat(t.startsAt)),
        node("p", "Mã vé: " + t.code),
        node("p", "Soát lúc: " + CineSnack.dinhDangSuat(result.checkedAt)),
      );
      field(f, "code").value = "";
      field(f, "code").focus();
    }, layPhanTu("checkin-error"));
  });
  layPhanTu("issue-form").addEventListener("submit", (event) => {
    event.preventDefault();
    const f = event.currentTarget;
    busy(f.querySelector('[type="submit"]'), async () => {
      layPhanTu("issue-result").replaceChildren();
      const result = await CineSnackApi.goi("/api/staff/tickets/issue", {
        method: "POST",
        body: { code: field(f, "code").value.trim() },
      });
      layPhanTu("issue-result").append(
        node("strong", "Đã cấp lượt in cho vé " + result.ticket.code),
      );
      field(f, "code").value = "";
      field(f, "code").focus();
      await CineSnackPrint.printIssued(result.ticket);
    }, layPhanTu("issue-error"));
  });
  try {
    user = await CineSnackApi.taiKhoan();
    if (!user) {
      layPhanTu("theater-login").hidden = false;
      return;
    }
    if (!["NHAN_VIEN", "QUAN_TRI"].includes(user.role))
      throw new Error("Tài khoản không có quyền vận hành rạp.");
    layPhanTu("theater-workspace").hidden = false;
    layPhanTu("tab-rooms").hidden = user.role !== "QUAN_TRI";
    layPhanTu("issue-panel").hidden = user.role !== "NHAN_VIEN";
    layPhanTu("ticket-modes").hidden = user.role !== "NHAN_VIEN";
    layPhanTu("scan-panel").hidden = user.role === "NHAN_VIEN";
    if (user.role === "QUAN_TRI") await CineSnackTheaterSettings.init();
    await showPanel(location.hash === "#check-in" ? "checkin" : "rooms");
  } catch (err) {
    error(err);
  }
})();
