/**
 * MediLink Application Router & Renderer
 * Handles page navigation and dynamic content loading
 */

const App = {
  currentPage: 'dashboard',
  flowRunning: false,

  init() {
    this.bindEvents();
    this.updateTokenStatus();
    this.loadDashboardData();
    setInterval(() => this.liveUpdate(), 3000);
  },

  bindEvents() {
    document.addEventListener('DOMContentLoaded', () => {
      MediLinkCharts.initMainChart();
      MediLinkCharts.initDonutChart();
    });
  },

  showPage(pageName, navEl) {
    document.querySelectorAll('.page').forEach(p => p.classList.remove('active'));
    document.querySelectorAll('.nav-item').forEach(n => n.classList.remove('active'));
    
    const page = document.getElementById(`page-${pageName}`);
    if (page) page.classList.add('active');
    if (navEl) navEl.classList.add('active');
    
    this.currentPage = pageName;
    this.updatePageInfo(pageName);
    
    if (pageName === 'ordonnances') this.loadOrdonnances();
    if (pageName === 'notifications') this.loadNotifications();
    if (pageName === 'dashboard') this.loadDashboardData();
  },

  updatePageInfo(page) {
    const titles = {
      dashboard: ['Vue d\'ensemble', 'Tableau de bord médical distribué'],
      notifications: ['Notifications', 'Centre de notifications — MongoDB'],
      services: ['Microservices', 'Santé de l\'infrastructure Spring Cloud'],
      patients: ['Patients', 'Gestion des patients actifs'],
      ordonnances: ['Ordonnances', 'Création et suivi des prescriptions'],
      appointments: ['Rendez-vous', 'Agenda médical'],
      logs: ['Logs système', 'Visualisation des logs centralisés'],
      gateway: ['API Gateway', 'Sécurité JWT · RBAC · Routage']
    };
    const info = titles[page] || [page, ''];
    document.getElementById('pageTitle').textContent = info[0];
    document.getElementById('pageSub').textContent = info[1];
  },

  async loadDashboardData() {
    try {
      const [ordoRes, notifRes] = await Promise.all([
        MediLinkAPI.get('/api/ordonnances'),
        MediLinkAPI.get('/api/notifications')
      ]);
      const ordos = await ordoRes.json();
      const notifs = await notifRes.json();
      document.getElementById('stat-ord').textContent = ordos.length;
      document.getElementById('stat-notif').textContent = notifs.length;
    } catch (e) {
      console.error('Failed to load dashboard data', e);
    }
  },

  async loadOrdonnances() {
    try {
      const res = await MediLinkAPI.get('/api/ordonnances');
      const data = await res.json();
      this.renderOrdonnancesTable(data);
    } catch (e) {
      this.showAlert('Erreur de chargement des ordonnances', 'error');
    }
  },

  renderOrdonnancesTable(data) {
    const tbody = document.getElementById('ordoTbody');
    if (!tbody) return;
    
    tbody.innerHTML = data.map(o => {
      const statusClass = { 'ACTIVE': 'chip-green', 'UPDATED': 'chip-blue', 'CANCELLED': 'chip-red' }[o.status] || 'chip-blue';
      return `<tr>
        <td class="td-mono">${o.id}</td>
        <td>${o.patientName}</td>
        <td style="color:var(--text2)">${o.doctorName}</td>
        <td>${o.diagnosis}</td>
        <td style="color:var(--text2)">${o.medications?.join(', ') || ''}</td>
        <td><div class="chip ${statusClass}">${o.status}</div></td>
        <td class="td-mono">${o.dateCreation || '—'}</td>
      </tr>`;
    }).join('');
  },

  async createOrdonnance() {
    const body = {
      doctorId: +document.getElementById('doctorId').value,
      doctorName: document.getElementById('doctorName').value,
      patientId: +document.getElementById('patientId').value,
      patientName: document.getElementById('patientName').value,
      diagnosis: document.getElementById('diagnosis').value,
      medications: document.getElementById('medications').value.split(',').map(m => m.trim()),
      notes: document.getElementById('notes').value
    };

    try {
      const res = await MediLinkAPI.post('/api/ordonnances', body);
      const data = await res.json();
      this.showAlert(`Ordonnance #${data.id} créée avec succès!`, 'success');
      this.loadOrdonnances();
    } catch (e) {
      this.showAlert(`Erreur: ${e.message}`, 'error');
    }
  },

  async loadNotifications() {
    const userId = document.getElementById('notifUserId')?.value || '2';
    try {
      const res = await MediLinkAPI.get(`/api/notifications/user/${userId}`);
      const data = await res.json();
      this.renderNotificationsTable(data);
      document.getElementById('notifBadge').textContent = data.filter(n => n.status === 'SENT').length;
    } catch (e) {
      this.showAlert('Erreur de chargement des notifications', 'error');
    }
  },

  renderNotificationsTable(data) {
    const tbody = document.getElementById('notificationsTable');
    if (!tbody) return;
    
    tbody.innerHTML = data.length ? data.map(n => `
      <div class="notif-item">
        <div class="notif-icon">🔔</div>
        <div style="flex:1">
          <div class="notif-text">${n.message}</div>
          <div class="notif-sub">${n.type} · ${n.status}</div>
        </div>
        <div class="notif-time">${n.createdAt?.substring(0,16) || '—'}</div>
      </div>
    `).join('') : '<em>Aucune notification</em>';
  },

  showAlert(message, type = 'info') {
    const alert = document.createElement('div');
    alert.className = `alert alert-${type}`;
    alert.textContent = message;
    alert.style.cssText = 'position:fixed;top:20px;right:20px;z-index:9999;padding:12px 20px;border-radius:8px;background:rgba(59,130,246,0.1);color:var(--accent);';
    document.body.appendChild(alert);
    setTimeout(() => alert.remove(), 4000);
  },

  liveUpdate() {
    // Update live counters
    const kpiOrdo = document.getElementById('kpi-ordo');
    if (kpiOrdo && Math.random() < 0.3) {
      const val = parseInt(kpiOrdo.textContent.replace(/\s/g, ''));
      kpiOrdo.textContent = (val + 1).toLocaleString('fr-FR');
    }
  },

  updateTokenStatus() {
    const el = document.getElementById('tokenStatus');
    if (el) el.textContent = MediLinkAuth.token ? '✅ Token configuré' : '❌ Non connecté';
  }
};

// Initialize on load
document.addEventListener('DOMContentLoaded', () => App.init());