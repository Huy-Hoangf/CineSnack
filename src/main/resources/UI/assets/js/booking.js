"use strict";
(async () => {
  const o = (id) => document.getElementById(id);
  let phim = [],
    suat = [],
    ghe = [],
    food,
    lanTai = 0,
    dangTai = true;
  let g = {
    version: 2,
    maSuatChieu: null,
    phim: null,
    suat: null,
    ghe: [],
    snacks: [],
    combos: [],
  };
  const luu = CineSnack.layGio();
  const params = new URLSearchParams(location.search);
  const maYeuCau = Number(params.get("phim"));
  const khoiPhuc = CineSnack.gioHopLe(luu) && (!params.has("phim") || maYeuCau === luu.phim?.id);
  const loi = (text) => {
    o("dat-ve-loi").textContent = text;
  };
  function hienTrangThaiGhe(text) {
    o("ghe-trang-thai").textContent = text;
    o("ghe-so-do-khung").hidden = Boolean(text);
  }
  function capNhat() {
    CineSnack.hienTomTat(g);
    o("tiep-dat-ve").disabled = dangTai || !o("chon-phim").value || !CineSnack.gioHopLe(g);
    o("tiep-goi-y").textContent = g.ghe.length
      ? `${g.ghe.length} ghế đã chọn.`
      : "Chọn ghế hoặc bắp nước để tiếp tục.";
  }
  function radio(v, ten, text, checked, change) {
    const label = document.createElement("label");
    label.className = "nhan-lua-chon";
    const i = document.createElement("input");
    Object.assign(i, { type: "radio", name: ten, value: String(v), checked });
    const span = document.createElement("span");
    span.textContent = text;
    i.addEventListener("change", change);
    label.append(i, span);
    return label;
  }
  async function chonSuat(s) {
    const lan = ++lanTai;
    g.suat = s;
    g.maSuatChieu = s?.id || null;
    g.ghe = [];
    ghe = [];
    o("ghe-loi").textContent = "";
    hienTrangThaiGhe(s ? "Đang tải sơ đồ ghế…" : "Chọn suất chiếu để xem ghế.");
    dangTai = true;
    capNhat();
    o("hang-ghe").replaceChildren();
    if (!s) {
      dangTai = false;
      capNhat();
      return;
    }
    try {
      const ds = await CineSnackApi.goi(`/api/showtimes/${s.id}/seats`);
      if (lan !== lanTai) return;
      ghe = ds;
      hienTrangThaiGhe(ds.length ? "" : "Suất chiếu chưa được cấu hình ghế.");
      const hang = new Map();
      for (const v of ds) {
        const ma = /^[A-Za-z]+/.exec(v.code)?.[0] || "Ghế";
        if (!hang.has(ma)) {
          const row = document.createElement("div");
          row.className = "hang-ghe";
          const name = document.createElement("span");
          name.textContent = ma;
          row.append(name);
          hang.set(ma, row);
          o("hang-ghe").append(row);
        }
        const b = document.createElement("button");
        b.type = "button";
        b.className = "nut-ghe" + (v.type === "VIP" ? " vip" : "");
        b.dataset.gheId = v.id;
        b.dataset.ghe = v.code;
        b.textContent = v.code;
        b.disabled = !v.available;
        b.setAttribute("aria-pressed", "false");
        b.setAttribute(
          "aria-label",
          `Ghế ${v.code}, ${CineSnack.dinhDangTien(v.price)}${v.available ? "" : ", đã đặt"}`,
        );
        b.addEventListener("click", () => {
          loi("");
          if (g.ghe.some((x) => x.id === v.id)) g.ghe = g.ghe.filter((x) => x.id !== v.id);
          else {
            if (g.ghe.length >= 10) {
              o("ghe-loi").textContent = "Tối đa 10 ghế mỗi đơn.";
              return;
            }
            g.ghe.push(v);
          }
          b.setAttribute("aria-pressed", String(g.ghe.some((x) => x.id === v.id)));
          capNhat();
        });
        hang.get(ma).append(b);
      }
    } catch (e) {
      if (lan === lanTai) {
        hienTrangThaiGhe("Không tải được ghế. Vui lòng chọn lại suất chiếu để thử lại.");
        loi(e.message);
      }
    } finally {
      if (lan === lanTai) {
        dangTai = false;
        capNhat();
      }
    }
  }
  function hienGio(ngay, maUuTien = null) {
    o("chon-gio").replaceChildren();
    const ds = suat.filter((s) => CineSnack.ngaySuat(s.startsAt) === ngay);
    const daChon = maUuTien ? ds.find((s) => s.id === maUuTien) : null;
    for (const s of ds)
      o("chon-gio").append(
        radio(
          s.id,
          "gio",
          CineSnack.gioSuat(s.startsAt) + " · " + s.roomName,
          s.id === daChon?.id,
          () => chonSuat(s),
        ),
      );
    return chonSuat(daChon || null);
  }
  async function doiPhim(maUuTien = null) {
    const lan = ++lanTai;
    g.phim = phim.find((p) => p.id === Number(o("chon-phim").value)) || null;
    g.ghe = [];
    g.suat = null;
    g.maSuatChieu = null;
    dangTai = true;
    capNhat();
    o("chon-ngay").replaceChildren();
    o("chon-gio").replaceChildren();
    o("hang-ghe").replaceChildren();
    o("ghe-loi").textContent = "";
    hienTrangThaiGhe(g.phim ? "Đang tải suất chiếu…" :
      o("chon-phim").value === "snacks" ? "Chỉ mua bắp nước: không cần chọn ghế." : "Chọn phim trước để xem suất chiếu.");
    loi("");
    if (!g.phim) {
      dangTai = false;
      capNhat();
      return;
    }
    try {
      const ds = await CineSnackApi.goi("/api/showtimes?movieId=" + g.phim.id);
      if (lan !== lanTai) return;
      suat = ds;
      const ngay = [...new Set(ds.map((s) => CineSnack.ngaySuat(s.startsAt)))];
      const daChon = ds.find((s) => s.id === maUuTien);
      const ngayChon = daChon ? CineSnack.ngaySuat(daChon.startsAt) : ngay[0];
      for (const n of ngay)
        o("chon-ngay").append(
          radio(n, "ngay", CineSnack.dinhDangNgay(n), n === ngayChon, () => hienGio(n)),
        );
      if (ngay.length) await hienGio(ngayChon, maUuTien);
      else {
        dangTai = false;
        hienTrangThaiGhe("Phim chưa có suất chiếu để chọn ghế.");
        loi("Phim chưa có suất nhận đặt vé. Bạn có thể mua bắp nước riêng.");
        capNhat();
      }
    } catch (e) {
      if (lan === lanTai) {
        dangTai = false;
        hienTrangThaiGhe("Không tải được suất chiếu. Vui lòng chọn lại phim để thử lại.");
        loi(e.message);
        capNhat();
      }
    }
  }
  function hienFood() {
    o("chon-combo").replaceChildren();
    for (const [key, idKey, ds] of [
      ["combos", "comboId", food.combos],
      ["snacks", "productId", food.products],
    ])
      for (const h of ds) {
        const row = document.createElement("article");
        row.className = "combo-hang";
        const info = document.createElement("div");
        info.className = "combo-mo-ta";
        const name = document.createElement("h3");
        name.textContent = h.name;
        const price = document.createElement("strong");
        price.textContent = CineSnack.dinhDangTien(h.price);
        info.append(name, price);
        const group = document.createElement("div");
        group.className = "chon-luong";
        group.setAttribute("role", "group");
        group.setAttribute("aria-label", "Số lượng " + h.name);
        const output = document.createElement("output");
        const daLuu = g[key].find((v) => v[idKey] === h.id)?.quantity || 0;
        output.textContent = daLuu;
        output.id = `luong-${key}-${h.id}`;
        output.setAttribute("aria-live", "polite");
        const thayDoi = (n) => {
          const old = g[key].find((v) => v[idKey] === h.id);
          const q = (old?.quantity || 0) + n;
          if (q < 0 || q > 10) return;
          g[key] = g[key].filter((v) => v[idKey] !== h.id);
          if (q)
            g[key].push({
              [idKey]: h.id,
              name: h.name,
              quantity: q,
              unitPrice: h.price,
            });
          output.textContent = q;
          tru.disabled = q === 0;
          cong.disabled = q === 10;
          capNhat();
        };
        const tru = document.createElement("button");
        tru.type = "button";
        tru.textContent = "−";
        tru.disabled = daLuu === 0;
        tru.setAttribute("aria-label", "Giảm " + h.name);
        tru.addEventListener("click", () => thayDoi(-1));
        const cong = document.createElement("button");
        cong.type = "button";
        cong.textContent = "+";
        cong.disabled = daLuu === 10;
        cong.dataset.combo = h.id;
        cong.dataset.loai = key;
        cong.dataset.doi = "1";
        cong.setAttribute("aria-label", "Tăng " + h.name);
        cong.addEventListener("click", () => thayDoi(1));
        group.append(tru, output, cong);
        row.append(info, group);
        o("chon-combo").append(row);
      }
  }
  o("chon-phim").addEventListener("change", () => doiPhim());
  o("tiep-dat-ve").addEventListener("click", () => {
    if (!CineSnack.gioHopLe(g)) {
      loi("Suất hoặc lựa chọn không còn hợp lệ. Hãy chọn lại.");
      return;
    }
    try {
      sessionStorage.setItem(CineSnack.khoaGio, JSON.stringify(g));
      location.href = "checkout.html";
    } catch {
      loi("Trình duyệt không cho phép lưu giỏ. Bật lưu trữ của trang để tiếp tục.");
    }
  });
  capNhat();
  try {
    const [p, f] = await Promise.all([
      CineSnackApi.goi("/api/movies?status=DANG_CHIEU&size=100"),
      CineSnackApi.goi("/api/snacks"),
    ]);
    phim = p;
    food = f;
    o("chon-phim").replaceChildren();
    const empty = document.createElement("option");
    empty.value = "";
    empty.textContent = "Chọn phim";
    o("chon-phim").append(empty);
    const onlySnacks = document.createElement("option");
    onlySnacks.value = "snacks";
    onlySnacks.textContent = "Chỉ mua bắp nước";
    o("chon-phim").append(onlySnacks);
    for (const p of phim) {
      const option = document.createElement("option");
      option.value = p.id;
      option.textContent = p.name;
      o("chon-phim").append(option);
    }
    const ma = khoiPhuc ? luu.phim?.id : maYeuCau;
    o("chon-phim").value = String(phim.find((p) => p.id === ma)?.id ||
      (khoiPhuc && !luu.phim ? "snacks" : ""));
    if (khoiPhuc) {
      for (const [key, idKey, ds] of [
        ["combos", "comboId", food.combos],
        ["snacks", "productId", food.products],
      ]) {
        g[key] = ds.flatMap((h) => {
          const chon = luu[key].find((v) => v[idKey] === h.id);
          return chon
            ? [
                {
                  [idKey]: h.id,
                  name: h.name,
                  quantity: chon.quantity,
                  unitPrice: h.price,
                },
              ]
            : [];
        });
      }
    }
    hienFood();
    await doiPhim(khoiPhuc ? luu.maSuatChieu : null);
    if (khoiPhuc && g.maSuatChieu === luu.maSuatChieu) {
      g.ghe = ghe.filter((v) => v.available && luu.ghe.some((old) => old.id === v.id));
      document
        .querySelectorAll("[data-ghe-id]")
        .forEach((n) =>
          n.setAttribute(
            "aria-pressed",
            String(g.ghe.some((v) => v.id === Number(n.dataset.gheId))),
          ),
        );
      if (g.ghe.length !== luu.ghe.length)
        loi("Một số ghế đã được giữ hoặc ngừng bán. Vui lòng chọn lại ghế.");
    } else if (khoiPhuc && luu.ghe.length)
      loi("Suất đã lưu không còn nhận đặt vé. Vui lòng chọn suất mới.");
    capNhat();
  } catch (e) {
    dangTai = true;
    hienTrangThaiGhe("Không tải được danh mục. Tải lại trang để thử lại.");
    loi(e.message + " Tải lại trang để thử lại.");
    capNhat();
  }
})();
