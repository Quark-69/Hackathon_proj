package com.hack.hackathon_proj;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import android.Manifest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;


public class CameraActivity extends AppCompatActivity {

    private ListenableFuture<ProcessCameraProvider> cameraProviderListenableFuture;

    private PreviewView previewView;

    private ImageCapture imageCapture;

    private int id;

    private Button captureButt;


    // Define the permission request code
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;

    // Define the permission string
    private static final String CAMERA_PERMISSION = Manifest.permission.CAMERA;

    // Check if camera permission is granted
    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(this, CAMERA_PERMISSION) == PackageManager.PERMISSION_GRANTED;
    }

    // Request camera permission
    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, new String[]{CAMERA_PERMISSION}, CAMERA_PERMISSION_REQUEST_CODE);
    }

    // Handle permission request result
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, CAMERA_PERMISSION)) {
                    // Request permission again
                    requestCameraPermission();
                } else {
                    // Redirect to settings
                    openSettings();
                    Toast.makeText(this, "Please enable camera permission in settings.", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void openSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }

    private void initializeCamera() {
        cameraProviderListenableFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderListenableFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderListenableFuture.get();
                if (cameraProvider != null) {
                    startCameraX(cameraProvider);
                } else {
                    Log.d("CameraActivity", "Camera provider is null");
                }
            } catch (ExecutionException | InterruptedException e) {
                Log.d("CameraActivity", "Error initializing camera: " + e.getMessage());
            }
        }, getExecutor());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        previewView = findViewById(R.id.preview_view);

        Intent intent = getIntent();
        id = intent.getIntExtra("ID", 0);
        captureButt = findViewById(R.id.capture_button);

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (hasCameraPermission()) {
            initializeCamera();
            captureButt.setOnClickListener(v -> {
                try {
                    capturePhoto();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } else {
            requestCameraPermission();
        }
    }

    private void capturePhoto() throws IOException {
        File directory = new File(getFilesDir(), String.valueOf(id));
        if (!directory.exists()) {
            if(!directory.mkdirs())
            {
                throw new IOException("Failed to create directory.");
            }
        }

        Date date = new Date();
        String timestamp = String.valueOf(date.getTime());

        String imgpath = directory.getAbsolutePath() + "/" + "face" + ".jpg"; // Append timestamp to filename

        File imgFile = new File(imgpath);

        imageCapture.takePicture(
                new ImageCapture.OutputFileOptions.Builder(imgFile).build(),
                getExecutor(),
                new ImageCapture.OnImageSavedCallback()
                {

                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        Log.d("CameraActivity","Saved at " + imgpath);
                        Intent returnIntent = new Intent();
                        returnIntent.putExtra("timestamp", timestamp);
                        setResult(RESULT_OK, returnIntent);
                        finish();
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.d("CameraActivity","Failed to save..");
                    }
                }
        );
    }

    private void startCameraX(ProcessCameraProvider processCameraProvider)
    {
        processCameraProvider.unbindAll();

        CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_FRONT).build();

        Preview preview = new Preview.Builder().build();

        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        imageCapture = new ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build();


        processCameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
    }

    private Executor getExecutor()
    {
        return ContextCompat.getMainExecutor(this);
    }
}