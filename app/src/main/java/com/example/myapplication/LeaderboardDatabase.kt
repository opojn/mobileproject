import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [LeaderboardEntry::class], version = 1, exportSchema = false)
abstract class LeaderboardDatabase : RoomDatabase() {
    abstract fun leaderboardDao(): LeaderboardDao

    companion object {
        @Volatile
        private var INSTANCE: LeaderboardDatabase? = null

        fun getDatabase(context: Context): LeaderboardDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LeaderboardDatabase::class.java,
                    "leaderboard_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
