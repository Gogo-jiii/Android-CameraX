package com.example.camerax;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureConfig;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.lifecycle.LifecycleOwner;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Rational;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    Button btnClickPhoto, btnOpenCamera;
    TextureView textureView;

    private PermissionManager permissionManager;
    private String[] permissions = {Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
    private ImageCapture imgCap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnClickPhoto = findViewById(R.id.btnClickPhoto);
        btnOpenCamera = findViewById(R.id.btnOpenCamera);
        textureView = findViewById(R.id.textureView);

        permissionManager = PermissionManager.getInstance(this);

        btnOpenCamera.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (!permissionManager.checkPermissions(permissions)) {
                    permissionManager.askPermissions(MainActivity.this, permissions, 100);
                } else {
                    //permission granted
                    openCamera();
                }
            }
        });

        btnClickPhoto.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                clickPhoto();
            }
        });
    }

    @Override public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                                     @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            permissionManager.handlePermissionResult(MainActivity.this, 100, permissions,
                    grantResults);

            //permission granted
            openCamera();
        }
    }

    private void openCamera() {
        CameraX.unbindAll();

        Rational aspectRatio = new Rational(textureView.getWidth(), textureView.getHeight());
        Size screen = new Size(textureView.getWidth(), textureView.getHeight()); //size of the
        // screen

        PreviewConfig pConfig =
                new PreviewConfig.Builder().setTargetAspectRatio(aspectRatio).setTargetResolution(screen).build();
        Preview preview = new Preview(pConfig);

        preview.setOnPreviewOutputUpdateListener(
                new Preview.OnPreviewOutputUpdateListener() {
                    //to update the surface texture we  have to destroy it first then re-add it
                    @Override
                    public void onUpdated(Preview.PreviewOutput output) {
                        ViewGroup parent = (ViewGroup) textureView.getParent();
                        parent.removeView(textureView);
                        parent.addView(textureView, 0);

                        textureView.setSurfaceTexture(output.getSurfaceTexture());
                        updateTransform();
                    }
                });


        ImageCaptureConfig imageCaptureConfig =
                new ImageCaptureConfig.Builder().setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY)
                        .setTargetRotation(getWindowManager().getDefaultDisplay().getRotation()).build();
        imgCap = new ImageCapture(imageCaptureConfig);

        //bind to lifecycle:
        CameraX.bindToLifecycle((LifecycleOwner) this, preview, imgCap);
    }

    private void updateTransform() {
        Matrix mx = new Matrix();
        float w = textureView.getMeasuredWidth();
        float h = textureView.getMeasuredHeight();

        float cX = w / 2f;
        float cY = h / 2f;

        int rotationDgr;
        int rotation = (int) textureView.getRotation();

        switch (rotation) {
            case Surface.ROTATION_0:
                rotationDgr = 0;
                break;
            case Surface.ROTATION_90:
                rotationDgr = 90;
                break;
            case Surface.ROTATION_180:
                rotationDgr = 180;
                break;
            case Surface.ROTATION_270:
                rotationDgr = 270;
                break;
            default:
                return;
        }

        mx.postRotate((float) rotationDgr, cX, cY);
        textureView.setTransform(mx);
    }

    private void clickPhoto() {
        File file =
                new File(Environment.getExternalStorageDirectory() + "/" + System.currentTimeMillis() + ".png");
        imgCap.takePicture(file, new ImageCapture.OnImageSavedListener() {
            @Override
            public void onImageSaved(@NonNull File file) {
                String msg = "Pic captured at " + file.getAbsolutePath();
                Bitmap bitmap = BitmapFactory.decodeFile(file.getPath());
                Toast.makeText(getBaseContext(), msg, Toast.LENGTH_LONG).show();

                addImageToGallery(file.getPath(), MainActivity.this);
            }

            @Override
            public void onError(@NonNull ImageCapture.UseCaseError useCaseError,
                                @NonNull String message,
                                @Nullable Throwable cause) {
                String msg = "Pic capture failed : " + message;
                Toast.makeText(getBaseContext(), msg, Toast.LENGTH_LONG).show();
                if (cause != null) {
                    cause.printStackTrace();
                }
            }
        });
    }

    public static void addImageToGallery(final String filePath, final Context context) {

        ContentValues values = new ContentValues();

        values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.MediaColumns.DATA, filePath);

        context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
    }

}