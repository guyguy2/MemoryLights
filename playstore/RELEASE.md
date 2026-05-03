# Memory Lights — Release Build Guide

End-to-end steps to produce a signed AAB ready for upload to the Play Console.

---

## 1. Generate a signing keystore (one-time)

The Play Store requires you to sign your app with a stable upload key. Generate one and **never lose it** — losing the key means you cannot ship updates under the same package name without going through Play App Signing recovery.

```bash
keytool -genkey -v \
  -keystore ~/keystores/memory-lights-upload.jks \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -alias memory-lights
```

When prompted:
- **Keystore password:** strong password — store in 1Password or similar
- **Key password:** same as keystore password (simpler)
- **Distinguished name:** "Memory Lights, [your org], [city], [state], [country]"

After the keystore is created, **back it up to two separate locations** (e.g., 1Password attached file + iCloud or external drive). If this file is lost, you cannot release new versions.

---

## 2. Wire signing config into Gradle

The current `app/build.gradle.kts` does not have a signing config. Two ways to add one:

### Option A — keystore.properties file (recommended; keeps secrets out of git)

Create `keystore.properties` at the repo root (already gitignored if you add the line below):

```properties
storeFile=/Users/guy/keystores/memory-lights-upload.jks
storePassword=YOUR_KEYSTORE_PASSWORD
keyAlias=memory-lights
keyPassword=YOUR_KEY_PASSWORD
```

Add to `.gitignore`:
```
keystore.properties
```

Edit `app/build.gradle.kts` — add inside `android { ... }` before `buildTypes`:

```kotlin
val keystoreProperties = java.util.Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

signingConfigs {
    create("release") {
        if (keystoreProperties.containsKey("storeFile")) {
            storeFile = file(keystoreProperties["storeFile"] as String)
            storePassword = keystoreProperties["storePassword"] as String
            keyAlias = keystoreProperties["keyAlias"] as String
            keyPassword = keystoreProperties["keyPassword"] as String
        }
    }
}
```

Then update `buildTypes.release` to reference it:

```kotlin
buildTypes {
    release {
        signingConfig = signingConfigs.getByName("release")
        isMinifyEnabled = true
        isShrinkResources = true
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
    }
}
```

### Option B — environment variables (CI-friendly)

Same shape, but read from `System.getenv("KEYSTORE_PASSWORD")` etc. Skip unless you're setting up CI.

---

## 3. Build the AAB

```bash
./gradlew clean bundleRelease
```

Output: `app/build/outputs/bundle/release/app-release.aab`

Verify:
```bash
ls -lh app/build/outputs/bundle/release/
# Expect: ~7-8 MB
```

Confirm signature:
```bash
jarsigner -verify -verbose -certs app/build/outputs/bundle/release/app-release.aab | tail -5
# Expect: "jar verified"
```

---

## 4. Test the release build on a real device

Don't upload an unsigned-tested build. Install the release APK locally first:

```bash
./gradlew assembleRelease
adb install -r app/build/outputs/apk/release/app-release.apk
```

Smoke test on the emulator or a physical device:
- App launches without crash
- Play one full round in 4-button mode
- Switch to Memory Lights+
- Switch sound packs
- Trigger Share button on game over
- Verify high score persists across kill + relaunch

If you hit a release-only crash (R8 stripping, ProGuard rules), check `proguard-rules.pro` and add keep rules for the affected class.

---

## 5. Upload to Play Console

1. Go to https://play.google.com/console
2. Create a new app (one-time):
   - App name: `Memory Lights`
   - Default language: English (United States)
   - App or game: **Game**
   - Free or paid: **Free**
   - Accept declarations
3. Go to **Production → Create new release** (after testing tracks if you want; internal testing first is recommended)
4. Upload `app-release.aab`
5. Fill in **Release notes** (use the snippet from `listing.md`)
6. Fill in **Store listing**:
   - App title, short description, full description from `listing.md`
   - Upload 7 screenshots from `playstore/screenshots/`
   - Upload 512x512 app icon (extract or upscale from existing launcher)
   - Upload 1024x500 feature graphic (still TODO — see `feature_graphic.md`)
7. Fill in **App content** declarations (from `listing.md` → Content rating section)
8. Submit IARC questionnaire for content rating
9. Privacy policy URL — paste hosted URL (see `privacy_policy.md`)
10. Submit for review

First review typically takes 1-7 days for new apps.

---

## 6. Internal testing before production (recommended)

Before pushing to Production, ship to **Internal testing** track:

1. Play Console → Testing → Internal testing → Create new release
2. Upload same `.aab`
3. Add yourself + 1-2 trusted testers as testers (by email)
4. Share opt-in URL → testers install via Play Store on their devices
5. Soak test for 1-2 days
6. Promote the same build to Production from the Internal track release page (no re-upload, no re-review)

---

## 7. Post-launch

- Tag the release commit: `git tag v1.0.0 && git push origin v1.0.0`
- Bump `versionCode` for the next release (every Play Store upload requires a strictly higher `versionCode`)
- Monitor Play Console **Crashes & ANRs** for the first week
- Read every review for the first 50 — early signal on what to fix in v1.1

---

## Checklist before clicking "Submit for review"

- [ ] Keystore backed up to two separate locations
- [ ] `keystore.properties` exists and is gitignored
- [ ] `./gradlew bundleRelease` produces a signed AAB
- [ ] Release APK smoke-tested on at least one device
- [ ] All 7 screenshots uploaded
- [ ] App icon (512x512) uploaded
- [ ] Feature graphic (1024x500) uploaded
- [ ] Privacy policy hosted at a public URL and pasted into Play Console
- [ ] Short and full descriptions filled in
- [ ] Content rating IARC questionnaire submitted
- [ ] Data safety form filled (collect: nothing, share: nothing)
- [ ] Internal testing track release tested for at least 24 hours
