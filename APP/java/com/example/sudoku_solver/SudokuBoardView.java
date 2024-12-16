package com.example.sudoku_solver;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class SudokuBoardView extends View {
    private final int boardColor;
    private final int letterColor;
    private final Paint boardColorPaint = new Paint();

    private final Paint letterPaint = new Paint();
    private final Rect letterPaintBounds = new Rect();
    private int[][] sudokuMatrix = new int[9][9];
    private int cellSize;

    public SudokuBoardView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        /* Read custom attribute in attrs */
        TypedArray array = context.getTheme().obtainStyledAttributes(attrs, R.styleable.SudokuBoardView, 0, 0);
        try {
            boardColor = array.getInteger(R.styleable.SudokuBoardView_boardColor, 0);
            letterColor = array.getInteger(R.styleable.SudokuBoardView_letterColor, 0);
        } finally {
            array.recycle();
        }
    }

    public void setSolvedMatrix(int[][] matrix) {
        /* Set sudoku matrix */
        this.sudokuMatrix = new int[9][9];
        this.sudokuMatrix = matrix;
        invalidate();
    }

    @Override
    protected void onMeasure(int width, int height) {
        /* Measure width and height of sudoku board based on the screen and also make sure it always rectangle */
        super.onMeasure(width, height);
        int dimension = Math.min(this.getMeasuredWidth(), this.getMeasuredHeight());
        dimension = dimension - 60;
        cellSize = dimension / 9;
        setMeasuredDimension(dimension, dimension);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        /* Draw sudoku board */
        boardColorPaint.setStyle(Paint.Style.STROKE);
        boardColorPaint.setStrokeWidth(16);
        boardColorPaint.setColor(boardColor);
        boardColorPaint.setAntiAlias(true);

        letterPaint.setStyle(Paint.Style.FILL);
        letterPaint.setAntiAlias(true);
        letterPaint.setColor(letterColor);
        canvas.drawRect(0, 0, getWidth(), getHeight(), boardColorPaint);
        drawBoard(canvas);
        drawNumbers(canvas);
    }

    private void drawThickLine() {
        /* Draw thick line */
        boardColorPaint.setStyle(Paint.Style.STROKE);
        boardColorPaint.setStrokeWidth(10);
        boardColorPaint.setColor(boardColor);
    }

    private void drawThinLine() {
        /* Draw thin line */
        boardColorPaint.setStyle(Paint.Style.STROKE);
        boardColorPaint.setStrokeWidth(4);
        boardColorPaint.setColor(boardColor);
    }

    private void drawBoard(Canvas canvas) {
        /* Draw thick line every 3 line and thin in any other line*/
        for (int i = 0; i < 10; i++) {
            if (i % 3 == 0) {
                drawThickLine();
            } else {
                drawThinLine();
            }
            canvas.drawLine(cellSize * i, 0, cellSize * i, getWidth(), boardColorPaint);
        }
        for (int i = 0; i < 10; i++) {
            if (i % 3 == 0) {
                drawThickLine();
            } else {
                drawThinLine();
            }
            canvas.drawLine(0, cellSize * i, getWidth(), cellSize * i, boardColorPaint);
        }
    }

    private void drawNumbers(Canvas canvas) {
        /* Fill the board with the value in the matrix */
        letterPaint.setTextSize(cellSize);
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                String text = Integer.toString(sudokuMatrix[i][j]);
                float width, height;
                letterPaint.getTextBounds(text, 0, text.length(), letterPaintBounds);
                width = letterPaint.measureText(text);
                height = letterPaintBounds.height();
                canvas.drawText(text, (j * cellSize) + ((cellSize - width) / 2), (i * cellSize + cellSize) - ((cellSize - height) / 2), letterPaint);
            }
        }
    }
}

