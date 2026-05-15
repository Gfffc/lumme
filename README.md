# Lumme — App + Backend Unificado

Projeto integrado contendo:
- **App Android** (Kotlin + Jetpack Compose) — pasta `app/`
- **Backend Node.js** (Express + Prisma + TypeScript) — pasta `src/`

Este projeto consolida as correções aplicadas durante o desenvolvimento:
- ✅ Botão de adicionar dispositivo **funcional** (cria dispositivo demo no servidor)
- ✅ Fix do erro 401 (snackbar não aparece mais quando o app inicia)
- ✅ Deep link `lumme://oauth-callback` funcionando
- ✅ `launchMode="singleTask"` (volta na mesma instância da Activity)
- ✅ Abre OAuth no Chrome externo (Custom Tab causava bloqueio)
- ✅ NavGraph reativo sem loop
- ✅ CSRF tolerante a race condition no mock
- ✅ Logs `tag:LummeOAuth` no Logcat
- ✅ Timeout HTTP 60s (Render free hiberna)
- ✅ Backend Postgres com `prisma db push` no build

## Configuração já aplicada

| Item | Valor |
|---|---|
| Backend Base URL | `https://lumme-2kng.onrender.com/api/v1/` |
| Deep link | `lumme://oauth-callback` |
| Backend Database | Postgres (no schema.prisma) |
| Build command | `prisma generate && prisma db push --accept-data-loss && tsc` |

---

## 1) App Android

### Como rodar
1. Abra a **raiz do repositório** (não a pasta `app/`) no Android Studio
2. Aguarda Gradle sync (~5min na 1ª vez)
3. Se faltar wrapper, rode `gradle wrapper --gradle-version 8.10`
4. Cria emulador **Pixel** com Google Play Services (precisa ter Chrome)
5. ▶ Run app

### Fluxo de uso
1. Clica em **"Conectar com SmartThings"**
2. Chrome abre a página verde "Conectando ao Lumme..."
3. Página dispara `lumme://oauth-callback?code=<jwt>` automaticamente
4. App captura, troca o code por JWT, vai para o Dashboard
5. No menu lateral, **Dispositivos** → vê os 7 dispositivos mockados
6. Clica no **FAB +** (canto inferior direito) → adiciona um dispositivo demo
7. Long-press num dispositivo `demo-...` → opção de remover

### Debug
No Logcat, filtra por `tag:LummeOAuth` pra ver o fluxo passo a passo:
```
onNewIntent called
handleOAuthIntent intent=Intent {...}
deep link scheme=lumme host=oauth-callback code=... state=...
VM.onCallback code=... state=...
exchange OK — JWT salvo
```

---

## 2) Backend

### Endpoints

| Método | Path | Auth | Descrição |
|---|---|---|---|
| GET  | `/health`                          | não | Status |
| POST | `/api/v1/auth/smartthings/start`   | não | Inicia OAuth |
| GET  | `/api/v1/auth/smartthings/callback`| não | Callback (página HTML) |
| POST | `/api/v1/auth/smartthings/exchange`| não | Troca code por JWT |
| POST | `/api/v1/auth/refresh`             | JWT | Renova JWT |
| GET  | `/api/v1/devices`                  | JWT | Lista (SmartThings + demo) |
| GET  | `/api/v1/devices/:id`              | JWT | Detalhe |
| **POST** | **`/api/v1/devices`**          | **JWT** | **Cria dispositivo demo** |
| **DELETE** | **`/api/v1/devices/:id`**    | **JWT** | **Remove dispositivo demo** |
| POST | `/api/v1/devices/:id/command`      | JWT | Comando on/off |
| GET  | `/api/v1/consumption`              | JWT | Consumo total |
| GET  | `/api/v1/goals/active`             | JWT | Meta ativa |
| POST | `/api/v1/goals`                    | JWT | Cria meta |

### Variáveis de ambiente no Render

| Variável | Valor |
|---|---|
| `DATABASE_URL` | Internal Database URL do Postgres |
| `JWT_SECRET` | 32+ chars (gerar com `openssl rand -base64 32`) |
| `TOKEN_ENCRYPTION_KEY` | 64 chars hex (gerar com `openssl rand -hex 32`) |
| `MOCK_SMARTTHINGS` | `true` |
| `APP_REDIRECT_URI` | `lumme://oauth-callback` |
| `ST_REDIRECT_URI` | `https://lumme-2kng.onrender.com/api/v1/auth/smartthings/callback` |
| `NODE_ENV` | `production` |

### Adicionar dispositivo via curl

```bash
JWT="seu_jwt_aqui"

curl -X POST https://lumme-2kng.onrender.com/api/v1/devices \
  -H "Authorization: Bearer $JWT" \
  -H "Content-Type: application/json" \
  -d '{
    "label": "Ventilador Sala",
    "type": "OUTLET",
    "roomName": "Sala"
  }'
```

Resposta:
```json
{
  "id": "demo-1778801234567-abc123",
  "label": "Ventilador Sala",
  "roomName": "Sala",
  "type": "OUTLET",
  "status": { "isOn": false, "powerWatts": null, "online": true },
  "capabilities": ["switch"],
  "lastUpdate": "2026-05-14T..."
}
```

### Rodar localmente (dev)

```bash
npm install
cp .env.example .env
# Edita .env: gerar JWT_SECRET, TOKEN_ENCRYPTION_KEY, DATABASE_URL local
npx prisma generate
npx prisma db push
npm run dev
```

---

## Estrutura

```
lumme/
├── app/                     ← Android (Kotlin + Compose)
│   └── src/main/
│       ├── java/com/univesp/lumme/
│       │   ├── data/        Retrofit + Room + DataStore
│       │   ├── domain/      Models + interfaces
│       │   ├── di/          Hilt
│       │   ├── presentation/UI Compose + ViewModels
│       │   ├── LummeApp.kt
│       │   └── MainActivity.kt
│       ├── res/             XML resources
│       └── AndroidManifest.xml
├── src/                     ← Backend (Node + TypeScript)
│   ├── auth/
│   ├── devices/             Inclui POST /devices (criar) e DELETE
│   ├── consumption/
│   ├── goals/
│   ├── smartthings/
│   ├── app.ts
│   └── server.ts
├── prisma/schema.prisma     PostgreSQL
├── package.json             Backend
├── tsconfig.json
├── build.gradle.kts         Android root
├── settings.gradle.kts
└── gradle/libs.versions.toml
```
