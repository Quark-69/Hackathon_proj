package com.hack.hackathon_proj;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
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
        utilities.beginEncryption(timestamp);
        utilities.signData(getFilesDir().getAbsolutePath() + "/" + id + "/");
        utilities.pack(getFilesDir().getAbsolutePath() + "/" + id + "/", getFilesDir().getAbsolutePath() + "/" + id + ".zip");
        utilities.cleanDir(new File(getFilesDir().getAbsolutePath() + "/" + id + "/"));
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

        Intent intent = getIntent();
        if (Objects.equals(intent.getAction(), "com.auth.ACTION_AUTHENTICATE")) {
            id = intent.getIntExtra("ID", 0);
        }

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

        utilities = new Utilities(this, id);

        Button cameraButton = findViewById(R.id.Camera);
        ProgressBar progressBar = findViewById(R.id.progressBar);

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

                        runOnUiThread(() -> progressBar.setVisibility(View.VISIBLE));

                        CompletableFuture.runAsync(() -> utilities.downloadPublicKey())
                                .thenRun(() -> {
                                    Intent returnIntent = new Intent();
                                    try {
                                        prepareDataForSending(timestamp);
                                        boolean authenticated = utilities.requestAuth();
                                        returnIntent.putExtra("authenticated", authenticated);
                                    } catch (Exception e) {
                                        throw new RuntimeException(e);
                                    }
                                    runOnUiThread(() -> progressBar.setVisibility(View.GONE));
                                    setResult(RESULT_OK, returnIntent);
                                    finish();
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