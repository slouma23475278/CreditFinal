/**
 * MediLink Application Router & Renderer
 * Compatible with the existing index.html structure
 */

const App = {
  currentPage: 'home',

  init() {
    this.updateTokenStatus();
  },

  showTab(name, btn) {
    document.querySelectorAll('.tab-content').forEach(t => t.classList.remove('active'));
    document.querySelectorAll('nav button').forEach(b => b.classList.remove('active'));
    document.getElementById('tab-' + name).classList.add('active');
    btn.classList.add('active');
    this.currentPage = name;
    if (name === 'home') this.loadStats();
  },

  updateTokenStatus() {
    const el = document.getElementById('tokenStatus');
    if (el) el.textContent = TOKEN ? '✅ Token configuré' : '❌ Non connecté';
  },

  async loadStats() {
    try {
      const res = await fetch(GATEWAY + '/api/ordonnances', { headers: this.getHeaders() });
      if (res.ok) document.getElementById('statOrd').textContent = (await res.json()).length;
    } catch(e) { document.getElementById('statOrd').textContent = '—'; }
    try {
      const res = await fetch(GATEWAY + '/api/notifications', { headers: this.getHeaders() });
      if (res.ok) document.getElementById('statNotif').textContent = (await res.json()).length;
    } catch(e) { document.getElementById('statNotif').textContent = '—'; }
  },

  getHeaders() {
    return { 
      'Content-Type': 'application/json', 
      'Authorization': TOKEN ? `Bearer ${TOKEN}` : '' 
    };
  },

  showAlert(message, type = 'info') {
    const box = document.getElementById('msgBox') || document.getElementById('notifMsgBox') || document.createElement('div');
    box.innerHTML = `<div class="alert alert-${type}">${message}</div>`;
    setTimeout(() => { box.innerHTML = ''; }, 4000);
  },

  // Ordonnances functions
  async loadOrdonnances() {
    try {
      const res = await fetch(GATEWAY + '/api/ordonnances', { headers: this.getHeaders() });
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      const data = await res.json();
      this.renderOrdonnances(data);
    } catch(e) {
      document.getElementById('ordonnancesTable').innerHTML = '<em style="color:red">Erreur de connexion.</em>';
    }
  },

  renderOrdonnances(data) {
    if (!data.length) { 
      document.getElementById('ordonnancesTable').innerHTML = '<em>Aucune ordonnance.</em>'; 
      return; 
    }
    let html = `<table><thead><tr><th>ID</th><th>Patient</th><th>Médecin</th><th>Diagnostic</th><th>Date</th><th>Statut</th><th>Actions</th></tr></thead><tbody>`;
    data.forEach(o => {
      const s = (o.status||'ACTIVE').toLowerCase();
      html += `<tr>
        <td>${o.id}</td><td>${o.patientName}</td><td>${o.doctorName}</td>
        <td>${o.diagnosis}</td><td>${o.dateCreation||'—'}</td>
        <td><span class="status-${s}">${o.status||'ACTIVE'}</span></td>
        <td style="display:flex;gap:6px">
          <button class="btn btn-warning" style="padding:5px 10px;font-size:0.8rem" onclick="cancelOrdonnance(${o.id})">Annuler</button>
          <button class="btn btn-danger" style="padding:5px 10px;font-size:0.8rem" onclick="deleteOrdonnance(${o.id})">Suppr.</button>
        </td>
      </tr>`;
    });
    html += '</tbody></table>';
    document.getElementById('ordonnancesTable').innerHTML = html;
  },

  // Notifications functions  
  async loadNotifications() {
    const uid = document.getElementById('notifUserId')?.value || '2';
    try {
      const res = await fetch(GATEWAY + '/api/notifications/user/' + uid, { headers: this.getHeaders() });
      const data = await res.json();
      this.renderNotifications(data);
      const badge = document.getElementById('notifBadge');
      if (badge) badge.textContent = data.filter(n => n.status === 'SENT').length;
    } catch(e) {
      document.getElementById('notificationsTable').innerHTML = '<em style="color:red">Erreur de connexion.</em>';
    }
  },

  renderNotifications(data) {
    if (!data.length) { 
      document.getElementById('notificationsTable').innerHTML = '<em>Aucune notification.</em>'; 
      return; 
    }
    let html = `<table><thead><tr><th>ID</th><th>Type</th><th>Message</th><th>Statut</th><th>Date</th><th>Action</th></tr></thead><tbody>`;
    data.forEach(n => {
      const s = (n.status||'').toLowerCase();
      html += `<tr>
        <td style="font-size:0.75rem">${n.id}</td>
        <td><code style="font-size:0.78rem">${n.type}</code></td>
        <td style="max-width:300px;font-size:0.82rem">${n.message}</td>
        <td><span class="status-${s}">${n.status}</span></td>
        <td style="font-size:0.8rem">${n.createdAt ? n.createdAt.replace('T',' ').substring(0,16) : '—'}</td>
        <td>${n.status === 'SENT' ? `<button class="btn btn-success" style="padding:4px 10px;font-size:0.78rem" onclick="markRead('${n.id}')">Lu</button>` : '✓'}</td>
      </tr>`;
    });
    html += '</tbody></table>';
    document.getElementById('notificationsTable').innerHTML = html;
  },

  async filterByPatient() {
    const pid = document.getElementById('filterPatientId').value;
    if (!pid) { this.loadOrdonnances(); return; }
    try {
      const res = await fetch(GATEWAY + '/api/ordonnances/patient/' + pid, { headers: this.getHeaders() });
      const data = await res.json();
      this.renderOrdonnances(data);
    } catch(e) {
      this.showAlert('❌ Erreur filtrage.', 'error');
    }
  }
};

// Initialize
document.addEventListener('DOMContentLoaded', () => App.init());