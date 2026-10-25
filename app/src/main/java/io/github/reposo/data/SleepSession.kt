package io.github.reposo.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

/** Una noche. [endMs] es null mientras la sesión sigue en curso. */
@Entity(tableName = "sleep_sessions")
data class SleepSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startMs: Long,
    val endMs: Long? = null,
    val mixTitle: String? = null,
)

/** Un clip de ronquido guardado en `filesDir/snore/`. [peakDb] es negativo (-96..0). */
@Entity(tableName = "snore_clips")
data class SnoreClip(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startMs: Long,
    val durationMs: Long,
    val file: String,
    val peakDb: Int,
)

/**
 * Tope de duración de una sesión. Al cerrar una noche atascada (el teléfono se
 * apagó, nadie tocó "Pausar") el fin no puede ser "ahora": convertiría 3 días
 * reales en una sesión de 72 h que destroza las medias. 14 h cubre de sobra
 * cualquier noche razonable.
 */
const val MAX_SESSION_MS = 14L * 3600_000

@Dao
interface SleepSessionDao {
    @Insert
    suspend fun insert(session: SleepSession): Long

    @Query("UPDATE sleep_sessions SET endMs = :endMs WHERE id = :id")
    suspend fun finish(id: Long, endMs: Long)

    /** La primera sesión abierta, si la hay; null si todas las noches están cerradas. */
    @Query("SELECT * FROM sleep_sessions WHERE endMs IS NULL ORDER BY startMs LIMIT 1")
    suspend fun openSession(): SleepSession?

    /**
     * Cierra TODAS las sesiones abiertas. Es lo que usa start() antes de insertar:
     * antes una sesión atascada bloqueaba para siempre el registro de noches nuevas.
     * El fin se recorta al tope desde el inicio de cada sesión.
     */
    @Query("UPDATE sleep_sessions SET endMs = MIN(:nowMs, startMs + :maxDurationMs) WHERE endMs IS NULL")
    suspend fun closeOpen(nowMs: Long, maxDurationMs: Long): Int

    @Query("SELECT * FROM sleep_sessions ORDER BY startMs DESC")
    fun observeAll(): Flow<List<SleepSession>>

    @Query("SELECT * FROM sleep_sessions WHERE endMs IS NOT NULL ORDER BY startMs DESC LIMIT :limit")
    suspend fun recentCompleted(limit: Int): List<SleepSession>

    /** Borrado total, usado por "Borrar todos los datos" en Ajustes. */
    @Query("DELETE FROM sleep_sessions")
    suspend fun deleteAll()
}

@Dao
interface SnoreClipDao {
    @Insert
    suspend fun insert(clip: SnoreClip): Long

    @Query("SELECT * FROM snore_clips ORDER BY startMs DESC")
    fun observeAll(): Flow<List<SnoreClip>>

    @Query("DELETE FROM snore_clips WHERE startMs < :cutoff")
    suspend fun cleanupOld(cutoff: Long)

    @Query("DELETE FROM snore_clips WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM snore_clips")
    suspend fun deleteAll()
}

@Database(
    entities = [SleepSession::class, SnoreClip::class, MovementBucket::class],
    version = 3,
    exportSchema = false,
)
abstract class ReposoDatabase : RoomDatabase() {
    abstract fun sleepSessions(): SleepSessionDao
    abstract fun snoreClips(): SnoreClipDao
    abstract fun movementBuckets(): MovementBucketDao

    companion object {
        @Volatile private var instance: ReposoDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `snore_clips` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`startMs` INTEGER NOT NULL, " +
                        "`durationMs` INTEGER NOT NULL, " +
                        "`file` TEXT NOT NULL, " +
                        "`peakDb` INTEGER NOT NULL)",
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `movement_buckets` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`startMs` INTEGER NOT NULL, " +
                        "`score` INTEGER NOT NULL)",
                )
            }
        }

        fun get(context: Context): ReposoDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, ReposoDatabase::class.java, "reposo.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build().also { instance = it }
        }
    }
}
