# Release Process

This document describes how to create and publish releases for NerdCalci.

## Prerequisites

- Push access to the GitHub repository
- All changes committed and pushed to main branch
- Version bumped in `app/build.gradle.kts`

## Initial Setup (One Time Only)

To enable automated signing, you must configure secrets in your GitHub repository:

### 1. Generate Upload Keystore
If you don't have one, generate a keystore:
```bash
keytool -genkey -v -keystore release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias nerdcalci
```

### 2. Encode Keystore
Get the base64 string of your keystore file:
```bash
base64 -i release.jks
# Copy the output string
```

### 3. Add GitHub Secrets
Go to **Settings > Secrets and variables > Actions** and add these 4 secrets:

1.  `KEYSTORE_FILE`: The base64 string from step 2.
2.  `KEYSTORE_PASSWORD`: Password for the keystore.
3.  `KEY_ALIAS`: Alias name (`nerdcalci`).
4.  `KEY_PASSWORD`: Password for the key alias. **(If `keytool` didn't ask for a second password, it's the same as `KEYSTORE_PASSWORD`)**.

## Automated Release (Recommended)

### 1. Update Version

Edit `app/build.gradle.kts`:

```kotlin
versionCode = 101  // Increment by 1
versionName = "1.0.1"  // Follow semver
```

**This is the single source of truth!** The workflow extracts the version from here automatically.

### 2. Update Changelogs

Create `fastlane/metadata/android/en-US/changelogs/{versionCode}.txt`:

```bash
# Example for versionCode 101
touch fastlane/metadata/android/en-US/changelogs/101.txt
```

Write changelog (max 500 characters):
```
New features:
• Added feature X
• Improved Y
• Fixed bug Z
```

### 3. Commit with [release] Prefix

```bash
git add .
git commit -m "[release] Release v1.1.0 with new features"
git push origin main
```

**Important:** The commit message MUST start with `[release]` to trigger the release workflow.

### 4. Automated Actions

Once you push the commit, GitHub Actions will automatically:
- Extract version from `build.gradle.kts` (e.g., `1.0.1`)
- Build the release APK
- Create git tag `v1.0.1` automatically
- Create a GitHub release with the APK attached

Check the release at: https://github.com/vishaltelangre/NerdCalci/releases

**No manual tagging required!** The version in gradle is the single source of truth.

## Manual Release

If you need to build locally:

### Build Release APK

```bash
./gradlew assembleRelease
```

The APK will be at: `app/build/outputs/apk/release/app-release-unsigned.apk`

### Build Signed APK (Manual)

To build a signed APK locally using the same injection method as CI:

```bash
./gradlew assembleRelease \
  -Pandroid.injected.signing.store.file=$(pwd)/release.jks \
  -Pandroid.injected.signing.store.password='keytore_password_here' \
  -Pandroid.injected.signing.key.alias=nerdcalci \
  -Pandroid.injected.signing.key.password='key_password_here'
```


## F-Droid Submission

1. Test locally (requires Docker):
   ```bash
   cd /path/to/fdroiddata
   fdroid readmeta
   fdroid rewritemeta com.vishaltelangre.nerdcalci
   fdroid checkupdates --allow-dirty com.vishaltelangre.nerdcalci
   fdroid lint com.vishaltelangre.nerdcalci
   fdroid build com.vishaltelangre.nerdcalci
   ```

2. Commit and push:
   ```bash
   git add metadata/com.vishaltelangre.nerdcalci.yml
   git commit -m "New App: NerdCalci"
   git push origin com.vishaltelangre.nerdcalci
   ```

3. Create a merge request at https://gitlab.com/fdroid/fdroiddata

4. Wait for F-Droid maintainers to review and merge

### Subsequent Updates

F-Droid will automatically detect new releases if:
- You push a new git tag (e.g., `v1.1.0`)
- The tag matches `versionName` in `build.gradle.kts`
- The commit has the corresponding changelog file
