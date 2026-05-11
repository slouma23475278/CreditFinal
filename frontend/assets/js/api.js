/**
 * MediLink API Layer
 * Wrapper for fetch with JWT authentication and error handling
 */

const API_CONFIG = {
  baseUrl: localStorage.getItem('ml_gateway') || 'http://localhost:8560',
  token: localStorage.getItem('ml_token') || ''
};

const updateConfig = () => {
  API_CONFIG.baseUrl = localStorage.getItem('ml_gateway') || 'http://localhost:8560';
  API_CONFIG.token = localStorage.getItem('ml_token') || '';
};

const getHeaders = () => ({
  'Content-Type': 'application/json',
  'Authorization': API_CONFIG.token ? `Bearer ${API_CONFIG.token}` : ''
});

const handleResponse = async (response) => {
  if (response.status === 401) {
    localStorage.removeItem('ml_token');
    window.location.reload();
    throw new Error('Unauthorized');
  }
  if (!response.ok) {
    const error = await response.text();
    throw new Error(error || `HTTP ${response.status}`);
  }
  return response;
};

window.MediLinkAPI = {
  get: async (endpoint) => {
    updateConfig();
    const res = await fetch(`${API_CONFIG.baseUrl}${endpoint}`, {
      method: 'GET',
      headers: getHeaders()
    });
    return handleResponse(res);
  },

  post: async (endpoint, data) => {
    updateConfig();
    const res = await fetch(`${API_CONFIG.baseUrl}${endpoint}`, {
      method: 'POST',
      headers: getHeaders(),
      body: JSON.stringify(data)
    });
    return handleResponse(res);
  },

  put: async (endpoint, data) => {
    updateConfig();
    const res = await fetch(`${API_CONFIG.baseUrl}${endpoint}`, {
      method: 'PUT',
      headers: getHeaders(),
      body: JSON.stringify(data)
    });
    return handleResponse(res);
  },

  patch: async (endpoint) => {
    updateConfig();
    const res = await fetch(`${API_CONFIG.baseUrl}${endpoint}`, {
      method: 'PATCH',
      headers: getHeaders()
    });
    return handleResponse(res);
  },

  delete: async (endpoint) => {
    updateConfig();
    const res = await fetch(`${API_CONFIG.baseUrl}${endpoint}`, {
      method: 'DELETE',
      headers: getHeaders()
    });
    return handleResponse(res);
  },

  upload: async (endpoint, formData) => {
    updateConfig();
    const res = await fetch(`${API_CONFIG.baseUrl}${endpoint}`, {
      method: 'POST',
      headers: {
        'Authorization': API_CONFIG.token ? `Bearer ${API_CONFIG.token}` : ''
      },
      body: formData
    });
    return handleResponse(res);
  }
};