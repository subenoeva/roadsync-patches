package app.subenoeva.patches.roadsync.pairing

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

/**
 * `VehiclesRepository.getVehicleCatalogs(boolean, Continuation)` (emitted as `f`).
 *
 * The pairing **Start** screen (`OnboardingPairingStartViewModel`) loads its list of motorcycle
 * models from here: the method delegates to `getVehicleCatalogs$2`, which calls
 * `ModelApi.listModelCatalogs` over gRPC. With no valid session that call throws
 * `UNAUTHENTICATED` (confirmed on-device), so the Start screen can never render offline.
 *
 * Member names are R8-obfuscated and the method has no string literals, so the match anchors on
 * the defining class plus the unique `(boolean, Continuation) -> Object` signature (it is the only
 * repository method whose first parameter is a primitive `boolean`).
 */
internal object GetVehicleCatalogsFingerprint : Fingerprint(
    definingClass = "Lcom/drivemode/sab/common/data/vehicles/VehiclesRepository;",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Ljava/lang/Object;",
    parameters = listOf("Z", "Lkotlin/coroutines/Continuation;"),
)

/**
 * `VehiclesRepository.getVehicleSeries(List, Continuation)` (emitted as `h`).
 *
 * The pairing **SelectModel** screen (`OnboardingPairingSelectModelViewModel.loadVehicles`) calls
 * this with the tapped model's series label; it delegates to `getVehicleSeries$2` →
 * `ModelApi` (server). Same auth wall as [GetVehicleCatalogsFingerprint], so SelectModel cannot
 * render offline either.
 *
 * Anchored on the class plus the unique `(List, Continuation) -> Object` signature — the only
 * repository method whose sole non-continuation parameter is a `List`.
 */
internal object GetVehicleSeriesFingerprint : Fingerprint(
    definingClass = "Lcom/drivemode/sab/common/data/vehicles/VehiclesRepository;",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Ljava/lang/Object;",
    parameters = listOf("Ljava/util/List;", "Lkotlin/coroutines/Continuation;"),
)

/**
 * `VehiclesRepository.getVehicles(List, List, String, Continuation)` (emitted as `i`).
 *
 * The **hidden third server gate**: `OnboardingPairingResetViewModel$saveVehicle$1` (the coroutine
 * that actually writes the paired `KnownVehicle` after a successful BLE bond) calls this with the
 * bike's BLE series code before persisting. It delegates to `getVehicles$2` → `ModelApi.listModels`
 * (server) and throws `UNAUTHENTICATED` offline, which aborts the save Single so
 * `BondingState.Complete` never fires — i.e. pairing fails at the last step even though the bond
 * succeeded. saveVehicle's own `if (result.isEmpty())` branch falls back to the user-selected
 * vehicle, so returning an empty list here is enough to let the save finish offline.
 *
 * Anchored on the class plus the unique `(List, List, String, Continuation) -> Object` signature.
 */
internal object GetVehiclesFingerprint : Fingerprint(
    definingClass = "Lcom/drivemode/sab/common/data/vehicles/VehiclesRepository;",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Ljava/lang/Object;",
    parameters = listOf(
        "Ljava/util/List;",
        "Ljava/util/List;",
        "Ljava/lang/String;",
        "Lkotlin/coroutines/Continuation;",
    ),
)
