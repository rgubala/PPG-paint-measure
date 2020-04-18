package com.example.ppg_test;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_IMAGE_PICK = 1;
    private static final int REQUEST_CAMERA = 2;
    public static final String IMAGE_URI = "uri";
    public static final int REQUEST_CAMERA_PERMISSION_CODE = 99;
    public static final int REQUEST_READ_EXTERNAL_STORAGE_CODE = 98;
    String currentPhotoPath;
    Uri capturedImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void pickImage(View view) {
        if(checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
        {
            dispatchPickImageIntent();
        }
        else {
            String[] permissionRequest = {Manifest.permission.READ_EXTERNAL_STORAGE};
            requestPermissions(permissionRequest, REQUEST_READ_EXTERNAL_STORAGE_CODE);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void takePhoto(View view) {
        if(checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
            dispatchTakePictureIntent();
        else {
            String[] permissionRequest = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
           requestPermissions(permissionRequest, REQUEST_CAMERA_PERMISSION_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == REQUEST_CAMERA_PERMISSION_CODE) {
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED)
                dispatchTakePictureIntent();
            if(grantResults[0] == PackageManager.PERMISSION_DENIED)
                Toast.makeText(this, R.string.camPermission, Toast.LENGTH_LONG).show();
            if(grantResults[1] == PackageManager.PERMISSION_DENIED)
                Toast.makeText(this, R.string.filesPermission, Toast.LENGTH_LONG).show();
        }

        if(requestCode == REQUEST_READ_EXTERNAL_STORAGE_CODE) {
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED)
                dispatchPickImageIntent();
            else
                Toast.makeText(this, R.string.filesPermission, Toast.LENGTH_LONG).show();
        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // error
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this, "com.example.ppg_test.fileProvider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                capturedImageUri = photoURI;
                startActivityForResult(takePictureIntent, REQUEST_CAMERA);
            }
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "PAINT_MEASURED_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void dispatchPickImageIntent() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_IMAGE_PICK);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Auto-generated method stub
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_PICK) {
                Intent intent = new Intent(this, LoadActivity.class);
                Uri imageUri = data.getData();
                String uriStr = imageUri.toString();
                intent.putExtra(IMAGE_URI, uriStr);
                startActivity(intent);
            }
            if (requestCode == REQUEST_CAMERA) {
                Intent intent = new Intent(this, LoadActivity.class);
                String uriStr = capturedImageUri.toString();
                intent.putExtra(IMAGE_URI, uriStr);
                startActivity(intent);

            }
        }
    }
}
