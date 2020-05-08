package com.PaintMeasure.datasave;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "data_table")
public class Data {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "word")
    private String mWord;


   @ColumnInfo(name = "surfaceG")
    public double surface;

    public Data( @NonNull String word, double givenSurface) {this.mWord = word; this.surface = givenSurface;}
    public Data(@NonNull String word) {this.mWord = word;}
    public String getWord(){return this.mWord;}
   public double getSurface(){return this.surface;}
}
