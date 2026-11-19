"use strict";
(function (root) {
  function start({ expiresAt, serverTime, requestedAt, onTick = () => {}, onExpire = () => {},
    now = () => Date.now(), schedule = (fn) => setInterval(fn, 1000),
    cancel = (id) => clearInterval(id) }) {
    const deadline = Date.parse(expiresAt);
    const server = serverTime == null ? null : Date.parse(serverTime);
    if (!Number.isFinite(deadline) || (server !== null && !Number.isFinite(server)))
      throw new Error("Không xác định được thời hạn giữ chỗ.");
    const receivedAt = now();
    // Charge the full round trip conservatively; a delayed response must not
    // grant extra hold time when synchronizing with the server clock.
    const latency = requestedAt == null ? 0 : Math.max(0, receivedAt - requestedAt);
    const offset = server === null ? 0 : server + latency - receivedAt;
    let timer, stopped = false;
    function stop() {
      stopped = true;
      if (timer !== undefined) cancel(timer);
    }
    function refresh() {
      if (stopped) return false;
      const seconds = Math.max(0, Math.ceil((deadline - now() - offset) / 1000));
      onTick(`${String(Math.floor(seconds / 60)).padStart(2, "0")}:${String(seconds % 60).padStart(2, "0")}`);
      if (!seconds) {
        stop();
        onExpire();
        return false;
      }
      return true;
    }
    if (refresh()) timer = schedule(refresh);
    return { refresh, isActive: refresh, stop };
  }
  const api = { start };
  if (typeof module !== "undefined" && module.exports) module.exports = api;
  if (root) root.CineSnackDeadline = api;
})(typeof window !== "undefined" ? window : null);
