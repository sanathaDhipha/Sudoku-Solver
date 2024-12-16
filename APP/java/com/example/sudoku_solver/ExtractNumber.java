package com.example.sudoku_solver;

import android.app.Dialog;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

public class ExtractNumber {
    private final Context context;

    public ExtractNumber(Context context) {
        this.context = context;
    }

    public List<Mat> splitBoxes(Mat img) {
        List<Mat> boxes = new ArrayList<>();

        try {
            int rows = 9; // Number of rows
            int cols = 9; // Number of columns

            // Get the dimensions of the image
            int imgHeight = img.rows();
            int imgWidth = img.cols();

            // Calculate the height and width of each box
            int boxHeight = imgHeight / rows;
            int boxWidth = imgWidth / cols;

            // Loop through each row and column to create the boxes
            for (int row = 0; row < rows; row++) {
                for (int col = 0; col < cols; col++) {
                    // Define the region of interest (ROI) for each box
                    Rect roi = new Rect(col * boxWidth, row * boxHeight, boxWidth, boxHeight);

                    // Extract the box from the image
                    Mat box = new Mat(img, roi);

                    // Add the box to the list
                    boxes.add(box);
                }
            }
        } catch (Exception e) {
            Log.e("splitBoxes", "Error while splitting the image: " + e.getMessage());
        }
        return boxes;
    }

    public List<Mat> processBoxes(List<Mat> boxes) {
        PreprocessImage preprocessImage = new PreprocessImage();
        int minSize = 20;
        int maxSize = 300;
        int edgeThreshold = 2;
        int w = 50;
        int h = 50;
        List<Mat> processedBoxes = new ArrayList<>();

        for (Mat targetImage : boxes) {
            try {
                // Step 1: Convert the image to grayscale
                Mat processedImage = preprocessImage.matToGray(targetImage);
                processedImage = preprocessImage.matResize(processedImage, w, h);
                processedImage = preprocessImage.binaryThresh(processedImage);

                // Step 3: Find connected components
                Mat labels = new Mat();
                Mat stats = new Mat();
                Mat centroids = new Mat();
                int numLabels = Imgproc.connectedComponentsWithStats(processedImage, labels, stats, centroids);

                // Initialize variables to find the best component
                Mat bestComponent = null;
                int bestSize = 0;
                int centerY = h / 2;
                int centerX = w / 2;

                for (int label = 1; label < numLabels; label++) { // Skip background label
                    int size = (int) stats.get(label, Imgproc.CC_STAT_AREA)[0];

                    int minX = (int) stats.get(label, Imgproc.CC_STAT_LEFT)[0];
                    int minY = (int) stats.get(label, Imgproc.CC_STAT_TOP)[0];
                    int width = (int) stats.get(label, Imgproc.CC_STAT_WIDTH)[0];
                    int height = (int) stats.get(label, Imgproc.CC_STAT_HEIGHT)[0];

                    int maxX = minX + width;
                    int maxY = minY + height;

                    // Check size and edge proximity criteria
                    if (size >= minSize && size <= maxSize &&
                            minY > edgeThreshold && maxY < (h - edgeThreshold) &&
                            minX > edgeThreshold && maxX < (w - edgeThreshold)) {

                        // Update the best component based on size
                        if (size > bestSize) {
                            bestSize = size;
                            bestComponent = new Mat();
                            Core.compare(labels, new Scalar(label), bestComponent, Core.CMP_EQ);
                        }
                    }
                }

                // Step 5: Create a mask for the best component
                Mat outputImage = new Mat(h, w, CvType.CV_8UC1, new Scalar(0)); // All black image
                if (bestComponent != null) {
                    bestComponent.convertTo(bestComponent, CvType.CV_8UC1);
                    outputImage.setTo(new Scalar(255), bestComponent);
                }

                // Step 6: Store the processed image
                processedBoxes.add(outputImage);
            } catch (Exception e) {
                Log.e("processBoxes", "Error processing box: " + e.getMessage());
            }
        }
        return processedBoxes;
    }



    private MappedByteBuffer loadModelFile(String modelName) throws IOException {
        try {
            AssetFileDescriptor fileDescriptor = context.getAssets().openFd(modelName);
            FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
            FileChannel fileChannel = inputStream.getChannel();
            long startOffset = fileDescriptor.getStartOffset();
            long declaredLength = fileDescriptor.getDeclaredLength();
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
        } catch (IOException e) {
            Log.e("loadModelFile", "Error loading model file: " + e.getMessage());
            throw e;
        }
    }

