# 🏸 Rally Score

> ⭐ **If you find Rally Score useful, please star this repo!** It helps other players find it and keeps the project going.

A big, simple two-player scoreboard for badminton and other rally sports. Put your phone on a stand by the court and keep score with a swipe or a Bluetooth remote. Built for AMOLED screens with true blacks.

---

## 📥 Install

1. Open the [**Releases**](https://github.com/kushaagr/RallyScore/releases) page on your phone.
2. Under **Latest build**, tap the `.apk` file to download it.
3. Open the download and tap **Install**. If Android asks, allow installing apps from your browser.

New versions install over the old one; your scores and settings are kept.

Requires Android 8.0 or newer.

---

## 🎯 Keeping score

### On screen

| Action | How |
| --- | --- |
| Add a point | Swipe **up** on a player's tile |
| Remove a point | Swipe **down** on a player's tile |

### With volume buttons or a Bluetooth remote

| Action | How |
| --- | --- |
| Point to left / top player | **Volume Up** |
| Point to right / bottom player | **Volume Down** |
| Quickly add many points | **Hold** a button: after a moment the score climbs until the game is won |
| Undo | Press **both buttons together** |
| Start a new game | After a win, **double-press the winner's button** |

Any cheap Bluetooth "selfie" or media remote that sends volume buttons works. The phone's own volume buttons work the same way.

> 💡 Holding a button counts as a single step, so one undo reverses the whole climb.

---

## 🧰 Toolbar

The row of buttons at the bottom of the screen, from left to right:

| Button | What it does |
| --- | --- |
| **Reset** | Sets both scores to 0 (you can undo this) |
| **Undo** / **Redo** | Steps back or forward through every change, including resets |
| **Swap sides** | Switches the players' sides when you change ends. Buttons stay tied to the side, not the player |
| **Orientation** | Cycles: rotate with phone → portrait → landscape |
| **Volume** | Opens the system volume slider (controls the score announcements) |
| **Help** | Quick guide inside the app |
| **Settings** | All options below |

---

## 🏆 Scoring rules

By default Rally Score follows badminton rules:

- First to **21** points wins, but you must lead by **2**.
- At 29–29, the next point wins: **30** wins outright.

Both numbers can be changed in **Settings → Scoring**.

During play you'll see:

- A **dot** next to the player who serves (the one who won the last rally).
- **GAME POINT** when a player is one point from winning.
- **GAME** and a green score when a player has won.

---

## 🔊 Score announcements

After every change the app speaks the score in badminton style, for example *"five, three"*, *"twenty all. Game point."* or *"Game. Player 1 wins, 21 to 19."*

When you hold a button, only the final score is announced. Use the toolbar's **Volume** button to adjust loudness, or turn announcements off in **Settings → Feedback**.

---

## ⚙️ Settings

### Scoring
- **Points to win**: default 21, with a 2-point lead required.
- **Maximum points**: default 30, wins outright.

### Players
- **Names**: shown on the tiles and used in announcements.
- **Show names / Show server**: hide or show the names and the serve dot.

### Display
- **Theme**: Dark (pure AMOLED black), Light, or follow the system.
- **Orientation**: rotate with phone, portrait, landscape, or lock the current one.
- **When the phone turns**: either only the scores and buttons rotate while the screen stays put, or the whole screen rotates.
- **Tile colors**: a background color for each player so you can tell them apart from across the court. Choose **None** for a pure black (or white) tile.

### Feedback
- **Announce score**: spoken score after each change.
- **Vibrate**: short buzz for points, undo and reset.

### Buttons & remote
- **Use volume / remote buttons**: turn off to make volume buttons change the volume again.
- **Left / Right counter → Change**: press any remote or phone button to assign it to a side.
- **Test buttons**: shows every button event your remote sends and the time between them.
- **Fine-tuning sliders** (each has a ↺ reset-to-default button):

| Slider | Default | What it controls |
| --- | --- | --- |
| Hold delay | 700 ms | How long to hold before the score starts climbing |
| Climb speed | 150 ms | Time between points while holding |
| Repeat gap | 120 ms | Button events closer than this count as one held press |
| Double-press window | 450 ms | Max time between the two presses that start a new game |
| Both-buttons window | 250 ms | Presses this close together count as "both buttons" (undo) |

### Screen protection
- **Pixel shift**: moves the layout a few pixels every minute.
- **Fade controls**: dims the toolbar after 8 seconds without input.
- **Dim when idle**: lowers brightness after a few minutes without input. The screen never turns off.

---

## 🎮 Setting up a Bluetooth remote

1. Pair the remote in your phone's Bluetooth settings.
2. Open Rally Score; the remote's volume buttons now score.
3. If your remote sends different buttons, go to **Settings → Buttons & remote → Change** and press the remote button you want for each side.

**If holding the remote only adds one point at a time**, open **Test buttons** and hold a button. Note the gap (in ms) between events, then set **Repeat gap** a little higher than that.

**If two quick taps count as one**, lower **Repeat gap**.

> ⚠️ Volume buttons only score while Rally Score is open and on screen. The app keeps the screen on for this reason.

---

## 🛠️ Building it yourself

Every push to this repo builds the APK automatically with GitHub Actions:

- Pushes to the default branch update the **Latest build** pre-release.
- Pushing a tag like `v1.0` creates a versioned release.
- Each run's APK is also available under **Actions → run → Artifacts**.

To build locally you need JDK 17, the Android SDK and Gradle 8.11:

```bash
gradle assembleRelease
```

The APK appears in `app/build/outputs/apk/release/`.

### Signing

Builds are signed with `keystore/shared.keystore` so updates install over each other. That key is public, which is fine for personal use. To sign with your own key, add these repository secrets:

| Secret | Value |
| --- | --- |
| `SIGNING_KEYSTORE_BASE64` | Output of `base64 -w0 your.keystore` |
| `SIGNING_STORE_PASSWORD` | Keystore password |
| `SIGNING_KEY_ALIAS` | Key alias |
| `SIGNING_KEY_PASSWORD` | Key password |

Switching keys means uninstalling the old app once.

**Tech:** Kotlin · Jetpack Compose · Material 3 · minSdk 26 · targetSdk 35

## Credits

Designed and tested by [@kushaagr](https://github.com/kushaagr). Code written with the help of [Claude](https://claude.ai) by Anthropic.

---

⭐ **Enjoying Rally Score? Star the repo** and share it with your playing partners!
