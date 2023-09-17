package com.qrcode;

import android.app.SearchManager;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.net.URL;

public class ScannerActivity extends AppCompatActivity {

    private TextView qrCodeContentTextView;
    private DatabaseReference textRef;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);


        qrCodeContentTextView = findViewById(R.id.qr_code_content);

        ImageView copyTextButton = findViewById(R.id.copy_text_button);
        copyTextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyQRCodeContentToClipboard();
            }
        });

        ImageView scanAgainButton = findViewById(R.id.scan_again_button);
        scanAgainButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startQRCodeScan();
            }
        });
        ImageView uploadButton = findViewById(R.id.upload_button);
        // Initialize Firebase Database reference
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        textRef = database.getReference("text_data");

        // Set an onClickListener for the upload button
        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadTextToFirebase();
            }
        });

        ImageView shareButton = findViewById(R.id.share_button);
        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareScannedText();
            }
        });

        // Initialize the scanner
        startQRCodeScan();

        ImageView downloadButton = findViewById(R.id.download_button);
        // Set an OnClickListener for the download button
        downloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //handleDownloadButtonClick();
            }
        });

        ImageView openInBrowserButton = findViewById(R.id.open_in_browser_button);
        openInBrowserButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String qrCodeContent = qrCodeContentTextView.getText().toString();
                if (isValidUrl(qrCodeContent)) {
                    // If it's a valid URL, open it in a browser
                    openUrlInBrowser(qrCodeContent);
                } else {
                    // If it's not a URL, perform a web search
                    performWebSearch(qrCodeContent);
                }
            }
        });
    }
    private void uploadTextToFirebase() {
        // Your text data to be uploaded
        String scannedText = qrCodeContentTextView.getText().toString();

        // Check if the text is not empty
        if (!TextUtils.isEmpty(scannedText)) {
            // Create a unique key for the data
            String dataKey = textRef.push().getKey();

            if (dataKey != null) {
                // Upload the text to Firebase Database
                textRef.child(dataKey).setValue(scannedText)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                // Text successfully uploaded
                                final String dataLink = "https://qr-code-generator-c69cd-default-rtdb.firebaseio.com/text_data/" + dataKey + ".json";
                                showLinkDialog(dataLink);
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // Handle the error
                                Toast.makeText(ScannerActivity.this, "Error uploading text to Database: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
            } else {
                Toast.makeText(ScannerActivity.this, "Error generating data key.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(ScannerActivity.this, "Text is empty. Nothing to upload.", Toast.LENGTH_SHORT).show();
        }
    }


    private void showLinkDialog(String generatedLink) {
        LinkDialogFragment dialogFragment = new LinkDialogFragment(generatedLink);
        dialogFragment.show(getSupportFragmentManager(), "LinkDialogFragment");
    }

    private void startQRCodeScan() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
        integrator.setPrompt("Scan a QR Code");
        integrator.setCameraId(0);
        integrator.setBeepEnabled(true);
        integrator.setOrientationLocked(true); // Lock the orientation to portrait
        // Additional customization options (optional)
        integrator.setBarcodeImageEnabled(true); // Save a copy of the captured barcode image

        // Initiate the QR code scan
        integrator.initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);

        if (result != null) {
            if (result.getContents() == null) {
                // Handle the case where scanning was canceled
            } else {
                // Display the QR code content in the TextView
                String qrCodeContent = result.getContents();
                qrCodeContentTextView.setText(qrCodeContent);

                // Enable auto-linking for web URLs in the TextView
                Linkify.addLinks(qrCodeContentTextView, Linkify.WEB_URLS);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void copyQRCodeContentToClipboard() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            CharSequence text = qrCodeContentTextView.getText();
            clipboard.setText(text);
            // Display a toast or a message to indicate that the text has been copied.
            // For example: Toast.makeText(this, "Text copied to clipboard", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendToWhatsApp() {
        String qrCodeContent = qrCodeContentTextView.getText().toString();

        // Create an Intent to open WhatsApp
        Intent whatsappIntent = new Intent(Intent.ACTION_SEND);
        whatsappIntent.setType("text/plain");
        whatsappIntent.setPackage("com.whatsapp"); // Package name of WhatsApp

        // Add the scanned text to the Intent
        whatsappIntent.putExtra(Intent.EXTRA_TEXT, qrCodeContent);

        try {
            startActivity(whatsappIntent);
        } catch (android.content.ActivityNotFoundException ex) {
            // Handle the case where WhatsApp is not installed
        }
    }

    private void openUrlInBrowser(String url) {
        Uri uri = Uri.parse(url);
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(browserIntent);
    }

    private void performWebSearch(String query) {
        Intent webSearchIntent = new Intent(Intent.ACTION_WEB_SEARCH);
        webSearchIntent.putExtra(SearchManager.QUERY, query);
        startActivity(webSearchIntent);
    }

    // Helper method to check if a string is a valid URL
    private boolean isValidUrl(String text) {
        try {
            new URL(text).toURI();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    private void shareScannedText() {
        String qrCodeContent = qrCodeContentTextView.getText().toString();

        // Create an intent for sharing text
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, qrCodeContent);

        // Display the Android system's share sheet
        startActivity(Intent.createChooser(shareIntent, "Share using"));

        // You can also customize the sharing options and specify the apps you want to include.
    }
}
