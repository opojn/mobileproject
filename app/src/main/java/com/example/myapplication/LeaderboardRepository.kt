import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LeaderboardRepository(private val leaderboardDao: LeaderboardDao) {

    suspend fun insertScore(playerName: String, score: Int) {
        val leaderboard = Leaderboard(playerName = playerName, score = score)
        leaderboardDao.insert(leaderboard)
    }

    suspend fun getLeaderboard(): List<Leaderboard> {
        return leaderboardDao.getAllScores()
    }
}
