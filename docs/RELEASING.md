# Releasing

A release is a version number like `2.1.0`. Releasing one:

- runs every test (engine, server and app)
- builds the APK signed with the permanent release key, with the online server from
  `gradle.properties` built in
- publishes a GitHub Release `v2.1.0` with `dominoes-2.1.0.apk` attached and
  generated release notes
- publishes the server image as `ghcr.io/dwzg/domino-server:2.1.0`, `:2.1` and `:latest`
- updates your server through a Portainer webhook, if you've set one up, and waits
  until `/version` reports the new version

## One-time setup: the signing key

Android only installs an update if it's signed with the same key as the installed app,
so every release must use one permanent key. Keep a backup of it (for example in a
password manager). If it's lost, nobody can update the app; everyone would have to
uninstall and reinstall, losing their saved games and stats.

Add these two secrets on GitHub under
**Settings → Secrets and variables → Actions → New repository secret**:

| Secret | Value |
|---|---|
| `KEYSTORE_BASE64` | The keystore file, base64-encoded (`base64 -w0 dominoes-release.jks`) |
| `KEYSTORE_PASSWORD` | Its password |

The key alias is `dominoes`, and the key password is the same as the keystore password.

## Making a release

1. Go to **Actions → Release → Run workflow** (works from the GitHub website on your phone too).
2. Enter the version, e.g. `2.1.0`, and click **Run workflow**.
3. After about 10 minutes the release appears under **Releases**, with the APK.

Pick the version number by what changed:
- `2.1.0` → `2.1.1` for fixes
- `2.1.0` → `2.2.0` for new features
- `2.1.0` → `3.0.0` for big changes, such as anything that needs an updated server

The app's version code is worked out from it (`2.1.0` → `20100`), so always go up.

## Building a signed APK yourself

```sh
export DOMINO_KEYSTORE=/path/to/dominoes-release.jks
export DOMINO_KEYSTORE_PASSWORD=...
./gradlew :app:assembleRelease -PappVersion=2.1.0
```

Without `DOMINO_KEYSTORE` the APK is signed with your local debug key. That's fine for
testing, but it can't update an installed release.

## Updating the server

### Automatically, with a Portainer webhook

1. In Portainer, open the stack, go to the **Editor** tab, turn on **Create a stack webhook**
   and click **Copy link**. The link looks like
   `https://<portainer>/api/stacks/webhooks/<id>`.
2. On GitHub, add it as the repository secret `PORTAINER_WEBHOOK_URL`
   (**Settings → Secrets and variables → Actions**).

From then on, each release calls the webhook after the image is published. Portainer
pulls the new `latest` image and recreates the container. The workflow then checks
`https://dominoes.convoluted.space/version` (the `dominoServerUrl` from
`gradle.properties`) until it reports the new version, and fails if that takes longer than
5 minutes. To release without touching the server, untick **Update the server** when
starting the workflow.

Things to know:
- Stack webhooks are part of Portainer **Business Edition** (free for up to 3 nodes).
  Community Edition doesn't offer them for stacks made in the web editor.
- GitHub's servers must be able to reach Portainer, so its address must be public
  (for example a proxy host in Nginx Proxy Manager). Port 9443 directly works too, but
  Portainer's self-signed certificate will be refused.
- Keep the link secret: anyone who has it can redeploy the stack. If it leaks, turn the
  webhook off and on again in Portainer to get a new one.
- The stack must use the `latest` tag (as `deploy/portainer/docker-compose.yml` does).
  A stack pinned to a version, like `:2.1.0`, is only redeployed with that same version.

Without a webhook, the release only publishes the image, and you update by hand.

### By hand

In Portainer, open the stack and choose **Update the stack** with **Re-pull image** on.
To stay on a specific release instead of `latest`, use `ghcr.io/dwzg/domino-server:2.1.0`
in the stack.
