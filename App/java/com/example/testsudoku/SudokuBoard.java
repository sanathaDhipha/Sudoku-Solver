package com.example.testsudoku;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class SudokuBoard extends AppCompatActivity {

    private SudokuBoardView sudokuBoardView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sudoku_board);

        button();
        sudokuBoardView = findViewById(R.id.SudokuBoard);
        /* Take the flatten matrix and turn it back to 2D array then pass set the matrix in the view */
        int[] flatMatrix = getIntent().getIntArrayExtra("SUDOKU_MATRIX");
        int[][] matrix = new int[9][9];
        if (flatMatrix != null) {
            int index = 0;
            for (int i = 0; i < 9; i++) {
                for (int j = 0; j < 9; j++) {
                    matrix[i][j] = flatMatrix[index++];
                }
            }

            sudokuBoardView.setSolvedMatrix(matrix);
        } else {
            Log.e("SudokuBoard", "Matrix data not found.");
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    public void button() {
        /* Button to go to main from SudokuBoard */
        Button buttonBackToMain = findViewById(R.id.buttonFromSudokuBackToMain);
        buttonBackToMain.setOnClickListener(view -> {
            finish();
        });
    }
}