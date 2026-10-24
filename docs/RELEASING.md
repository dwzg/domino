# Releasing

A release is a version number like `2.1.0`. Releasing one:

- runs every test (engine, server and app)
- builds the APK signed with the permanent release key, with the online server from
  `gradle.properties` built in
- publishes a GitHub Release `v2.1.0` with `dominoes-2.1.0.apk` attached and
  generated release notes
- publishes the server image as `ghcr.io/dwzg/domino-server:2.1.0`, `:2.1` and `:latest`

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

In Portainer, open the stack and choose **Update the stack** with **Re-pull image** on.
To stay on a specific release instead of `latest`, use `ghcr.io/dwzg/domino-server:2.1.0`
in the stack.
