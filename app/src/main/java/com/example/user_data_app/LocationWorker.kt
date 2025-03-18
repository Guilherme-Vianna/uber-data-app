import android.Manifest
import android.content.Context
import android.location.Location
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresPermission
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class LocationWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl("http://10.0.2.2:3000/data/") // Corrected base URL with trailing slash
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val apiService = retrofit.create(ApiService::class.java)

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    override fun doWork(): Result {
        getLocation()
        return Result.success()
    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    private fun getLocation() {
        val locationRequest = LocationRequest.create().apply {
            interval = 2
            fastestInterval = 1
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult?.let { result ->
                    for (location in result.locations) {
                        sendLocationToServer(location)
                    }
                }
            }
        }

        // Use a Handler to post to the main thread to avoid Looper issue
        Handler(Looper.getMainLooper()).post {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        }
    }

    private fun sendLocationToServer(location: Location) {
        val locationData = LocationData(location.latitude, location.longitude)

        apiService.createLocation(locationData).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    println("Location successfully sent to the server")
                } else {
                    println("Failed to send location: ${response.message()}")
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                println("Error sending location: ${t.message}")
            }
        })
    }
}
