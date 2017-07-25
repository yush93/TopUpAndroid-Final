package com.asaartech.topupandroid;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Camera;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.MenuItem;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class ScanActivity extends AppCompatActivity {

    String carrier;
    private static boolean FLASH_STATE = false;


    private ImageButton btnCapture;
    private ImageButton btnFlash;
    private TextureView textureView;
    private TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) { //width: 540 height: 960
            setupCamera(width, height);
            connectCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
        }
    };

    private CameraDevice cameraDevice;
    private CameraDevice.StateCallback cameraDeviceStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;
            startPreview();
            //Toast.makeText(getApplicationContext(), "Camera connection granted!", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            camera.close();
            cameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int i) {
            camera.close();
            cameraDevice = null;
        }
    };

    private String mCameraId;
    int totalRotation;
    private Size mPreviewSize;

    private void setupCamera(int width, int height) {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String cameraId : cameraManager.getCameraIdList()) {
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
                if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }

                StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                int deviceOrientation = getWindowManager().getDefaultDisplay().getRotation();
                totalRotation = sensorToDeviceRotation(cameraCharacteristics, deviceOrientation);

                boolean swapRotation = totalRotation == 90 || totalRotation == 270;

                int rotatedWidth = width;
                int rotatedHeight = height;
                if (swapRotation) {
                    rotatedWidth = height;
                    rotatedHeight = width;
                }

                mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), rotatedWidth, rotatedHeight);
                imageSize = chooseOptimalSize(map.getOutputSizes(ImageFormat.JPEG), rotatedWidth, rotatedHeight);

                imageReader = ImageReader.newInstance(imageSize.getWidth(), imageSize.getHeight(), ImageFormat.JPEG, 1);
                imageReader.setOnImageAvailableListener(onImageAvailableListener, mBackgroundHandler);
                mCameraId = cameraId;
                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }




    private void closeCamera() {
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
    }

    boolean hasFlash;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        hasFlash = getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);

        carrier = getIntent().getStringExtra("CARRIER");
        setTitle(carrier);

        createImageFolder();

        textureView = (TextureView) findViewById(R.id.textureView);

        btnFlash = (ImageButton)findViewById(R.id.btnFlash);
        btnFlash.setImageResource(R.drawable.ic_flash_on_black_24dp);



        btnFlash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!FLASH_STATE){
                    //Turn on flash
                    btnFlash.setImageResource(R.drawable.ic_flash_on_black_24dp);
                    FLASH_STATE = true;
                } else {
                    btnFlash.setImageResource(R.drawable.ic_flash_off_black_24dp);
                    FLASH_STATE = false;
                    //turn off flash
                }
            }
        });

        btnCapture = (ImageButton) findViewById(R.id.btnCapture);
        btnCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                lockFocus();
                checkWriteStoragePermission();
                Toast.makeText(ScanActivity.this, "Scan Complete!", Toast.LENGTH_LONG).show();

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Intent nav = new Intent(getBaseContext(), TopUpActivity.class);
                        nav.putExtra("CARRIER", carrier);
                        startActivity(nav);
                    }
                },2000);

            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            this.closeCamera();
            this.finish();
        }

        return super.onOptionsItemSelected(item);
    }

    //to see if textureview is available... if not it will initialize it
    @Override
    protected void onResume() {
        super.onResume();
        startBackgroundThread();
        if (textureView.isAvailable()) {
            setupCamera(textureView.getWidth(), textureView.getHeight());
            connectCamera();
        } else {
            textureView.setSurfaceTextureListener(surfaceTextureListener);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION_RESULT) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Camera Permission Rejected! Requires Camera Permission!", Toast.LENGTH_SHORT).show();
            }
        }
        if (requestCode == REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION_RESULT) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                try {
                    createImageFileName();
                    //What to do when capture button is pressed
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Toast.makeText(this, "Storage Permission Granted.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "App needs to save Image to run.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onPause() {
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

//    //To make it full screen sticky
//    @Override
//    public void onWindowFocusChanged(boolean hasFocus) {
//        super.onWindowFocusChanged(hasFocus);
//        //To get our own view
//        View decorView = getWindow().getDecorView();
//        if (hasFocus) {
//            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
//                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
//                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
//                    | View.SYSTEM_UI_FLAG_FULLSCREEN
//                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
//        }
//    }

    //Multi threading for efficiency
    private HandlerThread mBackgroundHandlerThread;
    private Handler mBackgroundHandler;

    private void startBackgroundThread() {
        mBackgroundHandlerThread = new HandlerThread("Camera2VideoImage");
        mBackgroundHandlerThread.start();
        mBackgroundHandler = new Handler(mBackgroundHandlerThread.getLooper());
    }

    private void stopBackgroundThread() {
        mBackgroundHandlerThread.quitSafely();
        try {
            mBackgroundHandlerThread.join();
            mBackgroundHandlerThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    //Camera Orientation
    private static SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 0);
        ORIENTATIONS.append(Surface.ROTATION_90, 90);
        ORIENTATIONS.append(Surface.ROTATION_180, 180);
        ORIENTATIONS.append(Surface.ROTATION_270, 270);
    }

    private static int sensorToDeviceRotation(CameraCharacteristics cameraCharacteristics, int deviceOrientation) {
        int sensorOrientation = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        deviceOrientation = ORIENTATIONS.get(deviceOrientation);
        return (sensorOrientation + deviceOrientation + 360) % 360;
    }

    //Setting Preview size dimensions
    ///////////////////////////////EDIT HERE/////////////////////////
    //Refer
    //https://www.youtube.com/watch?v=z3LAbtDh1VE&index=1&list=PL9jCwTXYWjDIHNEGtsRdCTk79I9-95TbJ&t=24s#t=486.353675
    private static class CompareSizeByArea implements Comparator<Size> {
        @Override
        public int compare(Size lhs, Size rhs) {
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() /
                    (long) rhs.getWidth() * rhs.getHeight());
        }
    }

    private static Size chooseOptimalSize(Size[] choices, int width, int height) {
        List<Size> bigEnough = new ArrayList<Size>();
        for (Size option : choices) {
            if (option.getHeight() == option.getWidth() * height / width
                    && option.getWidth() >= width
                    && option.getHeight() >= height) {
                bigEnough.add(option);
            }
        }
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizeByArea());
        } else {
            return choices[0];
        }
    }

    //Connecting to camera
    private static final int REQUEST_CAMERA_PERMISSION_RESULT = 0;
    private static final int REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION_RESULT = 1;

    private void connectCamera() {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    cameraManager.openCamera(mCameraId, cameraDeviceStateCallback, mBackgroundHandler);
                } else {
                    if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                        Toast.makeText(this, "Video app requires access to camera", Toast.LENGTH_SHORT).show();
                    }
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION_RESULT);
                }
            } else {
                cameraManager.openCamera(mCameraId, cameraDeviceStateCallback, mBackgroundHandler);
            }

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    //Previewing from camera
    private CaptureRequest.Builder mCaptureRequestBuilder;

    private void startPreview() {
        SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
        surfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        Surface previewSurface = new Surface(surfaceTexture);

        try {
            mCaptureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mCaptureRequestBuilder.addTarget(previewSurface);

            cameraDevice.createCaptureSession(Arrays.asList(previewSurface, imageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            previewCaptureSession = session;
                            try {
                                previewCaptureSession.setRepeatingRequest(mCaptureRequestBuilder.build(),
                                        null, mBackgroundHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                            Toast.makeText(ScanActivity.this, "Configuration Error!", Toast.LENGTH_SHORT).show();
                        }
                    }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////////
    //Storing the file
    private File imageFolder;
    private String imageFileName;

    private void createImageFolder() {
        File imageFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        imageFolder = new File(imageFile, "stu");
        if (!imageFolder.exists()) {
            imageFolder.mkdir();
        }
    }

    File imageFile;
    private File createImageFileName() throws IOException {
        imageFile = new File(imageFolder, "myImage.jpg");
        imageFile.createNewFile();
        //imageFileName = imageFile.getAbsolutePath();
        imageFileName = imageFile.getPath();
        return imageFile;
    }

    //Handling permissions for Marshmallow
    //call the following function at button click to capture image
    private void checkWriteStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                try {
                    createImageFileName();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    Toast.makeText(this, "App needs to be able to save Images.", Toast.LENGTH_SHORT).show();
                }
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION_RESULT);
            }
        } else {
            try {
                createImageFileName();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //For Image Capture
    private Size previewSize;
    private Size imageSize;
    private ImageReader imageReader;
    private CameraCaptureSession previewCaptureSession;
    private static final int STATE_PREVIEW = 0;
    private static final int STATE_WAIT_LOCK = 1;
    private int captureState = STATE_PREVIEW;

    private CameraCaptureSession.CaptureCallback previewCaptureCallback = new CameraCaptureSession.CaptureCallback() {
        ////////////////////////////////////////////////////////////////////////////////////
        //Might require some editing
        private void process(TotalCaptureResult captureResult) {
            switch (captureState) {
                case STATE_PREVIEW:
                    //Do nothing
                    break;
                case STATE_WAIT_LOCK:
                    captureState = STATE_PREVIEW;
                    Integer afState = captureResult.get(CaptureResult.CONTROL_AF_STATE);
                    if (afState == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED ||
                            afState == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED) {
                        startStillCaptureRequest();
                    }
                    break;
            }
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);

            process(result);
        }
    };
    private final ImageReader.OnImageAvailableListener onImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader imageReader) {
            mBackgroundHandler.post(new ImageSaver(imageReader.acquireLatestImage()));
        }
    };


    /////////////////////////////////////////////////////////////////////////////
    //Focus locking
    private void lockFocus() {
        captureState = STATE_WAIT_LOCK;
        mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_START);
        try {
            previewCaptureSession.capture(mCaptureRequestBuilder.build(), previewCaptureCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    //Capturing Image now
    private void startStillCaptureRequest() {
        try {
            mCaptureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            mCaptureRequestBuilder.addTarget(imageReader.getSurface());
            mCaptureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, totalRotation );

            CameraCaptureSession.CaptureCallback stillCaptureCallback = new
                    CameraCaptureSession.CaptureCallback() {
                        @Override
                        public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
                            super.onCaptureStarted(session, request, timestamp, frameNumber);
                            try {
                                createImageFileName();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    };
            previewCaptureSession.capture(mCaptureRequestBuilder.build(), stillCaptureCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private class ImageSaver implements Runnable{
        private final Image mImage;

        public ImageSaver(Image image){
            mImage = image;
        }

        @Override
        public void run() {
            ByteBuffer byteBuffer = mImage.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[byteBuffer.remaining()];
            byteBuffer.get(bytes);

            FileOutputStream fileOutputStream = null;
            try {
                fileOutputStream = new FileOutputStream(imageFileName);
                fileOutputStream.write(bytes);
                fileOutputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mImage.close();
                if(fileOutputStream != null){
                    try {
                        fileOutputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }


    /////////////////////////////////////////////////////////////////////////////////////////////
    //For flash


}
