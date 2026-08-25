# 🧩 subenoeva Morphe patches

Morphe patches for **Honda RoadSync** (`com.honda.ms.dm.sab`).

Not affiliated with, or endorsed by, Honda or the Morphe project. These patches are for use with
Morphe.

## How to use these patches

Add this source to Morphe Manager: https://morphe.software/add-source?github=subenoeva/roadsync-patches

## 🩹 Patches

### Add music sources

RoadSync bundles the Drivemode music SDK, which resolves music apps three ways:

- **supported** — a hardcoded set of `Resolvable` implementations (Spotify, YouTube Music, Apple
  Music, Amazon Music, Deezer, SoundCloud, VLC, TuneIn, Poweramp, Samsung Music, ...).
- **generic** — any installed app that declares an `android.intent.action.MEDIA_BUTTON` receiver and
  is not blacklisted, wrapped in a `GenericMusicApp`.
- **media browser whitelist** — nine hardcoded package names. Only apps on this list get their
  `android.media.browse.MediaBrowserService` bound, which is what turns a music app from
  "play/pause/skip" into a browsable library.

The patch appends packages to that third list. It does not touch the blacklist or the supported set.

Default packages:

| Package | App | Media browser service |
|---|---|---|
| `com.aspiro.tidal` | TIDAL | `com.aspiro.wamp.player.MusicService` |
| `app.morphe.android.apps.youtube.music` | YouTube Music patched with Morphe | `com.google.android.apps.youtube.music.mediabrowser.MusicBrowserService` |

Both are already discovered by RoadSync as generic music apps, so transport control works without
the patch; whitelisting is what adds library browsing.

#### Option: `extraMusicPackages`

Replaces the default list. An app only benefits from being listed if it declares a
`android.media.browse.MediaBrowserService` — check with:

```bash
aapt2 dump xmltree --file AndroidManifest.xml app.apk | grep -B40 'android.media.browse.MediaBrowserService'
```

Apps without one already work as plain transport targets and gain nothing from being listed.

> [!WARNING]
> Whitelisting an app whose media browser service refuses connections from unknown clients can make
> it *worse* than not patching: once a package is whitelisted, RoadSync binds the browser service
> instead of falling back to plain media session control. Spotify is the known case — its browser
> service only accepts allowlisted, signed clients, which is why it is not a default here.

<!-- PATCHES_START EXPANDED -->
> **[v1.0.0](https://github.com/subenoeva/roadsync-patches/releases/tag/v1.0.0)**&nbsp;&nbsp;•&nbsp;&nbsp;`main`&nbsp;&nbsp;•&nbsp;&nbsp;1 patches total
<details open>
<summary>📦 RoadSync&nbsp;&nbsp;•&nbsp;&nbsp;1 patch</summary>
<br>

**🎯 Supported versions:**

| 26.4.10 |
| :---: |

| 💊&nbsp;Patch | 📜&nbsp;Description | ⚙️&nbsp;Options |
|----------|----------------|-----------|
| [Add music sources](#add-music-sources) | Adds music apps to the list of sources RoadSync can browse and control from the handlebar. Apps must expose a media browser service to be browsable. | • Music app packages |

</details>

<!-- PATCHES_END -->
