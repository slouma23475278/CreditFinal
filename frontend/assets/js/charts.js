/**
 * MediLink Chart Manager
 * Chart.js visualizations for dashboard
 */

window.MediLinkCharts = {
  mainChart: null,
  donutChart: null,

  initMainChart() {
    const ctx = document.getElementById('mainChart');
    if (!ctx) return;
    
    const labels = Array.from({length: 30}, (_, i) => {
      const d = new Date();
      d.setDate(d.getDate() - 29 + i);
      return `${d.getDate()}/${('0'+(d.getMonth()+1)).slice(-2)}`;
    });

    this.mainChart = new Chart(ctx, {
      type: 'bar',
      data: {
        labels,
        datasets: [
          { label: 'Créées', data: labels.map(() => Math.floor(20+Math.random()*40)), backgroundColor: 'rgba(59,130,246,0.6)', borderRadius: 3 },
          { label: 'Modifiées', data: labels.map(() => Math.floor(5+Math.random()*20)), backgroundColor: 'rgba(20,184,166,0.5)', borderRadius: 3 },
          { label: 'Annulées', data: labels.map(() => Math.floor(1+Math.random()*8)), backgroundColor: 'rgba(245,158,11,0.5)', borderRadius: 3 }
        ]
      },
      options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { labels: { color: '#8b9ab8', boxWidth: 10, font: { size: 11 } } } }, scales: { x: { grid: { color: 'rgba(99,130,180,0.06)' }, ticks: { color: '#4a5a78', font: { size: 9 }, maxTicksLimit: 8 } }, y: { grid: { color: 'rgba(99,130,180,0.08)' }, ticks: { color: '#4a5a78', font: { size: 10 } } } } }
    });
  },

  initDonutChart() {
    const ctx = document.getElementById('donutChart');
    if (!ctx) return;

    this.donutChart = new Chart(ctx, {
      type: 'doughnut',
      data: {
        labels: ['CREATED', 'UPDATED', 'CANCELLED'],
        datasets: [{ data: [52, 31, 17], backgroundColor: ['#3b82f6', '#14b8a6', '#f59e0b'], borderWidth: 0, hoverOffset: 6 }]
      },
      options: { responsive: true, maintainAspectRatio: false, cutout: '68%', plugins: { legend: { display: false } } }
    });
  }
};