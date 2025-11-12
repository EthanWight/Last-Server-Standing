package edu.commonwealthu.lastserverstanding.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import edu.commonwealthu.lastserverstanding.data.entities.SaveGameEntity;
import java.util.List;

/**
 * DAO for accessing saved games
 */
@Dao
public interface SaveGameDao {

    @Insert
    long insert(SaveGameEntity saveGame);

    @Update
    void update(SaveGameEntity saveGame);

    @Delete
    void delete(SaveGameEntity saveGame);

    @Query("SELECT * FROM save_games ORDER BY timestamp DESC")
    LiveData<List<SaveGameEntity>> getAllSaves();

    @Query("SELECT * FROM save_games WHERE id = :saveId")
    LiveData<SaveGameEntity> getSaveById(int saveId);

    @Query("SELECT * FROM save_games WHERE id = :saveId")
    SaveGameEntity getSaveByIdSync(int saveId);

    @Query("SELECT * FROM save_games WHERE isAutoSave = 1 ORDER BY timestamp DESC LIMIT 1")
    LiveData<SaveGameEntity> getLatestAutoSave();

    @Query("SELECT * FROM save_games WHERE isAutoSave = 1 ORDER BY timestamp DESC LIMIT 1")
    SaveGameEntity getLatestAutoSaveSync();

    @Query("DELETE FROM save_games WHERE isAutoSave = 1")
    void deleteAllAutoSaves();

    @Query("SELECT COUNT(*) FROM save_games")
    int getSaveCount();
}
