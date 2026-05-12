const GATEWAY = 'http://localhost:8560';
const KEYCLOAK = 'http://localhost:8080';
const REALM   = 'medilink';
const CLIENT  = 'medilink-client';

// ── Token storage ──────────────────────────────────────────────
function saveSession(token, decoded) {
  sessionStorage.setItem('ml_token', token);
  sessionStorage.setItem('ml_user',  JSON.stringify(decoded));
}

function getToken()   { return sessionStorage.getItem('ml_token'); }
function getUser()    { const u = sessionStorage.getItem('ml_user'); return u ? JSON.parse(u) : null; }
function clearSession(){ sessionStorage.clear(); }

// ── JWT decode (no library needed) ─────────────────────────────
function decodeJWT(token) {
  try {
    const payload = token.split('.')[1];
    const decoded = JSON.parse(atob(payload.replace(/-/g, '+').replace(/_/g, '/')));
    return decoded;
  } catch (e) {
    return null;
  }
}

// ── Extract roles from Keycloak JWT ────────────────────────────
function getRoles(decoded) {
  // Keycloak puts roles in realm_access.roles
  return decoded?.realm_access?.roles || [];
}

function hasRole(role) {
  const user = getUser();
  if (!user) return false;
  return getRoles(user).includes(role);
}

function getPrimaryRole() {
  if (hasRole('ADMIN'))   return 'ADMIN';
  if (hasRole('DOCTOR'))  return 'DOCTOR';
  if (hasRole('PATIENT')) return 'PATIENT';
  return null;
}

// ── Keycloak login (Resource Owner Password) ───────────────────
async function login(username, password) {
  const url = `${KEYCLOAK}/realms/${REALM}/protocol/openid-connect/token`;
  const body = new URLSearchParams({
    grant_type: 'password',
    client_id:  CLIENT,
    username,
    password
  });

  const res = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body
  });

  const data = await res.json();

  if (!res.ok || !data.access_token) {
    throw new Error(data.error_description || 'Identifiants incorrects');
  }

  const decoded = decodeJWT(data.access_token);
  if (!decoded) throw new Error('Token invalide');

  saveSession(data.access_token, decoded);
  return { token: data.access_token, decoded };
}

// ── Redirect based on role ──────────────────────────────────────
function redirectByRole() {
  const role = getPrimaryRole();
  if (role === 'ADMIN')   { window.location.href = 'dashboard-admin.html';   return; }
  if (role === 'DOCTOR')  { window.location.href = 'dashboard-doctor.html';  return; }
  if (role === 'PATIENT') { window.location.href = 'dashboard-patient.html'; return; }
  throw new Error('Aucun rôle reconnu (DOCTOR, PATIENT, ADMIN)');
}

// ── Guard: call at top of each dashboard ───────────────────────
function requireRole(expectedRole) {
  const token = getToken();
  if (!token) { window.location.href = 'index.html'; return false; }

  const user = getUser();
  if (!user)  { window.location.href = 'index.html'; return false; }

  if (expectedRole && !hasRole(expectedRole)) {
    // Wrong dashboard for this role
    redirectByRole();
    return false;
  }
  return true;
}

// ── Logout ─────────────────────────────────────────────────────
function logout() {
  clearSession();
  window.location.href = 'index.html';
}

// ── Auth header for API calls ──────────────────────────────────
function authHeaders() {
  return {
    'Content-Type':  'application/json',
    'Authorization': 'Bearer ' + getToken()
  };
}