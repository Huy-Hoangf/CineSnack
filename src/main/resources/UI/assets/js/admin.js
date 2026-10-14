"use strict";
(async () => {
  const o = (id) => document.getElementById(id),
    e = CineSnackOrders.e;
  let user,
    trang = 0,
    lanChiTiet = 0,
    lanDanhSach = 0,
    ds = [],
    phim = [],
    suat = [];
  const loi = (error) => {
    o("admin-loi").textContent = error.message;
    o("admin-loi").focus();
  };
  function dong(values) {
    const tr = e("tr");
    for (const v of values) tr.append(e("td", v));
    return tr;
  }
  async function xem(code) {
    const n = ++lanChiTiet;
    try {
      await CineSnackOrders.hienChiTiet(
        o("don-chi-tiet"),
        code,
        user,
        taiDon,
        () => n === lanChiTiet,
      );
      if (n !== lanChiTiet) return;
      o("don-chi-tiet-tieu-de").textContent = "Đơn " + code;
      if (!o("don-hop").open) o("don-hop").showModal();
    } catch (error) {
      loi(error);
    }
  }
  function bangDon(box, list) {
    box.replaceChildren();
    if (!list.length) {
      const tr = e("tr"),
        td = e("td", "Không có đơn trên trang này.");
      td.colSpan = 6;
      tr.append(td);
      box.append(tr);
    }
    for (const d of list) {
      const tr = dong([
        d.code,
        d.email,
        CineSnack.dinhDangSuat(d.createdAt),
        CineSnack.dinhDangTien(d.total),
        CineSnackOrders.nhan[d.status],
      ]);
      const td = e("td"),
        b = e("button", "Chi tiết", "chu-nut");
      b.type = "button";
      b.dataset.don = d.code;
      b.addEventListener("click", () => xem(d.code));
      td.append(b);
      tr.append(td);
      box.append(tr);
    }
  }
  function locDon() {
    const tu = CineSnack.boDau(o("quan-tri-don-tim").value),
      state = o("don-trang-thai").value;
    const list = ds.filter(
      (d) =>
        (state === "tat-ca" || state === d.status) &&
        CineSnack.boDau(d.code + " " + d.email).includes(tu),
    );
    bangDon(o("quan-tri-don"), list);
    o("don-so").textContent = `Trang ${trang + 1} · ${list.length} đơn`;
  }
  async function taiDon() {
    const n = ++lanDanhSach;
    const moi = await CineSnackApi.goi(`/api/admin/orders?page=${trang}&size=20`);
    if (n !== lanDanhSach) return;
    ds = moi;
    locDon();
    bangDon(o("moi-don"), ds.slice(0, 4));
    o("admin-truoc").disabled = trang === 0;
    o("admin-sau").disabled = ds.length < 20;
    o("thong-ke").replaceChildren();
    for (const [name, value] of [
      ["Đơn trên trang", ds.length],
      ["Chờ thanh toán", ds.filter((d) => d.status === "CHO_THANH_TOAN").length],
      ["Đã thanh toán", ds.filter((d) => d.status === "DA_THANH_TOAN").length],
      [
        "Tổng đơn đã trả trên trang",
        CineSnack.dinhDangTien(
          ds.filter((d) => d.status === "DA_THANH_TOAN").reduce((a, d) => a + d.total, 0),
        ),
      ],
    ]) {
      const card = e("article", undefined, "the-thong-ke");
      card.append(e("span", name, "thong-ke-nhan"), e("strong", value));
      o("thong-ke").append(card);
    }
  }
  function hienPhim() {
    o("quan-tri-phim").replaceChildren();
    const tu = CineSnack.boDau(o("quan-tri-phim-tim").value);
    const list = phim.filter((p) => CineSnack.boDau(p.name + " " + p.genre).includes(tu));
    for (const p of list) {
      const tr = dong([p.name, p.duration + " phút", p.ageRating, p.status]);
      const td = e("td"),
        a = e("a", "Xem phim", "chu-nut");
      a.href = "movies.html?phim=" + p.id;
      td.append(a);
      tr.append(td);
      o("quan-tri-phim").append(tr);
    }
    o("dem-phim").textContent = list.length + " phim (tối đa 100 kết quả)";
  }
  function hienSuat() {
    o("quan-tri-suat").replaceChildren();
    const tu = CineSnack.boDau(o("quan-tri-suat-tim").value);
    const list = suat.filter((s) => CineSnack.boDau(s.movieName + " " + s.roomName).includes(tu));
    for (const s of list) {
      const tr = dong([
        s.movieName,
        CineSnack.dinhDangSuat(s.startsAt),
        s.roomName,
        CineSnack.dinhDangTien(s.price),
        s.status,
      ]);
      o("quan-tri-suat").append(tr);
    }
    o("suat-so").textContent = list.length + " suất đang nhận đặt vé";
  }
  document.querySelectorAll("[data-muc]").forEach((b) =>
    b.addEventListener("click", () => {
      const id = b.dataset.muc;
      document.querySelectorAll(".quan-tri-muc").forEach((x) => {
        x.hidden = x.id !== "muc-" + id;
      });
      document.querySelectorAll(".quan-tri-dieu-huong-nut").forEach((x) => {
        x.classList.toggle("dang-chon", x.dataset.muc === id);
        x.setAttribute("aria-pressed", String(x.dataset.muc === id));
      });
      o("quan-tri-duong-dan").textContent = b.textContent.trim();
    }),
  );
  o("quan-tri-don-tim").addEventListener("input", locDon);
  o("don-trang-thai").addEventListener("change", locDon);
  o("quan-tri-phim-tim").addEventListener("input", hienPhim);
  o("quan-tri-suat-tim").addEventListener("input", hienSuat);
  for (const [id, n] of [
    ["admin-truoc", -1],
    ["admin-sau", 1],
  ])
    o(id).addEventListener("click", async () => {
      trang = Math.max(0, trang + n);
      try {
        await taiDon();
      } catch (error) {
        loi(error);
      }
    });
  o("tra-don-form").addEventListener("submit", (event) => {
    event.preventDefault();
    xem(o("tra-ma-don").value.trim());
  });
  document.querySelectorAll(".doanh-thu-khung,.noi-bat-khung").forEach((x) => {
    x.hidden = true;
  });
  try {
    user = await CineSnackApi.taiKhoan();
    if (!user) {
      o("admin-login").hidden = false;
      return;
    }
    if (!["NHAN_VIEN", "QUAN_TRI"].includes(user.role)) {
      loi(new Error("Tài khoản không có quyền quản trị hoặc nhận tiền."));
      return;
    }
    o("admin-email").textContent = user.email;
    o("quan-tri-ngay").textContent = CineSnack.dinhDangNgay(new Date().toISOString());
    await taiDon();
    [phim, suat] = await Promise.all([
      CineSnackApi.goi("/api/movies?size=100"),
      CineSnackApi.goi("/api/showtimes"),
    ]);
    hienPhim();
    hienSuat();
  } catch (error) {
    loi(error);
  }
})();
