package app.subenoeva.patches.roadsync.weather

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.subenoeva.patches.shared.Constants.COMPATIBILITY_ROADSYNC
import java.io.ByteArrayInputStream
import java.util.Base64
import java.util.function.Supplier

private const val EXTENSION = "Lapp/subenoeva/roadsync/OpenMeteoWeather;"

/**
 * RoadSync fetches weather from Honda's gRPC server, which rejects the unauthenticated requests a
 * re-signed build can make (see the login bypass). This swaps both lookups for Open-Meteo
 * (no API key), mapping its WMO codes onto the OpenWeatherMap ids the app's icons are keyed on.
 * Descriptions are English only.
 */
@Suppress("unused")
val openMeteoWeatherPatch = bytecodePatch(
    name = "Open-Meteo weather",
    description = "Gets the weather widget's current conditions and forecast from Open-Meteo " +
        "instead of Honda's server, which is unreachable without a Google sign-in.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_ROADSYNC)

    extendWith(Supplier { ByteArrayInputStream(Base64.getDecoder().decode(OPEN_METEO_DEX_BASE64)) })

    execute {
        // p1 is the Location (and p2 the hours-ahead list); reuse p1 for the result so no extra
        // register is needed.
        CurrentWeatherFingerprint.method.addInstructions(
            0,
            """
                invoke-static { p1 }, $EXTENSION->current(Landroid/location/Location;)Lio/reactivex/Single;
                move-result-object p1
                return-object p1
            """,
        )
        WeatherForecastFingerprint.method.addInstructions(
            0,
            """
                invoke-static { p1, p2 }, $EXTENSION->forecast(Landroid/location/Location;Ljava/util/List;)Lio/reactivex/Single;
                move-result-object p1
                return-object p1
            """,
        )
    }
}
