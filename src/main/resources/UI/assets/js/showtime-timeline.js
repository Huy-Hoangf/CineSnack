"use strict";
(function (root) {
  const DAY = 86400000;
  const labels = { SAP_CHIEU: "Sắp chiếu", DANG_CHIEU: "Đang chiếu", DA_CHIEU: "Đã chiếu", HUY: "Đã hủy" };
  const time = (value) => new Intl.DateTimeFormat("vi-VN", {
    timeZone: "Asia/Ho_Chi_Minh", hour: "2-digit", minute: "2-digit", day: "2-digit", month: "2-digit",
  }).format(new Date(value));
  const clock = new Intl.DateTimeFormat("vi-VN", {
    timeZone: "Asia/Ho_Chi_Minh", hour: "2-digit", minute: "2-digit",
  });
  function dayStart(date) { return Date.parse(date + "T00:00:00+07:00"); }
  function shiftDate(date, delta) {
    return new Date(Date.parse(date + "T00:00:00Z") + delta * DAY).toISOString().slice(0, 10);
  }
  function layout(shows, date) {
    const start = dayStart(date), end = start + DAY;
    const items = shows.filter((show) => Date.parse(show.startsAt) < end && Date.parse(show.endsAt) > start)
      .map((show) => ({ ...show,
        startMinute: (Math.max(start, Date.parse(show.startsAt)) - start) / 60000,
        endMinute: (Math.min(end, Date.parse(show.endsAt)) - start) / 60000,
      })).sort((a, b) => a.roomId - b.roomId || Date.parse(a.startsAt) - Date.parse(b.startsAt) || a.id - b.id);
    let roomId, lanes = [];
    for (const item of items) {
      if (roomId !== item.roomId) { roomId = item.roomId; lanes = []; }
      let lane = lanes.findIndex((until) => until <= item.startMinute);
      if (lane < 0) lane = lanes.length;
      lanes[lane] = item.endMinute;
      item.lane = lane;
    }
    return items;
  }
  function element(tag, text, className) {
    const node = document.createElement(tag);
    if (text != null) node.textContent = text;
    if (className) node.className = className;
    return node;
  }
  function action(text, callback) {
    const node = element("button", text, "account-button");
    node.type = "button";
    node.addEventListener("click", callback);
    return node;
  }
  function render(container, shows, date, rooms, onEdit) {
    const items = layout(shows, date);
    container.replaceChildren();
    if (!items.length) {
      container.append(element("p", "Không có suất chiếu phù hợp trong ngày này."));
      return;
    }
    const scroll = element("div", null, "showtime-scroll");
    scroll.tabIndex = 0;
    scroll.setAttribute("role", "region");
    scroll.setAttribute("aria-label", "Timeline lịch chiếu ngày " + date + ". Cuộn ngang để xem đủ 24 giờ.");
    const chart = element("div", null, "showtime-chart");
    const axis = element("div", null, "showtime-axis");
    axis.append(element("strong", "Phòng / giờ", "showtime-room"));
    for (let hour = 0; hour <= 24; hour++) {
      const tick = element("span", String(hour).padStart(2, "0") + ":00", "showtime-tick");
      tick.style.left = (160 + hour * 60) + "px";
      axis.append(tick);
    }
    chart.append(axis);
    const roomMap = new Map(rooms.map((room) => [Number(room.id), room.name]));
    for (const show of items) roomMap.set(show.roomId, show.roomName);
    for (const [roomId, name] of [...roomMap].sort((a, b) => a[0] - b[0])) {
      const roomShows = items.filter((show) => show.roomId === roomId);
      const row = element("div", null, "showtime-row");
      row.style.height = Math.max(1, ...roomShows.map((show) => show.lane + 1)) * 88 + "px";
      row.append(element("strong", name, "showtime-room"));
      for (const show of roomShows) {
        const description = show.movieName + " · " + time(show.startsAt) + " → " + time(show.endsAt) + " · " + (labels[show.status] || show.status);
        const block = action("", () => onEdit(show));
        block.className = "showtime-block";
        block.dataset.status = show.status;
        block.style.left = (160 + show.startMinute) + "px";
        block.style.width = Math.max(1, show.endMinute - show.startMinute - 2) + "px";
        block.style.top = (show.lane * 88 + 4) + "px";
        block.title = description;
        block.setAttribute("aria-label", "Sửa " + description);
        block.append(element("strong", show.movieName), element("small", time(show.startsAt) + " → " + time(show.endsAt)), element("small", labels[show.status] || show.status));
        row.append(block);
      }
      chart.append(row);
    }
    scroll.append(chart);
    container.append(scroll);
    const agenda = element("ol", null, "showtime-agenda");
    agenda.setAttribute("aria-label", "Suất chiếu theo giờ bắt đầu");
    const byStart = [...items].sort((a, b) => Date.parse(a.startsAt) - Date.parse(b.startsAt) || a.id - b.id);
    for (const show of byStart) {
      const row = element("li");
      const entry = action("", () => onEdit(show));
      entry.className = "agenda-entry";
      const start = new Date(show.startsAt), end = new Date(show.endsAt);
      const hours = clock.format(start) + " – " + clock.format(end);
      const span = element("span", hours, "agenda-hours");
      if (start.getTime() < dayStart(date)) span.append(element("small", "Từ hôm trước"));
      if (end.getTime() >= dayStart(date) + DAY) span.append(element("small", "Đến hôm sau"));
      const info = element("span", null, "agenda-info");
      info.append(element("strong", show.movieName), element("small", show.roomName + " · " + (labels[show.status] || show.status)));
      entry.append(span, info);
      entry.setAttribute("aria-label", "Sửa " + show.movieName + ", " + time(show.startsAt) + " → " + time(show.endsAt) + ", " + info.lastChild.textContent);
      row.append(entry);
      agenda.append(row);
    }
    container.append(agenda);
    const details = element("details", null, "showtime-accessible");
    details.append(element("summary", "Danh sách đầy đủ trong ngày (" + items.length + " suất)"));
    const tableScroll = element("div", null, "bang-cuon");
    tableScroll.tabIndex = 0;
    tableScroll.setAttribute("role", "region");
    tableScroll.setAttribute("aria-label", "Danh sách suất chiếu trong ngày");
    const table = element("table", null, "workspace-table");
    table.append(element("caption", "Lịch chiếu ngày " + date + " · giờ Việt Nam"));
    const header = element("tr");
    for (const text of ["Phòng", "Phim", "Bắt đầu → kết thúc", "Trạng thái", "Thao tác"]) {
      const th = element("th", text); th.scope = "col"; header.append(th);
    }
    const head = element("thead"); head.append(header); table.append(head);
    const body = element("tbody");
    for (const show of items) {
      const tr = element("tr");
      for (const text of [show.roomName, show.movieName, time(show.startsAt) + " → " + time(show.endsAt), labels[show.status] || show.status]) tr.append(element("td", text));
      const td = element("td"); td.append(action("Sửa " + show.movieName, () => onEdit(show))); tr.append(td); body.append(tr);
    }
    table.append(body); tableScroll.append(table); details.append(tableScroll); container.append(details);
  }
  function init({ container, listElement, countElement, request, onEdit, onError }) {
    let active = false, generation = 0, rooms = [], roomsLoaded = false;
    const toolbar = element("div", null, "workspace-toolbar showtime-controls");
    const controls = element("form", null, "workspace-form showtime-controls");
    controls.hidden = true;
    const view = element("div"); view.hidden = true;
    const status = element("p", "", "workspace-hint"); status.setAttribute("role", "status");
    const date = element("input"); date.type = "date"; date.required = true;
    date.value = new Intl.DateTimeFormat("en-CA", { timeZone: "Asia/Ho_Chi_Minh" }).format(new Date());
    function labeled(text, input) {
      const label = element("label", text);
      input.setAttribute("aria-label", text);
      label.append(input);
      return label;
    }
    const room = element("select"), filter = element("select");
    function option(select, value, text) { const node = element("option", text); node.value = value; select.append(node); }
    option(room, "", "Tất cả phòng"); option(filter, "", "Tất cả trạng thái");
    for (const [value, text] of Object.entries(labels)) option(filter, value, text);
    const listButton = action("Danh sách", () => switchView(false));
    const timelineButton = action("Lịch theo ngày", () => switchView(true));
    toolbar.setAttribute("role", "group"); toolbar.setAttribute("aria-label", "Chế độ xem lịch chiếu");
    toolbar.append(listButton, timelineButton);
    controls.append(action("← Ngày trước", () => move(-1)), labeled("Ngày (giờ Việt Nam)", date), action("Ngày sau →", () => move(1)), labeled("Phòng", room), labeled("Trạng thái", filter));
    const refreshButton = element("button", "Cập nhật", "account-button"); refreshButton.type = "submit"; controls.append(refreshButton);
    container.append(toolbar, controls, status, view);
    function edit(show) { Promise.resolve().then(() => onEdit(show)).catch(onError); }
    async function refresh() {
      if (!active || !controls.reportValidity()) return;
      const current = ++generation, selectedDate = date.value;
      status.textContent = "Đang tải lịch chiếu…"; view.replaceChildren(); view.setAttribute("aria-busy", "true");
      try {
        const params = new URLSearchParams({ date: selectedDate, status: filter.value });
        if (room.value) params.set("roomId", room.value);
        const [shows, loadedRooms] = await Promise.all([
          request("/api/admin/showtimes/timeline?" + params),
          roomsLoaded ? Promise.resolve(rooms) : request("/api/admin/rooms"),
        ]);
        if (current !== generation || !active) return;
        rooms = loadedRooms; roomsLoaded = true;
        const selectedRoom = room.value, known = new Map(rooms.map((r) => [Number(r.id), r.name]));
        for (const show of shows) known.set(show.roomId, show.roomName);
        // Keep archived rooms selectable after changing day or status.
        for (const previous of [...room.options].slice(1)) if (!known.has(Number(previous.value))) known.set(Number(previous.value), previous.textContent);
        room.replaceChildren(); option(room, "", "Tất cả phòng");
        for (const [id, name] of [...known].sort((a, b) => a[0] - b[0])) option(room, id, name);
        room.value = selectedRoom;
        const visibleRooms = rooms.filter((r) => !selectedRoom || String(r.id) === selectedRoom);
        render(view, shows, selectedDate, visibleRooms, edit);
        status.textContent = selectedDate + " · " + shows.length + " suất · giờ Việt Nam";
      } catch (error) {
        if (current !== generation || !active) return;
        status.textContent = "Không tải được lịch chiếu. " + error.message;
      } finally { if (current === generation) view.removeAttribute("aria-busy"); }
    }
    function switchView(value, reload = true) {
      active = value; generation++;
      controls.hidden = view.hidden = !value;
      listElement.hidden = countElement.hidden = value;
      listButton.setAttribute("aria-pressed", String(!value)); timelineButton.setAttribute("aria-pressed", String(value));
      status.textContent = "";
      if (value && reload) refresh();
    }
    function move(delta) { if (date.value) { date.value = shiftDate(date.value, delta); refresh(); } }
    controls.addEventListener("submit", (event) => { event.preventDefault(); refresh(); });
    for (const input of [date, room, filter]) input.addEventListener("change", refresh);
    switchView(true, false);
    return { refresh };
  }
  const api = { layout, shiftDate, render, init };
  if (typeof module !== "undefined" && module.exports) module.exports = api;
  else root.CineSnackTimeline = api;
})(typeof window === "undefined" ? globalThis : window);
