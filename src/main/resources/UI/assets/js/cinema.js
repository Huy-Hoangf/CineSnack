(function () {
  "use strict";
  const movies = [
    {
      id: "dune",
      title: "Dune: Hành tinh cát – Phần hai",
      genre: "Khoa học viễn tưởng, Phiêu lưu",
      duration: 166,
      age: "T16",
      year: 2024,
      image: "dune.jpg",
      status: "now",
      director: "Denis Villeneuve",
      description:
        "Paul Atreides tiếp tục hành trình trên hành tinh Arrakis, nơi những lựa chọn của anh có thể thay đổi số phận của cả vũ trụ.",
    },
    {
      id: "inside-out",
      title: "Những mảnh ghép cảm xúc 2",
      genre: "Hoạt hình, Gia đình",
      duration: 96,
      age: "P",
      year: 2024,
      image: "inside-out.jpg",
      status: "now",
      director: "Kelsey Mann",
      description:
        "Riley bước vào tuổi mới và những cảm xúc mới cũng xuất hiện. Một cuộc phiêu lưu đầy màu sắc về việc lớn lên và thấu hiểu chính mình.",
    },
    {
      id: "kung-fu-panda",
      title: "Kung Fu Panda 4",
      genre: "Hoạt hình, Hành động",
      duration: 94,
      age: "P",
      year: 2024,
      image: "kung-fu-panda.jpg",
      status: "now",
      director: "Mike Mitchell",
      description:
        "Po bước vào một nhiệm vụ mới, gặp gỡ những người bạn mới và tìm kiếm ý nghĩa của việc trở thành một người dẫn đường.",
    },
    {
      id: "godzilla",
      title: "Godzilla x Kong: Đế chế mới",
      genre: "Hành động, Phiêu lưu",
      duration: 115,
      age: "T13",
      year: 2024,
      image: "godzilla.jpg",
      status: "now",
      director: "Adam Wingard",
      description:
        "Hai người khổng lồ đối mặt với một mối đe dọa mới. Hành trình khám phá những bí mật sâu dưới lòng Trái Đất bắt đầu.",
    },
    {
      id: "spider-man",
      title: "Người Nhện: Du hành vũ trụ nhện",
      genre: "Hoạt hình, Phiêu lưu",
      duration: 140,
      age: "T13",
      year: 2023,
      image: "spider-man.jpg",
      status: "soon",
      director: "Joaquim Dos Santos và cộng sự",
      description:
        "Miles Morales bước qua nhiều thế giới, gặp những Người Nhện khác và tìm kiếm con đường của riêng mình.",
    },
    {
      id: "interstellar",
      title: "Interstellar: Hố đen tử thần",
      genre: "Khoa học viễn tưởng",
      duration: 169,
      age: "T13",
      year: 2014,
      image: "interstellar.jpg",
      status: "soon",
      director: "Christopher Nolan",
      description:
        "Một nhóm phi hành gia đi qua những giới hạn của không gian và thời gian trong hành trình tìm kiếm tương lai cho nhân loại.",
    },
  ];
  const times = ["10:00", "13:15", "16:30", "19:30", "21:45"];
  const combos = [
    {
      id: "solo",
      name: "Combo Một Mình Vui",
      detail: "1 bắp vừa + 1 nước ngọt",
      price: 59000,
    },
    {
      id: "duo",
      name: "Combo Hẹn Hò",
      detail: "1 bắp lớn + 2 nước ngọt",
      price: 99000,
    },
  ];
  function normalize(value) {
    return value
      .normalize("NFD")
      .replace(/[\u0300-\u036f]/g, "")
      .replace(/đ/gi, "d")
      .toLowerCase()
      .trim();
  }
  function dates(now = new Date()) {
    return [0, 1, 2].map((offset) => {
      const date = new Date(
        now.getFullYear(),
        now.getMonth(),
        now.getDate() + offset,
      );
      return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
    });
  }
  function displayDate(value) {
    const [year, month, day] = value.split("-");
    return `${day}/${month}/${year}`;
  }
  function seatPrice(seat) {
    return "DEF".includes(seat[0]) ? 95000 : 75000;
  }
  function occupied(movie, date, time) {
    const shift =
      (movie.length + Number(date.slice(-2)) + times.indexOf(time)) % 3;
    return new Set(
      ["A", "B", "C", "D", "E", "F"].flatMap((row, i) => [
        row + (3 + shift + (i % 2)),
        row + (7 + (i % 2)),
      ]),
    );
  }
  function totals(cart) {
    const tickets = cart.seats.reduce((sum, seat) => sum + seatPrice(seat), 0);
    const snacks = combos.reduce(
      (sum, combo) => sum + combo.price * cart.combos[combo.id],
      0,
    );
    return { tickets, snacks, total: tickets + snacks };
  }
  function validCart(cart, now = new Date()) {
    if (
      !cart ||
      !movies.some((m) => m.id === cart.movie && m.status === "now") ||
      !dates(now).includes(cart.date) ||
      !times.includes(cart.time)
    )
      return false;
    if (
      !Array.isArray(cart.seats) ||
      cart.seats.length < 1 ||
      cart.seats.length > 10 ||
      new Set(cart.seats).size !== cart.seats.length
    )
      return false;
    if (
      !cart.seats.every(
        (s) =>
          typeof s === "string" &&
          /^[A-F](?:[1-9]|10)$/.test(s) &&
          !occupied(cart.movie, cart.date, cart.time).has(s),
      )
    )
      return false;
    return (
      !!cart.combos &&
      combos.every(
        (c) =>
          Number.isInteger(cart.combos[c.id]) &&
          cart.combos[c.id] >= 0 &&
          cart.combos[c.id] <= 10,
      )
    );
  }
  function money(value) {
    return new Intl.NumberFormat("vi-VN", {
      style: "currency",
      currency: "VND",
    }).format(value);
  }
  const api = {
    movies,
    times,
    combos,
    normalize,
    dates,
    displayDate,
    seatPrice,
    occupied,
    totals,
    validCart,
    money,
    storageKey: "cinesnack-demo-cart",
  };
  if (typeof module !== "undefined" && module.exports) module.exports = api;
  else window.CineSnack = api;
})();
