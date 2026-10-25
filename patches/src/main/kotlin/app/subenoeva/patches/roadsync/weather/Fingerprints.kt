package app.subenoeva.patches.roadsync.weather

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

/** `SabWeatherProvider.getWeather(Location)`: the current conditions gRPC call to Honda's server. */
internal object CurrentWeatherFingerprint : Fingerprint(
    definingClass = "Lcom/drivemode/sab/common/weather/provider/SabWeatherProvider;",
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "Lio/reactivex/Single;",
    parameters = listOf("Landroid/location/Location;"),
)

/** `SabWeatherProvider.getWeatherForecast(Location, List<Integer> hoursAhead)`. */
internal object WeatherForecastFingerprint : Fingerprint(
    definingClass = "Lcom/drivemode/sab/common/weather/provider/SabWeatherProvider;",
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "Lio/reactivex/Single;",
    parameters = listOf("Landroid/location/Location;", "Ljava/util/List;"),
)
