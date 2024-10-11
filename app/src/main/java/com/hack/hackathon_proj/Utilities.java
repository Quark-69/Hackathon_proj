package com.hack.hackathon_proj;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import org.apache.commons.io.FileUtils;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.bouncycastle.util.io.pem.PemWriter;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class Utilities {


    private final Context context;

    private final int id;

    private String timestamp;

    private final byte[] aes_key;
    private final byte[] iv;


    private final String severUrl;

    public Utilities(Context context, int id){
        this.context = context;
        this.id = id;
        severUrl = "https://kx6j3ih30gna.share.zrok.io";
        aes_key = GenerateSecurePattern(32);
        iv = GenerateSecurePattern(16);
    }



    public void downloadPublicKey() {
        String url = severUrl + "/publickey";
        File file = new File(context.getFilesDir().getAbsolutePath() + "/" + "public_key.pem");

        OkHttpClient client = new OkHttpClient.Builder()
                .build();
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

    public void beginEncryption(String timestamp) throws Exception {
        this.timestamp = timestamp;
        EncryptAESkey();
        EncryptImage();
        createLog();
    }

    private void EncryptWithAES(String filename) throws Exception {

        String[] parts = filename.split("\\.");

        SecretKeySpec secretKeySpec = new SecretKeySpec(aes_key, "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivSpec);

        File file = new File(context.getFilesDir().getAbsolutePath() + "/" + id + "/" + filename);
        byte[] fileBytes = Files.readAllBytes(file.toPath());

        byte[] encryptedFile = cipher.doFinal(fileBytes);

        String encryptedFilePath = context.getFilesDir().getAbsolutePath() + "/" + id + "/" + "encrypted_" + parts[0] + "." + parts[1];
        FileOutputStream fileOutputStream = new FileOutputStream(encryptedFilePath);
        fileOutputStream.write(iv);
        fileOutputStream.write(encryptedFile);
        fileOutputStream.close();

        if(!file.delete())
        {
            throw new IOException("Failed to delete file: " + file.getAbsolutePath());
        }
    }

    private void EncryptImage() throws Exception {
        EncryptWithAES("face.jpg");
    }

    private void EncryptAESkey() throws Exception {
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


    private void savePubKey(String filePath) throws Exception
    {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        KeyStore.Entry entry = keyStore.getEntry(context.getString(R.string.KEY_ALIAS), null);

        assert entry != null;
        PublicKey publicKey = ((KeyStore.PrivateKeyEntry) entry).getCertificate().getPublicKey();

        try (FileWriter fileWriter = new FileWriter(filePath);
             PemWriter pemWriter = new PemWriter(fileWriter)) {
            pemWriter.writeObject(new PemObject("PUBLIC KEY", publicKey.getEncoded()));
        } catch (IOException e) {
            throw new RuntimeException("Failed to write public key to file", e);
        }

    }

    public void pack(String sourceDirPath, String zipFilePath) throws Exception {

        savePubKey(context.getFilesDir().getAbsolutePath() + "/" + id + "/user_public_key.pem");


        Path zipPath = Paths.get(zipFilePath);

        if (Files.exists(zipPath)) {
            Files.delete(zipPath);
        }

        Path p = Files.createFile(zipPath);
        try (ZipOutputStream zs = new ZipOutputStream(Files.newOutputStream(p))) {
            Path pp = Paths.get(sourceDirPath);
            Files.walk(pp)
                    .filter(path -> !Files.isDirectory(path))
                    .forEach(path -> {
                        ZipEntry zipEntry = new ZipEntry(pp.relativize(path).toString());
                        try {
                            zs.putNextEntry(zipEntry);
                            Files.copy(path, zs);
                            zs.closeEntry();
                        } catch (IOException e) {
                            Log.d("Utilities", "Failed to pack.");
                        }
                    });
        }
    }

    @SuppressLint("HardwareIds")
    public void createLog() throws Exception {
        JSONObject log = new JSONObject();
        log.put("device_type", "Android");
        log.put("os_version", Build.VERSION.RELEASE);
        log.put("device_model", Build.MODEL);
        log.put("Android_ID",Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID));
        log.put("timestamp", timestamp);

        try {
            File file = new File(context.getFilesDir().getAbsoluteFile() + "/" + id  + "/", "log.json");
            FileOutputStream writer = new FileOutputStream(file);
            writer.write(log.toString().getBytes());
            writer.close();
        } catch (Exception e) {
            Log.e("Error", "Saving JSON to file: " + e.getMessage());
        }

        EncryptWithAES("log.json");
    }

    private void sign(String filePath)
    {
        try {
            KeyStore keyStore = KeyStore.getInstance(context.getString(R.string.KEYSTORE_PROVIDER));
            keyStore.load(null);
            PrivateKey privateKey = (PrivateKey) keyStore.getKey(context.getString(R.string.KEY_ALIAS), null);

            File file = new File(filePath);
            byte[] fileContent = Files.readAllBytes(file.toPath());

            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);
            signature.update(fileContent);
            byte[] signedHash = signature.sign();

            FileOutputStream fos = new FileOutputStream(file.getAbsolutePath() + ".sig");
            fos.write(signedHash);
            fos.close();

           Log.d("Utilities","Signed " + file.getName() + " successfully.");

        } catch (Exception e) {
           Log.d("Utilities", "Failed to sign file.");
        }
    }

    public void signData(String folderPath) {
        File folder = new File(folderPath);
        if (folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles((dir, name) -> new File(dir, name).isFile());

            if (files != null) {
                for (File file : files) {
                    sign(file.getAbsolutePath());
                }
            } else {
                Log.d("Utilities", "No files found in the directory.");
            }
        } else {
            Log.d("Utilities","Invalid directory.");
        }
    }

    public void cleanDir(File dir) throws IOException {
        FileUtils.cleanDirectory(dir);
    }

    public boolean requestAuth()
    {
        File zipFile = new File(context.getFilesDir().getAbsolutePath() + "/" + id + ".zip");
        RequestBody zipRequestBody = RequestBody.create(zipFile, MediaType.get("application/zip"));

        MultipartBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", zipFile.getName(), zipRequestBody)
                .build();

        Request request = new Request.Builder()
                .url(severUrl + "/authorize" + "/" +id)
                .post(requestBody)
                .build();

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                assert response.body() != null;
                JSONObject jsonObject = new JSONObject(response.body().string());
                String status = jsonObject.getString("status");
                return status.equals("authenticated");
            } else {
                Log.d("Utilities", Integer.toString(response.code()));
            }
        } catch(Exception e)
        {
            e.printStackTrace();
        }

        return false;
    }
}
