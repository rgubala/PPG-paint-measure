package com.example.ppg_test;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import java.io.IOException;

public class LoadActivity extends AppCompatActivity {

    ImageView targetImage;
    Button buttonEdit;
    public static final String EDIT = "edit";
    public static final String FILE_URI = "uri";
    public static final String CURRENT_ANGLE = "current_angle";
    public static final int EDIT_REQUEST = 8;
    Bitmap bitmap;
    private View decorView;
    private int angle = 0;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_load_image);

        decorView = getWindow().getDecorView();
        decorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                if(visibility == 0)
                    decorView.setSystemUiVisibility(hideSystemBars());
            }
        });

        targetImage = findViewById(R.id.targetImage);
        buttonEdit = findViewById(R.id.buttonEdit);
        Intent intent = getIntent();
        final String uriStr = intent.getStringExtra(MainActivity.IMAGE_URI);
        Uri imageUri = Uri.parse(uriStr);

        try {
            bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));
            fitImageViewToImage(targetImage, bitmap);
            targetImage.setImageBitmap(bitmap);
        } catch (IOException e) {
            e.printStackTrace();
        }

        targetImage.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Toast.makeText(LoadActivity.this, "touch", Toast.LENGTH_SHORT).show();
                return false;
            }
        });

        buttonEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoadActivity.this, EditActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString(FILE_URI, uriStr);
                bundle.putInt(CURRENT_ANGLE, angle);
                intent.putExtra(EDIT, bundle);
                startActivityForResult(intent, EDIT_REQUEST);
            }
        });
    }

    public static void fitImageViewToImage(ImageView i, Bitmap b) {
        i.setAdjustViewBounds(true);
        i.setMaxHeight(b.getHeight());
        i.setMaxWidth(b.getWidth());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if(requestCode == EDIT_REQUEST && data != null) {
                Bundle bundle = data.getBundleExtra(EditActivity.IMAGE_CHANGES);
                angle = bundle.getInt(EditActivity.ANGLE);
                showImage(targetImage, bitmap, angle);
            }
        }
    }

    public static void showImage(ImageView i, Bitmap b, int a) {
        Matrix matrix = new Matrix();
        matrix.postRotate(a);
        b = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), matrix, true);
        fitImageViewToImage(i, b);
        i.setImageBitmap(b);
    }

    @Override
    public void onWindowFocusChanged (boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if(hasFocus) {
            decorView.setSystemUiVisibility(hideSystemBars());
        }
    }

    public static int hideSystemBars () {
        return  View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
    }
}