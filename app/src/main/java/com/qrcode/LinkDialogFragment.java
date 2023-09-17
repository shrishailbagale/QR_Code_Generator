package com.qrcode;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.fragment.app.DialogFragment;

public class LinkDialogFragment extends DialogFragment {

    private String generatedLink;

    public LinkDialogFragment(String generatedLink) {
        this.generatedLink = generatedLink;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.link_dialog, container, false);

        TextView linkTextView = view.findViewById(R.id.link_text_view);
        Button copyButton = view.findViewById(R.id.copy_button);

        linkTextView.setText(generatedLink);

        copyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyToClipboard(generatedLink);
            }
        });

        return view;
    }

    private void copyToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Link", text);
        clipboard.setPrimaryClip(clip);
    }
}
