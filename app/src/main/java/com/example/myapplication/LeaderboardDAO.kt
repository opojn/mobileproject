import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface LeaderboardDao {
    @Insert
    suspend fun insertScore(entry: LeaderboardEntry)

    @Query("SELECT * FROM leaderboard ORDER BY score DESC")
    suspend fun getLeaderboard(): List<LeaderboardEntry>
}
