package com.sh1r0.caffe_android_demo;

import android.graphics.Bitmap;
import android.graphics.Color;

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
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                pixels[i*height + j] = allPixels[i*tatu.getWidth() + j];
            }
        }
        return pixels;
    }


    public static void processRow(int[]pixelsRow, int[] depth  ) {

        int[]pixelsRes = new int[pixelsRow.length];

        List<Integer> directions = new ArrayList<Integer>();
        directions.add(1);
        directions.add(-1);

        // Задаём два направления и бегаем по ним, за исключением последних пикселей (чтобы не вывалиться)
        for (int direction : directions) {
            //Индекс, бегающий по исходному изображению
            int pointer = pixelsRow.length/2;

            //Индекс, набранный суммированием исходных пикселей с весом 1
            double current = 0;

            for(int i = pixelsRow.length/2; i > 1 || i < pixelsRow.length - 1; i = i + direction){

                List<Integer> pixelsToTransform = new ArrayList<Integer>();

                //Считаем гипотенузу - столько, сколько нужно исходных пикселей на один пиксель результата
                double heightDiff = Math.abs(depth[i] - depth[i + direction]);
                double required = Math.sqrt( Math.pow(heightDiff, 2.0) + 1);

                //Добавляем пиксели, пока их суммарная еденичная длина не перекроет необходимую
                while (current < required){

                    //Проверка, что не дошли до края, если дошли - мержим, что набрали
                    if (direction == -1 && pointer > 0)
                        pixelsRes[i] = transformPixels(pixelsToTransform);

                    if (direction == 1 && pointer < pixelsRow.length)
                        pixelsRes[i] = transformPixels(pixelsToTransform);

                    pixelsToTransform.add(pixelsRow[pointer]);
                    current += 1;
                    pointer += direction;
                }

                //Мержим пиксели в искомый, вычитаем полученную сумму.
                pixelsRes[i] = transformPixels(pixelsToTransform);
                current -= required;
            }
        }


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
        return Color.argb(a_m/pixels.length, r_m/pixels.length, g_m/pixels.length, b_m/pixels.length);
    }
}
