package app.subenoeva.patches.roadsync.music

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

/**
 * Builds the set of package names that are allowed to expose a `MediaBrowserService` to RoadSync.
 *
 * RoadSync bundles the Drivemode music SDK, which resolves music apps in three ways:
 *
 * - `supported`: a hardcoded set of `Resolvable` implementations (Spotify, YouTube Music, VLC, ...).
 * - `generic`: any installed app declaring an `android.intent.action.MEDIA_BUTTON` receiver that is
 *   not blacklisted, wrapped in a `GenericMusicApp`.
 * - `mediaBrowserWhitelist`: the set matched by this fingerprint. Only apps listed here get their
 *   `android.media.browse.MediaBrowserService` bound, which is what enables library browsing
 *   instead of plain transport control.
 *
 * The method is a private static Kotlin lambda body with no parameters returning a `Set`, holding
 * the package literals below. Class and package names of the SDK are not obfuscated, but member
 * names are, so the match relies on the string literals rather than on the method name.
 */
internal object MediaBrowserWhitelistFingerprint : Fingerprint(
    definingClass = "Lcom/drivemode/sdk/music/provider/impl/MusicAppProviderImpl;",
    accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.STATIC, AccessFlags.FINAL),
    returnType = "Ljava/util/Set;",
    parameters = emptyList(),
    // Unordered on purpose: the order the literals are emitted in depends on register allocation
    // and is not stable between builds.
    strings = listOf(
        "org.videolan.vlc",
        "com.example.android.mediasession",
        "com.drivemode.android.music",
    ),
)

/**
 * `DmMusicManager.d()`: returns the list of music apps shown in the music source picker and used to
 * resolve the selected source, by delegating to `MusicAppProviderImpl.g()`. Its twin `c()` has the
 * same `() -> List` signature and delegates to `f()`, so the match anchors on the call to `g()`.
 */
internal object MusicSourceListFingerprint : Fingerprint(
    definingClass = "Lcom/drivemode/sdk/music/DmMusicManager;",
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "Ljava/util/List;",
    parameters = emptyList(),
    custom = { method, _ ->
        method.implementation?.instructions?.any {
            val reference = (it as? ReferenceInstruction)?.reference as? MethodReference
            reference != null &&
                reference.name == "g" &&
                reference.definingClass == "Lcom/drivemode/sdk/music/provider/impl/MusicAppProviderImpl;"
        } == true
    },
)

/**
 * `MusicManager$ControllerWrapper.w(ControllerWrapper, CompletableEmitter)`: starts playback with
 * `controller.play(false, callback)`, i.e. with auto launch disabled.
 */
internal object PlayWithoutAutoLaunchFingerprint : Fingerprint(
    definingClass = "Lcom/drivemode/sab/common/music/MusicManager\$ControllerWrapper;",
    // Excludes the synthetic bridge `f(ControllerWrapper, CompletableEmitter)` that forwards to it.
    accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.STATIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf(
        "Lcom/drivemode/sab/common/music/MusicManager\$ControllerWrapper;",
        "Lio/reactivex/CompletableEmitter;",
    ),
)
