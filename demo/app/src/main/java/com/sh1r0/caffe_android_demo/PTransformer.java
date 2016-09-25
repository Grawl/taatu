package com.sh1r0.caffe_android_demo;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class PTransformer {

    static int[] getPixelsFromBitmap(Bitmap tatu, int x, int y, int width, int height) {
        int[] allPixels = new int[tatu.getWidth()*tatu.getHeight()];
        try {
            if (x + width >= tatu.getWidth()) {
                width = tatu.getWidth() - x;
            }
            if (y + height >= tatu.getHeight()) {
                height = tatu.getHeight() - y;
            }
            tatu.getPixels(allPixels, 0, tatu.getWidth(), x, y, width, height);
        }
        catch (Exception e) {
            return new int[]{};
        }
        int[] pixels = new int[width * height];
        int pixelsIndex = 0;
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                pixels[pixelsIndex] = allPixels[i + j * tatu.getWidth()];
                pixelsIndex++;
            }
        }
        return pixels;
    }

    public static Bitmap makeTatu(Bitmap tatu, int[] depthMap, int depthMapHeight, int depthMapWidth) {
        int[][] intermediateResult = new int[tatu.getHeight()][];
        int ratio = tatu.getHeight()/depthMapHeight;
        for (int i = 0; i < tatu.getHeight(); i++) {
            int[] row = getPixelsFromBitmap(tatu, 0, i, tatu.getWidth(), 1);
            int[] depthRow = getRowFromDepthMap(depthMap, depthMapWidth, i / ratio, tatu.getWidth());
            int[] processedRow = processRow(row, depthRow);
            intermediateResult[i] = processedRow;
        }
        /*
        int[][] finalResult = new int[tatu.getWidth()][];
        for (int j = 0; j < tatu.getWidth(); j++) {
            int[] col = getPixelsFromBitmap(tatu, j, 0, tatu.getHeight(), 1);
            int[] depthCol = getColFromDepthMap(depthMap, depthMapWidth, depthMapHeight, j / ratio, tatu.getWidth());
            int[] processedCol = processRow(col, depthCol);
            finalResult[j] = processedCol;
        }*/
        int[][] finalResult = intermediateResult;
        int[] buffer = new int[tatu.getHeight() * tatu.getWidth()];
        int bufIndex = 0;
        int sum = 0;
        for (int i = 0; i < finalResult.length; i++) {
            for (int j = 0; j < finalResult[i].length; j++) {
                buffer[bufIndex] = finalResult[i][j];
                bufIndex++;
                if (buffer[bufIndex - 1] != 0)
                    sum++;
            }
        }
        Bitmap transformedTatu = Bitmap.createBitmap(tatu.getWidth(), tatu.getHeight(), Bitmap.Config.ARGB_8888);
        // vector is your int[] of ARGB
        transformedTatu.copyPixelsFromBuffer(IntBuffer.wrap(buffer));
        return transformedTatu;
    }

    static int[] getColFromDepthMap(int[] depthMap, int depthMapWidth, int depthMapHeight, int colNumber, int tatuHeight) {
        int[] row = new int[tatuHeight];
        int ratio = tatuHeight/depthMapHeight;
        int rowIndex = 0;
        for (int i = colNumber; i < colNumber * depthMapWidth; i+=depthMapWidth) {
            for (int j = 0; j < ratio; j++) {
                row[rowIndex] = depthMap[i];
            }
        }
        return row;
    }

    static int[] getRowFromDepthMap(int[] depthMap, int depthMapWidth, int rowNumber, int tatuWidth) {
        int[] row = new int[tatuWidth];
        int ratio = tatuWidth/depthMapWidth;
        int rowIndex = 0;
        for (int i = rowNumber * depthMapWidth; i < (rowNumber + 1) * depthMapWidth; i++) {
            for (int j = 0; j < ratio; j++) {
                row[rowIndex] = depthMap[i];
                rowIndex++;
            }
        }
        return row;
    }

    public static int[] processRow(int[]pixelsRow, int[] depth  ) {
        int[]pixelsRes = new int[pixelsRow.length];
        List<Integer> directions = new ArrayList<Integer>();
        directions.add(1);
        directions.add(-1);        // Задаём два направления и бегаем по ним, за исключением последних пикселей (чтобы не вывалиться)

        /*depth = new int[depth.length];
        for (int i = 0; i < depth.length; i++) {
            if (i > depth.length/2)
                depth[i] = (i - depth.length/2) * 3;
        }
        */
        for (int direction : directions) {
            //Индекс, бегающий по исходному изображению
            int pointer = pixelsRow.length/2;            //Индекс, набранный суммированием исходных пикселей с весом 1
            double current = 0;
            for(int i = pixelsRow.length/2; i > 1 && i < pixelsRow.length - 1; i = i + direction){
                List<Integer> pixelsToTransform = new ArrayList<Integer>();                //Считаем гипотенузу - столько, сколько нужно исходных пикселей на один пиксель результата
                double heightDiff = Math.abs(depth[i] - depth[i + direction]);
                double required = Math.sqrt( Math.pow(heightDiff, 2.0) + 1);                //Добавляем пиксели, пока их суммарная еденичная длина не перекроет необходимую
                while (current < required){                    //Проверка, что не дошли до края, если дошли - мержим, что набрали
                    if (direction == -1 && pointer == 0) {
                        break;
                    }
                    if (direction == 1 && pointer == pixelsRow.length - 1) {
                        break;
                    }
                    pixelsToTransform.add(pixelsRow[pointer]);
                    current += 1;
                    pointer += direction;
                }                //Мержим пиксели в искомый, вычитаем полученную сумму.
                    if (pixelsToTransform.size() > 0)
                        pixelsRes[i] = transformPixels(pixelsToTransform);

                current -= required;

            }

        }
        return pixelsRes;
    }

    public static int transformPixels(List<Integer> pixels) {
        int r_m = 0;
        int g_m = 0;
        int b_m = 0;
        int a_m = 0;
        for (int pixel : pixels) {
            int r = Color.red(pixel);
            int g = Color.green(pixel);
            int b = Color.blue(pixel);
            int a = Color.alpha(pixel);
            r_m += r;
            g_m += g;
            b_m += b;
            a_m += a;
        }
        return Color.argb(a_m/pixels.size(), r_m/pixels.size(), g_m/pixels.size(), b_m/pixels.size());
    }
}
