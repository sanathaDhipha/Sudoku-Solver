package com.example.sudoku_solver;

public class Solver {
    private boolean isValidMove(int[][] matrix, int row, int col, int num) {
        // Check row
        for (int i = 0; i < 9; i++) {
            if (matrix[row][i] == num) {
                return false;
            }
        }
        // Check column
        for (int i = 0; i < 9; i++) {
            if (matrix[i][col] == num) {
                return false;
            }
        }
        // Check 3x3 grid
        int startRow = 3 * (row / 3);
        int startCol = 3 * (col / 3);
        for (int i = startRow; i < startRow + 3; i++) {
            for (int j = startCol; j < startCol + 3; j++) {
                if (matrix[i][j] == num) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean isSolvable(int[][] matrix) {
        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++) {
                if (matrix[row][col] == 0) { // Empty cell found
                    for (int num = 1; num <= 9; num++) { // Numbers 1 to 9
                        if (isValidMove(matrix, row, col, num)) {
                            matrix[row][col] = num; // Place the number
                            if (isSolvable(matrix)) { // Recursive call
                                return true;
                            }
                            matrix[row][col] = 0; // Backtrack
                        }
                    }
                    return false; // No valid number found, backtrack
                }
            }
        }
        return true; // Puzzle solved
    }

    public int[][] solveSudoku(int[][] matrix) {
        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++) {
                if (matrix[row][col] == 0) { // Empty cell found
                    for (int num = 1; num <= 9; num++) { // Numbers 1 to 9
                            matrix[row][col] = num; // Place the number
                            matrix[row][col] = 0; // Backtrack
                    }
                }
            }
        }
        return matrix;
    }
}
