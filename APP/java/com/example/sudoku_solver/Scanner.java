package com.example.sudoku_solver;

import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;

import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class Scanner extends CameraActivity {

    private final int selectFromLocalCode = 100;
    private CameraBridgeViewBase cameraBridgeViewBase;
    private Mat currentFrame;
    private Bitmap bitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sudoku_scanner);

        /* Check camera permission */
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            finish();
            return;
        }

        /* Check if OpenCV load correctly */
        if (OpenCVLoader.initLocal()) Log.d("LOADED", "Success Loading OPENCV");
        else Log.d("LOADED", "Error Loading OPENCV");

        /* Launch live camera */
        cameraBridgeViewBase = findViewById(R.id.cameraView);
        cameraBridgeViewBase.setCameraIndex(0);
        cameraBridgeViewBase.setCvCameraViewListener(new CameraBridgeViewBase.CvCameraViewListener2() {
            @Override
            public void onCameraViewStarted(int width, int height) {}

            @Override
            public void onCameraViewStopped() {}

            @Override
            public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
                currentFrame = inputFrame.rgba();
                return currentFrame;
            }
        });

        if (OpenCVLoader.initLocal()) cameraBridgeViewBase.enableView();

        backToMain();
        selectFromLocal();
        captureFromCamera();
    }

    private void backToMain() {
        /* Back to main from Scanner activity */
        Button buttonBackToMain = findViewById(R.id.buttonBackToMain);
        buttonBackToMain.setOnClickListener(view -> finish());
    }

    private void selectFromLocal() {
        /* Select picture from local storage */
        Button buttonSelectFromLocal = findViewById((R.id.buttonInsert));
        buttonSelectFromLocal.setOnClickListener(View -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT).setType("image/*");
            startActivityForResult(intent, selectFromLocalCode);
        });
    }

    private void captureFromCamera() {
        /* Capture image from live camera*/
        Button buttonCaptureFromCamera = findViewById(R.id.buttonCamera);
        buttonCaptureFromCamera.setOnClickListener(view -> {
            /* If the current frame is showing image then take the current frame and
            create a bitmap out of it then proceed to popUpDialog */
            if (currentFrame != null) {
                bitmap = Bitmap.createBitmap(currentFrame.cols(), currentFrame.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(currentFrame, bitmap);
                popUpDialog();
            } else {
                Toast.makeText(this, "No frame captured!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String matrixToString(int[][] matrix) {
        /* Turn matrix into string */
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[i].length; j++) {
                sb.append(matrix[i][j]);
                if (j < matrix[i].length - 1) {
                    sb.append(" ");
                }
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private void showSolvedSudokuDialog() {
        /* Process the image extract the number in the grid to matrix and if the sudoku puzzle
        which is represented by matrix is solvable then showResult() */
        Sudoku sudoku;
        PreprocessImage preprocessImage = new PreprocessImage();
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

    private void noSudokuDialog(){
        Dialog dialog = new Dialog(Scanner.this);
        dialog.setContentView(R.layout.layout_solved_sudoku_dialog_no_grid);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawable(getDrawable(R.drawable.solved_sudoku_dialog_bg));
        dialog.setCancelable(false);

        ImageView imageView = dialog.findViewById(R.id.imageView);
        imageView.setImageBitmap(bitmap);

        Button buttonCancelDialog = dialog.findViewById(R.id.noGridBack);
        buttonCancelDialog.setOnClickListener(View -> dialog.dismiss());
        dialog.show();
    }

    private void testShowSplitResult(Bitmap bitmap) {
        ExtractNumber extractNumber = new ExtractNumber(this);
        PreprocessImage preprocessImage = new PreprocessImage();
        List<Mat> boxes = extractNumber.testShowGridImage(bitmap);

        Dialog dialog = new Dialog(Scanner.this);
        dialog.setContentView(R.layout.layout_solved_sudoku_dialog);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawable(getDrawable(R.drawable.solved_sudoku_dialog_bg));
        dialog.setCancelable(false);

        int n = 0;
        // Check if the index n is within the bounds of the boxes list
        if (n >= 0 && n < boxes.size()) {
            // Get the Mat at index n
            Mat matAtIndex = boxes.get(n);

            // Convert Mat to Bitmap
            Bitmap bitmapAtIndex = matToBitmap(matAtIndex);

            // Set the Bitmap to the ImageView
            ImageView imageView = dialog.findViewById(R.id.testImageView2);
            imageView.setImageBitmap(bitmapAtIndex);
        } else {
            // If index is out of bounds, show an error message
            Toast.makeText(Scanner.this, "Index out of bounds", Toast.LENGTH_SHORT).show();
        }

        ImageView imageView = dialog.findViewById(R.id.testImageView1);
        imageView.setImageBitmap(preprocessImage.processImage(bitmap));

        Button buttonCancelDialog = dialog.findViewById(R.id.cancelTestDialog);
        buttonCancelDialog.setOnClickListener(View -> dialog.dismiss());
        dialog.show();
    }

    // Method to convert Mat to Bitmap
    private Bitmap matToBitmap(Mat mat) {
        Bitmap bmp = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mat, bmp);  // Convert Mat to Bitmap
        return bmp;
    }

    private void popUpDialog(){
//        testShowSplitResult(bitmap);
        /* Detect sudoku grid in the image*/
        PreprocessImage preprocessImage = new PreprocessImage();
        if (preprocessImage.isGrid(bitmap)){
            showSolvedSudokuDialog();
        }else{
            noSudokuDialog();
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

        Intent intentSudokuScanner = new Intent(Scanner.this, SudokuBoard.class);
        intentSudokuScanner.putExtra("SUDOKU_MATRIX", flatMatrix);

        finish();
        startActivity(intentSudokuScanner);
    }

    @Override
    protected void onResume() {
        super.onResume();
        cameraBridgeViewBase.enableView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraBridgeViewBase.disableView();
    }

    @Override
    protected void onPause() {
        super.onPause();
        cameraBridgeViewBase.disableView();
    }

    @Override
    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(cameraBridgeViewBase);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode==selectFromLocalCode && data != null){
            try {
                bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), data.getData());
                popUpDialog();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Failed to load the image!", Toast.LENGTH_SHORT).show();
            }
        } else if (resultCode == RESULT_CANCELED) {
            Toast.makeText(Scanner.this, "Image selection canceled.", Toast.LENGTH_SHORT).show();
        }
    }
}