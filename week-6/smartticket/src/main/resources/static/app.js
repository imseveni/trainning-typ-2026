const state = {
  events: [], page: 1, pageSize: 8, totalPages: 0, totalElements: 0,
  selectedEventId: null, showtimes: [], selectedShowtimeId: null,
  seatMap: [], selectedSeatIds: new Set(), currentHoldId: null, user: null,
};

const $ = (selector) => document.querySelector(selector);
const money = new Intl.NumberFormat("vi-VN", { maximumFractionDigits: 0 });
const dateTime = new Intl.DateTimeFormat("vi-VN", { dateStyle: "medium", timeStyle: "short" });

function apiBase() {
  const value = $("#apiBase").value.trim();
  return (value || window.location.origin).replace(/\/$/, "");
}

// Role nằm trong JWT được server dùng để phân quyền. `/api/auth/me` đọc role
// mới nhất từ DB, nên sau khi đổi role bằng SQL hai giá trị có thể chưa khớp.
function getTokenRoles(token) {
  try {
    const payload = token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/");
    const paddedPayload = payload.padEnd(Math.ceil(payload.length / 4) * 4, "=");
    const claims = JSON.parse(atob(paddedPayload));
    return Array.isArray(claims.roles) ? claims.roles : [];
  } catch {
    return [];
  }
}

async function api(path, options = {}) {
  const { skipAuth = false, authToken, ...requestOptions } = options;
  const storedToken = localStorage.getItem("smartticket-access-token");
  const token = authToken ?? storedToken;
  const response = await fetch(apiBase() + path, {
    ...requestOptions,
    headers: {
      ...(requestOptions.body ? { "Content-Type": "application/json" } : {}),
      ...(!skipAuth && token ? { Authorization: `Bearer ${token}` } : {}),
      ...(requestOptions.headers || {}),
    },
  });
  const text = await response.text();
  let data = null;
  if (text) {
    try { data = JSON.parse(text); } catch { data = text; }
  }
  if (!response.ok) {
    if (response.status === 401 && token && !skipAuth && token === storedToken) {
      localStorage.removeItem("smartticket-access-token");
      state.user = null;
      updateAuthUi();
    }
    const fallbackMessage = response.status === 401
      ? "Chưa xác thực hoặc JWT đã hết hạn. Hãy đăng nhập lại."
      : response.status === 403
        ? "Backend từ chối quyền truy cập. Đây không phải lỗi tranh chấp ghế; hãy kiểm tra quyền/token và phiên bản backend đang chạy."
        : "Request failed";
    const message = data?.message || data?.detail || data?.error || data?.failureReason || (typeof data === "string" ? data : fallbackMessage);
    const error = new Error(response.status + " · " + message);
    error.status = response.status;
    throw error;
  }
  return data;
}

function toast(title, message = "", type = "success") {
  const item = document.createElement("div");
  item.className = `toast ${type === "error" ? "error" : ""}`;
  item.innerHTML = `<b>${escapeHtml(title)}</b>${escapeHtml(message)}`;
  $("#toastRegion").append(item);
  window.setTimeout(() => item.remove(), 4200);
}

