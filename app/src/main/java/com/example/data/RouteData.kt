package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@Entity(tableName = "stops")
data class Stop(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val address: String,
    val recipientName: String,
    val status: String, // "PENDING", "COMPLETED", "SKIPPED"
    val sequence: Int, // order in route
    val latitude: Double,
    val longitude: Double,
    val routeDate: String, // format: "dd-MM-yyyy" (e.g., "19-06-2026")
    val notes: String = "",
    val phoneNumber: String = "",
    val estimatedTime: String = "" // e.g., "14:15"
)

@androidx.room.Dao
interface StopDao {
    @Query("SELECT * FROM stops WHERE routeDate = :routeDate ORDER BY sequence ASC")
    fun getStopsForRoute(routeDate: String): Flow<List<Stop>>

    @Query("SELECT DISTINCT routeDate FROM stops ORDER BY routeDate DESC")
    fun getAllRouteDates(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStop(stop: Stop): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStops(stops: List<Stop>)

    @Update
    suspend fun updateStop(stop: Stop)

    @Query("DELETE FROM stops WHERE id = :id")
    suspend fun deleteStop(id: Int)

    @Query("DELETE FROM stops WHERE routeDate = :routeDate")
    suspend fun deleteRoute(routeDate: String)
}

@Database(entities = [Stop::class], version = 1, exportSchema = false)
abstract class RouteDatabase : RoomDatabase() {
    abstract fun stopDao(): StopDao

    companion object {
        @Volatile
        private var INSTANCE: RouteDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): RouteDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RouteDatabase::class.java,
                    "route_database"
                )
                .addCallback(DatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateDatabase(database.stopDao())
                    }
                }
            }

            suspend fun populateDatabase(stopDao: StopDao) {
                // Pre-seed mock delivery route stops matching the dates in screenshots
                val stops19 = listOf(
                    Stop(
                        address = "Rua Visconde de Montalegre, 412 - Jardim Santa Fe, São Paulo",
                        recipientName = "Diego dos Santos",
                        status = "COMPLETED",
                        sequence = 1,
                        latitude = -23.7850,
                        longitude = -46.6810,
                        routeDate = "19-06-2026",
                        notes = "Entregar na portaria",
                        phoneNumber = "+55 11 99999-1111",
                        estimatedTime = "10:15"
                    ),
                    Stop(
                        address = "Estrada da Colonia Mário Reimberg Christe, 1050 - Jardim Novo Parelheiros",
                        recipientName = "Ana Oliveira",
                        status = "SKIPPED",
                        sequence = 2,
                        latitude = -23.7920,
                        longitude = -46.6910,
                        routeDate = "19-06-2026",
                        notes = "Ligar antes de chegar",
                        phoneNumber = "+55 11 99999-2222",
                        estimatedTime = "11:30"
                    ),
                    Stop(
                        address = "Chácara Bosque do Sol, 55 - Recanto Campo Belo",
                        recipientName = "Marcos Souza",
                        status = "PENDING",
                        sequence = 3,
                        latitude = -23.7980,
                        longitude = -46.6850,
                        routeDate = "19-06-2026",
                        notes = "Cuidado com o cão bravo",
                        phoneNumber = "+55 11 99999-3333",
                        estimatedTime = "12:00"
                    )
                )

                val stops20 = listOf(
                    Stop(
                        address = "Rua Jardim Novo Parelheiros, 85 - Jardim Novo Parelheiros",
                        recipientName = "Beatriz Lima",
                        status = "PENDING",
                        sequence = 1,
                        latitude = -23.7915,
                        longitude = -46.6890,
                        routeDate = "20-06-2026",
                        notes = "Deixar com o vizinho se ausente",
                        phoneNumber = "+55 11 99999-4444",
                        estimatedTime = "14:15"
                    ),
                    Stop(
                        address = "Avenida Jardim Silveira, 204 - Jardim Silveira",
                        recipientName = "Carlos Pereira",
                        status = "PENDING",
                        sequence = 2,
                        latitude = -23.7995,
                        longitude = -46.6960,
                        routeDate = "20-06-2026",
                        notes = "Bloco B, Apto 43",
                        phoneNumber = "+55 11 99999-5555",
                        estimatedTime = "15:00"
                    )
                )

                stopDao.insertStops(stops19)
                stopDao.insertStops(stops20)
            }
        }
    }
}

class RouteRepository(private val stopDao: StopDao) {
    fun getStopsForRoute(routeDate: String): Flow<List<Stop>> =
        stopDao.getStopsForRoute(routeDate)

    val allRouteDates: Flow<List<String>> = stopDao.getAllRouteDates()

    suspend fun insertStop(stop: Stop) = stopDao.insertStop(stop)

    suspend fun insertStops(stops: List<Stop>) = stopDao.insertStops(stops)

    suspend fun updateStop(stop: Stop) = stopDao.updateStop(stop)

    suspend fun deleteStop(id: Int) = stopDao.deleteStop(id)

    suspend fun deleteRoute(routeDate: String) = stopDao.deleteRoute(routeDate)
}
