package com.asaartech.topupandroid;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

public class TopUpActivity extends AppCompatActivity {

    String carrier;
    ImageView imageView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_top_up);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        carrier = getIntent().getStringExtra("CARRIER");
        setTitle(carrier);

        imageView = (ImageView) findViewById(R.id.imageView);


        checkReadStoragePermission();


    }

    public void loadImage(){
        File imageFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File imgFile = new File(imageFile + "/stu/myImage.jpg");

        if(imgFile.exists()){

            Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getPath());
            int width = myBitmap.getWidth();
            int height = myBitmap.getHeight();
            Matrix matrix = new Matrix();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                matrix.postRotate(0);
            } else {
                matrix.postRotate(90);
            }


            Bitmap resizedBitmap = Bitmap.createBitmap(myBitmap, 0, 0,
                    width, height, matrix, true);

            // make a Drawable from Bitmap to allow to set the BitMap
            // to the ImageView, ImageButton or what ever
            BitmapDrawable bitmapDrawable = new BitmapDrawable(resizedBitmap);

            imageView.setImageDrawable(bitmapDrawable);

            // center the Image
            imageView.setScaleType(ImageView.ScaleType.FIT_START);


        }
    }

    public static final int REQUEST_READ_EXTERNAL_STORAGE_PERMISSION_RESULT = 1;

    private void checkReadStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                loadImage();

            } else {
                if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    Toast.makeText(this, "App needs to be able to read from card.", Toast.LENGTH_SHORT).show();
                }
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_READ_EXTERNAL_STORAGE_PERMISSION_RESULT);
            }
        } else {
            loadImage();

        }

    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_READ_EXTERNAL_STORAGE_PERMISSION_RESULT) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadImage();
                Toast.makeText(this, "Read Permission Granted.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "App needs to read from card to run.", Toast.LENGTH_SHORT).show();
            }
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            this.finish();
        }

        return super.onOptionsItemSelected(item);
    }
}
