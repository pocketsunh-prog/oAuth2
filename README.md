# OAuth2 Authorization Server

A complete OAuth2 authorization server with a Spring Boot backend and React frontend.

## Tech Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| Backend | Spring Boot | 4.1.0 |
| Backend | Spring Security / Authorization Server | 7.x |
| Backend | Java | 21 |
| Backend | H2 Database | 2.x |
| Backend | springdoc-openapi (Swagger) | 2.8.4 |
| Frontend | React | 18.3 |
| Frontend | Vite | 5.4 |
| Frontend | Tailwind CSS | 3.4 |

## Project Structure

```
OAuth/
├── backend/          # Spring Boot OAuth2 Authorization Server
│   ├── src/
│   │   └── main/
│   │       ├── java/com/oauth/server/
│   │       │   ├── OAuth2ServerApplication.java
│   │       │   ├── config/         # Security, Authorization Server, OpenAPI configs
│   │       │   ├── controller/     # REST controllers (Auth, Token, Exception Handler)
│   │       │   ├── model/          # JPA entities (User, OAuth2Client, UserToken)
│   │       │   ├── repository/     # Spring Data JPA repositories
│   │       │   ├── service/        # Business logic services
│   │       │   ├── dto/            # Data transfer objects
│   │       │   └── security/       # Custom authentication filter & provider
│   │       └── resources/
│   │           ├── application.yml
│   │           └── data.sql        # Seed data
│   └── pom.xml
│
└── frontend/         # React + Tailwind CSS Frontend
    ├── src/
    │   ├── api/          # API client
    │   ├── components/   # Reusable components (Layout)
    │   ├── context/      # Auth context provider
    │   ├── pages/        # LoginPage, TokenManagerPage
    │   ├── App.jsx
    │   ├── main.jsx
    │   └── index.css
    ├── index.html
    ├── vite.config.js
    ├── tailwind.config.js
    └── package.json
```

## Backend

### Prerequisites
- Java 21+ (required by Spring Boot 4.x)
- Maven 3.8+

### Run

```bash
cd backend
mvn spring-boot:run
```

The backend starts on `http://localhost:8080`.

### API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/auth/register` | Register a new user |
| POST | `/api/auth/login` | Log in, receive tokens |
| GET  | `/api/auth/me` | Get current user profile |
| GET  | `/api/tokens` | List user's tokens |
| DELETE | `/api/tokens/{id}` | Revoke a token |
| DELETE | `/api/tokens` | Revoke all tokens |

### Admin API Endpoints (require ADMIN role)

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/admin/users` | Create a new user |
| GET  | `/api/admin/users` | List all users |
| POST | `/api/admin/service-tokens` | Create a service token |
| GET  | `/api/admin/service-tokens` | List all service tokens |
| DELETE | `/api/admin/service-tokens/{id}` | Revoke a service token |

### OAuth2 Endpoints

| Endpoint | Description |
|----------|-------------|
| `/oauth2/authorize` | Authorization endpoint |
| `/oauth2/token` | Token endpoint |
| `/oauth2/introspect` | Token introspection |
| `/.well-known/jwks.json` | JSON Web Key Set |

### Tools

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **H2 Console**: http://localhost:8080/h2-console (JDBC URL: `jdbc:h2:mem:oauth2db`, user: `sa`)

### Demo Credentials

**Admin User (seeded in data.sql):**
- Username: `admin`
- Password: `admin123`
- Role: `ADMIN` (can access Admin Dashboard)

**OAuth2 Client (seeded in data.sql):**
- Client ID: `frontend-client`
- Client Secret: `frontend-secret`
- Grant Types: `authorization_code`, `refresh_token`, `client_credentials`
- Scopes: `read`, `write`
- Redirect URI: `http://localhost:5173/callback`

## Frontend

### Prerequisites
- Node.js 18+
- npm 9+

### Run

```bash
cd frontend
npm install
npm run dev
```

The frontend starts on `http://localhost:5173`.

### Pages
- **Login** (`/login`) - Authentication and registration
- **Token Manager** (`/tokens`) - View and revoke OAuth2 tokens
- **Admin Dashboard** (`/admin`) - User and service token management (admin only)

## Architecture

### Roles

Users have a `role` field that determines their access level:
- **USER** — Can log in, view their own tokens, and revoke them
- **ADMIN** — Can do everything a USER can, plus access the Admin Dashboard to create users and manage service tokens

### Service Tokens

Service tokens (`srv_` prefixed) are long-lived API keys for service-to-service communication:
- Created and managed by admins via the Admin Dashboard
- Not tied to a user session — they authenticate as standalone credentials
- Optional expiration date and scopes
- The full token value is shown only once at creation time; afterwards only a masked preview is displayed

### Authentication Flow
1. User enters credentials on the login page
2. Backend validates credentials against the H2 database
3. Backend generates access/refresh tokens and stores them in the database
4. Frontend stores the access token in localStorage
5. Subsequent API calls include the token in the `Authorization: Bearer <token>` header
6. The `BearerTokenAuthenticationFilter` validates the token against the database

### Token Management
- Tokens are stored in the `user_tokens` table
- Each token is associated with a user and client
- Users can list, view, and revoke their tokens
- Revoked tokens are immediately invalidated
