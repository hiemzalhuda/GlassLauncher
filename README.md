# Glass Launcher

A minimal Android launcher built from scratch — no root, no Xposed, no
forks. Every app icon sits on a **frosted-glass tile**: the tile background
is a *true blur* of the wallpaper region behind the tile (cropped, downscaled
and box-blurred in pure Kotlin), plus a translucent white overlay and a thin
light border. A plain translucent tile with no blur is explicitly not what
this does.

## v1 scope

- Home screen: app grid over the live wallpaper, glass tiles.
- App drawer: all launchable apps, alphabetical, glass tiles.
- Tap an icon to launch the app.
- No permissions requested.

**Not in v1:** widgets, folders, gestures, icon packs, search, theming.
Those may come later; v1 is deliberately small and readable.

## Build

Debug APKs are built on GitHub Actions (`.github/workflows/build-apk.yml`,
Gradle 8.7 + AGP 8.5.2 + JDK 17, compileSdk/targetSdk 35, minSdk 26).
No signing key is needed for the debug build.

## Install (Pixel 7a, LineageOS)

1. Download the APK in the browser.
2. Allow install from the browser when asked.
3. Open Glass Launcher, then set it as the default Home app.
