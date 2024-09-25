package com.hack.hackathon_proj;


import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.security.keystore.StrongBoxUnavailableException;
import android.util.Log;


import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;

public class KeyManager {

    private final Context context;

    public KeyManager(Context context)
    {
        this.context = context;
    }

    private void generate() throws NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_RSA, context.getString(R.string.KEYSTORE_PROVIDER));

        KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(
                context.getString(R.string.KEY_ALIAS),
                KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY)
                .setDigests(KeyProperties.DIGEST_SHA256)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
                .setKeySize(2048)
                .setUserAuthenticationRequired(false)
                .setKeyValidityStart(Calendar.getInstance().getTime())
                .setKeyValidityEnd(getValidityEndDate())
                .setIsStrongBoxBacked(true);  // Request StrongBox backing

        try {
            keyPairGenerator.initialize(builder.build());
            keyPairGenerator.generateKeyPair();
            Log.d("KeyManager", "Key generated using StrongBox");
        }
        catch (StrongBoxUnavailableException e) {

            Log.w("KeyManager", "StrongBox not available. Falling back to TEE.");

            builder.setIsStrongBoxBacked(false);
            keyPairGenerator.initialize(builder.build());
            keyPairGenerator.generateKeyPair();

        } catch (Exception e) {
            Log.d("KeyManager", Objects.requireNonNull(e.getMessage()));
        }
    }

    private boolean isKeyPresent() throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException {
        KeyStore keyStore = KeyStore.getInstance(context.getString(R.string.KEYSTORE_PROVIDER));
        keyStore.load(null);
        return (keyStore.containsAlias( context.getString(R.string.KEY_ALIAS)));
    }

    public boolean isKeyExpired() throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException {
        KeyStore keyStore = KeyStore.getInstance(context.getString(R.string.KEYSTORE_PROVIDER));
        keyStore.load(null);

        X509Certificate cert = (X509Certificate) keyStore.getCertificate(context.getString(R.string.KEY_ALIAS));

        Date notAfter = cert.getNotAfter();
        Date now = new Date();

        return now.after(notAfter);
    }

    public void revokeKey() throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException {
        KeyStore keyStore = KeyStore.getInstance(context.getString(R.string.KEYSTORE_PROVIDER));
        keyStore.load(null);
        if (keyStore.containsAlias( context.getString(R.string.KEY_ALIAS))) {
            keyStore.deleteEntry( context.getString(R.string.KEY_ALIAS));
            Log.d("KeyManager","Revoked the older key");
        }
    }

    private Date getValidityEndDate() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, context.getResources().getInteger(R.integer.KEY_VALIDITY_DURATION));
        return calendar.getTime();
    }

    public void generateKeyIfNeeded() throws NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException, CertificateException, KeyStoreException, IOException {

        if(!isKeyPresent())
        {
            generate();
        }
        else if(isKeyExpired())
        {
            revokeKey();
            generate();
        }
    }
}