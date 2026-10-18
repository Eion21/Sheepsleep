package io.github.reposo.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Ventana de 10 s de actividad del acelerómetro dentro de una sesión.
 * 0 = quieto, 1 = ligero, 2 = medio, 3 = inquieto. Sin FK: se une a la
 * sesión por rango temporal (startMs dentro de [sesión]).
 */
@Entity(tableName = "movement_buckets")
data class MovementBucket(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startMs: Long,
    val score: Int,
)

@Dao
interface MovementBucketDao {
    @Insert
    suspend fun insertAll(buckets: List<MovementBucket>)

    @Query("SELECT * FROM movement_buckets WHERE startMs >= :since ORDER BY startMs")
    fun observeRecent(since: Long): Flow<List<MovementBucket>>

    @Query("DELETE FROM movement_buckets WHERE startMs < :cutoff")
    suspend fun cleanupOld(cutoff: Long)

    @Query("DELETE FROM movement_buckets")
    suspend fun deleteAll()
}
