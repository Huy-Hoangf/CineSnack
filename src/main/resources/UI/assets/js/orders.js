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
    guestMode = false,
  ) {
    if (!hopLe()) return;
    const lan = lanMoi(box);
    let dangThuTien = false;
    let prefix = user && !guestMode ? "/api/orders" : "/api/guest/orders";
    let data;
    try {
      data = await CineSnackApi.goi(prefix + "/" + encodeURIComponent(code));
    } catch (error) {
      if (user?.role !== "KHACH_HANG" || guestMode || ![403, 404].includes(error.status))
        throw error;
      prefix = "/api/guest/orders";
      data = await CineSnackApi.goi(prefix + "/" + encodeURIComponent(code));
    }
    const d = data.order,
      root = e("div", undefined, "order-detail"),
      dl = e("dl");
    for (const [key, value, id] of [
      ["Mã đơn", d.code, "ma-don"],
      ["Trạng thái", nhan[d.status] || d.status, "trang-thai-don"],
      ["Tổng tiền", CineSnack.dinhDangTien(d.total), "tong-tien-don"],
      ["Email", d.email],
      ["Hết hạn giữ chỗ", d.expiresAt ? CineSnack.dinhDangSuat(d.expiresAt) : "—"],
    ]) {
      const row = e("div"),
        dd = e("dd", value);
      if (id) dd.id = id;
      row.append(e("dt", key), dd);
      dl.append(row);
    }
    if (data.tickets.length) {
      const first = data.tickets[0];
      const show = e("section", undefined, "order-show");
      show.append(e("h3", first.movieName || "Vé xem phim"));
      const parts = [
        first.roomName,
        first.startsAt ? CineSnack.dinhDangSuat(first.startsAt) : null,
      ].filter(Boolean);
      if (parts.length) show.append(e("p", parts.join(" · ")));
      show.append(e("strong", "Ghế " + data.tickets.map((v) => v.seatCode || v.seatId).join(", ")));
      root.append(show);
    }
    root.append(dl);
    const list = e("ul");
    for (const v of data.tickets)
      list.append(
        e("li", `Ghế ${v.seatCode || "#" + v.seatId} · ${CineSnack.dinhDangTien(v.unitPrice)}`),
      );
    for (const s of data.snacks)
      list.append(
        e("li", `${s.quantity} × ${s.name} · ${CineSnack.dinhDangTien(s.quantity * s.unitPrice)}`),
      );
    root.append(list);
    if (data.tickets.length && d.status === "DA_THANH_TOAN") {
      const codes = e("details", undefined, "ticket-codes"),
        values = e("ul");
      for (const ticket of data.tickets)
        values.append(e("li", `${ticket.seatCode || "Ghế"}: ${ticket.code}`));
      codes.append(e("summary", "Xem mã vé để cung cấp tại rạp"), values);
      root.append(codes);
    }
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
        await hienChiTiet(box, code, user, thayDoi, hopLe, guestMode);
        await thayDoi();
      }),
    );
    const cancel = e("button", "Hủy đơn", "nut phu");
    cancel.type = "button";
    cancel.id = "huy-don";
    cancel.disabled = d.status !== "CHO_THANH_TOAN";
    cancel.addEventListener("click", () =>
      run(cancel, async () => {
        await CineSnackApi.goi(`${prefix}/${encodeURIComponent(code)}/cancel`, {
          method: "POST",
        });
        await hienChiTiet(box, code, user, thayDoi, hopLe, guestMode);
        await thayDoi();
      }),
    );
    actions.append(refresh, cancel);
    root.append(actions, message);
    if (["NHAN_VIEN", "QUAN_TRI"].includes(user?.role) && d.status === "CHO_THANH_TOAN") {
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
            "Tiền thừa phải trả khách: " + CineSnack.dinhDangTien(receipt.change),
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
      const transfer = e("form", undefined, "cash-form");
      const transferLabel = e("label", "Mã giao dịch đã đối chiếu trên tài khoản ngân hàng");
      const transferCode = e("input");
      transferCode.required = true;
      transferCode.minLength = 4;
      transferCode.maxLength = 100;
      transferCode.pattern = "[A-Za-z0-9_-]{4,100}";
      transferLabel.append(transferCode);
      const amountLabel = e("label", "Số tiền thực nhận (VND)");
      const amountInput = e("input");
      amountInput.type = "number";
      amountInput.min = "1";
      amountInput.step = "1";
      amountInput.required = true;
      amountInput.inputMode = "numeric";
      amountLabel.append(amountInput);
      const confirmTransfer = e("button", "Xác nhận chuyển khoản", "nut");
      confirmTransfer.type = "submit";
      transfer.append(e("h3", "Đối chiếu chuyển khoản"),
        e("p", "Chỉ xác nhận sau khi kiểm tra đúng số tiền và mã đơn trong tài khoản nhận tiền."),
        transferLabel, amountLabel, confirmTransfer);
      transfer.addEventListener("submit", (event) => {
        event.preventDefault();
        run(confirmTransfer, async () => {
          await CineSnackApi.goi(`/api/orders/${encodeURIComponent(code)}/payments/transfer`, {
            method: "POST", body: {
              transactionCode: transferCode.value.trim(), amountReceived: Number(amountInput.value),
            },
          });
          await hienChiTiet(box, code, user, thayDoi, hopLe, guestMode);
          await thayDoi();
        });
      });
      root.append(transfer);
    }
    if (d.status === "CHO_THANH_TOAN") {
      const qrBox = e("section", undefined, "invoice");
      qrBox.append(e("h3", "QR chuyển khoản đúng tổng tiền"));
      try {
        const qr = await CineSnackApi.goi(`${prefix}/${encodeURIComponent(code)}/payment-qr`);
        const img = e("img");
        img.src = qr.imageUrl;
        img.alt = `QR chuyển khoản ${CineSnack.dinhDangTien(qr.amount)}, nội dung ${qr.content}`;
        img.width = 260;
        img.height = 260;
        img.loading = "lazy";
        img.addEventListener("error", () => {
          img.remove();
          qrBox.append(e("p", "Không tải được ảnh QR. Vui lòng cập nhật đơn để thử lại.", "loi"));
        });
        qrBox.append(img,
          e("p", `Số tiền: ${CineSnack.dinhDangTien(qr.amount)} · Nội dung: ${qr.content}`),
          e("p", "Sau khi chuyển tiền, chờ nhân viên đối chiếu và xác nhận. Quét QR không tự hoàn tất đơn."));
      } catch (error) {
        qrBox.append(e("p", error.message, "loi"));
      }
      root.append(qrBox);
      if (d.expiresAt) {
        const conLai = Date.parse(d.expiresAt) - Date.now();
        setTimeout(() => {
          qrBox.replaceChildren(e("p", "Đã hết thời gian giữ chỗ. Cập nhật đơn để xem trạng thái mới.", "loi"));
        }, Math.max(0, conLai));
      }
    }
    const invoices = e("div");
    invoices.id = "hoa-don";
    root.append(e("h3", "Hóa đơn đã thanh toán"), invoices);
    if (d.status === "DA_THANH_TOAN") {
      try {
        const ds = await CineSnackApi.goi(`${prefix}/${encodeURIComponent(code)}/invoices`);
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
          e("p", "Đơn đã thanh toán. Chưa tải được hóa đơn: " + error.message, "loi"),
        );
      }
    } else invoices.append(e("p", "Đơn chưa thanh toán, chưa có hóa đơn được phát hành."));
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
    scope = "account",
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
      const order = await CineSnackOrders.hienChiTiet(
        o("chi-tiet-don"),
        code,
        user,
        taiDanhSach,
        () => n === lan,
        scope === "guest",
      );
      if (n !== lan) return;
      if (user && order.userId == null && scope !== "guest") {
        scope = "guest";
        o("orders-scope").value = scope;
        o("guest-order-note").hidden = false;
        trang = 0;
        await taiDanhSach();
      }
      if (n === lan)
        history.replaceState(
          null,
          "",
          "orders.html?code=" + encodeURIComponent(code) + (scope === "guest" ? "&guest=1" : ""),
        );
    } catch (e) {
      if (n === lan) loi(e);
    }
  }
  async function taiDanhSach() {
    const n = ++lanDanhSach;
    const ds = await CineSnackApi.goi(
      `${scope === "guest" ? "/api/guest/orders" : "/api/orders"}?page=${trang}&size=20`,
    );
    if (n !== lanDanhSach) return;
    list.replaceChildren();
    if (!ds.length)
      list.append(
        CineSnackOrders.e(
          "li",
          user
            ? "Chưa có đơn trên trang này. Bạn có thể chọn phim để đặt vé mới."
            : "Phiên trình duyệt này chưa có đơn. Hãy đặt vé bằng email hoặc đăng nhập để xem đơn tài khoản.",
        ),
      );
    for (const d of ds) {
      const li = document.createElement("li"),
        b = CineSnackOrders.e("button", undefined, "chu-nut order-card-button");
      b.append(
        CineSnackOrders.e("strong", CineSnackOrders.nhan[d.status]),
        CineSnackOrders.e("span", CineSnack.dinhDangTien(d.total)),
        CineSnackOrders.e("small", d.code),
        CineSnackOrders.e("small", CineSnack.dinhDangSuat(d.createdAt)),
      );
      b.type = "button";
      b.addEventListener("click", () => chiTiet(d.code));
      li.append(b);
      list.append(li);
    }
    o("don-truoc").disabled = trang === 0;
    o("don-sau").disabled = ds.length < 20;
    return ds;
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
  o("orders-scope").addEventListener("change", async () => {
    scope = o("orders-scope").value;
    trang = 0;
    lan++;
    o("chi-tiet-don").replaceChildren(CineSnackOrders.e("p", "Chọn một đơn để xem chi tiết."));
    o("guest-order-note").hidden = scope !== "guest";
    history.replaceState(null, "", "orders.html" + (scope === "guest" ? "?guest=1" : ""));
    try {
      await taiDanhSach();
    } catch (error) {
      loi(error);
    }
  });
  try {
    user = await CineSnackApi.taiKhoan();
    scope =
      !user || new URLSearchParams(location.search).get("guest") === "1" ? "guest" : "account";
    o("orders-scope").value = scope;
    o("orders-scope-label").hidden = !user;
    o("guest-order-note").hidden = scope !== "guest";
    if (!user) {
      o("orders-login").hidden = false;
      o("orders-login").href =
        "authentication.html?next=" + encodeURIComponent("orders.html" + location.search);
      o("guest-order-note").hidden = false;
    }
    o("orders-content").hidden = false;
    const ds = await taiDanhSach();
    const code = new URLSearchParams(location.search).get("code");
    if (code) await chiTiet(code);
    else if (ds?.length) await chiTiet(ds[0].code);
  } catch (e) {
    loi(e);
  }
})();
