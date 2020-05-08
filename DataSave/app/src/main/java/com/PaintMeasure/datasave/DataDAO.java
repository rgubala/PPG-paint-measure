package com.PaintMeasure.datasave;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface DataDAO {

    // allowing the insert of the same word multiple times by passing a
    // conflict resolution strategy
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(Data data);

    @Query("DELETE FROM data_table")
    void deleteAll();

    @Query("SELECT * from data_table ORDER BY word ASC")
    LiveData<List<Data>> getAlphabetizedWords();
}