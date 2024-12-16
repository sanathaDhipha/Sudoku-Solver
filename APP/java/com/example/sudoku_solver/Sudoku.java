package com.example.sudoku_solver;

import android.graphics.Bitmap;

public class Sudoku {
    private Bitmap bitmap;
    private int[][] sudokuMatrix = new int[9][9];
    private int[][] solvedSudokuMatrix = new int[9][9];

    public Sudoku(Bitmap bitmap, int[][] sudokuMatrix, int[][] solvedSudokuMatrix) {
        this.bitmap = bitmap;
        this.sudokuMatrix = sudokuMatrix;
        this.solvedSudokuMatrix = solvedSudokuMatrix;
    }

    public Sudoku(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public int[][] getSudokuMatrix() {
        return sudokuMatrix;
    }

    public void setSudokuMatrix(int[][] sudokuMatrix) {
        this.sudokuMatrix = sudokuMatrix;
    }

    public int[][] getSolvedSudokuMatrix() {
        return solvedSudokuMatrix;
    }

    public void setSolvedSudokuMatrix(int[][] solvedSudokuMatrix) {
        this.solvedSudokuMatrix = solvedSudokuMatrix;
    }
}
