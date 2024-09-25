package com.hack.hackathon_proj;

import android.content.Context;
import android.util.Log;

import org.bouncycastle.util.io.pem.PemReader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


public class Utilities {


    private final Context context;

    private final int id;

    private String timestamp;

    private final String serverIp;
    private final int port;

    public Utilities(Context context, int id)
    {
        this.context = context;
        this.id = id;
        serverIp = "192.168.122.1";
        port = 8010;
    }



    public void downloadPublicKey() {
        String url = "http://" + serverIp + ":" + port + "/" + "publickey";
        File file = new File(context.getFilesDir().getAbsolutePath() + "/" + "public_key.pem");

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }

            assert response.body() != null;
            Files.copy(response.body().byteStream(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);

            Log.d("Download", "Public key downloaded successfully");
        } catch (IOException e) {
            Log.e("Download", "Error downloading public key", e);
        }
    }


    private byte[] GenerateSecurePattern(int size)
    {
        SecureRandom secureRandom = new SecureRandom();
        byte[] pattern = new byte[size];
        secureRandom.nextBytes(pattern);

        return  pattern;
    }

    public void beginImageEncryption(String timestamp) throws Exception {
        this.timestamp = timestamp;
        EncryptImage();
    }

    private void EncryptImage() throws Exception {

        byte[] aes_key = GenerateSecurePattern(32);
        byte[] iv = GenerateSecurePattern(16);

        SecretKeySpec secretKeySpec = new SecretKeySpec(aes_key, "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivSpec);

        File imageFile = new File(context.getFilesDir().getAbsolutePath() + "/" + id + "/" + "face.jpg");
        byte[] imageBytes = Files.readAllBytes(imageFile.toPath());

        byte[] encryptedImage = cipher.doFinal(imageBytes);

        String encryptedImagePath = context.getFilesDir().getAbsolutePath() + "/" + id + "/" + "encrypted_face.enc";
        FileOutputStream fileOutputStream = new FileOutputStream(encryptedImagePath);
        fileOutputStream.write(iv);
        fileOutputStream.write(encryptedImage);
        fileOutputStream.close();

        if(!imageFile.delete())
        {
            throw new IOException("Failed to delete file: " + imageFile.getAbsolutePath());
        }
        EncryptAESkey(aes_key);
    }

    private void EncryptAESkey(byte[] aes_key) throws Exception {
        PemReader pemReader = new PemReader(new FileReader( context.getFilesDir().getAbsolutePath() + "/" + "public_key.pem"));
        byte[] publicKeyBytes = pemReader.readPemObject().getContent();
        pemReader.close();

        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encryptedKey = cipher.doFinal(aes_key);

        String encryptedKeyFileName = context.getFilesDir().getAbsolutePath() + "/" + id + "/" + "encrypted_aes_key.bin";
        FileOutputStream fileOutputStream = new FileOutputStream(encryptedKeyFileName);
        fileOutputStream.write(encryptedKey);
        fileOutputStream.close();
    }

}
