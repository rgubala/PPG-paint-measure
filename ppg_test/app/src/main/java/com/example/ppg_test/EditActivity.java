package com.example.ppg_test;

import androidx.appcompat.app.AppCompatActivity;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.FileNotFoundException;

public class EditActivity extends AppCompatActivity {

    ImageView editImage;
    Button buttonRotate;
    Button buttonSave;
    Button buttonCancel;
    Bitmap bitmapEdit;
    private View decorView;
    private int angle = 0;
    public static final String ANGLE = "angle";
    public static final String IMAGE_CHANGES = "bundle";

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);

        decorView = getWindow().getDecorView();
        decorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                if(visibility == 0)
                    decorView.setSystemUiVisibility(LoadActivity.hideSystemBars());
            }
        });

        editImage = findViewById(R.id.editImage);
        buttonRotate = findViewById(R.id.buttonRotate);
        buttonSave = findViewById(R.id.buttonSave);
        buttonCancel = findViewById(R.id.buttonCancel);
        Intent intent = getIntent();
        Bundle bundle = intent.getBundleExtra(LoadActivity.EDIT);
        String uriStr = bundle.getString(LoadActivity.FILE_URI);
        angle = bundle.getInt(LoadActivity.CURRENT_ANGLE);
        Uri imageUri = Uri.parse(uriStr);

        try {
            bitmapEdit = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));
            LoadActivity.showImage(editImage, bitmapEdit, angle);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        editImage.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Toast.makeText(EditActivity.this, "touch", Toast.LENGTH_SHORT).show();
                return false;
            }
        });

        buttonRotate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LoadActivity.showImage(editImage, bitmapEdit, rotationAngle());
            }
        });

        buttonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(EditActivity.this, LoadActivity.class);
                Bundle bundle = new Bundle();
                bundle.putInt(ANGLE, angle);
                intent.putExtra(IMAGE_CHANGES, bundle);
                setResult(RESULT_OK, intent);
                finish();
            }
        });

        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(EditActivity.this, LoadActivity.class);
                finish();
            }
        });
    }

    private int rotationAngle() {
        if(angle == 360)
            angle = 0;
        angle += 90;
        return angle;
    }

    @Override
    public void onWindowFocusChanged (boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if(hasFocus) {
            decorView.setSystemUiVisibility(LoadActivity.hideSystemBars());
        }
    }

}