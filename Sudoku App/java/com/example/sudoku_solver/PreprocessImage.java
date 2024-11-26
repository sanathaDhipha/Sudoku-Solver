package com.example.sudoku_solver;

import android.graphics.Bitmap;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class PreprocessImage {
    public Mat matToGray(Mat mat){
        Mat grayMat = new Mat();
        Imgproc.cvtColor(mat, grayMat, Imgproc.COLOR_BGR2GRAY);
        return grayMat;
    }

    public Mat matResize(Mat mat, int w, int h){
        Mat resizedMat = new Mat();
        Imgproc.resize(mat, resizedMat, new Size(w, h));
        return resizedMat;
    }

    public Mat gaussianBlur(Mat mat){
        Mat blurredMat = new Mat();
        Imgproc.GaussianBlur(mat, blurredMat, new Size(5, 5), 1);
        return blurredMat;
    }

    public  Mat equHist(Mat mat){
        Mat equalizedImage = new Mat();
        Imgproc.equalizeHist(mat, equalizedImage);
        return equalizedImage;
    }

    public Mat adaptiveThresh(Mat mat){
        Mat threshMat = new Mat();
        Imgproc.adaptiveThreshold(mat, threshMat, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY_INV, 15, 7);
        return threshMat;
    }

    public Mat binaryThresh(Mat mat){
        Mat threshMat = new Mat();
        Imgproc.threshold(mat, threshMat, 150, 255, Imgproc.THRESH_BINARY_INV);
        return threshMat;
    }

    public Mat dilation(Mat mat) {
        Mat dilatedMat = new Mat();
        Mat kernel = Mat.ones(3, 3, CvType.CV_8U);
        kernel.put(0, 0, 0);
        kernel.put(0, 2, 0);
        kernel.put(2, 0, 0);
        kernel.put(2, 2, 0);
        Imgproc.dilate(mat, dilatedMat, kernel, new Point(-1, -1), 1);
        return dilatedMat;
    }

    public int getMinIndex(double[] array) {
        int minIndex = 0;
        for (int i = 1; i < array.length; i++) {
            if (array[i] < array[minIndex]) {
                minIndex = i;
            }
        }
        return minIndex;
    }

    public int getMaxIndex(double[] array) {
        int maxIndex = 0;
        for (int i = 1; i < array.length; i++) {
            if (array[i] > array[maxIndex]) {
                maxIndex = i;
            }
        }
        return maxIndex;
    }

    public Mat preprocessImage(Mat mat){
        Mat grayMat = matToGray(mat);
        Mat blurredGrayMat = gaussianBlur(grayMat);
        Mat threshMat = adaptiveThresh(blurredGrayMat);
        Mat dilatedMat = dilation(threshMat);
        grayMat.release();
        blurredGrayMat.release();
        threshMat.release();
        return dilatedMat;
    }

    public boolean isGrid(Bitmap inputBitmap){
        Mat matImage = new Mat();
        Utils.bitmapToMat(inputBitmap, matImage);
        Mat preprocessedMat = preprocessImage(matImage);
        boolean result = isContour(preprocessedMat) && isRectangle(preprocessedMat);
        matImage.release();
        preprocessedMat.release();
        return result;
    }

    public List<MatOfPoint> findContours(Mat mat){
        Mat hierarchy = new Mat();
        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(mat, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        hierarchy.release();
        return contours;
    }

    public boolean isContour(Mat mat){
        List<MatOfPoint> contours = findContours(mat);
        return !contours.isEmpty();
    }

    public MatOfPoint findLargestContour(List<MatOfPoint> contours){
        return Collections.max(contours, Comparator.comparingDouble(Imgproc::contourArea));
    }

    public MatOfPoint2f approxContour(MatOfPoint largestContour){
        MatOfPoint2f largestContour2f = new MatOfPoint2f(largestContour.toArray());
        double epsilon = 0.02 * Imgproc.arcLength(largestContour2f, true);
        MatOfPoint2f approx = new MatOfPoint2f();
        Imgproc.approxPolyDP(largestContour2f, approx, epsilon, true);
        return approx;
    }

    public boolean isRectangle(Mat mat){
        List<MatOfPoint> contours = findContours(mat);
        MatOfPoint largestContour = findLargestContour(contours);
        MatOfPoint2f approx = approxContour(largestContour);
        return approx.total() == 4;
    }

    public Mat perspectiveTransform(MatOfPoint2f approx, Mat mat){
        // Order points and create perspective transform matrix
        Point[] pts = approx.toArray();
        Point[] rect = new Point[4];

        // Top-left (min sum), Bottom-right (max sum)
        double[] s = new double[pts.length];
        for (int i = 0; i < pts.length; i++) {
            s[i] = pts[i].x + pts[i].y;
        }
        rect[0] = pts[getMinIndex(s)]; // Top-left
        rect[2] = pts[getMaxIndex(s)]; // Bottom-right

        // Top-right (min diff), Bottom-left (max diff)
        double[] diff = new double[pts.length];
        for (int i = 0; i < pts.length; i++) {
            diff[i] = pts[i].y - pts[i].x;
        }
        rect[1] = pts[getMinIndex(diff)]; // Top-right
        rect[3] = pts[getMaxIndex(diff)]; // Bottom-left

        // Define the destination rectangle
        MatOfPoint2f dst = new MatOfPoint2f(
                new Point(0, 0),
                new Point(450, 0),
                new Point(450, 450),
                new Point(0, 450)
        );

        // Perform Perspective Transformation
        Mat transformationMatrix = Imgproc.getPerspectiveTransform(new MatOfPoint2f(rect), dst);
        Mat warpedMat = new Mat();
        Imgproc.warpPerspective(mat, warpedMat, transformationMatrix, new Size(450, 450));
        return warpedMat;
    }

    public Bitmap processImage(Bitmap inputBitmap) {
        Mat matImage = new Mat();
        Utils.bitmapToMat(inputBitmap, matImage);
        Mat preprocessedMat = preprocessImage(matImage);
        List<MatOfPoint> contours = findContours(preprocessedMat);
        MatOfPoint largestContour = findLargestContour(contours);
        MatOfPoint2f approx = approxContour(largestContour);
        Mat warpedMat = perspectiveTransform(approx, matImage);
        Bitmap bitmapProcessed = Bitmap.createBitmap(warpedMat.cols(), warpedMat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(warpedMat, bitmapProcessed);
        matImage.release();
        preprocessedMat.release();
        contours.clear();
        warpedMat.release();
        return bitmapProcessed;
    }
}