function escapeHtml(value) {
  return String(value ?? "").replace(/[&<>"']/g, (char) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[char]);
}

function setConnection(connected) {
  $("#connectionLight").classList.toggle("connected", connected);
  $("#connectionLabel").textContent = connected ? "Backend sẵn sàng" : "Chưa kết nối";
  $("#apiHostLabel").textContent = apiBase().replace(/^https?:\/\//, "");
}

async function checkConnection(showMessage = false) {
  try {
    await api("/api/events?page=1&size=1");
    setConnection(true);
    if (showMessage) toast("Kết nối thành công", `API đang phản hồi tại ${apiBase()}`);
    return true;
  } catch (error) {
    setConnection(false);
    if (showMessage) toast("Không kết nối được API", error.message, "error");
    return false;
  }
}

function navigate(view) {
  document.querySelectorAll(".view").forEach((section) => section.classList.toggle("active", section.id === `view-${view}`));
  document.querySelectorAll(".nav-item").forEach((button) => button.classList.toggle("active", button.dataset.view === view));
  const label = { overview: "Tổng quan", events: "Sự kiện", showtimes: "Suất diễn", seats: "Sơ đồ & giữ ghế", holds: "Lượt giữ & booking" }[view];
  $("#crumbCurrent").textContent = label;
  history.replaceState(null, "", `#${view}`);
  if (view === "showtimes") syncEventSelectors();
  if (view === "seats") syncEventSelectors();
}

function showError(error, context = "Thao tác thất bại") {
  toast(context, error.message || String(error), "error");
}

function setSelectOptions(select, items, placeholder, selectedValue) {
  const current = selectedValue ?? select.value;
  select.innerHTML = `<option value="">${escapeHtml(placeholder)}</option>` + items.map((item) => `<option value="${item.id}">${escapeHtml(item.label)}</option>`).join("");
  if (items.some((item) => String(item.id) === String(current))) select.value = String(current);
}

function syncEventSelectors() {
  const items = state.events.map((event) => ({ id: event.eventId, label: `${event.title} · #${event.eventId}` }));
  setSelectOptions($("#showtimeEventSelect"), items, "Chọn một sự kiện…", state.selectedEventId);
  setSelectOptions($("#seatEventSelect"), items, "Chọn một sự kiện…", state.selectedEventId);
}

async function loadEvents() {
  const data = await api(`/api/events?page=${state.page}&size=${state.pageSize}`);
  state.events = data.items || [];
  state.totalPages = data.totalPages || 0;
  state.totalElements = data.totalElements || 0;
  if (!state.events.some((event) => event.eventId === state.selectedEventId)) state.selectedEventId = state.events[0]?.eventId ?? null;
  $("#statEvents").textContent = String(state.totalElements);
  $("#eventPageLabel").textContent = state.totalPages ? `Trang ${data.page} / ${state.totalPages} · ${state.totalElements} sự kiện` : "Chưa có sự kiện";
  $("#prevPage").disabled = state.page <= 1;
  $("#nextPage").disabled = !state.totalPages || state.page >= state.totalPages;
  renderEvents();
  syncEventSelectors();
  await loadShowtimes(state.selectedEventId, { quiet: true });
}

function renderEvents() {
  const list = $("#eventList");
  if (!state.events.length) {
    list.innerHTML = `<div class="empty-state">Chưa có sự kiện. Tạo sự kiện đầu tiên bằng biểu mẫu bên cạnh.</div>`;
    return;
  }
  list.innerHTML = state.events.map((event) => {
    const initials = event.title.trim().split(/\s+/).slice(0, 2).map((word) => word[0]).join("").toUpperCase();
    const selected = event.eventId === state.selectedEventId;
    const adminActions = state.user?.role === "ADMIN"
      ? `<button class="small-action" data-event-action="edit" data-id="${event.eventId}">Sửa</button><button class="small-action delete" data-event-action="delete" data-id="${event.eventId}">Xoá</button>`
      : "";
    return `<article class="event-card ${selected ? "selected" : ""}" data-event-card="${event.eventId}">
      <span class="event-monogram">${escapeHtml(initials || "EV")}</span>
      <div class="event-info"><b>${escapeHtml(event.title)}</b><small>${escapeHtml(event.venue)} · #${event.eventId}</small></div>
      <div class="event-actions"><button class="small-action" data-event-action="select" data-id="${event.eventId}">Chọn</button>${adminActions}</div>
    </article>`;
  }).join("");
}

function updateAuthUi() {
  const authenticated = Boolean(state.user);
  const isAdmin = state.user?.role === "ADMIN";
  $("#userBadge").textContent = authenticated
    ? `${state.user.fullName} · ${state.user.role}`
    : "Chưa đăng nhập";
  $("#authButton").classList.toggle("hidden", authenticated);
  $("#logoutButton").classList.toggle("hidden", !authenticated);
  document.querySelectorAll(".admin-only").forEach((element) => {
    element.classList.toggle("hidden", !isAdmin);
  });
  $("#holdSelectedButton").disabled = !authenticated || state.selectedSeatIds.size === 0;
  $("#raceHoldButton").disabled = !authenticated;
  $("#holdLookupButton").disabled = !authenticated;
  $("#holdAuthHint").textContent = authenticated
    ? `Đang đặt vé với tài khoản ${state.user.email}.`
    : "Đăng nhập để giữ ghế.";
  renderEvents();
  renderShowtimes();
}

function setAuthMode(mode) {
  const registering = mode === "register";
  $("#authTitle").textContent = registering ? "Tạo tài khoản" : "Đăng nhập";
  $("#authSubmit").innerHTML = registering ? "Đăng ký <span>→</span>" : "Đăng nhập <span>→</span>";
  $("#authNameField").classList.toggle("hidden", !registering);
  $("#authFullName").required = registering;
  $("#authPassword").autocomplete = registering ? "new-password" : "current-password";
  $("#authHint").textContent = registering
    ? "Tài khoản mới được tạo với quyền USER."
    : "Đăng nhập để giữ vé hoặc dùng chức năng quản trị.";
  document.querySelectorAll("[data-auth-mode]").forEach((button) => {
    button.classList.toggle("active", button.dataset.authMode === mode);
  });
  $("#authForm").dataset.mode = mode;
}

function resetEventForm() {
  $("#eventForm").reset();
  $("#eventId").value = "";
  $("#eventFormKicker").textContent = "TẠO MỚI";
  $("#eventFormTitle").textContent = "Thêm sự kiện";
  $("#saveEventButton").innerHTML = "Tạo sự kiện <span>→</span>";
  $("#cancelEventEdit").classList.add("hidden");
}

async function chooseEvent(id) {
  const event = await api(`/api/events/${id}`);
  state.selectedEventId = event.eventId;
  $("#eventId").value = event.eventId;
  $("#eventTitle").value = event.title || "";
  $("#eventVenue").value = event.venue || "";
  $("#eventDescription").value = event.description || "";
  $("#eventFormKicker").textContent = `ĐANG CHỌN · #${event.eventId}`;
  $("#eventFormTitle").textContent = "Cập nhật sự kiện";
  $("#saveEventButton").innerHTML = "Lưu thay đổi <span>→</span>";
  $("#cancelEventEdit").classList.remove("hidden");
  renderEvents();
  syncEventSelectors();
  await loadShowtimes(event.eventId, { quiet: true });
}

async function deleteEvent(id) {
  const event = state.events.find((item) => item.eventId === Number(id));
  if (!event || !window.confirm(`Xoá sự kiện “${event.title}”? API sẽ từ chối nếu sự kiện còn suất diễn hoặc ghế.`)) return;
  await api(`/api/events/${id}`, { method: "DELETE" });
  if (state.selectedEventId === Number(id)) state.selectedEventId = null;
  resetEventForm();
  state.page = 1;
  await loadEvents();
  toast("Đã xoá sự kiện", event.title);
}

async function loadShowtimes(eventId, { quiet = false } = {}) {
  if (!eventId) {
    state.showtimes = [];
    state.selectedShowtimeId = null;
    renderShowtimes();
    renderSeatMap();
    return;
  }
  try {
    state.showtimes = await api(`/api/events/${eventId}/showtimes`);
    if (!state.showtimes.some((item) => item.showtimeId === state.selectedShowtimeId)) state.selectedShowtimeId = state.showtimes[0]?.showtimeId ?? null;
    renderShowtimes();
    renderSeatShowtimeOptions();
    $("#statShowtimes").textContent = String(state.showtimes.length);
    if (state.selectedShowtimeId) await loadSeatMap(state.selectedShowtimeId, { quiet: true });
    else renderSeatMap();
  } catch (error) {
    if (!quiet) showError(error, "Không tải được suất diễn");
  }
}

function renderShowtimes() {
  const rows = $("#showtimeRows");
  $("#showtimeCount").textContent = `${state.showtimes.length} suất`;
  if (!state.showtimes.length) {
    rows.innerHTML = `<tr><td colspan="4" class="empty-cell">Sự kiện chưa có suất diễn.</td></tr>`;
    return;
  }
  rows.innerHTML = state.showtimes.map((showtime) => {
    const adminActions = state.user?.role === "ADMIN"
      ? `<button class="small-action" data-showtime-action="edit" data-id="${showtime.showtimeId}">Sửa</button><button class="small-action delete" data-showtime-action="delete" data-id="${showtime.showtimeId}">Xoá</button>`
      : "";
    return `<tr>
    <td><strong>Suất #${showtime.showtimeId}</strong><small>Event #${showtime.eventId}</small></td>
    <td>${escapeHtml(formatDate(showtime.startAt))}</td>
    <td>${money.format(Number(showtime.ticketPrice))} ₫</td>
    <td><div class="table-actions"><button class="small-action" data-showtime-action="details" data-id="${showtime.showtimeId}">Xem</button>${adminActions}</div></td>
  </tr>`;
  }).join("");
}

function formatDate(value) {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : dateTime.format(date);
}

function toLocalDateTimeInput(value) {
  const date = new Date(value);
  return new Date(date.getTime() - date.getTimezoneOffset() * 60000).toISOString().slice(0, 16);
}

function renderSeatShowtimeOptions() {
  const items = state.showtimes.map((item) => ({ id: item.showtimeId, label: `Suất #${item.showtimeId} · ${formatDate(item.startAt)}` }));
  setSelectOptions($("#seatShowtimeSelect"), items, items.length ? "Chọn suất diễn…" : "Sự kiện chưa có suất diễn…", state.selectedShowtimeId);
}

async function loadSeatMap(showtimeId, { quiet = false } = {}) {
  if (!showtimeId) return;
  try {
    state.seatMap = await api(`/api/showtimes/${showtimeId}/seats`);
    state.selectedSeatIds = new Set([...state.selectedSeatIds].filter((id) => state.seatMap.some((seat) => seat.showtimeSeatId === id && seat.status === "AVAILABLE")));
    renderSeatMap();
  } catch (error) {
    state.seatMap = [];
    renderSeatMap();
    if (!quiet) showError(error, "Không tải được sơ đồ ghế");
  }
}

function renderSeatMap() {
  const grid = $("#seatGrid");
  if (!state.selectedShowtimeId) {
    grid.innerHTML = `<div class="empty-state">Chọn suất diễn để xem sơ đồ ghế.</div>`;
    $("#seatMapSummary").textContent = "Chưa tải sơ đồ";
    $("#deleteLayoutButton").disabled = !state.selectedEventId;
    updateSeatSelection();
    return;
  }
  if (!state.seatMap.length) {
    grid.innerHTML = `<div class="empty-state">Sơ đồ chưa có ghế hoặc chưa được tạo.<br>Dùng Layout Builder để tạo sơ đồ cho sự kiện.</div>`;
    $("#seatMapSummary").textContent = "0 ghế";
    $("#deleteLayoutButton").disabled = !state.selectedEventId;
    updateSeatSelection();
    return;
  }
  const rows = new Map();
  for (const seat of state.seatMap) {
    if (!rows.has(seat.rowLabel)) rows.set(seat.rowLabel, []);
    rows.get(seat.rowLabel).push(seat);
  }
  grid.innerHTML = [...rows.entries()].map(([label, seats]) => `<div class="seat-row"><span class="row-label">${escapeHtml(label)}</span>${seats.map((seat) => {
    const selected = state.selectedSeatIds.has(seat.showtimeSeatId);
    const statusClass = selected ? "selected" : seat.status.toLowerCase();
    const title = `${label}${seat.seatNumber} · ${seat.status}`;
    return `<button class="seat-button ${statusClass}" data-seat-id="${seat.showtimeSeatId}" title="${escapeHtml(title)}" aria-label="Ghế ${escapeHtml(title)}" ${seat.status !== "AVAILABLE" && !selected ? "disabled" : ""}>${seat.seatNumber}</button>`;
  }).join("")}</div>`).join("");
  const free = state.seatMap.filter((seat) => seat.status === "AVAILABLE").length;
  const held = state.seatMap.filter((seat) => seat.status === "HELD").length;
  const sold = state.seatMap.filter((seat) => seat.status === "SOLD").length;
  $("#seatMapSummary").textContent = `${state.seatMap.length} ghế · ${free} trống · ${held} đang giữ · ${sold} đã bán`;
  $("#deleteLayoutButton").disabled = !state.selectedEventId;
  updateSeatSelection();
}

function updateSeatSelection() {
  const selected = state.seatMap.filter((seat) => state.selectedSeatIds.has(seat.showtimeSeatId));
  $("#statSeats").textContent = String(selected.length);
  $("#selectionCount").textContent = `${selected.length} / 4`;
  $("#selectedSeats").innerHTML = selected.length ? selected.map((seat) => `<span class="selected-chip">${escapeHtml(seat.rowLabel)}${seat.seatNumber}<button type="button" data-remove-seat="${seat.showtimeSeatId}" aria-label="Bỏ ghế">×</button></span>`).join("") : `<span class="muted-copy">Chưa chọn ghế nào</span>`;
  const showtime = state.showtimes.find((item) => item.showtimeId === state.selectedShowtimeId);
  $("#bookingTotal").textContent = selected.length && showtime ? `${money.format(Number(showtime.ticketPrice) * selected.length)} ₫` : "—";
  const canSubmit = selected.length > 0 && selected.length <= 4 && !!state.selectedShowtimeId;
  $("#holdSelectedButton").disabled = !canSubmit || !state.user;
}

function selectedRequest() {
  return { showtimeId: Number(state.selectedShowtimeId), showtimeSeatIds: [...state.selectedSeatIds].sort((a, b) => a - b) };
}

async function renderHoldDetail(hold) {
  state.currentHoldId = hold.seatHoldId;
  $("#holdIdInput").value = hold.seatHoldId;
  const detail = $("#holdDetail");
  detail.classList.remove("hidden");
  const ids = (hold.showtimeSeatIds || []).map((id) => `<span class="selected-chip">Ghế #${id}</span>`).join("") || `<span class="muted-copy">Không có ghế</span>`;
  const canConfirm = hold.status === "ACTIVE";
  const canCancel = hold.status === "ACTIVE";
  detail.innerHTML = `<div class="hold-detail-head"><b>Hold #${hold.seatHoldId}</b><span class="hold-status ${hold.status}">${hold.status}</span></div>
    <div class="hold-data"><div><small>USER</small><b>#${hold.userId}</b></div><div><small>SHOWTIME</small><b>#${hold.showtimeId}</b></div><div><small>HẾT HẠN</small><b>${escapeHtml(formatDate(hold.expiresAt))}</b></div><div><small>GHẾ</small><b>${(hold.showtimeSeatIds || []).length} ghế</b></div></div>
    <div class="hold-seat-list">${ids}</div>
    <div class="hold-controls"><button class="button button-primary" data-hold-action="confirm" ${canConfirm ? "" : "disabled"}>Xác nhận booking <span>→</span></button><button class="button button-outline" data-hold-action="cancel" ${canCancel ? "" : "disabled"}>Huỷ lượt giữ</button></div>`;
}

// Global event delegation keeps handlers working after lists and maps rerender.
document.addEventListener("click", async (event) => {
  const nav = event.target.closest("[data-view]");
  if (nav) { navigate(nav.dataset.view); return; }
  const go = event.target.closest("[data-go]");
  if (go) { navigate(go.dataset.go); return; }
  const eventAction = event.target.closest("[data-event-action]");
  if (eventAction) {
    try {
      if (eventAction.dataset.eventAction === "select" || eventAction.dataset.eventAction === "edit") await chooseEvent(Number(eventAction.dataset.id));
      if (eventAction.dataset.eventAction === "delete") await deleteEvent(eventAction.dataset.id);
    } catch (error) { showError(error, "Không xử lý được sự kiện"); }
    return;
  }
  const showtimeAction = event.target.closest("[data-showtime-action]");
  if (showtimeAction) {
    const id = Number(showtimeAction.dataset.id);
    try {
      if (showtimeAction.dataset.showtimeAction === "details") {
        const detail = await api(`/api/showtimes/${id}`);
        $("#showtimeDetail").textContent = JSON.stringify(detail, null, 2);
        $("#showtimeDetail").classList.remove("hidden");
      }
      if (showtimeAction.dataset.showtimeAction === "edit") {
        const showtime = await api(`/api/showtimes/${id}`);
        $("#showtimeEditId").value = showtime.showtimeId;
        $("#showtimeStartAt").value = toLocalDateTimeInput(showtime.startAt);
        $("#showtimePrice").value = showtime.ticketPrice;
        $("#showtimeFormKicker").textContent = `ĐANG SỬA · #${id}`;
        $("#showtimeFormTitle").textContent = "Cập nhật suất diễn";
        $("#saveShowtimeButton").innerHTML = "Lưu thay đổi <span>→</span>";
        $("#cancelShowtimeEdit").classList.remove("hidden");
      }
      if (showtimeAction.dataset.showtimeAction === "delete") {
        if (window.confirm(`Xoá suất diễn #${id}?`)) {
          await api(`/api/showtimes/${id}`, { method: "DELETE" });
          toast("Đã xoá suất diễn", `Showtime #${id}`);
          state.selectedShowtimeId = null;
          await loadShowtimes(Number($("#showtimeEventSelect").value));
        }
      }
    } catch (error) { showError(error, "Không xử lý được suất diễn"); }
    return;
  }
  const seatButton = event.target.closest("[data-seat-id]");
  if (seatButton) {
    const id = Number(seatButton.dataset.seatId);
    if (state.selectedSeatIds.has(id)) state.selectedSeatIds.delete(id);
    else if (state.selectedSeatIds.size < 4) state.selectedSeatIds.add(id);
    else { toast("Tối đa 4 ghế", "Bỏ chọn một ghế trước khi chọn thêm.", "error"); return; }
    renderSeatMap();
    return;
  }
  const removeSeat = event.target.closest("[data-remove-seat]");
  if (removeSeat) { state.selectedSeatIds.delete(Number(removeSeat.dataset.removeSeat)); renderSeatMap(); return; }
  const holdAction = event.target.closest("[data-hold-action]");
  if (holdAction) {
    try {
      const id = Number(state.currentHoldId);
      if (holdAction.dataset.holdAction === "confirm") {
        const booking = await api(`/api/holds/${id}/confirm`, { method: "POST" });
        $("#holdDetail").classList.remove("hidden");
        $("#holdDetail").innerHTML = `<div class="hold-detail-head"><b>Booking #${booking.bookingId}</b><span class="hold-status CONFIRMED">${booking.status}</span></div><div class="hold-data"><div><small>HOLD</small><b>#${booking.seatHoldId}</b></div><div><small>SHOWTIME</small><b>#${booking.showtimeId}</b></div><div><small>USER</small><b>#${booking.userId}</b></div><div><small>THỜI ĐIỂM</small><b>${escapeHtml(formatDate(booking.createdAt))}</b></div></div><div class="hold-seat-list">${booking.showtimeSeatIds.map((seatId) => `<span class="selected-chip">Ghế #${seatId}</span>`).join("")}</div>`;
        toast("Booking đã xác nhận", `Booking #${booking.bookingId}`);
      } else {
        const hold = await api(`/api/holds/${id}`, { method: "DELETE" });
        await renderHoldDetail(hold);
        toast("Đã cập nhật lượt giữ", hold.status);
      }
      if (state.selectedShowtimeId) await loadSeatMap(state.selectedShowtimeId, { quiet: true });
    } catch (error) { showError(error, "Không xử lý được lượt giữ"); }
    return;
  }
  const openHold = event.target.closest("[data-open-hold]");
  if (openHold) {
    navigate("holds"); $("#holdIdInput").value = openHold.dataset.openHold; await lookupHold();
  }
});

$("#eventForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  const id = $("#eventId").value;
  const body = { title: $("#eventTitle").value.trim(), venue: $("#eventVenue").value.trim(), description: $("#eventDescription").value.trim() || null };
  try {
    const saved = await api(id ? `/api/events/${id}` : "/api/events", { method: id ? "PUT" : "POST", body: JSON.stringify(body) });
    state.selectedEventId = saved.eventId;
    resetEventForm();
    await loadEvents();
    toast(id ? "Đã cập nhật sự kiện" : "Đã tạo sự kiện", `${saved.title} · #${saved.eventId}`);
  } catch (error) { showError(error, "Không lưu được sự kiện"); }
});

$("#cancelEventEdit").addEventListener("click", resetEventForm);
$("#refreshEvents").addEventListener("click", () => loadEvents().catch((error) => showError(error, "Không tải được sự kiện")));
$("#prevPage").addEventListener("click", () => { if (state.page > 1) { state.page--; loadEvents().catch((error) => showError(error)); } });
$("#nextPage").addEventListener("click", () => { if (state.page < state.totalPages) { state.page++; loadEvents().catch((error) => showError(error)); } });

$("#showtimeEventSelect").addEventListener("change", async (event) => {
  const id = Number(event.target.value) || null;
  state.selectedEventId = id;
  await loadShowtimes(id);
});
$("#reloadShowtimes").addEventListener("click", () => loadShowtimes(Number($("#showtimeEventSelect").value)).catch((error) => showError(error)));
$("#showtimeForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  const eventId = Number($("#showtimeEventSelect").value);
  if (!eventId) { toast("Chọn sự kiện trước", "Suất diễn thuộc về một sự kiện.", "error"); return; }
  const editId = $("#showtimeEditId").value;
  const body = { startAt: new Date($("#showtimeStartAt").value).toISOString(), ticketPrice: Number($("#showtimePrice").value) };
  try {
    const showtime = await api(editId ? `/api/showtimes/${editId}` : `/api/events/${eventId}/showtimes`, { method: editId ? "PUT" : "POST", body: JSON.stringify(body) });
    resetShowtimeForm();
    state.selectedShowtimeId = showtime.showtimeId;
    await loadShowtimes(eventId);
    toast(editId ? "Đã cập nhật suất diễn" : "Đã tạo suất diễn", `Showtime #${showtime.showtimeId}`);
  } catch (error) { showError(error, "Không lưu được suất diễn"); }
});

function resetShowtimeForm() {
  $("#showtimeForm").reset();
  $("#showtimeEditId").value = "";
  $("#showtimeFormKicker").textContent = "THIẾT LẬP";
  $("#showtimeFormTitle").textContent = "Tạo suất diễn";
  $("#saveShowtimeButton").innerHTML = "Tạo suất diễn <span>→</span>";
  $("#cancelShowtimeEdit").classList.add("hidden");
}
$("#cancelShowtimeEdit").addEventListener("click", resetShowtimeForm);

$("#seatEventSelect").addEventListener("change", async (event) => {
  state.selectedEventId = Number(event.target.value) || null;
  state.selectedShowtimeId = null;
  state.selectedSeatIds.clear();
  await loadShowtimes(state.selectedEventId);
});
$("#seatShowtimeSelect").addEventListener("change", async (event) => {
  state.selectedShowtimeId = Number(event.target.value) || null;
  state.selectedSeatIds.clear();
  await loadSeatMap(state.selectedShowtimeId);
});
$("#refreshSeatMap").addEventListener("click", () => loadSeatMap(state.selectedShowtimeId));
$("#seatGenerateForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  if (!state.selectedEventId) { toast("Chọn sự kiện trước", "Layout ghế thuộc về một sự kiện.", "error"); return; }
  try {
    const result = await api(`/api/events/${state.selectedEventId}/seats/generate`, { method: "POST", body: JSON.stringify({ rows: Number($("#seatRows").value), seatsPerRow: Number($("#seatsPerRow").value) }) });
    toast("Đã tạo sơ đồ ghế", `${result.createdSeats} ghế · Event #${result.eventId}`);
    await loadShowtimes(state.selectedEventId);
  } catch (error) { showError(error, "Không tạo được layout"); }
});
$("#deleteLayoutButton").addEventListener("click", async () => {
  if (!state.selectedEventId || !window.confirm("Xoá toàn bộ layout ghế của sự kiện? API sẽ từ chối nếu sự kiện đã có suất diễn.")) return;
  try {
    await api(`/api/events/${state.selectedEventId}/seats`, { method: "DELETE" });
    state.seatMap = []; state.selectedSeatIds.clear();
    renderSeatMap();
    toast("Đã xoá layout", `Event #${state.selectedEventId}`);
  } catch (error) { showError(error, "Không xoá được layout"); }
});

