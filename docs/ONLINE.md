# Online play: running the game server

Online games go through a small server (the `server/` module). It deals the tiles,
checks every move, plays the computer seats and sends each player only their own hand.
Games are stored in a SQLite file.

You host it once; then everyone using the app enters its address
(**More → Settings → Online server**, or the prompt on **Play → Play with friends**).

## Option A: Fly.io (recommended)

Fly.io runs the server close to you and keeps the database on a persistent volume.
A tiny always-sleeping machine like this one usually fits in their free allowance.
Check fly.io/pricing for the current terms.

1. Install the Fly CLI and sign in:
   ```sh
   curl -L https://fly.io/install.sh | sh
   fly auth login
   ```
2. Pick a unique app name and put it in `fly.toml` (`app = "..."`). Change
   `primary_region` if you're not near Frankfurt (`fly platform regions` lists them).
3. Create the app and the volume that holds the database:
   ```sh
   fly apps create <your-app-name>
   fly volumes create domino_data --size 1 --region fra
   ```
4. Deploy from the repository root:
   ```sh
   fly deploy
   ```
5. Run exactly one machine. Live updates and SQLite both live on that one machine:
   ```sh
   fly scale count 1
   ```
6. Check it works: `https://<your-app-name>.fly.dev/health` should say `ok`.
7. In the app, set the online server to `<your-app-name>.fly.dev`.
   To build an APK with that server already filled in:
   ```sh
   ./gradlew :app:assembleRelease -PdominoServerUrl=https://<your-app-name>.fly.dev
   ```

To update the server later, pull the latest code and run `fly deploy` again.
Running games survive deploys because they're on the volume.

## Option B: Docker anywhere

Any host that runs a container and gives it a persistent disk works:

```sh
docker build -t dominoes-server .
docker run -d -p 8080:8080 -v dominoes-data:/data --name dominoes dominoes-server
```

Put it behind HTTPS (a reverse proxy such as Caddy or your host's built-in TLS).
The app only talks plain HTTP to `localhost` and `10.0.2.2`.
Hosts without a persistent disk (e.g. Google Cloud Run) lose the games on every restart.

## Trying it on your computer

```sh
./gradlew :server:run          # listens on http://localhost:8080
```

- **Android emulator:** use `http://10.0.2.2:8080` as the server.
- **Phone over USB:** run `adb reverse tcp:8080 tcp:8080`, then use `http://localhost:8080`.

## Configuration

| Variable | Default | Meaning |
|---|---|---|
| `PORT` | `8080` | Port to listen on |
| `DATABASE` | `domino.db` | SQLite file; point it at a persistent volume |

## How it works

- **Players:** you pick a name. The server returns a random token that the app keeps; there are no passwords. Clearing the app's data means starting over with a new identity.
- **Games:** the host creates a game and shares the 6-letter code or the invite link (`https://<server>/join/CODE`, which opens the app). Free seats are filled with computer players when the host starts.
- **Live games** stream updates over a WebSocket while the game is open.
- **Turn-by-turn games:** while the app is closed, it checks every 15 minutes in the background and notifies you when it's your turn. This avoids needing a push service, at the cost of a delay.
- **Fairness:** every action carries the game version it was based on, so moves made on an outdated screen are refused. Other players' tiles are never sent to your phone.

## API

| Method | Path | |
|---|---|---|
| GET | `/health` | Liveness check |
| POST | `/players` | Register `{name}` → `{playerId, token, name}` |
| PATCH | `/players/me` | Rename |
| GET | `/games` | Your games |
| POST | `/games` | Create `{mode, players, target, pace}` |
| POST | `/games/join` | Join `{code}` |
| GET | `/games/{id}` | Your view of a game |
| POST | `/games/{id}/start` | Host starts `{difficulty}` |
| POST | `/games/{id}/actions` | `{action: play/draw/pass/leave, version}` |
| WS | `/games/{id}/live?token=` | Your view, pushed on every change |
| GET | `/join/{code}` | Invite page that opens the app |

Authenticated calls send `Authorization: Bearer <token>`. Message formats are in
`protocol/src/main/kotlin/.../Protocol.kt`.
