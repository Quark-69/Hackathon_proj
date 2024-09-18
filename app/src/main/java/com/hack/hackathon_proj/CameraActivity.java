package com.hack.hackathon_proj;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.Manifest;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.util.Date;
import java.util.concurrent.ExecutionException;


public class CameraActivity extends AppCompatActivity {

    private Button captureButt;
    private ListenableFuture<ProcessCameraProvider> cameraProviderListenableFuture;

    private PreviewView previewView;

    private ImageCapture imageCapture;


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
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, initialize camera
                cameraProviderListenableFuture = ProcessCameraProvider.getInstance(this);
                cameraProviderListenableFuture.addListener(() -> {
                    try {
                        ProcessCameraProvider cameraProvider = cameraProviderListenableFuture.get();
                        startCameraX(cameraProvider);
                    } catch (ExecutionException | InterruptedException e) {
                        e.printStackTrace();
                    }
                }, getMainExecutor());
            } else {
                // Permission denied, show rationale or toast
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        captureButt = findViewById(R.id.capture_button);
        previewView = findViewById(R.id.preview_view);
        captureButt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                capturePhoto();
            }
        });

        if (hasCameraPermission()) {
            // Permission already granted, initialize camera
            cameraProviderListenableFuture = ProcessCameraProvider.getInstance(this);
            cameraProviderListenableFuture.addListener(() -> {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderListenableFuture.get();
                    startCameraX(cameraProvider);
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            }, getMainExecutor());
        } else {
            // Request camera permission
            requestCameraPermission();
        }
    }

    private void capturePhoto() {
        File directory = getFilesDir();
        Date date = new Date();
        String timestamp = String.valueOf(date.getTime());

        String imgpath = directory.getAbsolutePath() + "/" + timestamp + ".jpg";

        File imgFile = new File(imgpath);

        imageCapture.takePicture(
                new ImageCapture.OutputFileOptions.Builder(imgFile).build(),
                getMainExecutor(),
                new ImageCapture.OnImageSavedCallback()
                {

                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        Log.d("CameraActivity","Saved at " + imgpath);
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


        processCameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, preview, imageCapture);
    }
}