$("#holdSelectedButton").addEventListener("click", async () => {
  try {
    const hold = await api("/api/holds", { method: "POST", body: JSON.stringify(selectedRequest()) });
    state.currentHoldId = hold.seatHoldId;
    $("#holdIdInput").value = hold.seatHoldId;
    $("#holdResult").classList.remove("hidden", "error");
    $("#holdResult").innerHTML = `<b>Giữ ghế thành công · Hold #${hold.seatHoldId}</b>Ghế đang được giữ. <button class="text-button" data-open-hold="${hold.seatHoldId}">Mở chi tiết & xác nhận →</button>`;
    toast("Đã giữ ghế", `Hold #${hold.seatHoldId}`);
    await loadSeatMap(state.selectedShowtimeId, { quiet: true });
  } catch (error) { showError(error, "Không giữ được ghế"); }
});

$("#raceHoldButton").addEventListener("click", async () => {
  const seatIds = [...state.selectedSeatIds];
  if (seatIds.length !== 1 || !state.selectedShowtimeId) {
    toast("Chọn đúng một ghế", "Demo tranh chấp cần một ghế và một suất diễn.", "error");
    return;
  }
  const firstToken = localStorage.getItem("smartticket-access-token");
  if (!state.user || !firstToken) {
    toast("User 1 chưa đăng nhập", "Đăng nhập tài khoản thứ nhất trước.", "error");
    return;
  }

  const secondEmail = $("#raceUserEmail").value.trim();
  const secondPassword = $("#raceUserPassword").value;
  if (!secondEmail || !secondPassword) {
    toast("Thiếu thông tin User 2", "Nhập email và mật khẩu của tài khoản thứ hai.", "error");
    return;
  }

  const output = $("#raceResult");
  output.classList.remove("hidden", "error");
  const button = $("#raceHoldButton");
  button.disabled = true;
  output.textContent = "Đang xác thực User 2…";

  let secondUser;
  let results;
  try {
    const login = await api("/api/auth/login", {
      method: "POST",
      body: JSON.stringify({ email: secondEmail, password: secondPassword }),
      skipAuth: true,
    });
    secondUser = login.user;
    $("#raceUserPassword").value = "";

    if (secondUser.userId === state.user.userId) {
      throw new Error("User 1 và User 2 phải là hai tài khoản khác nhau.");
    }

    output.textContent = "Đang gửi song song hai request với hai JWT riêng…";
    const body = JSON.stringify(selectedRequest());
    results = await Promise.allSettled([
      api("/api/holds", { method: "POST", body, authToken: firstToken }),
      api("/api/holds", { method: "POST", body, authToken: login.accessToken }),
    ]);
  } catch (error) {
    output.classList.add("error");
    output.textContent = error.message || String(error);
    button.disabled = false;
    return;
  }

  const participants = [state.user, secondUser];
  output.innerHTML = results.map((result, index) => {
    const requestNumber = index + 1;
    const participant = participants[index];
    if (result.status === "fulfilled") {
      const hold = result.value;
      return `<div><b>Request ${requestNumber} · ${escapeHtml(participant.email)} (${escapeHtml(participant.role)}): GIỮ ĐƯỢC</b>Hold #${hold.seatHoldId} · <button class="text-button" data-open-hold="${hold.seatHoldId}">Mở lượt giữ</button></div>`;
    }
    const error = result.reason;
    const explanation = error.status === 409
      ? "Kết quả đúng: ghế đã được request kia giữ trước."
      : error.status === 401
        ? "Lỗi xác thực; request chưa kiểm tra tranh chấp ghế. Hãy đăng nhập lại."
        : error.status === 403
          ? "Lỗi phân quyền; request chưa kiểm tra tranh chấp ghế. Kết quả demo này không xác nhận cơ chế khóa ghế."
          : "Lỗi ngoài dự kiến; hãy kiểm tra backend log trước khi kết luận về tranh chấp ghế.";
    return `<div><b>Request ${requestNumber} · ${escapeHtml(participant.email)} (${escapeHtml(participant.role)}): BỊ TỪ CHỐI</b>${escapeHtml(error.message)} — ${escapeHtml(explanation)}</div>`;
  }).join("");
  button.disabled = !state.user;
  await loadSeatMap(state.selectedShowtimeId, { quiet: true });
});

