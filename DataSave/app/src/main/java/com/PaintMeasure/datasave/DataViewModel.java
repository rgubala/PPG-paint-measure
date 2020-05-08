package com.PaintMeasure.datasave;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

public class DataViewModel extends AndroidViewModel {

    private DataRepository mRepository;

    private LiveData<List<Data>> mAllWords;

    public DataViewModel (Application application) {
        super(application);
        mRepository = new DataRepository(application);
        mAllWords = mRepository.getAllWords();
    }

    LiveData<List<Data>> getAllWords() { return mAllWords; }

    public void insert(Data word) { mRepository.insert(word); }
}