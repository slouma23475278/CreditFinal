# 🏥 MediLink — Architecture Microservices

> Projet de validation — Applications Web Distribuées (5SAE)

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        CLIENT / FRONT                        │
└────────────────────────────┬────────────────────────────────┘
                             │ HTTP
                    ┌────────▼────────┐
                    │   API GATEWAY   │  :8560
                    │ Spring Cloud GW │
                    │  + Keycloak JWT │
                    └────┬───────┬────┘
                         │       │
          ┌──────────────▼┐     ┌▼──────────────────┐
          │  ORDONNANCE   │     │   NOTIFICATION    │
          │   SERVICE     │     │     SERVICE       │
          │  :8085        │     │     :8084         │
          │  MySQL + JPA  │─────│  MongoDB (NoSQL)  │
          │  Feign Client │ 3   │  RabbitMQ Consumer│
          │  RabbitMQ Pub │ scénarios              │
          └───────────────┘     └───────────────────┘
                    │ register         │ register
         ┌──────────▼──────────────────▼──────────┐
         │           EUREKA SERVER                 │  :8761
         └─────────────────────────────────────────┘
                    │ config fetch
         ┌──────────▼──────────────────────────────┐
         │          CONFIG SERVER                   │  :8888
         └─────────────────────────────────────────┘
```

---

## 📦 Services

| Service | Port | Tech | Description |
|---------|------|------|-------------|
| Eureka Server | 8761 | Spring Cloud Netflix | Service Discovery |
| Config Server | 8888 | Spring Cloud Config | Config centralisée |
| API Gateway | 8560 | Spring Cloud Gateway | Routage + Sécurité Keycloak |
| Ordonnance MS | 8085 | Spring Boot + MySQL + Feign + RabbitMQ | MS principal |
| Notification MS | 8084 | Spring Boot + **MongoDB** + RabbitMQ | MS technologie avancée |
| Keycloak | 8080 | Keycloak 25 | Auth + JWT + Rôles |
| RabbitMQ | 5672/15672 | RabbitMQ 3.13 | Message broker asynchrone |

---

## 🚀 Démarrage rapide

### 1. Lancer tout le projet
```bash
docker-compose up --build
```

### 2. Vérifier les services
- Eureka: http://localhost:8761
- RabbitMQ UI: http://localhost:15672 (guest/guest)
- Keycloak: http://localhost:8080 (admin/admin)
- Swagger Ordonnances: http://localhost:8085/swagger-ui.html
- Swagger Notifications: http://localhost:8084/swagger-ui.html
- Frontend: ouvrir `frontend/index.html` dans le navigateur

### 3. Configurer Keycloak
1. Aller sur http://localhost:8080
2. Créer un realm: `medilink`
3. Créer un client: `medilink-client` (Direct Access Grants: ON)
4. Créer les rôles: `DOCTOR`, `PATIENT`, `ADMIN`
5. Créer un utilisateur, lui assigner le rôle `DOCTOR`
6. Obtenir un token:
```bash
curl -X POST http://localhost:8080/realms/medilink/protocol/openid-connect/token \
  -d "grant_type=password&client_id=medilink-client&username=doctor1&password=password"
```

---

## 🔁 3 Scénarios de communication Feign

### Scénario 1 — Créer une ordonnance
```bash
POST http://localhost:8560/api/ordonnances
Authorization: Bearer <token>
{
  "doctorId": 1, "doctorName": "Dr. Ben Ali",
  "patientId": 2, "patientName": "Ahmed Trabelsi",
  "diagnosis": "Hypertension", "medications": ["Amlodipine 5mg"]
}
# → Feign POST /api/notifications (ORDONNANCE_CREATED)
# → RabbitMQ event CREATED
```

### Scénario 2 — Modifier une ordonnance
```bash
PUT http://localhost:8560/api/ordonnances/{id}
Authorization: Bearer <token>
# → Feign POST /api/notifications (ORDONNANCE_UPDATED)
# → RabbitMQ event UPDATED
```

### Scénario 3 — Annuler une ordonnance
```bash
PATCH http://localhost:8560/api/ordonnances/{id}/cancel
Authorization: Bearer <token>
# → Feign POST /api/notifications (ORDONNANCE_CANCELLED)
# → RabbitMQ event CANCELLED
```

---

## 🔒 Sécurité — Keycloak + Rôles

| Endpoint | Rôle requis |
|----------|------------|
| POST /api/ordonnances | DOCTOR |
| PUT /api/ordonnances/{id} | DOCTOR |
| PATCH /api/ordonnances/{id}/cancel | DOCTOR |
| GET /api/ordonnances | DOCTOR, ADMIN |
| GET /api/ordonnances/patient/{id} | DOCTOR, PATIENT, ADMIN |
| GET /api/notifications | ADMIN |
| GET /api/notifications/user/{id} | PATIENT, DOCTOR, ADMIN |
| DELETE /api/notifications/{id} | ADMIN |

---

## 📈 Valeurs ajoutées

- ✅ **MongoDB** pour Notification Service (changement de technologie NoSQL)
- ✅ **RabbitMQ** communication asynchrone en plus du Feign synchrone
- ✅ **Feign Fallback** pour la résilience
- ✅ **Swagger/OpenAPI** sur les deux microservices
- ✅ **Validation** des DTOs avec Jakarta Validation
- ✅ **Docker Compose** avec healthchecks et ordre de démarrage
- ✅ **Frontend** interactif HTML/JS

---

## 📁 Structure du projet

```
medilink-exam/
├── eureka-server/          # Service Discovery
├── config-server/          # Configuration centralisée
│   └── src/main/resources/config/
│       ├── ordonnance-service.properties
│       ├── notification-service.properties
│       └── gateway.properties
├── gateway/                # API Gateway + Keycloak
├── ordonnance-service/     # MS1 - MySQL + Feign + RabbitMQ
│   └── src/main/java/com/medilink/ordonnance/
│       ├── controller/     # REST endpoints
│       ├── service/        # Logique métier + Feign calls
│       ├── entity/         # JPA Entities (MySQL)
│       ├── repository/     # Spring Data JPA
│       ├── feign/          # Feign Client + Fallback
│       ├── messaging/      # RabbitMQ Publisher
│       └── config/         # Security + RabbitMQ config
├── notification-service/   # MS2 - MongoDB + RabbitMQ Consumer
│   └── src/main/java/com/medilink/notification/
│       ├── controller/     # REST endpoints
│       ├── service/        # Logique métier
│       ├── entity/         # MongoDB Documents
│       ├── repository/     # Spring Data MongoDB
│       ├── messaging/      # RabbitMQ Consumer
│       └── config/         # Security + RabbitMQ config
├── frontend/               # Frontend HTML/JS
│   └── index.html
└── docker-compose.yml      # Orchestration complète
```
