(function () {
  "use strict";
  const khoaGio = "cinesnack-gio-v2";
  const idHopLe = (n) => Number.isSafeInteger(n) && n > 0;
  const giaHopLe = (n) => typeof n === "number" && Number.isFinite(n) && n >= 0;
  function boDau(s) {
    return String(s)
      .normalize("NFD")
      .replace(/[\u0300-\u036f]/g, "")
      .replace(/đ/gi, "d")
      .toLowerCase()
      .trim();
  }
  function layNgay(now = new Date()) {
    return [0, 1, 2].map((n) => {
      const d = new Date(now.getFullYear(), now.getMonth(), now.getDate() + n);
      return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
    });
  }
  function dinhDangNgay(s) {
    return new Intl.DateTimeFormat("vi-VN", {
      dateStyle: "short",
      timeZone: "Asia/Ho_Chi_Minh",
    }).format(new Date(s.length === 10 ? s + "T00:00:00+07:00" : s));
  }
  function dinhDangSuat(s) {
    return new Intl.DateTimeFormat("vi-VN", {
      dateStyle: "short",
      timeStyle: "short",
      timeZone: "Asia/Ho_Chi_Minh",
    }).format(new Date(s));
  }
  function ngaySuat(s) {
    const parts = new Intl.DateTimeFormat("en-CA", {
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
      timeZone: "Asia/Ho_Chi_Minh",
    }).formatToParts(new Date(s));
    const get = (key) => parts.find((p) => p.type === key).value;
    return `${get("year")}-${get("month")}-${get("day")}`;
  }
  function gioSuat(s) {
    return new Intl.DateTimeFormat("vi-VN", {
      hour: "2-digit",
      minute: "2-digit",
      timeZone: "Asia/Ho_Chi_Minh",
    }).format(new Date(s));
  }
  function dinhDangTien(n) {
    return new Intl.NumberFormat("vi-VN", {
      style: "currency",
      currency: "VND",
    }).format(n);
  }
  function tinhTien(g) {
    const tienVe = g.ghe.reduce((a, v) => a + v.price, 0);
    const tienBap = [...g.snacks, ...g.combos].reduce((a, v) => a + v.unitPrice * v.quantity, 0);
    return { tienVe, tienBap, tong: tienVe + tienBap };
  }
  function gioHopLe(g, now = new Date()) {
    if (
      !g ||
      g.version !== 2 ||
      !Array.isArray(g.ghe) ||
      !Array.isArray(g.snacks) ||
      !Array.isArray(g.combos) ||
      g.ghe.length > 10 ||
      g.snacks.length > 20 ||
      g.combos.length > 20
    )
      return false;
    if (!g.ghe.length && !g.snacks.length && !g.combos.length) return false;
    if (g.ghe.length) {
      if (
        !idHopLe(g.maSuatChieu) ||
        g.suat?.id !== g.maSuatChieu ||
        !idHopLe(g.phim?.id) ||
        !(Date.parse(g.suat.startsAt) > now.getTime())
      )
        return false;
    } else if (g.maSuatChieu !== null) return false;
    if (
      new Set(g.ghe.map((v) => v?.id)).size !== g.ghe.length ||
      g.ghe.some((v) => !v || !idHopLe(v.id) || typeof v.code !== "string" || !giaHopLe(v.price))
    )
      return false;
    for (const [list, key] of [
      [g.snacks, "productId"],
      [g.combos, "comboId"],
    ]) {
      if (
        new Set(list.map((v) => v?.[key])).size !== list.length ||
        list.some(
          (v) =>
            !v ||
            !idHopLe(v[key]) ||
            !Number.isInteger(v.quantity) ||
            v.quantity < 1 ||
            v.quantity > 10 ||
            !giaHopLe(v.unitPrice),
        )
      )
        return false;
    }
    return true;
  }
  function taoYeuCau(g, email) {
    return {
      email,
      showTimeId: g.maSuatChieu,
      seatIds: g.ghe.map((v) => v.id),
      snacks: g.snacks.map((v) => ({
        productId: v.productId,
        quantity: v.quantity,
      })),
      combos: g.combos.map((v) => ({
        comboId: v.comboId,
        quantity: v.quantity,
      })),
    };
  }
  function layGio() {
    try {
      return JSON.parse(sessionStorage.getItem(khoaGio));
    } catch {
      return null;
    }
  }
  function poster(url) {
    if (url) {
      try {
        const u = new URL(url, location.href);
        if (["http:", "https:"].includes(u.protocol) && !u.username && !u.password) return u.href;
      } catch {}
    }
    return "../assets/images/poster-placeholder.svg";
  }
  const rap = {
    khoaGio,
    boDau,
    layNgay,
    dinhDangNgay,
    dinhDangSuat,
    ngaySuat,
    gioSuat,
    dinhDangTien,
    tinhTien,
    gioHopLe,
    taoYeuCau,
    layGio,
    poster,
  };
  if (typeof module !== "undefined" && module.exports) module.exports = rap;
  else window.CineSnack = rap;
})();
