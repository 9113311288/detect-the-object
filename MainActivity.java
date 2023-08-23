package com.example.dtodetecttheobject;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.dtodetecttheobject.ml.LiteModelEfficientdetLite0DetectionMetadata1;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;
import org.tensorflow.lite.support.image.TensorImage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    Button select, capture, predict, wikiButton, speakerButton;
    TextView result;
    Bitmap bitmap;
    ImageView imageView;
    TextToSpeech textToSpeech;
    WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getPermission();

        select = findViewById(R.id.select);
        capture = findViewById(R.id.capture);
        predict = findViewById(R.id.predict);
        imageView = findViewById(R.id.imageview);
        result = findViewById(R.id.result);
        wikiButton = findViewById(R.id.wiki_button);
        speakerButton = findViewById(R.id.speaker_button);
        webView = findViewById(R.id.webView);

        select.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(intent, 10);
            }
        });

        capture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(intent, 12);
            }
        });

        predict.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (bitmap == null) {
                    Toast.makeText(MainActivity.this, "Image not selected", Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    LiteModelEfficientdetLite0DetectionMetadata1 model = LiteModelEfficientdetLite0DetectionMetadata1.newInstance(MainActivity.this);

                    // Creates inputs for reference.
                    TensorImage image = TensorImage.fromBitmap(bitmap);

                    // Runs model inference and gets result.
                    LiteModelEfficientdetLite0DetectionMetadata1.Outputs outputs = model.process(image);
                    LiteModelEfficientdetLite0DetectionMetadata1.DetectionResult detectionResult = outputs.getDetectionResultList().get(0);

                    // Gets result from DetectionResult.
                    RectF location = detectionResult.getLocationAsRectF();
                    String category = detectionResult.getCategoryAsString();
                    float score = detectionResult.getScoreAsFloat();
                    result.setText(category);
                    // Releases model resources if no longer used.
                    model.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        wikiButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String category = result.getText().toString();
                if (bitmap == null) {
                    Toast.makeText(MainActivity.this, "No category predicted", Toast.LENGTH_SHORT).show();
                    return;
                }
                String wikipediaUrl = "https://www.google.com/search?q=" + category + " meaning";

                webView.setVisibility(View.VISIBLE);
                webView.setWebViewClient(new WebViewClient());
                webView.loadUrl(wikipediaUrl);
            }
        });

        speakerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (bitmap == null) {
                    Toast.makeText(MainActivity.this, "No image selected", Toast.LENGTH_SHORT).show();
                    return;
                }
                String category = result.getText().toString();
                speakText(category);
            }
        });

        textToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    textToSpeech.setLanguage(Locale.US);
                } else {
                    Toast.makeText(MainActivity.this, "TextToSpeech initialization failed", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == 10) {
            if (resultCode == RESULT_OK && data != null) {
                Uri uri = data.getData();
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
                    imageView.setImageBitmap(bitmap);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "Image selection canceled", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to select image", Toast.LENGTH_SHORT).show();
            }
        }
        if (requestCode == 12) {
            if (resultCode == RESULT_OK && data != null) {
                bitmap = (Bitmap) data.getExtras().get("data");
                imageView.setImageBitmap(bitmap);
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "Image capture canceled", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to capture image", Toast.LENGTH_SHORT).show();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    void getPermission() {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, 11);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 11) {
            if (grantResults.length > 0) {
                if (grantResults[1] != PackageManager.PERMISSION_GRANTED) {
                    this.getPermission();
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    void speakText(String text) {

        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (webView.getVisibility() == View.VISIBLE) {
            webView.setVisibility(View.GONE);
        } else {
            super.onBackPressed();
        }
    }
}