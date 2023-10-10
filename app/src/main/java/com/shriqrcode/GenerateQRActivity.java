package com.shriqrcode;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.qrcode.R;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class GenerateQRActivity extends AppCompatActivity {

    private EditText textInput;
    private ImageView qrCodeImageView;
    private ProgressDialog progressDialog;
    private FirebaseStorage storage;
    private StorageReference storageRef;
    ImageView upload;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generate_qr);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Downloading QR Code...");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setCancelable(false);

        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReferenceFromUrl("gs://qr-code-generator-c69cd.appspot.com"); // Replace with your Firebase Storage bucket URL

        textInput = findViewById(R.id.textInput);
        upload = findViewById(R.id.uploadButton);
        qrCodeImageView = findViewById(R.id.qrCodeImageView);
        Button generateQRButton = findViewById(R.id.generateQRButton);
        ImageView downloadButton = findViewById(R.id.downloadButton);

        generateQRButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = textInput.getText().toString();
                if (!text.isEmpty()) {
                    generateQRCode(text);
                } else {
                    Toast.makeText(GenerateQRActivity.this, "Enter text to generate QR code.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (qrCodeImageView.getDrawable() != null) {
                    uploadQRCodeImageToFirebase();
                } else {
                    Toast.makeText(GenerateQRActivity.this, "QR Code image is not available.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        downloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (qrCodeImageView.getDrawable() != null) {
                    // Check if QR code image is available
                    checkAndRequestPermission();
                } else {
                    Toast.makeText(GenerateQRActivity.this, "QR Code image is not available.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private static final int PERMISSION_REQUEST_CODE = 1001;
    private static final int PERMISSION_REQUEST_MANAGE_EXTERNAL_STORAGE = 1002;

    private void checkAndRequestPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    saveQRCodeImage();
                } else {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    intent.addCategory("android.intent.category.DEFAULT");
                    intent.setData(Uri.parse(String.format("package:%s", getPackageName())));
                    startActivityForResult(intent, PERMISSION_REQUEST_MANAGE_EXTERNAL_STORAGE);
                }
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
            }
        } else {
            saveQRCodeImage();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                saveQRCodeImage();
            } else {
                Toast.makeText(this, "Permission denied. Cannot save QR code image.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PERMISSION_REQUEST_MANAGE_EXTERNAL_STORAGE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    saveQRCodeImage();
                } else {
                    Toast.makeText(this, "Permission denied. Cannot save QR code image.", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void generateQRCode(String text) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, 500, 500);
            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
                }
            }

            qrCodeImageView.setImageBitmap(bitmap);
        } catch (WriterException e) {
            e.printStackTrace();
        }
    }

    private void saveQRCodeImage() {
        qrCodeImageView.setDrawingCacheEnabled(true);
        final Bitmap qrCodeBitmap = qrCodeImageView.getDrawingCache();

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_hhmmss", Locale.getDefault());
        final String currentDateTime = dateFormat.format(new Date());

        final String folderName = "QR Code Images"; // Change the folder name as desired
        final String fileName = "QRCode_" + currentDateTime + ".png";
        final String filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator + folderName;

        progressDialog.show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean success = saveImage(qrCodeBitmap, filePath, fileName);

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                progressDialog.dismiss();
                final boolean finalSuccess = success;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (finalSuccess) {
                            Toast.makeText(GenerateQRActivity.this, "QR Code is Downloaded", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(GenerateQRActivity.this, "Failed to save QR code image.", Toast.LENGTH_SHORT).show();
                        }
                        qrCodeImageView.setDrawingCacheEnabled(false);
                    }
                });
            }
        }).start();
    }

    private boolean saveImage(Bitmap bitmap, String filePath, String fileName) {
        try {
            File folder = new File(filePath);
            if (!folder.exists()) {
                folder.mkdirs(); // Create the folder if it doesn't exist
            }

            File file = new File(folder, fileName);
            FileOutputStream outputStream = new FileOutputStream(file);

            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            outputStream.flush();
            outputStream.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void uploadQRCodeImageToFirebase() {
        // Generate a unique file name or use your own naming convention
        String fileName = "QRCode_" + System.currentTimeMillis() + ".png";

        // Create a reference to the Firebase Storage location
        StorageReference qrCodeImageRef = storageRef.child("qr_code_images/" + fileName);

        // Get the Bitmap from the ImageView
        Bitmap qrCodeBitmap = ((BitmapDrawable) qrCodeImageView.getDrawable()).getBitmap();

        // Create a byte array from the Bitmap (PNG format)
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        qrCodeBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] data = baos.toByteArray();

        // Upload the image to Firebase Storage
        UploadTask uploadTask = qrCodeImageRef.putBytes(data);

        // Listen for upload success/failure
        uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // Image upload success
                Toast.makeText(GenerateQRActivity.this, "QR Code image uploaded to Firebase Storage.", Toast.LENGTH_SHORT).show();

                // Generate and display the download link
                qrCodeImageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        String downloadUrl = uri.toString();
                        // You can now use `downloadUrl` to share or display the link
                        shareGeneratedLink(downloadUrl);
                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                // Image upload failed
                Log.e("UploadError", "Upload failed: " + e.getMessage());
                Toast.makeText(GenerateQRActivity.this, "Failed to upload QR code image.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void shareGeneratedLink(String downloadUrl) {
        // Create an Intent to send the download URL
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, downloadUrl);

        // Create a chooser to display app options for sharing
        Intent chooserIntent = Intent.createChooser(shareIntent, "Share Download Link");

        // Check if there are apps available to handle the share intent
        if (shareIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(chooserIntent);
        } else {
            Toast.makeText(this, "No app available to handle sharing.", Toast.LENGTH_SHORT).show();
        }
    }


}
