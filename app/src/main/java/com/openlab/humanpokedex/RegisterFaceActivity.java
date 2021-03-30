package com.openlab.humanpokedex;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.extensions.HdrImageCaptureExtender;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.loader.content.AsyncTaskLoader;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class RegisterFaceActivity extends AppCompatActivity {

    private int REQUEST_CODE_PERMISSIONS = 1001;
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA", "android.permission.WRITE_EXTERNAL_STORAGE"};
    private Executor executor = Executors.newSingleThreadExecutor();
    private PreviewView registerPreview;
    private MaterialButton doneButton;
    private TextView instructionsText;
    private int flag = 0, photoCount = 0, capturePic = 0;
    private StorageReference storageReference;
    private ImageCapture imageCapture;
    private TextInputEditText registerFaceName;
    private String name;
    private static Uri capturedImageUri;
    private ArrayList<Uri> imageURIs;
    private int count = 0;
    private ProgressBar registerProgress;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register_face);

        registerPreview = findViewById(R.id.registerFacePreview);
        doneButton = findViewById(R.id.registerFaceDoneBtn);
        instructionsText = findViewById(R.id.registerInstructions);
        registerFaceName = findViewById(R.id.registerFaceNameET);
        registerProgress = findViewById(R.id.registerProgress);

        storageReference = FirebaseStorage.getInstance().getReference();
        name = registerFaceName.getText().toString();
        imageURIs = new ArrayList<>();

        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ++flag;
                changeCapture();
            }
        });
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(RegisterFaceActivity.this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Permission not granted to start camera.", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(RegisterFaceActivity.this, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        }
    }

    private void startCamera() {
        final ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    bindPreview(cameraProvider);

                } catch (ExecutionException | InterruptedException e) {
                    // No errors need to be handled for this Future.
                    // This should never be reached.
                }
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder()
                .build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build();

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .build();

        ImageCapture.Builder builder = new ImageCapture.Builder();

        //Vendor-Extensions (The CameraX extensions dependency in build.gradle)
        HdrImageCaptureExtender hdrImageCaptureExtender = HdrImageCaptureExtender.create(builder);

        // Query if extension is available (optional).
        if (hdrImageCaptureExtender.isExtensionAvailable(cameraSelector)) {
            // Enable the extension if available.
            hdrImageCaptureExtender.enableExtension(cameraSelector);
        }

        imageCapture = builder
                .setTargetRotation(this.getWindowManager().getDefaultDisplay().getRotation())
                .build();
        preview.setSurfaceProvider(registerPreview.);
        Camera camera = cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, preview, imageAnalysis, imageCapture);

    }

    private void changeCapture() {
        switch (flag) {
            case 0:
                doneButton.setText("Next");
                doneButton.setVisibility(View.GONE);
                String textLine_11 = "Hitting the \"Next\" button will capture your photos periodically. Try not to switch between ";
                String textLine_12 = "front and back cameras during the process. Follow the instructions.\n";
                String text1 = textLine_11 + textLine_12;
                instructionsText.setText(text1);
            case 1:
                doneButton.setText("Done, next step");
                doneButton.setVisibility(View.GONE);
                String textLine_21 = "Turn your head slowly from side to side, with a neutral face.\n";
                String textLine_22 = "Take 10 seconds to move from one side to the other.\n";
                String text2 = textLine_21 + textLine_22;
                instructionsText.setText(text2);
                capturePic = 1;
                captureImagesPeriodically();
            case 2:
                doneButton.setText("Done, next step");
                doneButton.setVisibility(View.GONE);
                String textLine_31 = "Turn your head slowly from side to side, with a smile.\n";
                String textLine_32 = "Take 10 seconds to move from one side to the other.\n";
                String text3 = textLine_31 + textLine_32;
                instructionsText.setText(text3);
                capturePic = 1;
                captureImagesPeriodically();
            case 3:
                doneButton.setText("Done, finish registration");
                doneButton.setVisibility(View.GONE);
                String textLine_41 = "Hold your phone in front of your face and stretch your hand.\n";
                String textLine_42 = "Move the camera towards and away from your face slowly.\n";
                String text4 = textLine_41 + textLine_42;
                instructionsText.setText(text4);
                capturePic = 1;
                captureImagesPeriodically();
            case 4:
                Intent intent = new Intent(RegisterFaceActivity.this, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                Toast.makeText(this, "Thanks! Your face has been registered.", Toast.LENGTH_SHORT).show();
        }
    }

    private void captureImagesPeriodically() {
        while (capturePic == 1) {
            SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
            File file = new File(Environment.getDataDirectory(), mDateFormat.format(new Date()) + ".jpg");

            ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(file).build();
            imageCapture.takePicture(outputFileOptions, executor, new ImageCapture.OnImageSavedCallback() {
                @Override
                public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                    new Handler().post(new Runnable() {
                        @Override
                        public void run() {
                            // Changes here
                            Toast.makeText(RegisterFaceActivity.this, "Image Captured: " + photoCount, Toast.LENGTH_SHORT).show();
                            photoCount++;
                            capturedImageUri = Uri.fromFile(file);
                            imageURIs.add(capturedImageUri);
                        }
                    });
                }

                @Override
                public void onError(@NonNull ImageCaptureException error) {
                    Toast.makeText(RegisterFaceActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

            try {
                Thread.sleep(2 * 1000);
            } catch (InterruptedException e) {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }

            if (photoCount == 10) {
                RegisterAsyncTask asyncTask = new RegisterAsyncTask();
                asyncTask.execute(imageURIs);

            }
        }
    }

    private void uploadImageArray() {
        capturePic = 0;
        instructionsText.setText("Done! You can proceed to the next step.\n");
        photoCount = 0;
    }

    // AsyncTask Inner Class
    private class RegisterAsyncTask extends AsyncTask<ArrayList<Uri>, Integer, Integer> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            registerProgress.setVisibility(View.VISIBLE);
            instructionsText.setText("Please wait, uploading your photos...");
        }

        @Override
        protected void onPostExecute(Integer integer) {
            super.onPostExecute(integer);
        }

        @Override
        protected Integer doInBackground(ArrayList<Uri>... Uris) {

            StorageReference registerFacesRef = storageReference.child("New Datasets/" + name);
            ArrayList<Uri> passedArray = new ArrayList<>();
            passedArray = Uris[0];
            for (int i = 0; i < Uris.length; i++) {
                capturedImageUri = passedArray.get(i);
                UploadTask uploadTask = registerFacesRef.putFile(capturedImageUri);
                uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        // finish this
                        count++;
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(RegisterFaceActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(RegisterFaceActivity.this, LoginActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    }
                });
            }

            return null;
        }
    }
}