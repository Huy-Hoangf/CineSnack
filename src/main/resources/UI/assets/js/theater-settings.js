"use strict";
window.CineSnackTheaterSettings = (() => {
  let selectedRoom = null, generation = 0;
  const byId = id => document.getElementById(id);
  const field = (form, name) => form.elements.namedItem(name);
  const priceNames = ["weekdayRegular", "weekdayVip", "weekendRegular", "weekendVip"];
  async function save(form, status, work, isCurrent = () => true) {
    const button = form.querySelector('[type="submit"]');
    if (button.disabled || !form.reportValidity()) return;
    status.textContent = "Đang lưu…";
    button.disabled = true;
    button.setAttribute("aria-busy", "true");
    try { await work(); if (isCurrent()) status.textContent = "Đã lưu."; }
    catch (err) { if (isCurrent()) status.textContent = err.message; }
    finally { if (isCurrent()) { button.disabled = false; button.removeAttribute("aria-busy"); } }
  }
  async function selectRoom(room) {
    const current = ++generation;
    selectedRoom = room;
    const form = byId("room-pricing-form"), status = byId("room-pricing-status");
    form.reset();
    byId("room-pricing-panel").hidden = false;
    byId("room-pricing-title").textContent = "Bảng giá · " + room.name;
    status.textContent = "Đang tải bảng giá…";
    form.querySelector('[type="submit"]').disabled = true;
    form.querySelector('[type="submit"]').removeAttribute("aria-busy");
    try {
      const data = await CineSnackApi.goi(`/api/admin/rooms/${room.id}/pricing`);
      if (current !== generation) return;
      for (const name of priceNames) field(form, name).value = data[name];
      status.textContent = "";
    } catch (err) {
      if (current !== generation) return;
      status.textContent = err.status === 409 ? "Phòng chưa có bảng giá. Nhập đủ bốn giá để dùng cho suất mới." : err.message;
    } finally {
      if (current === generation) form.querySelector('[type="submit"]').disabled = false;
    }
    if (current === generation) byId("room-pricing-title").focus();
  }
  function clearRoom() {
    generation++;
    selectedRoom = null;
    byId("room-pricing-panel").hidden = true;
  }
  async function init() {
    const prices = byId("room-pricing-form"), policy = byId("theater-policy-form");
    prices.addEventListener("submit", event => {
      event.preventDefault();
      const room = selectedRoom;
      const current = generation;
      if (!room) return;
      save(prices, byId("room-pricing-status"), async () => {
        const body = Object.fromEntries(priceNames.map(name => [name, Number(field(prices, name).value)]));
        if (body.weekdayVip < body.weekdayRegular || body.weekendVip < body.weekendRegular)
          throw new Error("Giá VIP phải lớn hơn hoặc bằng giá thường cùng loại ngày.");
        await CineSnackApi.goi(`/api/admin/rooms/${room.id}/pricing`, { method: "PUT", body });
      }, () => current === generation);
    });
    policy.addEventListener("submit", event => {
      event.preventDefault();
      save(policy, byId("theater-policy-status"), () => CineSnackApi.goi("/api/admin/theater-policy", {
        method: "PUT", body: { lateCheckInMinutes: field(policy, "lateCheckInMinutes").value === "" ? null : Number(field(policy, "lateCheckInMinutes").value) }
      }));
    });
    byId("theater-policy-panel").hidden = false;
    policy.querySelector('[type="submit"]').disabled = true;
    try {
      const data = await CineSnackApi.goi("/api/admin/theater-policy");
      field(policy, "lateCheckInMinutes").value = data.lateCheckInMinutes ?? "";
      policy.querySelector('[type="submit"]').disabled = false;
    } catch (err) { byId("theater-policy-status").textContent = err.message; }
  }
  return { init, selectRoom, clearRoom };
})();
