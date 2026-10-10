"use strict";
(function () {
  const C = CineSnack;
  let cart;
  try {
    cart = JSON.parse(sessionStorage.getItem(C.storageKey));
  } catch {
    cart = null;
  }
  if (!C.validCart(cart)) {
    document.querySelector("#empty-checkout").hidden = false;
    return;
  }
  document.querySelector("#checkout-content").hidden = false;
  C.renderSummary(cart);
  document.querySelector("#edit-booking").href =
    "dat-ve.html?movie=" + cart.movie;
  const form = document.querySelector("#checkout-form");
  const name = document.querySelector("#customer-name");
  name.addEventListener("input", () => name.setCustomValidity(""));
  form.addEventListener("submit", (event) => {
    event.preventDefault();
    if (name.value.trim().length < 2) {
      name.setCustomValidity("Vui lòng nhập họ tên có ít nhất 2 ký tự.");
      name.reportValidity();
      return;
    }
    if (!C.validCart(cart)) {
      document.querySelector("#checkout-error").textContent =
        "Suất chiếu trong đơn không còn hợp lệ. Quay lại để chọn suất mới.";
      return;
    }
    try {
      sessionStorage.removeItem(C.storageKey);
    } catch {
      document.querySelector("#checkout-error").textContent =
        "Không thể kết thúc phiên demo. Hãy cho phép lưu trữ rồi thử lại.";
      return;
    }
    const movie = C.movies.find((m) => m.id === cart.movie);
    const method = {
      qr: "Chuyển khoản / QR",
      wallet: "Ví điện tử",
      counter: "Tại quầy",
    }[form.elements.payment.value];
    const details = [
      ["Phim", movie.title],
      ["Suất chiếu", cart.time + " · " + C.displayDate(cart.date)],
      ["Ghế", cart.seats.join(", ")],
      [
        "Bắp & nước",
        C.combos
          .filter((c) => cart.combos[c.id])
          .map((c) => cart.combos[c.id] + " × " + c.name)
          .join(", ") || "Không chọn",
      ],
      ["Phương thức", method + " (demo)"],
      ["Tổng minh họa", C.money(C.totals(cart).total)],
    ];
    const container = document.querySelector("#success-details");
    details.forEach(([label, value]) => {
      const row = document.createElement("div"),
        dt = document.createElement("dt"),
        dd = document.createElement("dd");
      dt.textContent = label;
      dd.textContent = value;
      row.append(dt, dd);
      container.append(row);
    });
    form.reset();
    document.querySelector("#checkout-content").hidden = true;
    document.querySelectorAll(".progress-steps li").forEach((li, i) => {
      li.classList.toggle("current", i === 2);
      li.classList.toggle("complete", i < 2);
      li.removeAttribute("aria-current");
      if (i === 2) li.setAttribute("aria-current", "step");
    });
    const success = document.querySelector("#checkout-success");
    success.hidden = false;
    success.focus();
  });
})();
