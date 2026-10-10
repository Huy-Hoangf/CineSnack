"use strict";
(function () {
  const C = CineSnack;
  const requested = new URLSearchParams(location.search).get("movie");
  const initialMovie =
    C.movies.find((m) => m.id === requested && m.status === "now") ||
    C.movies[0];
  let cart = {
    movie: initialMovie.id,
    date: C.dates()[0],
    time: C.times[3],
    seats: [],
    combos: { solo: 0, duo: 0 },
  };
  try {
    const saved = JSON.parse(sessionStorage.getItem(C.storageKey));
    if (C.validCart(saved) && (!requested || requested === saved.movie))
      cart = saved;
  } catch {
    /* A new demo booking can still be started when storage is unavailable. */
  }
  const movieSelect = document.querySelector("#booking-movie");
  movieSelect.innerHTML = C.movies
    .filter((m) => m.status === "now")
    .map((m) => `<option value="${m.id}">${m.title}</option>`)
    .join("");
  movieSelect.value = cart.movie;
  document.querySelector("#date-options").innerHTML = C.dates()
    .map(
      (date, i) =>
        `<label class="choice-label"><input type="radio" name="date" value="${date}" ${date === cart.date ? "checked" : ""}><span><small>${i === 0 ? "Hôm nay" : i === 1 ? "Ngày mai" : "Ngày kia"}</small>${C.displayDate(date).slice(0, 5)}</span></label>`,
    )
    .join("");
  document.querySelector("#time-options").innerHTML = C.times
    .map(
      (time) =>
        `<label class="choice-label"><input type="radio" name="time" value="${time}" ${time === cart.time ? "checked" : ""}><span>${time}</span></label>`,
    )
    .join("");
  function update() {
    C.renderSummary(cart);
    document.querySelector("#continue-booking").disabled = !cart.seats.length;
    document.querySelector("#continue-hint").textContent = cart.seats.length
      ? `${cart.seats.length} ghế đã chọn. Bạn có thể thêm combo bên dưới.`
      : "Chọn ít nhất 1 ghế để tiếp tục.";
    document.querySelector("#booking-error").textContent = "";
  }
  function renderSeats() {
    const occupied = C.occupied(cart.movie, cart.date, cart.time);
    document.querySelector("#seat-rows").innerHTML = [..."ABCDEF"]
      .map(
        (row) =>
          `<div class="seat-row"><span aria-hidden="true">${row}</span>${Array.from(
            { length: 10 },
            (_, i) => {
              const seat = row + (i + 1),
                taken = occupied.has(seat),
                vip = "DEF".includes(row);
              return `<button type="button" class="seat ${vip ? "vip" : ""}" data-seat="${seat}" ${taken ? "disabled" : ""} aria-pressed="${cart.seats.includes(seat)}" aria-label="Ghế ${seat}, ${vip ? "VIP" : "thường"}, ${C.money(C.seatPrice(seat))}${taken ? ", đã đặt" : ""}">${i + 1}</button>`;
            },
          ).join("")}</div>`,
      )
      .join("");
  }
  function changeShow() {
    cart.movie = movieSelect.value;
    cart.date = document.querySelector("input[name=date]:checked").value;
    cart.time = document.querySelector("input[name=time]:checked").value;
    cart.seats = [];
    document.querySelector("#seat-error").textContent = "";
    document.querySelector("#selection-note").textContent =
      "Đã đổi suất chiếu. Vui lòng chọn lại ghế.";
    renderSeats();
    update();
  }
  movieSelect.addEventListener("change", changeShow);
  document
    .querySelectorAll("#date-options, #time-options")
    .forEach((group) => group.addEventListener("change", changeShow));
  document.querySelector("#seat-rows").addEventListener("click", (event) => {
    const button = event.target.closest("[data-seat]");
    if (!button || button.disabled) return;
    const seat = button.dataset.seat;
    document.querySelector("#seat-error").textContent = "";
    if (cart.seats.includes(seat))
      cart.seats = cart.seats.filter((s) => s !== seat);
    else if (cart.seats.length >= 10) {
      document.querySelector("#seat-error").textContent =
        "Bạn chỉ có thể chọn tối đa 10 ghế mỗi đơn.";
      return;
    } else cart.seats.push(seat);
    button.setAttribute("aria-pressed", String(cart.seats.includes(seat)));
    update();
  });
  document.querySelector("#combo-options").innerHTML = C.combos
    .map(
      (c) =>
        `<article class="combo-row"><span class="combo-icon" aria-hidden="true">▥</span><div class="combo-description"><h3>${c.name}</h3><p>${c.detail}</p><strong>${C.money(c.price)}</strong></div><div class="quantity-control" role="group" aria-label="Số lượng ${c.name}"><button type="button" data-combo="${c.id}" data-change="-1" aria-label="Giảm ${c.name}">−</button><output id="qty-${c.id}" aria-label="Số lượng ${c.name}" aria-live="polite">0</output><button type="button" data-combo="${c.id}" data-change="1" aria-label="Tăng ${c.name}">+</button></div></article>`,
    )
    .join("");
  function updateQuantities() {
    C.combos.forEach((c) => {
      document.querySelector("#qty-" + c.id).value = cart.combos[c.id];
      document.querySelector(
        `[data-combo="${c.id}"][data-change="-1"]`,
      ).disabled = cart.combos[c.id] === 0;
      document.querySelector(
        `[data-combo="${c.id}"][data-change="1"]`,
      ).disabled = cart.combos[c.id] === 10;
    });
  }
  document
    .querySelector("#combo-options")
    .addEventListener("click", (event) => {
      const button = event.target.closest("[data-combo]");
      if (!button || button.disabled) return;
      const next =
        cart.combos[button.dataset.combo] + Number(button.dataset.change);
      if (next < 0 || next > 10) return;
      cart.combos[button.dataset.combo] = next;
      updateQuantities();
      update();
    });
  document.querySelector("#continue-booking").addEventListener("click", () => {
    if (!C.validCart(cart)) {
      document.querySelector("#booking-error").textContent =
        "Lựa chọn không còn hợp lệ. Tải lại trang và chọn suất chiếu mới.";
      return;
    }
    try {
      sessionStorage.setItem(C.storageKey, JSON.stringify(cart));
      location.href = "thanh-toan.html";
    } catch {
      document.querySelector("#booking-error").textContent =
        "Trình duyệt không cho phép lưu lựa chọn trong phiên. Hãy bật lưu trữ của trang rồi thử lại.";
    }
  });
  renderSeats();
  updateQuantities();
  update();
})();
