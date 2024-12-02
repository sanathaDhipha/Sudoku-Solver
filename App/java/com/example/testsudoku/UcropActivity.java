package com.example.testsudoku;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.util.UUID;

public class UcropActivity extends AppCompatActivity {

    private Uri sourceUri;
    private String destinationUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ucrop);

        /* Take Uri file */
        sourceUri = getIntent().getData();
        destinationUri = new StringBuilder(UUID.randomUUID().toString()).append(".jpg").toString();

        /* UCrop configuration  */
        UCrop.Options options = new UCrop.Options();
        UCrop.of(sourceUri, Uri.fromFile(new File(getCacheDir(), destinationUri)))
                .withAspectRatio(1, 1)
                .withMaxResultSize(2000, 2000)
                .withOptions(options)
                .start(UcropActivity.this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
            /* Send cropped image back */
            final Uri resultUri = UCrop.getOutput(data);
            Intent intent = new Intent();
            intent.putExtra("CROP", resultUri.toString());
            setResult(104, intent);
            finish();
        } else if (resultCode == UCrop.RESULT_ERROR) {
            /* Return back */
            final Throwable cropError = UCrop.getError(data);
            Log.e("Ucrop", "Crop error: " + cropError.getMessage());
        }else if (resultCode == RESULT_CANCELED) {
            setResult(RESULT_CANCELED);
            finish();
        }
    }
}