import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface LeaderboardDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(score: Leaderboard)

    @Query("SELECT * FROM leaderboard ORDER BY score DESC")
    suspend fun getAllScores(): List<Leaderboard>
}
