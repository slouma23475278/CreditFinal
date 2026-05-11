/**
 * MediLink Authentication Manager
 * JWT token management and Keycloak integration
 */

window.MediLinkAuth = {
  token: localStorage.getItem('ml_token') || '',
  keycloakUrl: 'http://localhost:8080/realms/medilink',

  async login(username, password, clientId = 'medilink-client') {
    const tokenUrl = `${this.keycloakUrl}/protocol/openid-connect/token`;
    const body = new URLSearchParams({
      grant_type: 'password',
      client_id: clientId,
      username,
      password
    });

    try {
      const res = await fetch(tokenUrl, {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body
      });
      const data = await res.json();
      if (data.access_token) {
        this.token = data.access_token;
        localStorage.setItem('ml_token', this.token);
        return { success: true, expiresIn: data.expires_in };
      }
      return { success: false, error: data.error_description || 'Login failed' };
    } catch (e) {
      return { success: false, error: e.message };
    }
  },

  getRole() {
    if (!this.token) return null;
    try {
      const payload = JSON.parse(atob(this.token.split('.')[1]));
      const roles = payload.realm_access?.roles || [];
      return roles.find(r => ['DOCTOR', 'PATIENT', 'ADMIN'].includes(r.toUpperCase())) || null;
    } catch { return null; }
  },

  isExpired() {
    if (!this.token) return true;
    try {
      const payload = JSON.parse(atob(this.token.split('.')[1]));
      return payload.exp * 1000 < Date.now();
    } catch { return true; }
  },

  isAdmin() { return this.getRole() === 'ADMIN'; },
  isDoctor() { return this.getRole() === 'DOCTOR'; },
  isPatient() { return this.getRole() === 'PATIENT'; },

  logout() {
    this.token = '';
    localStorage.removeItem('ml_token');
  },

  getUsername() {
    if (!this.token) return null;
    try {
      const payload = JSON.parse(atob(this.token.split('.')[1]));
      return payload.preferred_username || payload.sub;
    } catch { return null; }
  }
};