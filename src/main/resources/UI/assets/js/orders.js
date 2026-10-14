"use strict";
window.CineSnackOrders = (() => {
  const lanTaiChiTiet = new WeakMap();
  function lanMoi(box) {
    const lan = (lanTaiChiTiet.get(box) || 0) + 1;
    lanTaiChiTiet.set(box, lan);
    return lan;
  }
  const nhan = {
    CHO_THANH_TOAN: "Chờ thanh toán",
    DA_THANH_TOAN: "Đã thanh toán",
    HUY: "Đã hủy",
    HET_HAN: "Hết hạn",
  };
  const e = (tag, text, cls) => {
    const n = document.createElement(tag);
    if (text !== undefined) n.textContent = text;
    if (cls) n.className = cls;
    return n;
  };
  async function hienChiTiet(
    box,
    code,
    user,
    thayDoi = async () => {},
    hopLe = () => true,
  ) {
    if (!hopLe()) return;
    const lan = lanMoi(box);
    let dangThuTien = false;
    const data = await CineSnackApi.goi(
      "/api/orders/" + encodeURIComponent(code),
    );
    const d = data.order,
      root = e("div", undefined, "order-detail"),
      dl = e("dl");
    for (const [key, value, id] of [
      ["Mã đơn", d.code, "ma-don"],
      ["Trạng thái", nhan[d.status] || d.status, "trang-thai-don"],
      ["Tổng tiền", CineSnack.dinhDangTien(d.total), "tong-tien-don"],
      ["Email", d.email],
      [
        "Hết hạn giữ chỗ",
        d.expiresAt ? CineSnack.dinhDangSuat(d.expiresAt) : "—",
      ],
    ]) {
      const row = e("div"),
        dd = e("dd", value);
      if (id) dd.id = id;
      row.append(e("dt", key), dd);
      dl.append(row);
    }
    root.append(dl);
    const list = e("ul");
    for (const v of data.tickets)
      list.append(
        e(
          "li",
          `${v.code} · Ghế ${v.seatCode || "#" + v.seatId} · ${CineSnack.dinhDangTien(v.unitPrice)}`,
        ),
      );
    for (const s of data.snacks)
      list.append(
        e(
          "li",
          `${s.quantity} × ${s.name} · ${CineSnack.dinhDangTien(s.quantity * s.unitPrice)}`,
        ),
      );
    root.append(list);
    const message = e("p", "", "loi");
    message.setAttribute("role", "alert");
    message.tabIndex = -1;
    async function run(b, fn) {
      if (b.disabled) return;
      b.disabled = true;
      b.setAttribute("aria-busy", "true");
      try {
        await fn();
      } catch (error) {
        message.textContent = error.message;
        message.focus();
      } finally {
        b.disabled = dangThuTien;
        b.removeAttribute("aria-busy");
      }
    }
    const actions = e("div", undefined, "order-actions");
    const refresh = e("button", "Cập nhật đơn", "nut phu");
    refresh.type = "button";
    refresh.id = "tai-lai-don";
    refresh.addEventListener("click", () =>
      run(refresh, async () => {
        await hienChiTiet(box, code, user, thayDoi, hopLe);
        await thayDoi();
      }),
    );
    const cancel = e("button", "Hủy đơn", "nut phu");
    cancel.type = "button";
    cancel.id = "huy-don";
    cancel.disabled = d.status !== "CHO_THANH_TOAN";
    cancel.addEventListener("click", () =>
      run(cancel, async () => {
        await CineSnackApi.goi(
          `/api/orders/${encodeURIComponent(code)}/cancel`,
          { method: "POST" },
        );
        await hienChiTiet(box, code, user, thayDoi, hopLe);
        await thayDoi();
      }),
    );
    actions.append(refresh, cancel);
    root.append(actions, message);
    if (
      ["NHAN_VIEN", "QUAN_TRI"].includes(user.role) &&
      d.status === "CHO_THANH_TOAN"
    ) {
      const form = e("form", undefined, "cash-form");
      form.id = "cash-form";
      const label = e("label", "Số tiền khách đưa (VND)");
      label.htmlFor = "tien-khach-dua";
      const input = e("input");
      Object.assign(input, {
        id: "tien-khach-dua",
        type: "number",
        min: String(d.total),
        max: "999999999999",
        step: "1",
        required: true,
        inputMode: "numeric",
      });
      const submit = e("button", "Xác nhận đã nhận tiền", "nut");
      submit.type = "submit";
      submit.id = "nhan-tien";
      form.append(e("h3", "Thanh toán tiền mặt"), label, input, submit);
      form.addEventListener("submit", (event) => {
        event.preventDefault();
        run(submit, async () => {
          dangThuTien = true;
          refresh.disabled = true;
          cancel.disabled = true;
          lanMoi(box);
          let receipt;
          try {
            receipt = await CineSnackApi.goi(
              `/api/orders/${encodeURIComponent(code)}/payments/cash`,
              { method: "POST", body: { amountReceived: Number(input.value) } },
            );
          } catch (error) {
            dangThuTien = false;
            refresh.disabled = false;
            cancel.disabled = d.status !== "CHO_THANH_TOAN";
            throw error;
          }
          dangThuTien = false;
          refresh.disabled = false;
          d.status = "DA_THANH_TOAN";
          root.querySelector("#trang-thai-don").textContent = nhan[d.status];
          cancel.disabled = true;
          form.hidden = true;
          const p = e(
            "p",
            "Tiền thừa phải trả khách: " +
              CineSnack.dinhDangTien(receipt.change),
            "thong-bao",
          );
          p.id = "tien-thua";
          if (hopLe()) box.prepend(p);
          try {
            await hienChiTiet(box, code, user, thayDoi, hopLe);
          } finally {
            if (hopLe()) box.prepend(p);
          }
          await thayDoi();
        });
      });
      root.append(form);
    }
    const invoices = e("div");
    invoices.id = "hoa-don";
    root.append(e("h3", "Hóa đơn đã thanh toán"), invoices);
    if (d.status === "DA_THANH_TOAN") {
      try {
        const ds = await CineSnackApi.goi(
          `/api/orders/${encodeURIComponent(code)}/invoices`,
        );
        for (const h of ds) {
          const card = e("article", undefined, "invoice");
          card.append(
            e("h3", h.type === "VE" ? "Hóa đơn vé" : "Hóa đơn bắp nước"),
            e("p", h.code + " · " + CineSnack.dinhDangTien(h.total)),
          );
          const lines = e("ul");
          for (const l of h.lines)
            lines.append(
              e(
                "li",
                `${l.quantity} × ${l.description} — ${CineSnack.dinhDangTien(l.quantity * l.unitPrice)}`,
              ),
            );
          card.append(lines);
          invoices.append(card);
        }
      } catch (error) {
        invoices.append(
          e(
            "p",
            "Đơn đã thanh toán. Chưa tải được hóa đơn: " + error.message,
            "loi",
          ),
        );
      }
    } else
      invoices.append(
        e("p", "Đơn chưa thanh toán, chưa có hóa đơn được phát hành."),
      );
    if (hopLe() && lanTaiChiTiet.get(box) === lan) box.replaceChildren(root);
    return d;
  }
  return { nhan, e, hienChiTiet };
})();
(async () => {
  const list = document.querySelector("#danh-sach-don");
  if (!list) return;
  const o = (id) => document.getElementById(id);
  let user,
    trang = 0,
    lan = 0,
    lanDanhSach = 0;
  function loi(error) {
    o("don-loi").textContent = error.message;
    o("don-loi").focus();
  }
  async function chiTiet(code) {
    const n = ++lan;
    try {
      await CineSnackOrders.hienChiTiet(
        o("chi-tiet-don"),
        code,
        user,
        taiDanhSach,
        () => n === lan,
      );
      if (n === lan)
        history.replaceState(
          null,
          "",
          "orders.html?code=" + encodeURIComponent(code),
        );
    } catch (e) {
      if (n === lan) loi(e);
    }
  }
  async function taiDanhSach() {
    const n = ++lanDanhSach;
    const ds = await CineSnackApi.goi(`/api/orders?page=${trang}&size=20`);
    if (n !== lanDanhSach) return;
    list.replaceChildren();
    if (!ds.length)
      list.append(CineSnackOrders.e("li", "Chưa có đơn trên trang này."));
    for (const d of ds) {
      const li = document.createElement("li"),
        b = CineSnackOrders.e(
          "button",
          `${d.code} · ${CineSnackOrders.nhan[d.status]} · ${CineSnack.dinhDangTien(d.total)}`,
          "chu-nut",
        );
      b.type = "button";
      b.addEventListener("click", () => chiTiet(d.code));
      li.append(b);
      list.append(li);
    }
    o("don-truoc").disabled = trang === 0;
    o("don-sau").disabled = ds.length < 20;
  }
  for (const [id, change] of [
    ["don-truoc", -1],
    ["don-sau", 1],
  ])
    o(id).addEventListener("click", async () => {
      trang = Math.max(0, trang + change);
      try {
        await taiDanhSach();
      } catch (e) {
        loi(e);
      }
    });
  o("tai-danh-sach").addEventListener("click", () => taiDanhSach().catch(loi));
  try {
    user = await CineSnackApi.taiKhoan();
    if (!user) {
      o("orders-login").hidden = false;
      o("orders-login").href =
        "authentication.html?next=" +
        encodeURIComponent("orders.html" + location.search);
      return;
    }
    o("orders-content").hidden = false;
    await taiDanhSach();
    const code = new URLSearchParams(location.search).get("code");
    if (code) await chiTiet(code);
  } catch (e) {
    loi(e);
  }
})();
