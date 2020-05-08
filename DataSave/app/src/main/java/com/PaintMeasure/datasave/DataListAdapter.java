package com.PaintMeasure.datasave;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class DataListAdapter  extends RecyclerView.Adapter<DataListAdapter.DataViewHolder> {

    class DataViewHolder extends RecyclerView.ViewHolder {
        private final TextView wordItemView;
        private final TextView surfaceItemView;

        private DataViewHolder(View itemView) {
            super(itemView);
            wordItemView = itemView.findViewById(R.id.textView);
            surfaceItemView=itemView.findViewById(R.id.textViewSurface);
        }
    }

    private final LayoutInflater mInflater;
    private List<Data> mWords; // Cached copy of words

    DataListAdapter(Context context) { mInflater = LayoutInflater.from(context); }

    @Override
    public DataViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = mInflater.inflate(R.layout.recyclerview_item, parent, false);
        return new DataViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(DataViewHolder holder, int position) {
        if (mWords != null) {
            Data current = mWords.get(position);
            holder.wordItemView.setText(current.getWord());
            holder.surfaceItemView.setText(Double.toString(current.getSurface()));
        } else {
            // Covers the case of data not being ready yet.
            holder.wordItemView.setText("No Word");
            holder.surfaceItemView.setText("No surface");
        }
    }

    void setWords(List<Data> words){
        mWords = words;
        notifyDataSetChanged();
    }

    // getItemCount() is called many times, and when it is first called,
    // mWords has not been updated (means initially, it's null, and we can't return null).
    @Override
    public int getItemCount() {
        if (mWords != null)
            return mWords.size();
        else return 0;
    }
}
