"use strict";
CineSnack.hienTomTat = function (g) {
  const tien = CineSnack.tinhTien(g);
  document.querySelector("#tom-tat-tieu-de").textContent =
    g.phim?.name || "Đơn bắp nước";
  const anh = document.querySelector("#tom-tat-anh");
  anh.src = CineSnack.poster(g.phim?.posterUrl);
  anh.alt = g.phim?.name ? "Poster " + g.phim.name : "CineSnack";
  document.querySelector("#tom-tat-suat").textContent = g.suat
    ? CineSnack.dinhDangSuat(g.suat.startsAt) + " · " + g.suat.roomName
    : "Không đặt vé";
  document.querySelector("#tom-tat-ghe").textContent = g.ghe.length
    ? g.ghe.map((v) => v.code).join(", ")
    : "Chưa chọn ghế";
  for (const [key, id] of [
    ["tienVe", "tienVe"],
    ["tienBap", "tienBap"],
    ["tong", "tong"],
  ])
    document.querySelector("#tom-tat-" + id).textContent =
      CineSnack.dinhDangTien(tien[key]);
};
