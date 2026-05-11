# MediLink Frontend

Enterprise-grade dashboard for the MediLink microservices architecture.

## Architecture

```
frontend/
├── index.html          # Entry point
├── assets/
│   ├── css/
│   │   └── medilink.css    # Component styles
│   └── js/
│       ├── api.js          # Fetch wrapper + endpoint definitions
│       ├── auth.js         # JWT/Keycloak management
│       ├── app.js          # Router + dynamic rendering
│       └── charts.js       # Chart.js visualizations
└── README.md
```

## API Endpoints (via Gateway on port 8560)

| Endpoint | Method | Auth Required | Description |
|----------|--------|---------------|-------------|
| `/api/ordonnances` | GET | DOCTOR | List all prescriptions |
| `/api/ordonnances` | POST | DOCTOR | Create prescription |
| `/api/ordonnances/{id}` | GET | DOCTOR/PATIENT/ADMIN | Get by ID |
| `/api/ordonnances/patient/{id}` | GET | DOCTOR/PATIENT/ADMIN | Filter by patient |
| `/api/ordonnances/{id}/cancel` | PATCH | DOCTOR | Cancel prescription |
| `/api/ordonnances/{id}` | DELETE | ADMIN | Delete prescription |
| `/api/notifications` | GET | ADMIN | List all notifications |
| `/api/notifications/user/{id}` | GET | Authenticated | Filter by user |
| `/api/notifications/{id}/read` | PATCH | Authenticated | Mark as read |

## Authentication Flow

1. **Login** via Keycloak at `http://localhost:8080`
2. **Configure realm**: `medilink` with client `medilink-client`
3. **Roles**: DOCTOR, PATIENT, ADMIN
4. **Get token**: Password grant type
5. **Store**: LocalStorage key `ml_token`

## Development

```bash
# Start all services
docker-compose up --build

# Frontend is static - serve via nginx or Gateway static endpoint
# Or open index.html directly (for development only)
```

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `ml_gateway` | `http://localhost:8560` | API Gateway URL |
| `ml_token` | - | JWT token from Keycloak |