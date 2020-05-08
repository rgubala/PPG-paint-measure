package com.PaintMeasure.datasave;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class NewDataActivity extends AppCompatActivity {

    public static final String EXTRA_REPLY = "com.example.android.wordlistsql.REPLY";

    private EditText mEditWordView;
    private EditText mEditSurface;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_data);
        mEditWordView = findViewById(R.id.edit_word);
        mEditSurface = findViewById(R.id.edit_surface);

        final Button button = findViewById(R.id.button_save);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent replyIntent = new Intent();
                if (TextUtils.isEmpty(mEditWordView.getText())) {
                    setResult(RESULT_CANCELED, replyIntent);
                } else {
                    Bundle myDataBundle = new Bundle();
                    String word = mEditWordView.getText().toString();
                    String surface =  mEditSurface.getText().toString();
                    myDataBundle.putString("word", word);
                    myDataBundle.putString("surf", surface);
                    replyIntent.putExtras(myDataBundle);
                    setResult(RESULT_OK, replyIntent);
                }
                finish();
            }
        });
    }
}