package com.example.data

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class PlaceAutocompleteResponse(
    val predictions: List<PlacePrediction> = emptyList(),
    val status: String = ""
)

@JsonClass(generateAdapter = true)
data class PlacePrediction(
    val description: String,
    @Json(name = "place_id") val placeId: String
)

@JsonClass(generateAdapter = true)
data class PlaceDetailsResponse(
    val result: PlaceDetailsResult?,
    val status: String = ""
)

@JsonClass(generateAdapter = true)
data class PlaceDetailsResult(
    val geometry: PlaceGeometry?
)

@JsonClass(generateAdapter = true)
data class PlaceGeometry(
    val location: PlaceLocation?
)

@JsonClass(generateAdapter = true)
data class PlaceLocation(
    val lat: Double,
    val lng: Double
)

interface PlacesApi {
    @GET("maps/api/place/autocomplete/json")
    suspend fun autocomplete(
        @Query("input") input: String,
        @Query("key") key: String,
        @Query("types") types: String = "geocode",
        @Query("language") language: String = "pt-BR"
    ): PlaceAutocompleteResponse

    @GET("maps/api/place/details/json")
    suspend fun details(
        @Query("place_id") placeId: String,
        @Query("key") key: String,
        @Query("fields") fields: String = "geometry"
    ): PlaceDetailsResponse
}

object PlacesClient {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    val api: PlacesApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://maps.googleapis.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(PlacesApi::class.java)
    }
}

object MockPlacesDatabase {
    val fallbackSuggestions = listOf(
        PlacePrediction("Avenida Senador Teotônio Vilela, 8000 - Jardim Novo Parelheiros, São Paulo - SP", "mock_teotonio"),
        PlacePrediction("Rua Terezinha de Jesus, 150 - Parelheiros, São Paulo - SP", "mock_terezinha"),
        PlacePrediction("Estrada da Colônia, 2500 - Jardim Santa Fé, São Paulo - SP", "mock_colonia"),
        PlacePrediction("Rua Amaro Alves do Nascimento, 88 - Jardim Novo Parelheiros, São Paulo - SP", "mock_amaro"),
        PlacePrediction("Avenida Sadamu Inoue, 5200 - Parelheiros, São Paulo - SP", "mock_sadamu"),
        PlacePrediction("Estrada de Engenheiro Marsilac, 1200 - Parelheiros, São Paulo - SP", "mock_marsilac"),
        PlacePrediction("Avenida Jequitibá, 450 - Jardim Silveira, São Paulo - SP", "mock_jequitiba"),
        PlacePrediction("Rua José Galdino de Oliveira, 12 - Jardim Novo Parelheiros, São Paulo - SP", "mock_jose_galdino"),
        PlacePrediction("Rua Alice Alves do Nascimento, 330 - Jardim Novo Parelheiros, São Paulo - SP", "mock_alice"),
        PlacePrediction("Avenida das Américas, 432 - Jardim das Flores, São Paulo - SP", "mock_americas"),
        PlacePrediction("Rua Jardim das Flores, 88 - Parelheiros, São Paulo - SP", "mock_flores")
    )

    fun getCoordinates(placeId: String): PlaceLocation {
        val baseLat = -23.7915
        val baseLon = -46.6890
        return when (placeId) {
            "mock_teotonio" -> PlaceLocation(-23.7850, -46.6920)
            "mock_terezinha" -> PlaceLocation(-23.7920, -46.6850)
            "mock_colonia" -> PlaceLocation(-23.7750, -46.7050)
            "mock_amaro" -> PlaceLocation(-23.7950, -46.6870)
            "mock_sadamu" -> PlaceLocation(-23.8010, -46.6780)
            "mock_marsilac" -> PlaceLocation(-23.8150, -46.6700)
            "mock_jequitiba" -> PlaceLocation(-23.7900, -46.6990)
            "mock_jose_galdino" -> PlaceLocation(-23.7940, -46.6880)
            "mock_alice" -> PlaceLocation(-23.7930, -46.6895)
            "mock_americas" -> PlaceLocation(-23.7820, -46.7110)
            "mock_flores" -> PlaceLocation(-23.7880, -46.6950)
            else -> {
                val offsetLat = (placeId.hashCode() % 100) / 10000.0
                val offsetLon = (placeId.hashCode() % 100) / 10000.0
                PlaceLocation(baseLat + offsetLat, baseLon + offsetLon)
            }
        }
    }
}

object PlacesRepository {
    private const val TAG = "PlacesRepository"

    suspend fun searchPlaces(query: String): List<PlacePrediction> = withContext(Dispatchers.IO) {
        if (query.trim().isEmpty()) return@withContext emptyList()

        val apiKey = try { BuildConfig.PLACES_API_KEY } catch (e: Exception) { "" }
        if (apiKey.isEmpty() || apiKey == "MY_PLACES_API_KEY" || apiKey == "unknown") {
            Log.d(TAG, "PLACES_API_KEY is not set or default. Using local fallback.")
            return@withContext MockPlacesDatabase.fallbackSuggestions.filter {
                it.description.contains(query, ignoreCase = true)
            }
        }

        try {
            val response = PlacesClient.api.autocomplete(input = query, key = apiKey)
            if (response.status == "OK") {
                response.predictions
            } else {
                Log.w(TAG, "Places Autocomplete API returned status: ${response.status}")
                MockPlacesDatabase.fallbackSuggestions.filter {
                    it.description.contains(query, ignoreCase = true)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching from Places Autocomplete API, using fallback", e)
            MockPlacesDatabase.fallbackSuggestions.filter {
                it.description.contains(query, ignoreCase = true)
            }
        }
    }

    suspend fun getPlaceCoordinates(placeId: String): PlaceLocation = withContext(Dispatchers.IO) {
        if (placeId.startsWith("mock_")) {
            return@withContext MockPlacesDatabase.getCoordinates(placeId)
        }

        val apiKey = try { BuildConfig.PLACES_API_KEY } catch (e: Exception) { "" }
        if (apiKey.isEmpty() || apiKey == "MY_PLACES_API_KEY" || apiKey == "unknown") {
            return@withContext MockPlacesDatabase.getCoordinates(placeId)
        }

        try {
            val response = PlacesClient.api.details(placeId = placeId, key = apiKey)
            val loc = response.result?.geometry?.location
            if (response.status == "OK" && loc != null) {
                loc
            } else {
                Log.w(TAG, "Places Details API returned status: ${response.status}")
                MockPlacesDatabase.getCoordinates(placeId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching from Places Details API, using fallback", e)
            MockPlacesDatabase.getCoordinates(placeId)
        }
    }
}
