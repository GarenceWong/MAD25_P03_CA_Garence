package np.ict.mad.mad25_p03_ca_garence

import android.content.Context
import androidx.room.Database
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase

@Entity(
    tableName = "users",
    indices = [Index(value = ["username"], unique = true)]
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val userId: Int = 0,
    val username: String,
    val passwordHash: String
)

@Entity(
    tableName = "scores",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["userId"])]
)
data class ScoreEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val score: Int,
    val timestamp: Long
)

data class LeaderboardRow(
    val userId: Int,
    val username: String,
    val bestScore: Int
)

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: UserEntity): Long

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    @Query("SELECT * FROM users WHERE username = :username AND passwordHash = :passwordHash LIMIT 1")
    suspend fun signIn(username: String, passwordHash: String): UserEntity?
}

@Dao
interface ScoreDao {
    @Insert
    suspend fun insertScore(score: ScoreEntity): Long

    @Query("SELECT MAX(score) FROM scores WHERE userId = :userId")
    suspend fun getPersonalBest(userId: Int): Int?

    @Query(
        """
    SELECT u.userId AS userId,
           u.username AS username,
           MAX(s.score) AS bestScore
    FROM users u
    INNER JOIN scores s ON s.userId = u.userId
    GROUP BY u.userId, u.username
    ORDER BY bestScore DESC, username ASC
"""
    )
    suspend fun getLeaderboard(): List<LeaderboardRow>
}

@Database(
    entities = [UserEntity::class, ScoreEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun scoreDao(): ScoreDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "wackamole.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
