// ── All API calls go through the Gateway on :8560 ──────────────

// ════════════════════════════════════════════════
//  ORDONNANCES  (MySQL via ordonnance-service)
// ════════════════════════════════════════════════

async function apiGetAllOrdonnances() {
  const res = await fetch(GATEWAY + '/api/ordonnances', { headers: authHeaders() });
  if (!res.ok) throw new Error(`Erreur ${res.status}`);
  return res.json();
}

async function apiGetOrdonnancesByPatient(patientId) {
  const res = await fetch(GATEWAY + `/api/ordonnances/patient/${patientId}`, { headers: authHeaders() });
  if (!res.ok) throw new Error(`Erreur ${res.status}`);
  return res.json();
}

async function apiGetOrdonnancesByDoctor(doctorId) {
  const res = await fetch(GATEWAY + `/api/ordonnances/doctor/${doctorId}`, { headers: authHeaders() });
  if (!res.ok) throw new Error(`Erreur ${res.status}`);
  return res.json();
}

// SCENARIO 1 — Crée ordonnance → Feign notifie patient + RabbitMQ CREATED
async function apiCreateOrdonnance(dto) {
  const res = await fetch(GATEWAY + '/api/ordonnances', {
    method: 'POST',
    headers: authHeaders(),
    body: JSON.stringify(dto)
  });
  if (!res.ok) throw new Error(`Erreur ${res.status}: ${await res.text()}`);
  return res.json();
}

// SCENARIO 2 — Modifie ordonnance → Feign notifie patient + RabbitMQ UPDATED
async function apiUpdateOrdonnance(id, dto) {
  const res = await fetch(GATEWAY + `/api/ordonnances/${id}`, {
    method: 'PUT',
    headers: authHeaders(),
    body: JSON.stringify(dto)
  });
  if (!res.ok) throw new Error(`Erreur ${res.status}: ${await res.text()}`);
  return res.json();
}

// SCENARIO 3 — Annule ordonnance → Feign notifie patient + RabbitMQ CANCELLED
async function apiCancelOrdonnance(id) {
  const res = await fetch(GATEWAY + `/api/ordonnances/${id}/cancel`, {
    method: 'PATCH',
    headers: authHeaders()
  });
  if (!res.ok) throw new Error(`Erreur ${res.status}`);
  return res.json();
}

// ADMIN only
async function apiDeleteOrdonnance(id) {
  const res = await fetch(GATEWAY + `/api/ordonnances/${id}`, {
    method: 'DELETE',
    headers: authHeaders()
  });
  if (!res.ok) throw new Error(`Erreur ${res.status}`);
  return res.text();
}

// ════════════════════════════════════════════════
//  NOTIFICATIONS  (MongoDB via notification-service)
// ════════════════════════════════════════════════

async function apiGetAllNotifications() {
  const res = await fetch(GATEWAY + '/api/notifications', { headers: authHeaders() });
  if (!res.ok) throw new Error(`Erreur ${res.status}`);
  return res.json();
}

async function apiGetNotificationsByUser(userId) {
  const res = await fetch(GATEWAY + `/api/notifications/user/${userId}`, { headers: authHeaders() });
  if (!res.ok) throw new Error(`Erreur ${res.status}`);
  return res.json();
}

async function apiGetUnreadByUser(userId) {
  const res = await fetch(GATEWAY + `/api/notifications/user/${userId}/unread`, { headers: authHeaders() });
  if (!res.ok) throw new Error(`Erreur ${res.status}`);
  return res.json();
}

async function apiCountUnread(userId) {
  const res = await fetch(GATEWAY + `/api/notifications/user/${userId}/count`, { headers: authHeaders() });
  if (!res.ok) throw new Error(`Erreur ${res.status}`);
  return res.json();
}

async function apiMarkAsRead(notifId) {
  const res = await fetch(GATEWAY + `/api/notifications/${notifId}/read`, {
    method: 'PATCH',
    headers: authHeaders()
  });
  if (!res.ok) throw new Error(`Erreur ${res.status}`);
  return res.json();
}

async function apiDeleteNotification(notifId) {
  const res = await fetch(GATEWAY + `/api/notifications/${notifId}`, {
    method: 'DELETE',
    headers: authHeaders()
  });
  if (!res.ok) throw new Error(`Erreur ${res.status}`);
  return res.text();
}