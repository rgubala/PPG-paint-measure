package com.PaintMeasure.datasave;

import android.app.Application;

import androidx.lifecycle.LiveData;

import java.util.List;

class DataRepository {

    private DataDAO mWordDao;
    private LiveData<List<Data>> mAllWords;

    // Note that in order to unit test the WordRepository, you have to remove the Application
    // dependency. This adds complexity and much more code, and this sample is not about testing.
    // See the BasicSample in the android-architecture-components repository at
    // https://github.com/googlesamples
    DataRepository(Application application) {
        DataRoomDatabase db = DataRoomDatabase.getDatabase(application);
        mWordDao = db.wordDao();
        mAllWords = mWordDao.getAlphabetizedWords();
    }

    // Room executes all queries on a separate thread.
    // Observed LiveData will notify the observer when the data has changed.
    LiveData<List<Data>> getAllWords() {
        return mAllWords;
    }

    // You must call this on a non-UI thread or your app will throw an exception. Room ensures
    // that you're not doing any long running operations on the main thread, blocking the UI.
    void insert(Data word) {
        DataRoomDatabase.databaseWriteExecutor.execute(() -> {
            mWordDao.insert(word);
        });
    }
}
