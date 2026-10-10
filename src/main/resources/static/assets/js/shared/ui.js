"use strict";
CineSnack.renderSummary = function (cart) {
  const C = CineSnack;
  const movie = C.movies.find((m) => m.id === cart.movie);
  const amount = C.totals(cart);
  document.querySelector("#summary-title").textContent = movie.title;
  const poster = document.querySelector("#summary-poster");
  poster.src = "assets/images/movies/" + movie.image;
  poster.alt = "Poster " + movie.title;
  document.querySelector("#summary-when").textContent =
    `${cart.time} · ${C.displayDate(cart.date)}`;
  document.querySelector("#summary-seats").textContent =
    [...cart.seats]
      .sort(
        (a, b) =>
          a[0].localeCompare(b[0]) || Number(a.slice(1)) - Number(b.slice(1)),
      )
      .join(", ") || "Chưa chọn ghế";
  for (const key of ["tickets", "snacks", "total"])
    document.querySelector("#summary-" + key).textContent = C.money(
      amount[key],
    );
};
