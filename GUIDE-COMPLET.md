# 🏥 **Guide Complet - Faire Fonctionner MediLink (Projet Microservices)**

Ce guide exhaustif vous permettra de lancer **TOUT le projet MediLink** en 15-20 minutes, y compris l'architecture complète (Eureka, Config Server, Gateway, microservices, frontend, Keycloak, bases de données). Le projet est **entièrement conteneurisé avec Docker Compose** – aucune installation complexe requise !

## 📋 **Prérequis (Vérification rapide - 1 minute)**

1. **Docker & Docker Compose installés** :
   ```bash
   docker --version
   docker compose version
   ```
   - Si absent : [Télécharger Docker Desktop](https://www.docker.com/products/docker-desktop/) (Windows/Mac) ou `sudo apt install docker.io docker-compose` (Linux).

2. **Git** (optionnel, si clonage) :
   ```bash
   git --version
   ```

3. **Navigateur web** moderne (Chrome/Firefox).

4. **Rien d'autre** ! Pas de Java/Maven/Node manuellement – tout est dans Docker.

**Espace disque** : ~2GB (images Docker).

---

## 🚀 **ÉTAPE 1 : Lancement Complet (3 minutes)**

Dans le dossier du projet (`c:/Users/USER/Downloads/medilink-exam/medilink-exam`) :

```bash
docker-compose up --build
```

**Ce qui se passe** (automatique) :
```
1. mysql-db:3306    (Ordonnances - SQL)
2. mongodb:27017    (Notifications - NoSQL)
3. rabbitmq:5672    (Messaging asynchrone)
4. keycloak:8080    (Authentification)
5. eureka-server:8761 (Discovery)
6. config-server:8888 (Configs)
7. gateway:8560     (API Gateway)
8. ordonnance-service:8085
9. notification-service:8084
```

**Attendre 2-3 minutes** jusqu'à voir :
```
ordonnance-service    | Started OrdonnanceApplication in 12.345 seconds
notification-service  | Started NotificationApplication in 10.987 seconds
```
**Ctrl+C** pour arrêter plus tard.

---

## ✅ **ÉTAPE 2 : Vérification Services (1 minute)**

Ouvrez ces URLs **dans l'ordre** :

| Service | URL | Statut attendu |
|---------|-----|----------------|
| **Frontend** | `file:///c:/Users/USER/Downloads/medilink-exam/medilink-exam/frontend/index.html` | Dashboard interactif |
| **Eureka** | [http://localhost:8761](http://localhost:8761) | 6 services enregistrés (vert) |
| **API Gateway** | [http://localhost:8560/actuator/health](http://localhost:8560/actuator/health) | `{\"status\":\"UP\"}` |
| **Swagger Ordonnances** | [http://localhost:8085/swagger-ui.html](http://localhost:8085/swagger-ui.html) | Interface API |
| **Swagger Notifications** | [http://localhost:8084/swagger-ui.html](http://localhost:8084/swagger-ui.html) | Interface API |
| **Keycloak** | [http://localhost:8080](http://localhost:8080) | Login admin/admin |
| **RabbitMQ** | [http://localhost:15672](http://localhost:15672) | UI guest/guest |

**Problème ?** Voir [Dépannage](#🔧-dépannage).

---

## 🔐 **ÉTAPE 3 : Configuration Keycloak (3 minutes)**

**Indispensable pour JWT + Rôles** !

1. Ouvrir **Keycloak** : [http://localhost:8080](http://localhost:8080)
2. **Login** : `admin` / `admin`
3. **Créer Realm** :
   - Clic **\"Create realm\"** → Nom : `medilink` → **Create**
4. **Créer Client** :
   - `Clients` → **\"Create client\"**
   - Client type : `OpenID Connect`
   - Client ID : `medilink-client` → **Next** → **Save**
   - **Activation** : `Client authentication: ON`, `Direct access grants: ON`
5. **Créer Rôles** :
   - `Realm roles` → **\"Create role\"** :
     | Nom | Description |
     |-----|-------------|
     | `DOCTOR` | Médecins créent ordonnances |
     | `PATIENT` | Patients voient notifications |
     | `ADMIN` | Admin total |
6. **Créer Utilisateur** :
   - `Users` → **\"Add user\"** → `doctor1` / `password` → **Save**
   - Onglet **Credentials** → `Set password` → `password` (temporary OFF)
   - Onglet **Role mapping** → **Assign role** → `DOCTOR`

**Token rapide** (dans Frontend ou curl) :
```bash
curl -X POST http://localhost:8080/realms/medilink/protocol/openid-connect/token ^
  -d \"grant_type=password&client_id=medilink-client&username=doctor1&password=password\"
```
Copiez `access_token` dans le frontend (champ JWT).

---

## 🎮 **ÉTAPE 4 : Test Complet via Frontend (5 minutes)**

Ouvrez **`frontend/index.html`** :

1. **Configurer Token** :
   - Remplir JWT (du curl ci-dessus)
   - **\"Enregistrer\"**

2. **Scénario 1 - Créer Ordonnance** :
   - Onglet **\"Ordonnances\"**
   - Remplir formulaire → **\"Créer l'ordonnance\"**
   - ✅ Notification Feign + RabbitMQ envoyée !

3. **Scénario 2 - Annuler** :
   - Clic **\"Annuler\"** sur une ordonnance
   - ✅ Notification via Feign (fallback activé si HS)

4. **Vérifier Notifications** :
   - Onglet **\"Notifications\"** → User ID `2`
   - ✅ Messages RabbitMQ stockés en MongoDB

**Stats Dashboard** s'actualisent automatiquement !

---

## 🔄 **ÉTAPE 5 : Commandes Utiles**

| Action | Commande |
|--------|----------|
| **Relancer** | `docker-compose up --build` |
| **Logs service** | `docker-compose logs ordonnance-service` |
| **Redémarrer un service** | `docker-compose restart ordonnance-service` |
| **Arrêter tout** | `docker-compose down` |
| **Nettoyer volumes** (reset DB) | `docker-compose down -v` |
| **Build sans cache** | `docker-compose build --no-cache` |

---

## 🧪 **Tests API Directs (Swagger)**

**Ordonnances** : [http://localhost:8085/swagger-ui.html](http://localhost:8085/swagger-ui.html)
```
POST /ordonnances  (Header: Authorization: Bearer <token>)
{
  \"doctorId\": 1, \"patientId\": 2,
  \"diagnosis\": \"Hypertension\",
  \"medications\": [\"Amlodipine 5mg\"]
}
```

---

## 🔧 **DÉPANNAGE Commun**

| Problème | Solution |
|----------|----------|
| **\"port already in use\"** | `docker-compose down` + `netstat -ano \| findstr :8080` (kill process) |
| **Eureka vide** | Attendre 2min + vérifier logs : `docker-compose logs eureka-server` |
| **Keycloak realm manquant** | Recréer `medilink` + client |
| **Token invalide** | Vérifier rôles/utilisateur dans Keycloak |
| **RabbitMQ échoue** | `docker-compose logs rabbitmq` |
| **DB connexion** | `docker-compose down -v &amp;&amp; docker-compose up` (reset) |
| **Windows lent** | Allouer + RAM à Docker Desktop (6GB+) |

**Logs complets** : `docker-compose logs -f`

---

## 📊 **Vérification Finale - Tout Fonctionne Si :**

- [ ] `docker-compose up` sans erreur
- [ ] Eureka montre 6 services verts
- [ ] Frontend charge sans erreur
- [ ] Token Keycloak valide
- [ ] Création ordonnance → notification auto
- [ ] RabbitMQ queues non vides : [http://localhost:15672](http://localhost:15672)
- [ ] MongoDB a données : `docker exec -it mongodb mongosh medilink_notifications`

## 🎉 **Félicitations !**

Votre stack **microservices complète** tourne :
- **Spring Cloud** (Eureka/Config/Gateway)
- **2 Microservices** (MySQL + MongoDB)
- **Messaging** (RabbitMQ + Feign)
- **Sécurité** (Keycloak JWT)
- **Frontend** interactif
- **Dockerisé** 100%

**Pour arréter** : `Ctrl+C` puis `docker-compose down`.

**Projet prêt pour examen/démo !** 🚀

