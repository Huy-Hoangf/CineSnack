"use strict";
(async () => {
  const layPhanTu = (id) => document.getElementById(id),
    taoPhanTu = CineSnackOrders.taoPhanTu;
  const nhan = {
    ...CineSnackOrders.nhan,
    DANG_CHIEU: "Đang chiếu",
    SAP_CHIEU: "Sắp chiếu",
    NGUNG_CHIEU: "Ngừng chiếu",
    DA_CHIEU: "Đã chiếu",
  };
  let user,
    section = "don",
    page = 0,
    moviePage = 0,
    showPage = 0,
    listGeneration = 0,
    detailGeneration = 0;
  let movies = [],
    shows = [],
    rooms = [],
    movieOptions = [];
  const isAdmin = () => user?.role === "QUAN_TRI";
  const timeline = CineSnackTimeline.init({
    container: layPhanTu("showtime-timeline"),
    listElement: layPhanTu("suat-list-panel"),
    countElement: layPhanTu("suat-so"),
    request: (url) => CineSnackApi.goi(url),
    onEdit: editShow,
    onError: error,
  });
  function error(err) {
    layPhanTu("admin-loi").textContent = err.message;
    layPhanTu("admin-loi").focus();
  }
  function notice(text) {
    layPhanTu("admin-status").textContent = text;
    layPhanTu("admin-status").hidden = !text;
  }
  function button(text, action, attrs = {}) {
    const b = taoPhanTu("button", text, "account-button");
    b.type = "button";
    Object.assign(b.dataset, attrs);
    b.addEventListener("click", async () => {
      if (b.disabled) return;
      b.disabled = true;
      try {
        await action();
      } catch (err) {
        error(err);
      } finally {
        b.disabled = false;
      }
    });
    return b;
  }
  function cell(row, text) {
    const td = taoPhanTu("td", text);
    row.append(td);
    return td;
  }
  function badge(text, tone = "pending") {
    return taoPhanTu("span", text, "workspace-badge " + tone);
  }
  async function showOrder(code) {
    const generation = ++detailGeneration;
    layPhanTu("admin-loi").textContent = "";
    await CineSnackOrders.hienChiTiet(
      layPhanTu("don-chi-tiet"),
      code,
      user,
      loadOrders,
      () => generation === detailGeneration,
    );
    if (generation !== detailGeneration) return;
    layPhanTu("don-chi-tiet-tieu-de").textContent = "Đơn " + code;
    if (!layPhanTu("don-hop").open) layPhanTu("don-hop").showModal();
  }
  layPhanTu("don-hop").addEventListener("close", () => detailGeneration++);
  async function loadOrders() {
    const generation = ++listGeneration;
    const q = layPhanTu("quan-tri-don-tim").value.trim(),
      status = layPhanTu("don-trang-thai").value;
    const ds = await CineSnackApi.goi(
      `/api/admin/orders?page=${page}&size=20&q=${encodeURIComponent(q)}&status=${encodeURIComponent(status)}`,
    );
    if (generation !== listGeneration) return;
    const tbody = layPhanTu("quan-tri-don");
    const focusedCode = tbody.contains(document.activeElement)
      ? document.activeElement.dataset.don
      : null;
    tbody.replaceChildren();
    if (!ds.length) {
      const row = taoPhanTu("tr");
      cell(row, "Không có đơn phù hợp trên trang này.").colSpan = 5;
      tbody.append(row);
    }
    for (const d of ds) {
      const row = taoPhanTu("tr"),
        first = cell(row);
      first.append(
        taoPhanTu("strong", d.code),
        taoPhanTu("small", d.email),
        badge(d.userId == null ? "Guest" : "User", "neutral"),
      );
      cell(row, CineSnack.dinhDangSuat(d.createdAt));
      cell(row, CineSnack.dinhDangTien(d.total));
      cell(row).append(
        badge(nhan[d.status] || d.status, d.status === "DA_THANH_TOAN" ? "paid" : d.status === "CHO_THANH_TOAN" ? "pending" : "closed"),
      );
      cell(row).append(
        button("Xem đơn", () => showOrder(d.code), {
          don: d.code,
        }),
      );
      tbody.append(row);
    }
    if (focusedCode) {
      const sameOrder = tbody.querySelector(`[data-don="${CSS.escape(focusedCode)}"]`);
      (sameOrder ?? layPhanTu("quan-tri-don-tim")).focus({ preventScroll: true });
    }
    const pending = ds.filter(order => order.status === "CHO_THANH_TOAN").length;
    const paid = ds.filter(order => order.status === "DA_THANH_TOAN").length;
    layPhanTu("don-so").textContent =
      `Trang ${page + 1} · ${ds.length} đơn · ${pending} chờ thanh toán · ${paid} đã thanh toán`;
    layPhanTu("admin-truoc").disabled = page === 0;
    layPhanTu("admin-sau").disabled = ds.length < 20;
  }
  async function loadMovies() {
    movies = await CineSnackApi.goi(
      `/api/admin/movies?page=${moviePage}&size=100`,
    );
    layPhanTu("quan-tri-phim").replaceChildren();
    for (const p of movies) {
      const row = taoPhanTu("tr"),
        first = cell(row);
      first.append(taoPhanTu("strong", p.name), taoPhanTu("small", p.genre));
      cell(row, p.duration + " phút");
      cell(row, p.ageRating);
      cell(row).append(
        badge(nhan[p.status] || p.status, p.status === "DANG_CHIEU" ? "paid" : "neutral"),
      );
      const actions = taoPhanTu("div", undefined, "workspace-row-actions");
      actions.append(
        button("Sửa", () => editMovie(p), { editMovie: p.id }),
        button(
          "Xóa / ngừng chiếu",
          () =>
            confirmDelete(
              `Xử lý phim “${p.name}”? Phim đã có lịch chiếu sẽ chuyển sang ngừng chiếu; phim chưa có lịch chiếu sẽ bị xóa.`,
              async () => {
                await CineSnackApi.goi(`/api/admin/movies/${p.id}`, {
                  method: "DELETE",
                });
                await loadMovies();
                notice("Đã cập nhật danh mục phim.");
              },
            ),
          { deleteMovie: p.id },
        ),
      );
      cell(row).append(actions);
      layPhanTu("quan-tri-phim").append(row);
    }
    layPhanTu("dem-phim").textContent =
      `Trang ${moviePage + 1} · ${movies.length} phim`;
    layPhanTu("phim-truoc").disabled = moviePage === 0;
    layPhanTu("phim-sau").disabled = movies.length < 100;
  }
  async function loadShows() {
    shows = await CineSnackApi.goi(
      `/api/admin/showtimes?page=${showPage}&size=100`,
    );
    layPhanTu("quan-tri-suat").replaceChildren();
    for (const s of shows) {
      const row = taoPhanTu("tr"),
        first = cell(row);
      first.append(
        taoPhanTu("strong", s.movieName),
        taoPhanTu("small", s.roomName),
      );
      const time = cell(row);
      time.append(
        taoPhanTu("strong", CineSnack.dinhDangSuat(s.startsAt)),
        taoPhanTu("small", "Đến " + CineSnack.dinhDangSuat(s.endsAt)),
      );
      cell(
        row,
        CineSnack.dinhDangTien(s.price) +
          " / " +
          CineSnack.dinhDangTien(Number(s.price) + Number(s.vipSurcharge || 0)),
      );
      cell(row).append(badge(nhan[s.status] || s.status, s.status === "HUY" ? "closed" : "neutral"));
      const actions = taoPhanTu("div", undefined, "workspace-row-actions");
      actions.append(
        button("Sửa", () => editShow(s), { editShow: s.id }),
        button(
          "Xóa",
          () =>
            confirmDelete(
              "Xóa suất chiếu này? Suất đã phát sinh vé sẽ được hệ thống bảo vệ.",
              async () => {
                await CineSnackApi.goi(`/api/admin/showtimes/${s.id}`, {
                  method: "DELETE",
                });
                await loadShows();
                notice("Đã cập nhật lịch chiếu.");
              },
            ),
          { deleteShow: s.id },
        ),
      );
      cell(row).append(actions);
      layPhanTu("quan-tri-suat").append(row);
    }
    layPhanTu("suat-so").textContent =
      `Trang ${showPage + 1} · ${shows.length} suất`;
    layPhanTu("suat-truoc").disabled = showPage === 0;
    layPhanTu("suat-sau").disabled = shows.length < 100;
    await timeline.refresh();
  }
  async function loadSnacks() {
    const data = await CineSnackApi.goi("/api/admin/snacks");
    const list = layPhanTu("bap-list");
    list.replaceChildren();
    if (!data.length) list.append(taoPhanTu("p", "Chưa có bắp nước."));
    for (const item of data) {
      const form = taoPhanTu("form", undefined, "workspace-form");
      const title = taoPhanTu(
        "strong",
        item.name + (item.kind === "COMBO" ? " · Combo" : " · Sản phẩm"),
      );
      const name = taoPhanTu("input");
      name.value = item.name;
      name.maxLength = 100;
      name.required = true;
      name.setAttribute("aria-label", "Tên " + item.name);
      const type = taoPhanTu("select");
      type.setAttribute("aria-label", "Loại món " + item.name);
      for (const [value, label] of [
        ["BAP", "Bắp"],
        ["NUOC", "Nước"],
        ["KHAC", "Khác"],
      ]) {
        const option = taoPhanTu("option", label);
        option.value = value;
        type.append(option);
      }
      type.value = item.productType || "KHAC";
      const price = taoPhanTu("input");
      price.type = "number";
      price.min = "0";
      price.step = "1";
      price.value = item.price;
      price.required = true;
      price.setAttribute("aria-label", "Giá " + item.name);
      const status = taoPhanTu("select");
      status.setAttribute("aria-label", "Tình trạng bán " + item.name);
      for (const [value, label] of [
        ["HOAT_DONG", "Đang bán"],
        ["NGUNG_HOAT_DONG", "Ngừng bán"],
      ]) {
        const option = taoPhanTu("option", label);
        option.value = value;
        status.append(option);
      }
      status.value = item.status;
      const save = taoPhanTu("button", "Lưu", "nut");
      save.type = "submit";
      const details = taoPhanTu("details", undefined, "ops-snack");
      const summary = taoPhanTu("summary");
      summary.append(title, taoPhanTu("span", CineSnack.dinhDangTien(item.price)),
        taoPhanTu("small", status.selectedOptions[0]?.textContent || item.status), taoPhanTu("span", "Sửa"));
      details.append(summary, form);
      for (const [label, input] of [["Tên món", name], ["Loại món", type], ["Giá bán (đồng)", price], ["Tình trạng", status]]) {
        if (input === type && item.kind !== "PRODUCT") continue;
        const fieldLabel = taoPhanTu("label", label);
        fieldLabel.append(input);
        form.append(fieldLabel);
      }
      form.append(save);
      form.addEventListener("submit", async (event) => {
        event.preventDefault();
        save.disabled = true;
        try {
          const body = { price: Number(price.value), status: status.value };
          if (name.value.trim() !== item.name) body.name = name.value.trim();
          if (item.kind === "PRODUCT" && type.value !== item.productType)
            body.productType = type.value;
          await CineSnackApi.goi(
            `/api/admin/snacks/${item.kind === "COMBO" ? "combos" : "products"}/${item.id}`,
            { method: "PUT", body },
          );
          notice("Đã cập nhật giá và tình trạng bán " + item.name + ".");
          await loadSnacks();
        } catch (err) {
          error(err);
        } finally {
          save.disabled = false;
        }
      });
      form.append(
        button("Xóa " + item.name, async () => {
          if (!confirm(`Xóa ${item.name} khỏi danh mục?`)) return;
          await CineSnackApi.goi(
            `/api/admin/snacks/${item.kind === "COMBO" ? "combos" : "products"}/${item.id}`,
            { method: "DELETE" },
          );
          notice("Đã xóa " + item.name + ".");
          await loadSnacks();
        }),
      );
      list.append(details);
    }
  }
  const snackCreate = layPhanTu("snack-create");
  function updateSnackTypeField() {
    const product = snackCreate.elements.kind.value === "products";
    layPhanTu("snack-product-type-row").hidden = !product;
    snackCreate.elements.productType.disabled = !product;
  }
  snackCreate.elements.kind.addEventListener("change", updateSnackTypeField);
  updateSnackTypeField();
  snackCreate.addEventListener("submit", async (event) => {
    event.preventDefault();
    const form = event.currentTarget;
    const submit = form.querySelector('[type="submit"]');
    submit.disabled = true;
    try {
      const kind = form.elements.kind.value;
      await CineSnackApi.goi(`/api/admin/snacks/${kind}`, {
        method: "POST",
        body: {
          name: form.elements.name.value.trim(),
          productType:
            kind === "products" ? form.elements.productType.value : null,
          price: Number(form.elements.price.value),
          status: form.elements.status.value,
        },
      });
      notice("Đã thêm vào danh mục bắp nước.");
      form.reset();
      updateSnackTypeField();
      await loadSnacks();
    } catch (err) {
      error(err);
    } finally {
      submit.disabled = false;
    }
  });
  layPhanTu("giao-scan-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    const submit = event.currentTarget.querySelector('[type="submit"]');
    if (submit.disabled) return;
    submit.disabled = true;
    const result = layPhanTu("giao-scan-result");
    result.textContent = "";
    try {
      const response = await CineSnackApi.goi("/api/admin/orders/snack-pickups/scan", {
        method: "POST", body: { code: layPhanTu("giao-scan-code").value.trim() },
      });
      result.textContent = `Đã giao ${response.deliveredLines} dòng món còn chờ của hóa đơn.`;
      layPhanTu("giao-scan-code").value = "";
      await loadPickups();
    } catch (err) {
      result.textContent = err.message;
    } finally {
      submit.disabled = false;
      layPhanTu("giao-scan-code").focus();
    }
  });
  const pickupList = layPhanTu("giao-list");
  const pickupDialog = layPhanTu("pickup-dialog");
  const pickupSlip = document.querySelector(".pickup-slip");
  const narrowCounter = matchMedia("(max-width: 700px)");
  let pickupOrders = new Map(), selectedPickup = null, pickupGeneration = 0;

  pickupDialog.addEventListener("close", () => {
    layPhanTu("pickup-slip-slot").append(pickupSlip);
    const selected = pickupList.querySelector('[aria-pressed="true"]');
    (selected || layPhanTu("giao-refresh")).focus();
  });
  narrowCounter.addEventListener("change", () => {
    if (!narrowCounter.matches && pickupDialog.open) pickupDialog.close();
  });

  function renderPickupSlip() {
    const slip = layPhanTu("giao-phieu");
    slip.replaceChildren();
    const items = pickupOrders.get(selectedPickup);
    if (!items) {
      slip.append(taoPhanTu("p", "Chọn một mã đơn để xem các món cần giao."));
      return;
    }
    slip.append(taoPhanTu("strong", selectedPickup, "pickup-code"));
    if (items[0].email) slip.append(taoPhanTu("small", items[0].email));
    const feedback = taoPhanTu("p");
    feedback.setAttribute("role", "status");
    for (const item of items) {
      const row = taoPhanTu("div", undefined, "pickup-line");
      const deliver = button("Giao món này", async () => {
        try {
          await CineSnackApi.goi(
            `/api/admin/orders/${encodeURIComponent(item.orderCode)}/snacks/${item.itemId}/deliver`,
            { method: "POST" },
          );
        } catch (err) {
          feedback.textContent = err.message;
          return;
        }
        const message = `Đã giao ${item.quantity} × ${item.name} của đơn ${item.orderCode}.`;
        notice(message);
        let result = message;
        try {
          await loadPickups();
        } catch (err) {
          const remaining = (pickupOrders.get(item.orderCode) || []).filter(line => line.itemId !== item.itemId);
          if (remaining.length) pickupOrders.set(item.orderCode, remaining);
          else pickupOrders.delete(item.orderCode);
          renderPickupCounter();
          result += " Chưa tải lại được hàng chờ: " + err.message;
          notice(result);
        }
        if (pickupDialog.open && selectedPickup === item.orderCode)
          layPhanTu("giao-phieu").querySelector('[role="status"]').textContent = result;
      });
      deliver.setAttribute("aria-label", `Giao món này ${item.quantity} × ${item.name}, đơn ${item.orderCode}`);
      row.append(taoPhanTu("strong", item.quantity + " ×", "pickup-quantity"), taoPhanTu("span", item.name), deliver);
      slip.append(row);
    }
    slip.append(feedback);
  }

  async function loadPickups() {
    const generation = ++pickupGeneration;
    const data = await CineSnackApi.goi("/api/admin/orders/snack-pickups");
    if (generation !== pickupGeneration) return;
    pickupOrders = new Map();
    for (const item of data) {
      if (!pickupOrders.has(item.orderCode)) pickupOrders.set(item.orderCode, []);
      pickupOrders.get(item.orderCode).push(item);
    }
    renderPickupCounter();
  }

  function renderPickupCounter() {
    const wasUsingSlip = pickupSlip.contains(document.activeElement) || pickupDialog.open;
    const focusedCode = document.activeElement.dataset.pickup;
    if (!pickupOrders.has(selectedPickup)) selectedPickup = null;
    pickupList.replaceChildren();
    for (const [code, items] of pickupOrders) {
      const choose = taoPhanTu("button", undefined, "pickup-order");
      choose.type = "button";
      choose.dataset.pickup = code;
      choose.setAttribute("aria-pressed", String(code === selectedPickup));
      choose.setAttribute("aria-controls", "giao-phieu");
      choose.append(taoPhanTu("strong", code), taoPhanTu("span", `${items.length} dòng món · Chờ nhận`));
      choose.addEventListener("click", () => {
        pickupList.querySelector('[aria-pressed="true"]')?.setAttribute("aria-pressed", "false");
        selectedPickup = code;
        choose.setAttribute("aria-pressed", "true");
        renderPickupSlip();
        if (narrowCounter.matches) {
          pickupDialog.append(pickupSlip);
          pickupDialog.showModal();
        }
        layPhanTu("pickup-slip-title").focus();
      });
      pickupList.append(choose);
    }
    if (!pickupOrders.size) pickupList.append(taoPhanTu("p", "Không có món đang chờ nhận."));
    renderPickupSlip();
    if (!selectedPickup && pickupDialog.open) pickupDialog.close();
    else if (wasUsingSlip) (selectedPickup ? layPhanTu("pickup-slip-title") : layPhanTu("giao-refresh")).focus();
    else if (focusedCode) (pickupList.querySelector(`[data-pickup="${CSS.escape(focusedCode)}"]`) || layPhanTu("giao-refresh")).focus();
  }
  const loadReport = () => CineSnackReport.load();
  async function loadStaff() {
    const data = await CineSnackApi.goi("/api/admin/staff");
    const list = layPhanTu("nhanvien-list");
    list.replaceChildren();
    if (!data.length) list.append(taoPhanTu("p", "Chưa có nhân viên."));
    for (const staff of data) {
      const row = taoPhanTu("article", undefined, "ops-person");
      const identity = taoPhanTu("div");
      identity.append(taoPhanTu("strong", staff.name), taoPhanTu("small", staff.email));
      const active = staff.status === "HOAT_DONG";
      row.append(identity, taoPhanTu("span", staff.role === "QUAN_TRI" ? "Quản trị" : "Nhân viên"),
        taoPhanTu("span", active ? "Đang hoạt động" : "Đã khóa", "workspace-badge " + (active ? "paid" : "closed")));
      if (staff.role === "NHAN_VIEN")
        row.append(
          button(
            active ? "Khóa tài khoản" : "Mở khóa",
            async () => {
              if (active && !confirm("Khóa tài khoản của " + staff.name + "?")) return;
              await CineSnackApi.goi(`/api/admin/staff/${staff.id}/status`, {
                method: "PUT",
                body: {
                  status:
                    staff.status === "HOAT_DONG"
                      ? "NGUNG_HOAT_DONG"
                      : "HOAT_DONG",
                },
              });
              await loadStaff();
            }, { destructive: active },
          ),
        );
      list.append(row);
    }
  }
  const cacMucQuanTri = {
    don: {
      title: "Đơn hàng",
      description:
        "Tra cứu đơn của user và guest, đối chiếu trước khi nhận tiền.",
      adminOnly: false,
      load: loadOrders,
    },
    phim: {
      title: "Phim tại rạp",
      description: "Quản lý danh mục, nội dung và trạng thái phim.",
      adminOnly: true,
      load: loadMovies,
    },
    suat: {
      title: "Lịch chiếu",
      description: "Xếp lịch theo phòng, giữ nguyên sơ đồ ghế có sẵn.",
      adminOnly: true,
      load: loadShows,
    },
    bap: {
      title: "Bắp nước",
      description: "Quản lý giá và tình trạng bán.",
      adminOnly: true,
      load: loadSnacks,
    },
    baocao: {
      title: "Báo cáo theo ngày",
      description: "Tổng hợp đơn, vé và bắp nước theo ngày thanh toán.",
      adminOnly: true,
      load: loadReport,
    },
    nhanvien: {
      title: "Nhân sự",
      description:
        "Tạo tài khoản vận hành theo vai trò và khóa hoặc mở khóa nhân viên.",
      adminOnly: true,
      load: loadStaff,
    },
    giao: {
      title: "Giao bắp nước",
      description: "Các món đã thanh toán đang chờ khách nhận.",
      adminOnly: false,
      load: loadPickups,
    },
  };

  async function openSection(id) {
    const muc = cacMucQuanTri[id];
    if (!user || (muc.adminOnly && !isAdmin())) return;
    section = id;
    layPhanTu("quan-tri-tieu-de").textContent = muc.title;
    const menu = layPhanTu("ops-menu");
    const hadMenuFocus = layPhanTu("ops-navigation").contains(document.activeElement);
    menu.textContent = layPhanTu("admin-role").textContent + " · " + muc.title;
    menu.setAttribute("aria-expanded", "false");
    if (hadMenuFocus && menu.offsetWidth) menu.focus();
    layPhanTu("quan-tri-phu").textContent = muc.description;
    document.querySelectorAll(".quan-tri-muc").forEach((panel) => {
      panel.hidden = panel.id !== "muc-" + id;
    });
    document.querySelectorAll("[data-muc]").forEach((button) => {
      button.setAttribute("aria-pressed", String(button.dataset.muc === id));
    });
    await muc.load();
  }
  function field(form, name) {
    return form.elements.namedItem(name);
  }
  layPhanTu("ops-menu").addEventListener("click", event => {
    const menu = event.currentTarget;
    menu.setAttribute("aria-expanded", String(menu.getAttribute("aria-expanded") !== "true"));
  });
  let posterPreviewUrl = null;
  function xoaPreviewTam() {
    if (posterPreviewUrl) URL.revokeObjectURL(posterPreviewUrl);
    posterPreviewUrl = null;
  }
  function hienPoster(src) {
    const preview = layPhanTu("poster-preview");
    preview.replaceChildren();
    preview.hidden = !src;
    if (src) {
      const image = document.createElement("img");
      image.alt = "Ảnh poster xem trước";
      image.src = src;
      preview.append(image);
    }
  }
  function kiemTraFilePoster(file) {
    if (!["image/jpeg", "image/png"].includes(file.type))
      throw new Error("Chỉ hỗ trợ ảnh JPG hoặc PNG.");
    if (!file.size || file.size > 5 * 1024 * 1024)
      throw new Error("Ảnh poster phải có dung lượng từ 1 byte đến 5 MB.");
  }
  field(layPhanTu("phim-form"), "posterFile").addEventListener(
    "change",
    (event) => {
      const input = event.currentTarget,
        file = input.files[0];
      xoaPreviewTam();
      layPhanTu("phim-form-error").textContent = "";
      if (!file) return hienPoster(input.form.dataset.posterUrl || null);
      try {
        kiemTraFilePoster(file);
        posterPreviewUrl = URL.createObjectURL(file);
        hienPoster(posterPreviewUrl);
      } catch (err) {
        input.value = "";
        hienPoster(input.form.dataset.posterUrl || null);
        layPhanTu("phim-form-error").textContent = err.message;
      }
    },
  );
  layPhanTu("phim-hop").addEventListener("close", () => {
    xoaPreviewTam();
    hienPoster(null);
  });
  function editMovie(movie) {
    const form = layPhanTu("phim-form");
    xoaPreviewTam();
    form.reset();
    layPhanTu("phim-form-error").textContent = "";
    field(form, "id").value = movie?.id || "";
    form.dataset.posterUrl = movie?.posterUrl || "";
    hienPoster(form.dataset.posterUrl || null);
    if (movie)
      for (const name of [
        "name",
        "genre",
        "description",
        "actors",
        "director",
        "trailerUrl",
        "duration",
        "ageRating",
        "status",
      ])
        field(form, name).value = movie[name] ?? "";
    layPhanTu("phim-form-title").textContent = movie ? "Sửa phim" : "Thêm phim";
    layPhanTu("phim-hop").showModal();
  }
  async function editShow(show) {
    const form = layPhanTu("suat-form");
    form.reset();
    layPhanTu("suat-form-error").textContent = "";
    [movieOptions, rooms] = await Promise.all([
      CineSnackApi.goi("/api/admin/movies?size=100"),
      CineSnackApi.goi("/api/admin/rooms"),
    ]);
    const movieSelect = field(form, "movieId"),
      roomSelect = field(form, "roomId");
    movieSelect.replaceChildren();
    roomSelect.replaceChildren();
    for (const p of movieOptions) {
      const option = taoPhanTu("option", p.name);
      option.value = p.id;
      movieSelect.append(option);
    }
    if (show && !movieOptions.some((p) => p.id === show.movieId)) {
      const option = taoPhanTu("option", show.movieName);
      option.value = show.movieId;
      movieSelect.append(option);
    }
    for (const r of rooms) {
      const option = taoPhanTu("option", r.name);
      option.value = r.id;
      roomSelect.append(option);
    }
    if (show && !rooms.some((r) => r.id === show.roomId)) {
      const option = taoPhanTu("option", show.roomName);
      option.value = show.roomId;
      roomSelect.append(option);
    }
    field(form, "id").value = show?.id || "";
    if (show) {
      field(form, "movieId").value = show.movieId;
      field(form, "roomId").value = show.roomId;
      field(form, "startsAt").value = show.startsAt.slice(0, 16);
      field(form, "price").value = show.price;
      field(form, "vipSurcharge").value = show.vipSurcharge || 0;
      field(form, "status").value = show.status;
    }
    CineSnackShowPricing.setMode(form, show?.pricingMode || "MANUAL");
    layPhanTu("suat-form-title").textContent = show
      ? "Sửa suất chiếu"
      : "Thêm suất chiếu";
    layPhanTu("suat-hop").showModal();
  }
  function confirmDelete(message, action) {
    layPhanTu("confirm-message").textContent = message;
    layPhanTu("confirm-error").textContent = "";
    layPhanTu("confirm-delete").onclick = async () => {
      const b = layPhanTu("confirm-delete");
      b.disabled = true;
      try {
        await action();
        layPhanTu("confirm-hop").close();
      } catch (err) {
        layPhanTu("confirm-error").textContent = err.message;
      } finally {
        b.disabled = false;
      }
    };
    layPhanTu("confirm-hop").showModal();
  }
  layPhanTu("confirm-cancel").addEventListener("click", () =>
    layPhanTu("confirm-hop").close(),
  );
  layPhanTu("phim-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    if (!isAdmin()) return;
    const form = event.currentTarget,
      submit = form.querySelector("button[type=submit]");
    if (submit.disabled || !form.reportValidity()) return;
    const nhanBanDau = submit.textContent;
    submit.disabled = true;
    submit.textContent = "Đang lưu phim…";
    submit.setAttribute("aria-busy", "true");
    layPhanTu("phim-form-error").textContent = "";
    try {
      const id = field(form, "id").value;
      const body = Object.fromEntries(
        ["name", "genre", "description", "actors", "director", "trailerUrl", "ageRating", "status"].map(
          (name) => [name, field(form, name).value.trim()],
        ),
      );
      body.duration = Number(field(form, "duration").value);
      body.posterUrl = form.dataset.posterUrl || null;
      const file = field(form, "posterFile").files[0];
      if (file) kiemTraFilePoster(file);
      const duLieu = new FormData();
      duLieu.append(
        "movie",
        new Blob([JSON.stringify(body)], { type: "application/json" }),
      );
      if (file) duLieu.append("poster", file);
      await CineSnackApi.goi("/api/admin/movies" + (id ? "/" + id : ""), {
        method: id ? "PUT" : "POST",
        body: duLieu,
      });
      layPhanTu("phim-hop").close();
      await loadMovies();
      notice("Đã lưu phim.");
    } catch (err) {
      layPhanTu("phim-form-error").textContent = err.message;
    } finally {
      submit.disabled = false;
      submit.textContent = nhanBanDau;
      submit.removeAttribute("aria-busy");
    }
  });
  layPhanTu("suat-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    if (!isAdmin()) return;
    const form = event.currentTarget,
      submit = form.querySelector("button[type=submit]");
    if (submit.disabled || !form.reportValidity()) return;
    const nhanBanDau = submit.textContent;
    submit.disabled = true;
    submit.textContent = "Đang lưu suất chiếu…";
    submit.setAttribute("aria-busy", "true");
    layPhanTu("suat-form-error").textContent = "";
    try {
      const id = field(form, "id").value,
        raw = field(form, "startsAt").value;
      const body = {
        movieId: Number(field(form, "movieId").value),
        roomId: Number(field(form, "roomId").value),
        startsAt: raw + (raw.length === 16 ? ":00" : "") + "+07:00",
        ...CineSnackShowPricing.request(form),
        status: field(form, "status").value,
      };
      await CineSnackApi.goi("/api/admin/showtimes" + (id ? "/" + id : ""), {
        method: id ? "PUT" : "POST",
        body,
      });
      layPhanTu("suat-hop").close();
      await loadShows();
      notice("Đã lưu suất chiếu.");
    } catch (err) {
      layPhanTu("suat-form-error").textContent = err.message;
    } finally {
      submit.disabled = false;
      submit.textContent = nhanBanDau;
      submit.removeAttribute("aria-busy");
    }
  });
  layPhanTu("them-phim").addEventListener("click", () => {
    if (isAdmin()) editMovie(null);
  });
  layPhanTu("them-suat").addEventListener("click", () => {
    if (isAdmin()) editShow(null).catch(error);
  });
  layPhanTu("giao-refresh").addEventListener("click", () =>
    loadPickups().catch(error),
  );
  layPhanTu("nhanvien-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    const form = event.currentTarget;
    const submit = form.querySelector('[type="submit"]');
    submit.disabled = true;
    try {
      await CineSnackApi.goi("/api/admin/staff", {
        method: "POST",
        body: {
          name: field(form, "name").value.trim(),
          email: field(form, "email").value.trim(),
          password: field(form, "password").value,
          role: field(form, "role").value,
        },
      });
      form.reset();
      await loadStaff();
      notice("Đã tạo tài khoản vận hành.");
    } catch (err) {
      error(err);
    } finally {
      submit.disabled = false;
    }
  });
  document
    .querySelectorAll("[data-muc]")
    .forEach((b) =>
      b.addEventListener("click", () =>
        openSection(b.dataset.muc).catch(error),
      ),
    );
  layPhanTu("don-search-form").addEventListener("submit", (event) => {
    event.preventDefault();
    page = 0;
    loadOrders().catch(error);
  });
  let searchTimer;
  layPhanTu("quan-tri-don-tim").addEventListener("input", () => {
    clearTimeout(searchTimer);
    searchTimer = setTimeout(() => {
      page = 0;
      loadOrders().catch(error);
    }, 250);
  });
  layPhanTu("don-trang-thai").addEventListener("change", () => {
    page = 0;
    capNhatBoLocNhanh();
    loadOrders().catch(error);
  });
  function capNhatBoLocNhanh() {
    document.querySelectorAll("[data-loc-don]").forEach((button) => {
      button.setAttribute(
        "aria-pressed",
        String(button.dataset.locDon === layPhanTu("don-trang-thai").value),
      );
    });
  }
  document.querySelectorAll("[data-loc-don]").forEach((button) => {
    button.addEventListener("click", () => {
      layPhanTu("don-trang-thai").value = button.dataset.locDon;
      page = 0;
      capNhatBoLocNhanh();
      loadOrders().catch(error);
    });
  });
  layPhanTu("admin-refresh").addEventListener("click", () =>
    loadOrders().catch(error),
  );
  setInterval(() => {
    if (
      section === "don" &&
      layPhanTu("don-trang-thai").value === "CHO_THANH_TOAN" &&
      !document.hidden &&
      user
    )
      loadOrders().catch(error);
  }, 10000);
  for (const [id, delta] of [
    ["admin-truoc", -1],
    ["admin-sau", 1],
  ])
    layPhanTu(id).addEventListener("click", () => {
      page = Math.max(0, page + delta);
      loadOrders().catch(error);
    });
  for (const [id, delta] of [
    ["phim-truoc", -1],
    ["phim-sau", 1],
  ])
    layPhanTu(id).addEventListener("click", () => {
      moviePage = Math.max(0, moviePage + delta);
      loadMovies().catch(error);
    });
  for (const [id, delta] of [
    ["suat-truoc", -1],
    ["suat-sau", 1],
  ])
    layPhanTu(id).addEventListener("click", () => {
      showPage = Math.max(0, showPage + delta);
      loadShows().catch(error);
    });
  layPhanTu("tra-don-form").addEventListener("submit", (event) => {
    event.preventDefault();
    showOrder(layPhanTu("tra-ma-don").value.trim()).catch(error);
  });
  layPhanTu("admin-logout").addEventListener("click", async () => {
    layPhanTu("admin-logout").disabled = true;
    try {
      await CineSnackApi.dangXuat();
      location.href = "index.html";
    } catch (err) {
      error(err);
      layPhanTu("admin-logout").disabled = false;
    }
  });
  try {
    user = await CineSnackApi.taiKhoan();
    if (!user) {
      layPhanTu("admin-login").hidden = false;
      return;
    }
    if (!["NHAN_VIEN", "QUAN_TRI"].includes(user.role)) {
      error(new Error("Tài khoản không có quyền vào không gian làm việc."));
      return;
    }
    layPhanTu("admin-role").textContent = isAdmin()
      ? "Quản trị viên"
      : "Nhân viên quầy";
    layPhanTu("admin-email").textContent = user.email;
    layPhanTu("admin-logout").hidden = false;
    layPhanTu("quan-tri-ngay").textContent = new Intl.DateTimeFormat("vi-VN", {
      timeZone: "Asia/Ho_Chi_Minh",
    }).format(new Date());
    document
      .querySelectorAll("[data-admin-only]")
      .forEach((node) => (node.hidden = !isAdmin()));
    document
      .querySelectorAll("[data-staff-only]")
      .forEach((node) => (node.hidden = user.role !== "NHAN_VIEN"));
    layPhanTu("admin-workspace").hidden = false;
    await openSection(isAdmin() ? "phim" : "don");
    const code = new URLSearchParams(location.search).get("code");
    if (code && /^[A-Za-z0-9]{1,30}$/.test(code)) {
      await openSection("don");
      await showOrder(code);
    }
  } catch (err) {
    error(err);
  }
})();
