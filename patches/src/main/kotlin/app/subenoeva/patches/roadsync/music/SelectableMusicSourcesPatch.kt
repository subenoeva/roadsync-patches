package app.subenoeva.patches.roadsync.music

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringsOption
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.subenoeva.patches.shared.Constants.COMPATIBILITY_ROADSYNC
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

private const val PROVIDER = "Lcom/drivemode/sdk/music/provider/impl/MusicAppProviderImpl;"
private const val APPEND_HELPER = "appendSelectableSources"
private const val MUSIC_APP = "Lcom/drivemode/sdk/music/apps/MusicApp;"

@Suppress("unused")
val selectableMusicSourcesPatch = bytecodePatch(
    name = "Selectable music sources",
    description = "Lists extra music apps in RoadSync's music source picker, so they can be chosen " +
        "even when they are not running. Without this, apps RoadSync does not know are only " +
        "found while they hold an active media session.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_ROADSYNC)

    val selectablePackages by stringsOption(
        key = "selectableMusicPackages",
        default = listOf("app.symfonik.music.player"),
        title = "Selectable music app packages",
        description = "Package names to list in the music source picker. Each must also be " +
            "allowed through \"Add music sources\" (media browser whitelist) to be resolved. " +
            "Do not list apps RoadSync already supports natively, they would appear twice.",
        required = true,
    )

    execute {
        val packages = selectablePackages
            .orEmpty()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .distinct()

        if (packages.isEmpty()) {
            throw PatchException("No selectable music packages configured.")
        }

        // Helper added to DmMusicManager: returns the provider's source list with the apps resolved
        // by its own lookup (supported apps first, then generic ones) appended. It is a separate
        // method because DmMusicManager.d() has a single register to work with.
        val manager = MusicSourceListFingerprint.classDef
        val helper = ImmutableMethod(
            manager.type,
            APPEND_HELPER,
            listOf(ImmutableMethodParameter(PROVIDER, null, null)),
            "Ljava/util/List;",
            AccessFlags.PRIVATE.value or AccessFlags.STATIC.value,
            null,
            null,
            MutableMethodImplementation(5),
        ).toMutable()
        manager.methods.add(helper)

        // v0..v3 are locals, p0 is the provider.
        val lookups = packages.mapIndexed { index, packageName ->
            """
                const-string v0, "$packageName"
                invoke-virtual { p0, v0 }, $PROVIDER->k(Ljava/lang/String;)$MUSIC_APP
                move-result-object v1
                if-eqz v1, :skip_$index
                invoke-interface { v2, v1 }, Ljava/util/List;->add(Ljava/lang/Object;)Z
                :skip_$index
                nop
            """
        }.joinToString("\n")
        helper.addInstructionsWithLabels(
            0,
            """
                invoke-virtual { p0 }, $PROVIDER->g()Ljava/util/List;
                move-result-object v2
                $lookups
                return-object v2
            """,
        )

        // DmMusicManager.d() is `return this.provider.g()`: call the helper instead of g().
        MusicSourceListFingerprint.method.apply {
            val callIndex = implementation!!.instructions.indexOfFirst {
                val reference = (it as? ReferenceInstruction)?.reference as? MethodReference
                reference != null && reference.name == "g" && reference.definingClass == PROVIDER
            }
            if (callIndex < 0) {
                throw PatchException("Could not find the call to the source list method.")
            }
            val providerRegister = getInstruction<FiveRegisterInstruction>(callIndex).registerC

            replaceInstruction(
                callIndex,
                "invoke-static { v$providerRegister }, ${manager.type}->$APPEND_HELPER(${PROVIDER})Ljava/util/List;",
            )
        }
    }
}

@Suppress("unused")
val launchMusicAppPatch = bytecodePatch(
    name = "Launch music app on demand",
    description = "Starts the selected music app when RoadSync begins playback and the app has no " +
        "active media session, instead of failing with a \"session not detected\" error.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_ROADSYNC)

    execute {
        PlayWithoutAutoLaunchFingerprint.method.apply {
            val impl = implementation!!
            val playIndex = impl.instructions.indexOfFirst {
                if (it.opcode != Opcode.INVOKE_INTERFACE) return@indexOfFirst false
                val reference = (it as ReferenceInstruction).reference as? MethodReference
                reference != null &&
                    reference.definingClass == "Lcom/drivemode/sdk/music/DmMediaController;" &&
                    reference.parameterTypes.firstOrNull()?.toString() == "Z"
            }
            if (playIndex < 0) {
                throw PatchException("Could not find the play call.")
            }

            // play(isAutoLaunch, callback): force isAutoLaunch to true right before the call.
            val autoLaunchRegister = getInstruction<FiveRegisterInstruction>(playIndex).registerD
            addInstruction(playIndex, "const/4 v$autoLaunchRegister, 0x1")
        }
    }
}
