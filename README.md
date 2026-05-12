<div align="center">

# 🔔 Notification Service
### MediLink — Distributed Microservices Platform

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.2-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![MongoDB](https://img.shields.io/badge/MongoDB-7.0-47A248?style=for-the-badge&logo=mongodb&logoColor=white)
![RabbitMQ](https://img.shields.io/badge/RabbitMQ-AMQP-FF6600?style=for-the-badge&logo=rabbitmq&logoColor=white)
![Keycloak](https://img.shields.io/badge/Keycloak-JWT%20%2B%20RBAC-4A90D9?style=for-the-badge&logo=keycloak&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Containerized-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![Eureka](https://img.shields.io/badge/Eureka-Service%20Discovery-green?style=for-the-badge)

**Port:** `8084` &nbsp;|&nbsp; **Database:** MongoDB `medilink_notifications` &nbsp;|&nbsp; **Message Queue:** `ordonnance.notification.queue`

</div>

---

## 📋 Table des matières

- [Vue d'ensemble](#-vue-densemble)
- [Architecture](#-architecture)
- [Dual Communication Pattern](#-dual-communication-pattern-feign--rabbitmq)
- [API Endpoints](#-api-endpoints)
- [MongoDB — Technologie Avancée](#-mongodb--technologie-avancée)
- [Sécurité & RBAC](#-sécurité--rbac-keycloak--jwt)
- [Structure du projet](#-structure-du-projet)
- [Configuration](#-configuration)
- [Lancement](#-lancement)
- [Tests](#-tests)

---

## 🎯 Vue d'ensemble

Le **Notification Service** est le microservice responsable de la **gestion et persistance des notifications** dans la plateforme MediLink. Il est déclenché par deux mécanismes complémentaires lors d'événements sur les ordonnances médicales.

**Ce qui le rend unique dans l'architecture MediLink :**

| Caractéristique | Détail |
|---|---|
| **Technologie avancée** | MongoDB (NoSQL) — différent de MySQL utilisé par `ordonnance-service` |
| **Double canal** | Reçoit les notifications via **Feign** (sync) ET **RabbitMQ** (async) |
| **Polyglot Persistence** | Schéma flexible adapté aux notifications multi-types |
| **RBAC Keycloak** | Contrôle d'accès par rôles `DOCTOR`, `PATIENT`, `ADMIN` |
| **Service Discovery** | Enregistré dans Eureka, accessible via `lb://notification-service` |

---

## 🏗️ Architecture

```
                                   ┌─────────────────────────────────────┐
                                   │         NOTIFICATION SERVICE         │
                                   │              :8084                   │
                                   │                                      │
  ┌──────────────────┐  Feign      │  ┌─────────────────────────────┐   │
  │                  │ ──────────► │  │   NotificationController    │   │
  │  ORDONNANCE      │  (sync)     │  │   POST /api/notifications   │   │
  │  SERVICE         │             │  │   GET  /user/{id}           │   │
  │  :8085           │             │  │   PATCH /{id}/read          │   │
  │                  │  RabbitMQ   │  └──────────────┬──────────────┘   │
  │                  │ ──────────► │                 │                   │
  └──────────────────┘  (async)   │  ┌──────────────▼──────────────┐   │
           │                      │  │   NotificationService        │   │
           │                      │  └──────────────┬──────────────┘   │
           │                      │                 │                   │
  ┌────────▼──────────┐           │  ┌──────────────▼──────────────┐   │
  │   ordonnance      │           │  │  NotificationRepository     │   │
  │   .exchange       │           │  │  (Spring Data MongoDB)      │   │
  │   (TopicExchange) │           │  └──────────────┬──────────────┘   │
  │                   │           │                 │                   │
  │  routing key:     │           │  ┌──────────────▼──────────────┐   │
  │  ordonnance.      │           │  │       MongoDB 7.0            │   │
  │  notification     │           │  │   medilink_notifications     │   │
  └────────┬──────────┘           │  │   Collection: notifications  │   │
           │                      │  └─────────────────────────────┘   │
  ┌────────▼──────────┐           │                                      │
  │  ordonnance.      │           │  ┌─────────────────────────────┐   │
  │  notification     │ ────────► │  │  OrdonnanceEventConsumer    │   │
  │  .queue           │  consume  │  │  @RabbitListener            │   │
  │  (durable)        │           │  └─────────────────────────────┘   │
  └───────────────────┘           └─────────────────────────────────────┘
```

---

## 🔄 Dual Communication Pattern (Feign + RabbitMQ)

C'est le cœur de la valeur ajoutée du projet. Chaque action sur une ordonnance génère **deux notifications** dans MongoDB.

### Flux complet pour une création d'ordonnance

```
POST /api/ordonnances
        │
        ├─── [1] Feign (synchrone) ──────────────────────────────────────►
        │         POST http://notification-service/api/notifications        MongoDB
        │         body: { type: "ORDONNANCE_CREATED", userId: 2, ... }  ──► type: "ORDONNANCE_CREATED"
        │
        └─── [2] RabbitMQ (asynchrone) ──────────────────────────────────►
                  ordonnance.exchange → ordonnance.notification.queue       MongoDB
                  event: { eventType: "CREATED", patientId: 2, ... }    ──► type: "ASYNC_CREATED"
```

### Résultat dans MongoDB après les 3 scénarios

```javascript
db.notifications.find().pretty()

// Scénario 1 — Création
{ type: "ORDONNANCE_CREATED",  source: "FEIGN",    status: "SENT", ordonnanceId: 1 }
{ type: "ASYNC_CREATED",        source: "RABBITMQ", status: "SENT", ordonnanceId: 1 }

// Scénario 2 — Modification
{ type: "ORDONNANCE_UPDATED",   source: "FEIGN",    status: "SENT", ordonnanceId: 1 }
{ type: "ASYNC_UPDATED",         source: "RABBITMQ", status: "SENT", ordonnanceId: 1 }

// Scénario 3 — Annulation
{ type: "ORDONNANCE_CANCELLED", source: "FEIGN",    status: "SENT", ordonnanceId: 1 }
{ type: "ASYNC_CANCELLED",       source: "RABBITMQ", status: "SENT", ordonnanceId: 1 }

// Total: 6 documents
db.notifications.countDocuments() // → 6
```

### Avantage de la double communication

| Aspect | Feign (sync) | RabbitMQ (async) |
|---|---|---|
| **Timing** | Immédiat, bloquant | Non-bloquant, quelques ms après |
| **Résilience** | Fallback si service down | Messages persistés en queue |
| **Garantie** | Best-effort avec fallback | At-least-once delivery |
| **Cas d'usage** | Confirmation immédiate | Audit trail, retry automatique |

---

## 📡 API Endpoints

Base URL via Gateway: `http://localhost:8560/api/notifications`

| Méthode | Endpoint | Rôle requis | Description |
|---|---|---|---|
| `POST` | `/api/notifications` | *(interne Feign)* | Créer une notification |
| `GET` | `/api/notifications` | `ADMIN` | Lister toutes les notifications |
| `GET` | `/api/notifications/user/{userId}` | `DOCTOR` `PATIENT` `ADMIN` | Notifications d'un utilisateur |
| `GET` | `/api/notifications/user/{userId}/unread` | `DOCTOR` `PATIENT` `ADMIN` | Notifications non lues |
| `GET` | `/api/notifications/user/{userId}/count` | `DOCTOR` `PATIENT` `ADMIN` | Nombre de non lues |
| `PATCH` | `/api/notifications/{id}/read` | `DOCTOR` `PATIENT` `ADMIN` | Marquer comme lue |
| `DELETE` | `/api/notifications/{id}` | `ADMIN` | Supprimer une notification |

### Exemples de requêtes

**Créer une notification (appelé par Feign depuis ordonnance-service)**
```http
POST /api/notifications
Content-Type: application/json

{
  "userId": 2,
  "recipientName": "Ahmed Trabelsi",
  "type": "ORDONNANCE_CREATED",
  "message": "Ordonnance #1 créée par Dr. Ben Ali. Diagnostic: Hypertension. Médicaments: [Amlodipine 5mg]",
  "ordonnanceId": 1
}
```

**Récupérer les notifications d'un patient (via Gateway)**
```http
GET /api/notifications/user/2
Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...

HTTP/1.1 200 OK
[
  {
    "id": "65f1a2b3c4d5e6f7a8b9c0d1",
    "userId": 2,
    "type": "ORDONNANCE_CREATED",
    "message": "...",
    "status": "SENT",
    "createdAt": "2025-01-15T10:30:00"
  }
]
```

**Swagger UI:** `http://localhost:8084/swagger-ui.html`

---

## 🍃 MongoDB — Technologie Avancée

> Choix délibéré de MongoDB vs MySQL utilisé par `ordonnance-service` → Pattern **Polyglot Persistence**

### Pourquoi MongoDB pour les notifications ?

- **Schéma flexible** : une notification `CREATED` a des champs différents d'une `CANCELLED`
- **TTL Index** : expiration automatique des vieilles notifications
- **Indexes sur `userId` et `status`** : requêtes rapides `getUnreadByUser()`
- **String ObjectId** : pas besoin de séquence SQL pour les IDs

### Document MongoDB — `Notification.java`

```java
@Document(collection = "notifications")   // Collection MongoDB
public class Notification {
    @Id
    private String id;           // ObjectId MongoDB (ex: "65f1a2b3c4d5e6f7")

    @Indexed                     // Index pour requêtes rapides par userId
    private Long userId;

    private String recipientName;
    private String type;         // ORDONNANCE_CREATED | ASYNC_CREATED | ...

    @Indexed                     // Index pour filtrer SENT/READ/PENDING
    private NotificationStatus status;

    private Long ordonnanceId;

    @CreatedDate                 // Auto-rempli par Spring Data MongoDB
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
}
```

### Commandes MongoDB utiles

```bash
# Accéder au shell MongoDB
docker exec -it mongodb mongosh

# Naviguer dans la base
use medilink_notifications

# Voir toutes les notifications
db.notifications.find().pretty()

# Compter par type
db.notifications.aggregate([
  { $group: { _id: "$type", count: { $sum: 1 } } }
])

# Notifications non lues d'un utilisateur
db.notifications.find({ userId: 2, status: "SENT" })

# Voir les indexes
db.notifications.getIndexes()
```

---

## 🔐 Sécurité & RBAC (Keycloak + JWT)

Le service valide chaque requête via les tokens JWT émis par Keycloak.

```
Client ──► Gateway :8560 ──► [JWT validation] ──► notification-service :8084
                                    │
                                    └─► Extrait realm_access.roles → ["PATIENT"]
                                        → @PreAuthorize("hasAnyRole('PATIENT','DOCTOR','ADMIN')")
```

### Configuration Spring Security

```java
// SecurityConfig.java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/notifications").permitAll()          // Feign interne
    .requestMatchers("/api/notifications/**").authenticated()   // JWT requis
    .anyRequest().permitAll()
)
.oauth2ResourceServer(oauth2 -> oauth2
    .jwt(jwt -> jwt.jwtAuthenticationConverter(keycloakJwtConverter()))
)
```

### Obtenir un token de test

```bash
curl -X POST http://localhost:8080/realms/medilink/protocol/openid-connect/token \
  -d 'grant_type=password&client_id=medilink-client&username=doctor1&password=password'
```

---

## 📁 Structure du projet

```
notification-service/
├── src/main/java/com/medilink/notification/
│   ├── NotificationApplication.java          # Point d'entrée @SpringBootApplication
│   │
│   ├── config/
│   │   ├── RabbitMQConfig.java               # TopicExchange + Queue durable + Jackson converter
│   │   └── SecurityConfig.java               # JWT + Keycloak RBAC
│   │
│   ├── controller/
│   │   └── NotificationController.java       # 7 endpoints REST + @PreAuthorize
│   │
│   ├── dto/
│   │   ├── NotificationDTO.java              # Réponse API
│   │   └── NotificationRequest.java          # Corps requête Feign
│   │
│   ├── entity/
│   │   ├── Notification.java                 # @Document MongoDB + @Indexed
│   │   └── NotificationStatus.java           # Enum: PENDING | SENT | READ | FAILED
│   │
│   ├── messaging/
│   │   ├── OrdonnanceEvent.java              # DTO événement RabbitMQ
│   │   └── OrdonnanceEventConsumer.java      # @RabbitListener consumer
│   │
│   ├── repository/
│   │   └── NotificationRepository.java       # Spring Data MongoDB
│   │
│   └── service/
│       └── NotificationService.java          # Logique métier
│
├── src/main/resources/
│   └── application.properties                # Config minimale (reste dans Config Server)
│
├── Dockerfile                                # Multi-stage Maven + JRE Alpine
└── pom.xml                                   # Spring Boot 3.4.2 + MongoDB + AMQP + OAuth2
```

---

## ⚙️ Configuration

### `application.properties` (config minimale)

```properties
spring.application.name=notification-service
spring.config.import=optional:configserver:http://config-server:8888
management.endpoints.web.exposure.include=refresh,health,info
```

### Config complète (servie par Config Server sur `:8888`)

```properties
server.port=8084

# MongoDB
spring.data.mongodb.host=mongodb
spring.data.mongodb.port=27017
spring.data.mongodb.database=medilink_notifications
spring.data.mongodb.auto-index-creation=true

# Eureka
eureka.client.service-url.defaultZone=http://eureka-server:8761/eureka/
eureka.instance.prefer-ip-address=true

# Keycloak JWT
spring.security.oauth2.resourceserver.jwt.issuer-uri=http://keycloak:8080/realms/medilink

# RabbitMQ
spring.rabbitmq.host=rabbitmq
spring.rabbitmq.port=5672
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest

# Swagger
springdoc.api-docs.path=/v3/api-docs
springdoc.swagger-ui.path=/swagger-ui.html
```

### RabbitMQ Topology

```
Exchange:   ordonnance.exchange  (TopicExchange)
Queue:      ordonnance.notification.queue  (durable: true)
Routing:    ordonnance.notification
Converter:  Jackson2JsonMessageConverter  (JSON ↔ OrdonnanceEvent)
```

---

## 🚀 Lancement

### Via Docker Compose (recommandé)

```bash
# Depuis la racine du projet medilink-exam
docker-compose up --build

# Vérifier que notification-service est UP
docker-compose ps notification-service
curl http://localhost:8084/actuator/health
# → {"status":"UP"}
```

### Ordre de démarrage (géré automatiquement)

```
mongodb ──► rabbitmq ──► eureka-server ──► config-server ──► notification-service
```

### Vérifications post-démarrage

```bash
# 1. Health check
curl http://localhost:8084/actuator/health

# 2. Enregistrement Eureka
# → http://localhost:8761 → NOTIFICATION-SERVICE doit apparaître

# 3. Config Server chargée
curl http://localhost:8888/notification-service/default

# 4. Swagger disponible
# → http://localhost:8084/swagger-ui.html
```

---

## 🧪 Tests

### Test 1 — Sécurité: sans token → 401

```bash
curl http://localhost:8560/api/notifications/user/2
# HTTP 401 Unauthorized
```

### Test 2 — Obtenir un token JWT

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/realms/medilink/protocol/openid-connect/token \
  -d 'grant_type=password&client_id=medilink-client&username=doctor1&password=password' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['access_token'])")
```

### Test 3 — Récupérer les notifications

```bash
curl http://localhost:8560/api/notifications/user/2 \
  -H "Authorization: Bearer $TOKEN"
```

### Test 4 — Vérifier MongoDB directement

```bash
docker exec -it mongodb mongosh \
  --eval "use medilink_notifications; db.notifications.find().pretty()"
```

### Test 5 — Vérifier la queue RabbitMQ

```bash
# Management UI
open http://localhost:15672  # guest/guest
# → Queues → ordonnance.notification.queue
```

### Test 6 — Démo communication asynchrone (2 terminaux)

```bash
# Terminal A: suivre les logs
docker-compose logs -f notification-service | grep RabbitMQ

# Terminal B: créer une ordonnance (déclenche le flow)
curl -X POST http://localhost:8560/api/ordonnances \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"doctorId":1,"doctorName":"Dr. Ali","patientId":2,"patientName":"Ahmed","diagnosis":"Test","medications":["Ibuprofene"]}'

# Terminal A affiche automatiquement:
# [RabbitMQ] Received event: type=CREATED, ordonnanceId=X, patientId=2
# [RabbitMQ] Notification async saved for patient 2
```

---

## 🔗 Services MediLink associés

| Service | Port | Rôle |
|---|---|---|
| [Ordonnance Service](../ordonnance-service) | `:8085` | Appelle ce service via **Feign** + **RabbitMQ** |
| [API Gateway](../gateway) | `:8560` | Point d'entrée unique, valide les JWT |
| [Eureka Server](../eureka-server) | `:8761` | Service Discovery |
| [Config Server](../config-server) | `:8888` | Configuration centralisée |
| [Keycloak](http://localhost:8080) | `:8080` | Identity Provider |
| [RabbitMQ](http://localhost:15672) | `:5672/15672` | Message Broker |
| [MongoDB](http://localhost:27017) | `:27017` | Base de données NoSQL |

---

<div align="center">
  <sub>MediLink Platform — 5SAE — Spring Boot 3.4.2 + Spring Cloud 2024.0.0</sub>
</div>