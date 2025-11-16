package edu.commonwealthu.lastserverstanding.data;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import edu.commonwealthu.lastserverstanding.data.dao.SaveGameDao;
import edu.commonwealthu.lastserverstanding.data.dao.SettingsDao;
import edu.commonwealthu.lastserverstanding.data.entities.SaveGameEntity;
import edu.commonwealthu.lastserverstanding.data.entities.SettingsEntity;

/**
 * Main Room database for the app
 */
@Database(entities = {SaveGameEntity.class, SettingsEntity.class}, version = 2, exportSchema = false)
public abstract class GameDatabase extends RoomDatabase {

    private static volatile GameDatabase INSTANCE;

    public abstract SaveGameDao saveGameDao();
    public abstract SettingsDao settingsDao();

    /**
     * Get database instance (singleton pattern)
     */
    public static GameDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (GameDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                        context.getApplicationContext(),
                        GameDatabase.class,
                        "last_server_standing_database"
                    )
                    .fallbackToDestructiveMigration() // Recreate DB on schema changes (dev mode)
                    .build();
                }
            }
        }
        return INSTANCE;
    }
}
