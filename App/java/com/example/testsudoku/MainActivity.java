    package com.example.testsudoku;

    import android.Manifest;
    import android.app.Dialog;
    import android.content.ContentValues;
    import android.content.Intent;
    import android.content.pm.PackageManager;
    import android.graphics.Bitmap;
    import android.graphics.BitmapFactory;
    import android.graphics.Matrix;
    import android.net.Uri;
    import android.os.Build;
    import android.os.Bundle;
    import android.os.Environment;
    import android.provider.MediaStore;
    import android.util.Log;
    import android.view.ViewGroup;
    import android.widget.Button;
    import android.widget.ImageButton;
    import android.widget.ImageView;
    import android.widget.Toast;

    import androidx.annotation.NonNull;
    import androidx.annotation.Nullable;
    import androidx.appcompat.app.AppCompatActivity;
    import androidx.camera.core.AspectRatio;
    import androidx.camera.core.Camera;
    import androidx.camera.core.CameraSelector;
    import androidx.camera.core.ImageCapture;
    import androidx.camera.core.ImageCaptureException;
    import androidx.camera.core.ImageProxy;
    import androidx.camera.core.Preview;
    import androidx.camera.core.TorchState;
    import androidx.camera.lifecycle.ProcessCameraProvider;
    import androidx.core.content.ContextCompat;

    import com.example.testsudoku.databinding.ActivityMainBinding;
    import com.google.common.util.concurrent.ListenableFuture;

    import org.opencv.android.OpenCVLoader;

    import java.io.ByteArrayOutputStream;
    import java.io.File;
    import java.io.FileNotFoundException;
    import java.io.FileOutputStream;
    import java.io.IOException;
    import java.io.OutputStream;
    import java.nio.ByteBuffer;
    import java.util.UUID;
    import java.util.concurrent.ExecutionException;

    public class MainActivity extends AppCompatActivity {
        private static final int CAMERA_PERMISSION_CODE = 100;
        private static final int SELECT_LOCAL_IMAGE_CODE = 101;
        private ActivityMainBinding mainBinding;
        private ImageCapture imageCapture;
        ProcessCameraProvider cameraProvider;
        private Bitmap bitmap;
        private Uri imageUri;
        private Dialog dialog;
        private Camera camera;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mainBinding = ActivityMainBinding.inflate(getLayoutInflater());
            setContentView(mainBinding.getRoot());

            /* Check and request camera permission */
            checkAndRequestPermission();

            /* Check if OpenCV load correctly */
            if (OpenCVLoader.initLocal()) Log.d("LOADED", "Success Loading OPENCV");
            else Log.d("LOADED", "Error Loading OPENCV");

            ListenableFuture<ProcessCameraProvider> cameraProviderListenableFuture = ProcessCameraProvider.getInstance(this);
            cameraProviderListenableFuture.addListener(() -> {
                try {
                    cameraProvider = cameraProviderListenableFuture.get();
                    startCameraX(cameraProvider);
                } catch (ExecutionException | InterruptedException e) {
                    throw new RuntimeException(e);
                }

            }, ContextCompat.getMainExecutor(this));
            captureFromCamera();
            selectFromLocal();
            buttonFlash();
        }

        private void startCameraX(ProcessCameraProvider cameraProvider){
            /* Start camera and camera preview */
            CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
            Preview preview = new Preview.Builder()
                    .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                    .build();
            imageCapture = new ImageCapture.Builder()
                    .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build();
            preview.setSurfaceProvider(mainBinding.previewView.getSurfaceProvider());
            try {
                cameraProvider.unbindAll();

                camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void buttonFlash() {
            /* Flash button */
            mainBinding.buttonFlash.setOnClickListener(view -> {
                if (camera != null && camera.getCameraInfo().hasFlashUnit()) {
                    int currentTorchState = camera.getCameraInfo().getTorchState().getValue();
                    if (currentTorchState == 0) {
                        camera.getCameraControl().enableTorch(true);
                        mainBinding.buttonFlash.setImageResource(R.drawable.button_flash_on);
                    } else {
                        camera.getCameraControl().enableTorch(false);
                        mainBinding.buttonFlash.setImageResource(R.drawable.button_flash_off);
                    }
                } else {
                    Toast.makeText(this, "Flash not supported on this device", Toast.LENGTH_SHORT).show();
                }
            });
        }

        private void captureFromCamera() {
            /* Capture button */
            mainBinding.buttonCapture.setOnClickListener(view -> {
                if (imageCapture == null) return;

                imageCapture.takePicture(ContextCompat.getMainExecutor(this), new ImageCapture.OnImageCapturedCallback() {
                    @Override
                    public void onCaptureSuccess(@NonNull ImageProxy image) {
                        bitmap = imageToBitmap(image);
                        image.close();
                        File tempFile = saveBitmapToTempFile(bitmap);
                        imageUri = Uri.fromFile(tempFile);
                        showImage();
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Toast.makeText(MainActivity.this, "Capture Unsuccessful: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.d("Camera Failure", exception.getMessage());
                    }
                });
            });
        }

        private void selectFromLocal() {
            /* Select picture from local storage */
            mainBinding.buttonSelect.setOnClickListener(view -> {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT).setType("image/*");
                startActivityForResult(intent, SELECT_LOCAL_IMAGE_CODE);
            });
        }

        private File saveBitmapToTempFile(Bitmap bitmap) {
            /* Temp file to temporarily store bitmap value */
            try {
                File tempFile = new File(getCacheDir(), UUID.randomUUID().toString() + ".jpg");
                FileOutputStream out = new FileOutputStream(tempFile);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                out.flush();
                out.close();
                return tempFile;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        private Bitmap imageToBitmap(ImageProxy image) {
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            Bitmap rawBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            int rotationDegrees = image.getImageInfo().getRotationDegrees();
            if (rotationDegrees == 0) {
                return rawBitmap;
            }
            Matrix matrix = new Matrix();
            matrix.postRotate(rotationDegrees);
            return Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.getWidth(), rawBitmap.getHeight(), matrix, true);
        }

        private void toCropImage(){
            Intent intent = new Intent(MainActivity.this, UcropActivity.class);
            intent.setData(imageUri);
            startActivityForResult(intent, 103);
        }

        private void showImage(){
            /* Show captured or selected image */
            dialog = new Dialog(MainActivity.this);
            dialog.setContentView(R.layout.show_image_dialog);
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawable(getDrawable(R.drawable.solved_sudoku_dialog_bg));
            dialog.setCancelable(false);

            ImageButton cropButton = dialog.findViewById(R.id.buttonCrop);
            cropButton.setOnClickListener(View -> toCropImage());

            ImageView imageView = dialog.findViewById(R.id.imageView);
            imageView.setImageBitmap(bitmap);

            Button buttonCancelDialog = dialog.findViewById(R.id.cancelDialog);
            buttonCancelDialog.setOnClickListener(View -> dialog.dismiss());

            Button buttonDoneDialog = dialog.findViewById(R.id.doneDialog);
            buttonDoneDialog.setOnClickListener(View -> {
                PreprocessImage preprocessImage = new PreprocessImage();
                if (preprocessImage.isGrid(bitmap)){
                    showSolvedSudokuDialog();
                }else{
                    noSudokuDialog();
                }
            });
            dialog.show();
        }

        private void noSudokuDialog(){
            /* Pop up message when there's no sudoku grid found in the image */
            dialog.dismiss();
            Dialog noSudoku = new Dialog(MainActivity.this);
            noSudoku.setContentView(R.layout.layout_solved_sudoku_dialog_no_grid);
            noSudoku.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            noSudoku.getWindow().setBackgroundDrawable(getDrawable(R.drawable.solved_sudoku_dialog_bg));
            noSudoku.setCancelable(false);

            ImageView imageView = noSudoku.findViewById(R.id.imageView);
            imageView.setImageBitmap(bitmap);

            Button buttonCancelDialog = noSudoku.findViewById(R.id.noGridBack);
            buttonCancelDialog.setOnClickListener(View -> noSudoku.dismiss());
            noSudoku.show();
        }

        private void showSolvedSudokuDialog() {
        /* Process the image extract the number in the grid to matrix and if the sudoku puzzle
        which is represented by matrix is solvable then showResult() */
            Sudoku sudoku;
            ExtractNumber extract = new ExtractNumber(this);
            Solver solver = new Solver();

            if (bitmap == null) {
                Log.e("ERROR", "Bitmap is null");
                return;
            }

            sudoku = new Sudoku(bitmap);
            sudoku.setSudokuMatrix(extract.extractNumber(bitmap));

            if (solver.isSolvable(sudoku.getSudokuMatrix()) == false){
                noSudokuDialog();
            }else{
                sudoku.setSolvedSudokuMatrix(solver.solveSudoku(sudoku.getSudokuMatrix()));
                showResult(sudoku.getSolvedSudokuMatrix());
            }
        }

        private void showResult(int[][] matrix) {
            /* Flatten 2D matrix to 1D and pass it through intent to SudokuBoard activity  */
            int[] flatMatrix = new int[81];
            int index = 0;
            for (int i = 0; i < matrix.length; i++) {
                for (int j = 0; j < matrix[i].length; j++) {
                    flatMatrix[index++] = matrix[i][j];
                }
            }
            Intent intent = new Intent(MainActivity.this, SudokuBoard.class);
            intent.putExtra("SUDOKU_MATRIX", flatMatrix);
            startActivity(intent);
        }

        @Override
        protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            if (requestCode == 103 && resultCode == 104) {
                /* For camera */
                dialog.dismiss();
                String resultUriStr = data.getStringExtra("CROP");
                Uri resultUri = Uri.parse(resultUriStr);
                try {
                    bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(resultUri));

                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                }
                showImage();
            } else if (requestCode == SELECT_LOCAL_IMAGE_CODE && data != null){
                /* For select from local */
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), data.getData());
                    File tempFile = saveBitmapToTempFile(bitmap);
                    imageUri = Uri.fromFile(tempFile);
                    showImage();
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Failed to load the image!", Toast.LENGTH_SHORT).show();
                }
            }
        }

        /* Check and Get Permission */

        @Override
        protected void onResume() {
            /* Check if permission granted when user comes back from the settings, if not make user go back to settings */
            super.onResume();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Camera permission is required. Please enable it in settings.", Toast.LENGTH_LONG).show();
                    openAppSettings();
                }
            }
        }

        private void checkAndRequestPermission() {
            /* Check and request camera permission */
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
                }
            }
        }

        @Override
        public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, int[] grantResults) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            /* Check camera permission */
            if (requestCode == CAMERA_PERMISSION_CODE) {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Camera permission granted.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Camera permission is required. Please enable it in settings.", Toast.LENGTH_LONG).show();
                    openAppSettings();
                }
            }
        }

        private void openAppSettings() {
            /* Open settings to ask permission */
            Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(android.net.Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        }

    }