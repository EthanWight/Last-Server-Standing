package edu.commonwealthu.lastserverstanding.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;

import edu.commonwealthu.lastserverstanding.data.entities.SaveGameEntity;

/**
 * DAO for accessing saved games
 */
@Dao
public interface SaveGameDao {

    @Insert
    long insert(SaveGameEntity saveGame);
}
