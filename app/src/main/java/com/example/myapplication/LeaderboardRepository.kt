import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LeaderboardRepository(private val dao: LeaderboardDao) {

    suspend fun insertScore(playerName: String, score: Int) {
        withContext(Dispatchers.IO) {
            dao.insertScore(LeaderboardEntry(playerName = playerName, score = score))
        }
    }

    suspend fun getLeaderboard(): List<LeaderboardEntry> {
        return withContext(Dispatchers.IO) {
            dao.getLeaderboard()
        }
    }
}
