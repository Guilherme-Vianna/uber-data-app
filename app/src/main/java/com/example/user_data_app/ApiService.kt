import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("location")
    fun createLocation(@Body data: LocationData): Call<Void>
}
