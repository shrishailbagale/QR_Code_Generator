package com.qrcode;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class UploadDialogFragment extends DialogFragment {

    private ProgressBar progressBar;

    public UploadDialogFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.upload_dialog, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        progressBar = view.findViewById(R.id.progress_bar);

        Button uploadButton = view.findViewById(R.id.upload_button);
        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadAndGenerateLink();
            }
        });
    }

    @SuppressLint("StaticFieldLeak")
    private void uploadAndGenerateLink() {
        // Show the progress bar
        progressBar.setVisibility(View.VISIBLE);

        // Simulate an upload process with AsyncTask (replace with actual upload logic)
        new AsyncTask<Void, Integer, String>() {
            @SuppressLint("StaticFieldLeak")
            @Override
            protected String doInBackground(Void... params) {
                // Simulate an upload process (you should replace this with your actual upload logic)
                for (int i = 0; i <= 100; i += 10) {
                    publishProgress(i);
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                // After the upload, return a generated link (replace with your actual link generation logic)
                return "https://example.com/generated-link";
            }

            @Override
            protected void onPostExecute(String generatedLink) {
                // Hide the progress bar
                progressBar.setVisibility(View.GONE);

                // Display the generated link in the popup or handle it as needed
                if (generatedLink != null) {
                    // You can show the link in a TextView, open it in a browser, or perform other actions here
                } else {
                    // Handle the case where link generation failed
                }
            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                // Update the progress bar during the upload
                progressBar.setProgress(values[0]);
            }
        }.execute();
    }
}