    private Mat preprocessImageForTFLite(Mat mat) {
        PreprocessImage preprocessImage = new PreprocessImage();

        Mat resizedImage = preprocessImage.matResize(mat, 50, 50);

        Mat equalizedImage = preprocessImage.equHist(resizedImage);

        Mat binaryImage = new Mat();
        Imgproc.threshold(equalizedImage, binaryImage, 150, 255, Imgproc.THRESH_BINARY);

        return binaryImage;
    }

    private int argmax(float[] array) {
        int maxIndex = 0;
        float maxValue = array[0];
        for (int i = 1; i < array.length; i++) {
            if (array[i] > maxValue) {
                maxValue = array[i];
                maxIndex = i;
            }
        }
        return maxIndex;
    }

    public int[][] extractNumber(Bitmap bitmap) {
        PreprocessImage preprocessImage = new PreprocessImage();
        int[][] sudokuMatrix = new int[9][9];
        Bitmap processedBitmap = null;

        try {
            processedBitmap = preprocessImage.processImage(bitmap);
            if (processedBitmap == null) {
                throw new NullPointerException("Processed bitmap is null after preprocessing.");
            }
            Log.d("extractNumber", "Bitmap successfully processed.");
        } catch (Exception e) {
            Log.e("extractNumber", "Error during image preprocessing: " + e.getMessage());
            return sudokuMatrix; // Return the default matrix in case of error
        }

        Mat matImage = new Mat();
        try {
            Utils.bitmapToMat(processedBitmap, matImage);
            Log.d("extractNumber", "Bitmap converted to Mat successfully.");
        } catch (Exception e) {
            Log.e("extractNumber", "Error converting bitmap to Mat: " + e.getMessage());
            return sudokuMatrix; // Return the default matrix in case of error
        }

        List<Mat> boxes = splitBoxes(matImage);
        if (boxes.isEmpty()) {
            Log.e("extractNumber", "No boxes were split from the image.");
            return sudokuMatrix;
        }

        boxes = processBoxes(boxes);

        // Initialize the TensorFlow Lite Interpreter
        try {
            Interpreter interpreter = new Interpreter(loadModelFile("num_classifier_COMBINED.tflite"));

            // Iterate over the 9x9 grid
            for (int i = 0; i < 9; i++) {
                for (int j = 0; j < 9; j++) {
                    int index = i * 9 + j;

                    if (index < boxes.size()) {
                        Mat box = boxes.get(index);

                        // Check if the box is not empty
                        if (Core.sumElems(box).val[0] > 0) {
                            Mat testImagePreprocessed = preprocessImageForTFLite(box);

                            // Prepare the input tensor
                            float[][][][] inputTensor = new float[1][50][50][1];
                            for (int x = 0; x < 50; x++) {
                                for (int y = 0; y < 50; y++) {
                                    inputTensor[0][x][y][0] = (float) testImagePreprocessed.get(x, y)[0];
                                }
                            }

                            // Run inference
                            float[][] output = new float[1][10]; // Output size 10 (for digits 0-9)
                            interpreter.run(inputTensor, output);

                            // Extract the digit from the output
                            int predictedDigit = argmax(output[0]);

                            // Update the Sudoku matrix
                            sudokuMatrix[i][j] = predictedDigit;
                            Log.d("extractNumber", "Predicted digit: " + predictedDigit + " at position (" + i + ", " + j + ")");
                        }
                    }
                }
            }
            Log.d("extractNumber", "Number extraction complete.");
        } catch (Exception e) {
            Log.e("extractNumber", "Error during TensorFlow Lite inference: " + e.getMessage());
        }

        return sudokuMatrix;
    }

    public List<Mat> testShowGridImage(Bitmap bitmap){
        PreprocessImage preprocessImage = new PreprocessImage();
        int[][] sudokuMatrix = new int[9][9];
        Bitmap processedBitmap = null;
        processedBitmap = preprocessImage.processImage(bitmap);

        Mat matImage = new Mat();
        Utils.bitmapToMat(processedBitmap, matImage);
        List<Mat> boxes = splitBoxes(matImage);
        boxes = processBoxes(boxes);
        return boxes;
    }

}