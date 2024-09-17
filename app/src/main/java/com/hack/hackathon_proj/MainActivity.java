package com.hack.hackathon_proj;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
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

import com.scottyab.rootbeer.RootBeer;


public class MainActivity extends AppCompatActivity {

    private KeyManager keyManager;

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

        try {
            ManageKey();
        } catch (InvalidAlgorithmParameterException | UnrecoverableEntryException | CertificateException | NoSuchAlgorithmException | KeyStoreException | IOException | InvalidKeySpecException | NoSuchProviderException e) {
            Log.e("MainActivity", "Error managing key: ", e);
        }

    }

}