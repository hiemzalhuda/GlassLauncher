# Glass Launcher

A minimal Android launcher built from scratch — no root, no Xposed, no
forks. Every app icon is a **liquid-glass squircle**: the glass IS the
icon's own adaptive shape at normal icon size, not a separate tile.

## v2 glass (rebuilt after v1 looked wrong)

v1 drew each app as a bulky 76dp rounded-rect tile with a flat white
overlay — milky boxes, and the blur went stale mid-scroll. v2 fixes all
three problems, following PixelLauncherEnhanced's icon glass
(`GlassStyle`: ~15% white outline, rim light, shadow):

- **Shape:** 64dp squircle (~30% corner radius, adaptive-icon look) with
  the full-color app glyph centered at 54dp. No separate tile, tighter
  96dp cells.
- **Liquid gloss, painted in order:** drop shadow under the squircle,
  blurred wallpaper through the squircle, light tint (~18% white so the
  wallpaper dominates), rim light along the top edge, specular highlight
  at the top-left, subtle bottom shade for depth, thin white outline
  (~15% opacity, 1dp), then the glyph on top.
- **Wallpaper alignment:** the system wallpaper is rendered ONCE into a
  screen-sized bitmap; that bitmap is both the launcher background AND
  the blur source (single source of truth, no center-crop assumption).
  It is pre-blurred once at quarter resolution, and every squircle crops
  its region live from it using its current screen position — so the
  glass stays glued to the wallpaper even mid-scroll in the drawer.
  Changing the system wallpaper refreshes the launcher.

## Scope

- Home screen: app grid over the wallpaper, glass squircles.
- App drawer: all launchable apps, alphabetical, glass squircles.
- Tap an icon to launch the app.
- No permissions requested.

**Not in v2:** widgets, folders, gestures, icon packs, search, theming.
A real AGSL liquid-refraction shader is a stretch goal; the painted
gloss above is the deliverable.

## Build

Debug APKs are built on GitHub Actions (`.github/workflows/build-apk.yml`,
Gradle 8.7 + AGP 8.5.2 + JDK 17, compileSdk/targetSdk 35, minSdk 26).
No signing key is needed for the debug build.

## Install (Pixel 7a, LineageOS)

1. Download the APK in the browser.
2. Allow install from the browser when asked.
3. Open Glass Launcher, then set it as the default Home app.
