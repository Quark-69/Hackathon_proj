package com.hack.hackathon_proj;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import com.scottyab.rootbeer.RootBeer;


public class MainActivity extends AppCompatActivity {

    private KeyManager keyManager;

    private ActivityResultLauncher<Intent> cameraActivityResultLauncher;

    private Utilities utilities;

    private int id;

    private String timestamp;


    private void prepareDataForSending(String timestamp) throws Exception {
        utilities.beginImageEncryption(timestamp);
        utilities.createLog();
        utilities.signData(getFilesDir().getAbsolutePath() + "/" + id + "/");
        utilities.pack(getFilesDir().getAbsolutePath() + "/" + id + "/", getFilesDir().getAbsolutePath() + "/" + id + ".zip");
        utilities.deleteDir(new File(getFilesDir().getAbsolutePath() + "/" + id + "/"));
    }


    private void ManageKey() throws Exception{
        keyManager.generateKeyIfNeeded();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        RootBeer rootBeer = new RootBeer(this);


        if (rootBeer.isRooted()) {
            Toast.makeText(this, "Device is rooted! Exiting app...", Toast.LENGTH_LONG).show();
            finishAffinity();
        }

        keyManager = new KeyManager(this);

        File directory = new File(getFilesDir(), String.valueOf(id));
        if (!directory.exists()) {
            if(!directory.mkdirs())
            {
                try {
                    throw new IOException("Failed to create directory.");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        try {
            utilities = new Utilities(this, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Intent intent = getIntent();
        if (Objects.equals(intent.getAction(), "com.auth.ACTION_REQUEST_DATA")) {
            id = intent.getIntExtra("ID", 0);
        }

        Button cameraButton = findViewById(R.id.Camera);

        try {
            ManageKey();
        } catch (Exception e) {
            Log.e("MainActivity", "Error managing key: ", e);
        }

        cameraActivityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        assert data != null;
                        timestamp = data.getStringExtra("timestamp");

                        CompletableFuture.runAsync(() -> utilities.downloadPublicKey())
                                .thenRun(() -> {
                                    try {
                                        prepareDataForSending(timestamp);
                                    } catch (Exception e) {
                                        throw new RuntimeException(e);
                                    }
                                });

                    }
                });

        cameraButton.setOnClickListener(this::openCameraActivity);

    }

    public void openCameraActivity(View view) {
        Intent intent = new Intent(this, CameraActivity.class);
        intent.putExtra("ID", id);
        cameraActivityResultLauncher.launch(intent);
    }

}