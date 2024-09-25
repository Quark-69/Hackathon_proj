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

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
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
    }


    private void ManageKey() throws InvalidAlgorithmParameterException, UnrecoverableEntryException, CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException, InvalidKeySpecException, NoSuchProviderException {
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

        utilities = new Utilities(this, id);

        Intent intent = getIntent();
        if (Objects.equals(intent.getAction(), "com.auth.ACTION_REQUEST_DATA")) {
            id = intent.getIntExtra("ID", 0);
        }

        Button cameraButton = findViewById(R.id.Camera);

        try {
            ManageKey();
        } catch (InvalidAlgorithmParameterException | UnrecoverableEntryException |
                 CertificateException | NoSuchAlgorithmException | KeyStoreException | IOException |
                 InvalidKeySpecException | NoSuchProviderException e) {
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