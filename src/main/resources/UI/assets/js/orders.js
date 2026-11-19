"use strict";
window.CineSnackOrders = (() => {
  window.addEventListener("pageshow", (event) => {
    if (event.persisted) location.reload();
  });
  const lanTaiChiTiet = new WeakMap();
  const activePaymentQr = new WeakMap();
  function lanMoi(box) {
    const lan = (lanTaiChiTiet.get(box) || 0) + 1;
    lanTaiChiTiet.set(box, lan);
    return lan;
  }
  const nhan = {
    CHO_THANH_TOAN: "Chờ thanh toán",
    DA_THANH_TOAN: "Đã thanh toán",
    CHO_NHAN: "Chờ nhận",
    DA_NHAN: "Đã giao",
    HUY: "Đã hủy",
  };
  const taoPhanTu = (tag, text, cls) => {
    const n = document.createElement(tag);
    if (text !== undefined) n.textContent = text;
    if (cls) n.className = cls;
    return n;
  };
  async function taiChiTietDon(code, user, guestMode, guestToken) {
    let prefix = guestMode || !user ? "/api/guest/orders" : "/api/orders";
    const accessHeaders =
      guestMode && guestToken ? { "X-Guest-Access-Token": guestToken } : {};
    let data;
    try {
      data = await CineSnackApi.goi(prefix + "/" + encodeURIComponent(code), {
        extraHeaders: accessHeaders,
      });
    } catch (error) {
      if (
        guestMode ||
        user?.role !== "KHACH_HANG" ||
        ![403, 404].includes(error.status)
      )
        throw error;
      prefix = "/api/guest/orders";
      data = await CineSnackApi.goi(prefix + "/" + encodeURIComponent(code));
    }
    return { data, prefix, accessHeaders };
  }

  function taoThongTinDon(data) {
    const donHang = data.order,
      root = taoPhanTu("div", undefined, "order-detail"),
      dl = taoPhanTu("dl");
    for (const [key, value, id] of [
      ["Mã đơn", donHang.code, "ma-don"],
      ["Trạng thái", nhan[donHang.status] || donHang.status, "trang-thai-don"],
      ["Tổng tiền", CineSnack.dinhDangTien(donHang.total), "tong-tien-don"],
      ["Email", donHang.email],
      [
        "Hết hạn giữ chỗ",
        donHang.expiresAt ? CineSnack.dinhDangSuat(donHang.expiresAt) : "—",
      ],
    ]) {
      const row = taoPhanTu("div"),
        dd = taoPhanTu("dd", value);
      if (id) dd.id = id;
      row.append(taoPhanTu("dt", key), dd);
      dl.append(row);
    }
    if (data.tickets.length) {
      const first = data.tickets[0];
      const show = taoPhanTu("section", undefined, "order-show");
      show.append(taoPhanTu("h3", first.movieName || "Vé xem phim"));
      const parts = [
        first.roomName,
        first.startsAt ? CineSnack.dinhDangSuat(first.startsAt) : null,
      ].filter(Boolean);
      if (parts.length) show.append(taoPhanTu("p", parts.join(" · ")));
      show.append(
        taoPhanTu(
          "strong",
          "Ghế " + data.tickets.map((v) => v.seatCode || v.seatId).join(", "),
        ),
      );
      root.append(show);
    }
    root.append(dl);
    return root;
  }

  function themVeVaBapNuoc(root, data, user, run, giaoMon) {
    const donHang = data.order;
    const list = taoPhanTu("ul");
    for (const v of data.tickets)
      list.append(
        taoPhanTu(
          "li",
          `Ghế ${v.seatCode || "#" + v.seatId} · ${CineSnack.dinhDangTien(v.unitPrice)}`,
        ),
      );
    for (const s of data.snacks) {
      const item = taoPhanTu(
        "li",
        `${s.quantity} × ${s.name} · ${CineSnack.dinhDangTien(s.quantity * s.unitPrice)} · ${nhan[s.status] || s.status}`,
      );
      if (
        ["NHAN_VIEN", "QUAN_TRI"].includes(user?.role) &&
        s.status === "CHO_NHAN"
      ) {
        const deliver = taoPhanTu(
          "button",
          "Xác nhận đã giao",
          "account-button",
        );
        deliver.type = "button";
        deliver.addEventListener("click", () =>
          run(deliver, async () => {
            await giaoMon(s.id);
          }),
        );
        item.append(" ", deliver);
      }
      list.append(item);
    }
    root.append(list);
    if (data.tickets.length && donHang.status === "DA_THANH_TOAN") {
      const codes = taoPhanTu("details", undefined, "ticket-codes"),
        values = taoPhanTu("ul");
      for (const ticket of data.tickets) {
        const item = taoPhanTu("li", `${ticket.seatCode ? "Ghế " + ticket.seatCode : "Vé"}: ${ticket.code}`);
        if (ticket.status === "DA_SOAT") item.append(taoPhanTu("strong", "Vé đã soát"));
        else if (ticket.status === "DA_THANH_TOAN") {
          try { item.append(CineSnackTicketCode.create(ticket.code)); }
          catch (error) { item.append(taoPhanTu("p", error.message)); }
        }
        values.append(item);
      }
      codes.append(
        taoPhanTu("summary", "Xem mã vé để cung cấp tại rạp"),
        values,
      );
      root.append(codes);
    }
  }

  async function themHoaDon(root, donHang, invoiceUrl, accessHeaders, tickets, user, run) {
    const invoices = taoPhanTu("div");
    invoices.id = "hoa-don";
    root.append(taoPhanTu("h3", "Hóa đơn đã thanh toán"), invoices);
    if (donHang.status === "DA_THANH_TOAN") {
      try {
        const ds = await CineSnackApi.goi(invoiceUrl, {
          extraHeaders: accessHeaders,
        });
        for (const h of ds) {
          const card = taoPhanTu("article", undefined, "invoice");
          card.append(
            taoPhanTu(
              "h3",
              h.type === "VE" ? "Hóa đơn vé" : "Hóa đơn bắp nước",
            ),
            taoPhanTu("p", h.code + " · " + CineSnack.dinhDangTien(h.total)),
          );
          const lines = taoPhanTu("ul");
          for (const l of h.lines)
            lines.append(
              taoPhanTu(
                "li",
                `${l.quantity} × ${l.description} — ${CineSnack.dinhDangTien(l.quantity * l.unitPrice)}`,
              ),
            );
          card.append(lines);
          if (window.CineSnackPrint) {
            if (h.type !== "VE") card.append(CineSnackPrint.invoiceButton(h));
            else if (user?.role === "NHAN_VIEN")
              card.append(CineSnackPrint.ticketsButton(tickets, run));
          }
          invoices.append(card);
        }
      } catch (error) {
        invoices.append(
          taoPhanTu(
            "p",
            "Đơn đã thanh toán. Chưa tải được hóa đơn: " + error.message,
            "loi",
          ),
        );
      }
    } else
      invoices.append(
        taoPhanTu("p", "Đơn chưa thanh toán, chưa có hóa đơn được phát hành."),
      );
  }

  async function hienChiTiet(
    box,
    code,
    user,
    thayDoi = async () => {},
    hopLe = () => true,
    guestMode = false,
    guestToken = null,
  ) {
    if (!hopLe()) return;
    const lan = lanMoi(box);
    let dangThuTien = false;
    let paymentExpired = false;
    const { data, prefix, accessHeaders } = await taiChiTietDon(
      code,
      user,
      guestMode,
      guestToken,
    );
    const donHang = data.order;
    const root = taoThongTinDon(data);
    themVeVaBapNuoc(root, data, user, run, async (itemId) => {
      await CineSnackApi.goi(
        `/api/admin/orders/${encodeURIComponent(code)}/snacks/${itemId}/deliver`,
        { method: "POST" },
      );
      await hienChiTiet(box, code, user, thayDoi, hopLe, guestMode, guestToken);
      await thayDoi();
    });
    const message = taoPhanTu("p", "", "loi");
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
        b.disabled = dangThuTien || (paymentExpired && b.type === "submit") ||
          (b === cancel && donHang.status !== "CHO_THANH_TOAN");
        b.removeAttribute("aria-busy");
      }
    }
    const actions = taoPhanTu("div", undefined, "order-actions");
    const refresh = taoPhanTu("button", "Cập nhật đơn", "nut phu");
    refresh.type = "button";
    refresh.id = "tai-lai-don";
    refresh.addEventListener("click", () =>
      run(refresh, async () => {
        await hienChiTiet(
          box,
          code,
          user,
          thayDoi,
          hopLe,
          guestMode,
          guestToken,
        );
        await thayDoi();
      }),
    );
    const cancel = taoPhanTu("button", "Hủy đơn", "nut phu");
    cancel.type = "button";
    cancel.id = "huy-don";
    cancel.disabled = donHang.status !== "CHO_THANH_TOAN";
    cancel.addEventListener("click", () =>
      run(cancel, async () => {
        await CineSnackApi.goi(`${prefix}/${encodeURIComponent(code)}/cancel`, {
          method: "POST",
        });
        donHang.status = "HUY";
        paymentQr?.dispose();
        paymentQr?.node.remove();
        root.querySelector("#trang-thai-don").textContent = nhan.HUY;
        root.querySelectorAll(".cash-form").forEach((form) => form.hidden = true);
        await hienChiTiet(box, code, user, thayDoi, hopLe, guestMode);
        await thayDoi();
      }),
    );
    actions.append(refresh, cancel);
    root.append(actions, message);
    if (
      ["NHAN_VIEN", "QUAN_TRI"].includes(user?.role) &&
      donHang.status === "CHO_THANH_TOAN"
    ) {
      const form = taoPhanTu("form", undefined, "cash-form");
      form.id = "cash-form";
      const label = taoPhanTu("label", "Số tiền khách đưa (VND)");
      label.htmlFor = "tien-khach-dua";
      const input = taoPhanTu("input");
      Object.assign(input, {
        id: "tien-khach-dua",
        type: "number",
        min: String(donHang.total),
        max: "999999999999",
        step: "1",
        required: true,
        inputMode: "numeric",
      });
      const submit = taoPhanTu("button", "Xác nhận đã nhận tiền", "nut");
      submit.type = "submit";
      submit.id = "nhan-tien";
      const amountDue = "Cần thu " + CineSnack.dinhDangTien(donHang.total);
      form.append(taoPhanTu("h3", "Thanh toán tiền mặt"), taoPhanTu("strong", amountDue, "ops-amount"), label, input, submit);
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
            cancel.disabled = donHang.status !== "CHO_THANH_TOAN";
            throw error;
          }
          dangThuTien = false;
          refresh.disabled = false;
          paymentQr?.dispose();
          paymentQr?.node.remove();
          donHang.status = "DA_THANH_TOAN";
          root.querySelector("#trang-thai-don").textContent =
            nhan[donHang.status];
          cancel.disabled = true;
          form.hidden = true;
          const p = taoPhanTu(
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
      const transfer = taoPhanTu("form", undefined, "cash-form");
      const transferLabel = taoPhanTu(
        "label",
        "Mã giao dịch đã đối chiếu trên tài khoản ngân hàng",
      );
      const transferCode = taoPhanTu("input");
      transferCode.required = true;
      transferCode.minLength = 4;
      transferCode.maxLength = 100;
      transferCode.pattern = "[A-Za-z0-9_-]{4,100}";
      transferLabel.append(transferCode);
      const amountLabel = taoPhanTu("label", "Số tiền thực nhận (VND)");
      const amountInput = taoPhanTu("input");
      amountInput.type = "number";
      amountInput.min = "1";
      amountInput.step = "1";
      amountInput.required = true;
      amountInput.inputMode = "numeric";
      amountLabel.append(amountInput);
      const confirmTransfer = taoPhanTu(
        "button",
        "Xác nhận chuyển khoản",
        "nut",
      );
      confirmTransfer.type = "submit";
      transfer.append(
        taoPhanTu("h3", "Đối chiếu chuyển khoản"),
        taoPhanTu("strong", amountDue, "ops-amount"),
        taoPhanTu(
          "p",
          "Chỉ xác nhận sau khi kiểm tra đúng số tiền và mã đơn trong tài khoản nhận tiền.",
        ),
        transferLabel,
        amountLabel,
        confirmTransfer,
      );
      transfer.addEventListener("submit", (event) => {
        event.preventDefault();
        run(confirmTransfer, async () => {
          await CineSnackApi.goi(
            `/api/orders/${encodeURIComponent(code)}/payments/transfer`,
            {
              method: "POST",
              body: {
                transactionCode: transferCode.value.trim(),
                amountReceived: Number(amountInput.value),
              },
            },
          );
          paymentQr?.dispose();
          paymentQr?.node.remove();
          donHang.status = "DA_THANH_TOAN";
          transfer.hidden = true;
          form.hidden = true;
          await hienChiTiet(box, code, user, thayDoi, hopLe, guestMode);
          await thayDoi();
        });
      });
      root.append(transfer);
    }
    const paymentQr = donHang.status === "CHO_THANH_TOAN"
      ? CineSnackOrderQr.create(donHang, prefix, () => {
          paymentExpired = true;
          root.querySelectorAll('.cash-form button[type="submit"]').forEach((b) => b.disabled = true);
        })
      : null;
    if (paymentQr) root.append(paymentQr.node);
    await themHoaDon(
      root,
      donHang,
      `${prefix}/${encodeURIComponent(code)}/invoices`,
      accessHeaders,
      data.tickets,
      user,
      run,
    );
    if (hopLe() && lanTaiChiTiet.get(box) === lan) {
      activePaymentQr.get(box)?.dispose();
      box.replaceChildren(root);
      if (paymentQr) {
        activePaymentQr.set(box, paymentQr);
        paymentQr.activate();
      } else activePaymentQr.delete(box);
    } else paymentQr?.dispose();
    return donHang;
  }
  return { nhan, taoPhanTu, hienChiTiet };
})();
(async () => {
  const list = document.querySelector("#danh-sach-don");
  if (!list) return;
  const layPhanTu = (id) => document.getElementById(id);
  let user,
    scope = "account",
    trang = 0,
    lan = 0,
    lanDanhSach = 0;
  const params = new URLSearchParams(location.search);
  const guestToken = new URLSearchParams(location.hash.slice(1)).get("access");
  const linkedCode = params.get("code");
  function loi(error) {
    layPhanTu("don-loi").textContent = error.message;
    layPhanTu("don-loi").focus();
  }
  async function chiTiet(code) {
    const n = ++lan;
    try {
      await CineSnackOrders.hienChiTiet(
        layPhanTu("chi-tiet-don"),
        code,
        user,
        taiDanhSach,
        () => n === lan,
        scope === "guest",
        code === linkedCode ? guestToken : null,
      );
      if (n !== lan) return;
      layPhanTu("don-chi-tiet-khung").hidden = false;
      if (n === lan)
        history.replaceState(
          null,
          "",
          "orders.html?code=" +
            encodeURIComponent(code) +
            (scope === "guest" ? "&guest=1" : "") +
            (code === linkedCode ? location.hash : ""),
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
    const rongTrangDau = ds.length === 0 && trang === 0;
    layPhanTu("don-trong").hidden =
      !rongTrangDau || Boolean(guestToken && linkedCode);
    const hasOpenOrder = Boolean(
      layPhanTu("chi-tiet-don").querySelector("#ma-don"),
    );
    layPhanTu("don-chi-tiet-khung").hidden =
      rongTrangDau && !guestToken && !hasOpenOrder;
    layPhanTu("don-phan-trang").hidden = rongTrangDau;
    if (!ds.length && trang > 0)
      list.append(
        CineSnackOrders.taoPhanTu(
          "li",
          user
            ? "Chưa có đơn trên trang này. Bạn có thể chọn phim để đặt vé mới."
            : "Phiên trình duyệt này chưa có đơn. Hãy đặt vé bằng email hoặc đăng nhập để xem đơn tài khoản.",
        ),
      );
    if (rongTrangDau && guestToken && linkedCode)
      list.append(
        CineSnackOrders.taoPhanTu(
          "li",
          "Đang xem đơn từ liên kết trong email.",
        ),
      );
    for (const donHang of ds) {
      const li = document.createElement("li"),
        b = CineSnackOrders.taoPhanTu(
          "button",
          undefined,
          "chu-nut order-card-button",
        );
      b.append(
        CineSnackOrders.taoPhanTu(
          "strong",
          CineSnackOrders.nhan[donHang.status],
        ),
        CineSnackOrders.taoPhanTu(
          "span",
          CineSnack.dinhDangTien(donHang.total),
        ),
        CineSnackOrders.taoPhanTu("small", donHang.code),
        CineSnackOrders.taoPhanTu(
          "small",
          CineSnack.dinhDangSuat(donHang.createdAt),
        ),
      );
      b.type = "button";
      b.addEventListener("click", () => chiTiet(donHang.code));
      li.append(b);
      list.append(li);
    }
    layPhanTu("don-truoc").disabled = trang === 0;
    layPhanTu("don-sau").disabled = ds.length < 20;
    return ds;
  }
  for (const [id, change] of [
    ["don-truoc", -1],
    ["don-sau", 1],
  ])
    layPhanTu(id).addEventListener("click", async () => {
      trang = Math.max(0, trang + change);
      try {
        await taiDanhSach();
      } catch (e) {
        loi(e);
      }
    });
  layPhanTu("tai-danh-sach").addEventListener("click", () =>
    taiDanhSach().catch(loi),
  );
  layPhanTu("orders-scope").addEventListener("change", async () => {
    if (user?.role === "KHACH_HANG") {
      layPhanTu("orders-scope").value = "account";
      return;
    }
    scope = layPhanTu("orders-scope").value;
    trang = 0;
    lan++;
    layPhanTu("chi-tiet-don").replaceChildren(
      CineSnackOrders.taoPhanTu("p", "Chọn một đơn để xem chi tiết."),
    );
    layPhanTu("guest-order-note").hidden = scope !== "guest";
    history.replaceState(
      null,
      "",
      "orders.html" + (scope === "guest" ? "?guest=1" : ""),
    );
    try {
      await taiDanhSach();
    } catch (error) {
      loi(error);
    }
  });
  try {
    user = await CineSnackApi.taiKhoan();
    scope =
      guestToken && linkedCode
        ? "guest"
        : user?.role === "KHACH_HANG"
          ? "account"
          : !user || new URLSearchParams(location.search).get("guest") === "1"
            ? "guest"
            : "account";
    layPhanTu("orders-scope").value = scope;
    layPhanTu("orders-scope-label").hidden =
      !user || user.role === "KHACH_HANG";
    layPhanTu("guest-order-note").hidden = scope !== "guest";
    if (!user) {
      layPhanTu("orders-login").hidden = false;
      layPhanTu("orders-login").href =
        "authentication.html?next=" +
        encodeURIComponent("orders.html" + location.search);
      layPhanTu("guest-order-note").hidden = false;
    }
    layPhanTu("orders-content").hidden = false;
    const ds = await taiDanhSach();
    const code = linkedCode;
    if (code) await chiTiet(code);
    else if (ds?.length) await chiTiet(ds[0].code);
  } catch (e) {
    loi(e);
  }
})();
