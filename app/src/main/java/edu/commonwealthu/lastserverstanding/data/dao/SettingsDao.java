package edu.commonwealthu.lastserverstanding.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import edu.commonwealthu.lastserverstanding.data.entities.SettingsEntity;

/**
 * DAO for accessing app settings
 */
@Dao
public interface SettingsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(SettingsEntity settings);

    @Update
    void update(SettingsEntity settings);

    @Query("SELECT * FROM settings WHERE id = 1")
    LiveData<SettingsEntity> getSettings();

    @Query("SELECT * FROM settings WHERE id = 1")
    SettingsEntity getSettingsSync(); // For immediate access
}
