# Game WebView (Android shell)

A minimal native Android app that does one thing: opens

```
https://dinglenutsxnex-crypto.github.io/-
```

full-screen, with the status bar and navigation bar hidden, so it feels like an
installed app instead of a browser tab.

## How it works

- `app/src/main/java/com/dinglenuts/gamewebview/MainActivity.kt` hosts a single
  `WebView` and hides the system bars (immersive sticky mode) on create,
  resume, and whenever the window regains focus.
- The URL is hardcoded as the `gameUrl` constant at the top of `MainActivity`.
  To point the shell at a different game, change that one line.
- Back button navigates the WebView's history before exiting the app.

## Building the APK

The APK is built automatically by GitHub Actions — no local Android Studio
setup required.

1. Push this repository to GitHub (this project is already wired with the
   workflow at `.github/workflows/build-apk.yml`).
2. The workflow runs on every push to `main` that touches `android-webview-app/**`,
   or you can trigger it manually from the Actions tab ("Run workflow").
3. When it finishes, open the workflow run and download the
   `game-webview-debug-apk` artifact — it contains `app-debug.apk`.
4. Transfer `app-debug.apk` to your Android device and open it to install.
   You'll need to allow "install unknown apps" for whichever app you use to
   open the file (Files, Chrome, etc.) — this is a normal one-time prompt for
   APKs installed outside the Play Store.

This produces a debug-signed APK, which Android allows you to install
directly. If you want a Play Store-ready release build signed with your own
key, that requires generating a keystore and adding signing config + secrets
to the workflow — ask if you want that set up.

## Changing the app name or icon

- App name: `app/src/main/res/values/strings.xml` (`app_name`).
- Icon: replace the PNGs under `app/src/main/res/mipmap-*/ic_launcher.png`
  (and `ic_launcher_round.png`) with your own artwork at the same sizes
  (48/72/96/144/192px).
