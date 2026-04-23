# 🚀 Backend Engineering Assignment — Grid07

A high-performance Spring Boot microservice with Redis-backed guardrails, atomic concurrency protection, and a smart notification engine.

---

## 🛠️ Tech Stack

- Java 17
- Spring Boot 3.x
- PostgreSQL (source of truth)
- Redis (gatekeeper — counters, cooldowns, notifications)
- Docker Compose

---

## ⚡ How to Run

### Prerequisites
- Java 17+
- Docker Desktop

### Steps

```bash
# 1. Clone the repo
git clone https://github.com/Prathamdiwakar/backend.git
cd backend

# 2. Start PostgreSQL and Redis using Docker
docker-compose up -d

# 3. Run the Spring Boot app
./mvnw spring-boot:run
```

App runs on `http://localhost:8080`

---

## 📡 API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/posts` | Create a new post |
| POST | `/api/posts/{postId}/like` | Like a post (+20 virality) |
| POST | `/api/posts/{postId}/comments` | Add a comment (human or bot) |

---

## 🔐 Phase 2 — Redis Atomic Locks (Thread Safety)

This is the core of the assignment. Three guardrails protect against bot abuse:

### 1. Vertical Cap (Depth Check)
Before saving any comment, we check if `depthLevel > 20`. If yes, the request is rejected immediately.

### 2. Horizontal Cap (Max 100 Bot Replies) — Atomic via Lua Script
**The problem with naive implementation:**
If 200 concurrent bot requests arrive at the same millisecond, a simple check-then-increment approach fails. All 200 threads read `count = 99`, all pass the check, and all 200 get saved — violating the cap.

**The solution — Redis Lua Script:**
```lua
local count = redis.call('INCR', KEYS[1])
if count > 100 then
  redis.call('DECR', KEYS[1])
  return -1
end
return count
```
Redis executes Lua scripts atomically — only one script runs at a time. So even with 200 concurrent requests, the counter increments one by one. The 101st request increments to 101, immediately decrements back to 100, and returns -1 (rejected). This guarantees exactly 100 bot replies — no more, no less.

### 3. Cooldown Cap (10 Minutes Per Bot-Human Pair)
A Redis key `cooldown:bot_{id}:human_{id}` is set with a TTL of 10 minutes when a bot interacts. If the key exists on the next request, the bot is blocked with a 429 response.

---

## 📊 Virality Score (Real-time Redis)

Every interaction updates a Redis key `post:{id}:virality_score`:

| Interaction | Points |
|-------------|--------|
| Human Like | +20 |
| Human Comment | +50 |
| Bot Reply | +1 |

---

## 🔔 Phase 3 — Notification Engine

### Throttler
When a bot interacts with a user's post:
- If user has **no cooldown** → log "Push Notification Sent to User" + set 15 min cooldown
- If user has **active cooldown** → push notification string into Redis List `user:{id}:pending_notifs`

### CRON Sweeper
A `@Scheduled` task runs every 5 minutes:
- Scans all `user:*:pending_notifs` keys in Redis
- Pops all pending messages, counts them
- Logs: `"Summarized Push Notification: Bot X and [N] others interacted with your posts."`
- Clears the Redis list

---

## 🏗️ Architecture

```
src/
├── controller/        # REST endpoints (PostController, CommentController)
├── service/           # Business logic (PostServiceImpl, CommentServiceImpl, NotificationService)
├── repository/        # Spring Data JPA repositories
├── entity/            # JPA entities (User, Bot, Post, Comment)
└── BackendApplication.java
```

**Data flow:**
```
Request → Controller → Service → Redis guardrail check → PostgreSQL save → Redis update
```

PostgreSQL = source of truth for content
Redis = gatekeeper for all counters, cooldowns, and notifications

---

## 🧪 Testing Concurrency

To simulate 200 concurrent bot requests locally:

```bash
for i in {1..200}; do
  curl -s -X POST http://localhost:8080/api/posts/1/comments \
    -H "Content-Type: application/json" \
    -d '{"authorId": 1001, "content": "bot spam", "depthLevel": 1}' &
done
wait
```

After this, the database should have exactly 100 bot comments — no more.

---

## 👨‍💻 Author

**Pratham Diwakar**
Java Backend Developer
📧 prathamdiwakar44@gmail.com
🔗 [GitHub](https://github.com/Prathamdiwakar)