async function lookupHold() {
  try {
    const id = Number($("#holdIdInput").value);
    const hold = await api(`/api/holds/${id}`);
    await renderHoldDetail(hold);
  } catch (error) { showError(error, "Không tìm được lượt giữ"); }
}
$("#holdLookupForm").addEventListener("submit", (event) => { event.preventDefault(); lookupHold(); });

$("#checkConnection").addEventListener("click", () => checkConnection(true));
$("#apiBase").addEventListener("change", () => { localStorage.setItem("smartticket-api-base", $("#apiBase").value.trim()); checkConnection(); });

$("#authButton").addEventListener("click", () => {
  setAuthMode("login");
  $("#authDialog").showModal();
});
$("#closeAuthDialog").addEventListener("click", () => $("#authDialog").close());
document.querySelectorAll("[data-auth-mode]").forEach((button) => {
  button.addEventListener("click", () => setAuthMode(button.dataset.authMode));
});
$("#authForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  const registering = event.currentTarget.dataset.mode === "register";
  const body = {
    email: $("#authEmail").value.trim(),
    password: $("#authPassword").value,
  };
  if (registering) body.fullName = $("#authFullName").value.trim();

  try {
    const result = await api(`/api/auth/${registering ? "register" : "login"}`, {
      method: "POST", body: JSON.stringify(body), skipAuth: true,
    });
    localStorage.setItem("smartticket-access-token", result.accessToken);
    state.user = result.user;
    $("#authForm").reset();
    $("#authDialog").close();
    updateAuthUi();
    toast(registering ? "Đã tạo tài khoản" : "Đăng nhập thành công", `${result.user.fullName} · ${result.user.role}`);
    await loadEvents();
  } catch (error) {
    showError(error, registering ? "Không đăng ký được" : "Không đăng nhập được");
  }
});
$("#logoutButton").addEventListener("click", () => {
  localStorage.removeItem("smartticket-access-token");
  state.user = null;
  $("#holdDetail").classList.add("hidden");
  $("#holdResult").classList.add("hidden");
  $("#raceResult").classList.add("hidden");
  updateAuthUi();
  toast("Đã đăng xuất");
});

