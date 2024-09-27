package com.hack.hackathon_proj;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import org.bouncycastle.util.io.pem.PemReader;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Comparator;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

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

    byte[] aes_key;
    byte[] iv;

    public Utilities(Context context, int id) throws Exception {
        this.context = context;
        this.id = id;
        serverIp = "192.168.122.1";
        port = 8010;

        aes_key = GenerateSecurePattern(32);
        iv = GenerateSecurePattern(16);

        EncryptAESkey(aes_key);
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

    public void pack(String sourceDirPath, String zipFilePath) throws IOException {
        Path p = Files.createFile(Paths.get(zipFilePath));
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

    public void createLog() throws Exception {
        JSONObject log = new JSONObject();
        log.put("os_type", "Android");
        log.put("os_version", Build.VERSION.RELEASE);
        log.put("device_model", Build.MODEL);
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

            // Save the signed hash to a file with the same name and a .sig extension
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
            // Get all files in the folder
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

    public void deleteDir(File dir) throws IOException {
        try (Stream<Path> pathStream = Files.walk(dir.toPath())) {
            pathStream.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }
}
