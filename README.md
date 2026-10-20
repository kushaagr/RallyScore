# Rally Score

A two-counter scoreboard for badminton (or any rally sport), built for AMOLED screens
and controllable with a Bluetooth volume-button remote.

## Getting the APK

Every push runs **.github/workflows/build.yml**, which builds a release APK:

- **Phone-friendly:** each push to the default branch refreshes a pre-release called
  **"Latest build"** on the repo's *Releases* page. Open it on your phone and tap the `.apk`.
- **Artifact:** the APK is also attached to each workflow run (*Actions* tab → run → *Artifacts*, downloads as a zip).
- **Versioned release:** push a tag such as `v1.0` and the APK is attached to a release with that name.

### Setup

1. Create a GitHub repository and push this folder to it.
2. Make sure *Settings → Actions → General → Workflow permissions* is set to **Read and write**
   (needed to publish releases).
3. Push. The first build takes ~5 minutes.

### Signing

All builds are signed with `keystore/shared.keystore`, so each new APK installs over the previous one
and your scores/settings are kept. Because this key is public in the repo, it's fine for personal use
but not for the Play Store. To use your own private key, add these repository secrets:
`SIGNING_KEYSTORE_BASE64` (output of `base64 -w0 my.keystore`), `SIGNING_STORE_PASSWORD`,
`SIGNING_KEY_ALIAS`, `SIGNING_KEY_PASSWORD`. Switching keys means uninstalling once.

## Controls

| Action | How |
| --- | --- |
| +1 / −1 | Swipe up / down on a counter |
| +1 with buttons | Volume Up → left/top, Volume Down → right/bottom (reassignable) |
| Fast climb | Hold a button; after the hold delay the score climbs until the game is won |
| New game | After a win, double-press the winner's button |
| Undo | Press both buttons together, or the undo button (also undoes a reset) |
| Swap sides | Swap button; buttons stay tied to the side |

Scoring defaults to badminton: 21 points with a 2-point lead, 30 wins outright. Both limits are configurable.

## Remote tuning

Settings → *Buttons & remote* → **Test buttons** shows each event your remote sends and the time gap
between them. While holding, if the gaps are larger than **Repeat gap**, raise Repeat gap above them.

## Notes

- Volume buttons only work while the app is open and the screen is on (the app keeps it on).
- Announcements use the media volume; the speaker button opens the system volume slider.
- Burn-in protection: pure-black background, soft off-white digits, a 1-minute pixel-shift cycle,
  fading controls, and brightness dimming after inactivity (all toggleable).

## Tech

Kotlin, Jetpack Compose, minSdk 26 (Android 8.0), targetSdk 35. CI uses Gradle 8.11.1 directly
(no wrapper jar needed). To build locally: `gradle assembleRelease` with JDK 17 and the Android SDK.