async function boot() {
  const savedBase = localStorage.getItem("smartticket-api-base");
  if (savedBase) $("#apiBase").value = savedBase;
  $("#todayLabel").textContent = new Intl.DateTimeFormat("vi-VN", { dateStyle: "full" }).format(new Date());
  const hashView = location.hash.replace("#", "");
  if (["overview", "events", "showtimes", "seats", "holds"].includes(hashView)) navigate(hashView);
  setAuthMode("login");
  const token = localStorage.getItem("smartticket-access-token");
  let roleChanged = false;
  if (token) {
    try {
      state.user = await api("/api/auth/me");
      const expectedRole = `ROLE_${state.user.role}`;
      if (!getTokenRoles(token).includes(expectedRole)) {
        // Buộc đăng nhập lại để nhận token có role vừa cập nhật ở DB.
        localStorage.removeItem("smartticket-access-token");
        state.user = null;
        roleChanged = true;
      }
    }
    catch { localStorage.removeItem("smartticket-access-token"); state.user = null; }
  }
  updateAuthUi();
  if (roleChanged) {
    toast("Quyền tài khoản vừa thay đổi", "Vui lòng đăng nhập lại để nhận quyền mới.", "error");
  }
  const connected = await checkConnection();
  if (connected) {
    try { await loadEvents(); }
    catch (error) { showError(error, "Không tải được dữ liệu khởi tạo"); }
  } else {
    $("#eventList").innerHTML = `<div class="empty-state">Chưa kết nối được backend.<br>Kiểm tra API URL hoặc khởi động Spring Boot.</div>`;
  }
}

boot();

