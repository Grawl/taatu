package com.sh1r0.caffe_android_demo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;

public class Transformer {

    static int transformPixels(int[] pixels) {
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

    static int processPixel(int[] depthMap, int depthMapWidth, int depthMapHeight, int smallestDepth,
                             int i, int j_n, int coeff, Bitmap tatu, int[] transformedTatu) {
        int depth = depthMap[j_n * depthMapWidth + i];
        int tatuPixelsLength = (int)Math.round(Math.sqrt(1 + (smallestDepth - depth)*(smallestDepth - depth)));
        int[] pixels = getPixelsFromBitmap(tatu, i * coeff, j_n * coeff, tatuPixelsLength, coeff);
        /*int[] pixels = new int[tatuPixelsLength * coeff];
        tatu.getPixels(pixels, 0, tatu.getWidth(), i * coeff, j_n * coeff, tatuPixelsLength, coeff);
        */int d_p = transformPixels(pixels);
        transformedTatu[i * depthMapHeight + j_n] = d_p;
        return tatuPixelsLength;
    }

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

    static int processRow(int[] depthMap, int depthMapHeight, int depthMapWidth, int heightCoeff,
                           int i_n, int j, Bitmap tatu, int[] transformedTatu, int rowStart, int colStart,
                          boolean direction) {
        /*
        int[] allPixels = new int[tatu.getWidth()*tatu.getHeight()];
        tatu.getPixels(allPixels, 0, tatu.getWidth(), i_n * heightCoeff, j * heightCoeff, heightCoeff, heightCoeff);
        int sum = 0;
        for (int i = 0; i < allPixels.length; i++) {
            if (allPixels[i] != 0) {
                sum++;
            }
        }
        int[] pixels = new int[heightCoeff * heightCoeff];
        for (int x = 0; x < heightCoeff; x++) {
            for (int y = 0; y < heightCoeff; y++) {
                pixels[x*heightCoeff + y] = allPixels[x*tatu.getWidth() + y];
            }
        }
        */
        int[] pixels = getPixelsFromBitmap(tatu, colStart, rowStart, heightCoeff, heightCoeff);
        int d_p = transformPixels(pixels);
        transformedTatu[i_n * depthMapHeight + j] = d_p;
        int smallestDepth = depthMap[i_n * depthMapWidth + j];
        for (int i = i_n + 1; i < depthMapWidth; i++) {
            if (i * heightCoeff >= tatu.getWidth())
                break;
            processPixel(depthMap, depthMapWidth, depthMapHeight, smallestDepth, i, j, heightCoeff, tatu, transformedTatu);
            smallestDepth = depthMap[j * depthMapWidth + i];
        }
        smallestDepth = depthMap[i_n * depthMapWidth + j];
        for (int i = i_n - 1; i >= 0; i--) {
            processPixel(depthMap, depthMapWidth, depthMapHeight, smallestDepth, i, j, heightCoeff, tatu, transformedTatu);
            smallestDepth = depthMap[j * depthMapWidth + i];
        }
        if (direction && j != depthMapHeight - 1)
            return processPixel(depthMap, depthMapWidth, depthMapHeight, smallestDepth, i_n, j + 1, heightCoeff, tatu, transformedTatu);
        else if (!direction && j != 0)
            return processPixel(depthMap, depthMapWidth, depthMapHeight, smallestDepth, i_n, j - 1, heightCoeff, tatu, transformedTatu);
        else return 0;
    }

    public static int[] makeTatu(Bitmap tatu, int[] depthMap, int depthMapHeight, int depthMapWidth, float tatuSize) {
        int[] transformedTatu = new int[depthMapHeight*depthMapWidth];
        int coeff = tatu.getHeight()/depthMapHeight;
        int[] nearestPixel = findNearestPixel(depthMap, depthMapHeight, depthMapWidth);
        int i_n = nearestPixel[0];
        int j_n = nearestPixel[1];
        int heightCoeff = coeff;
        for (int j = j_n; j < depthMapHeight; j++) {
            if (j * heightCoeff >= tatu.getHeight()) {
                break;
            }
            heightCoeff = processRow(depthMap, depthMapHeight, depthMapWidth, heightCoeff, i_n, j,
                    tatu, transformedTatu, );

        }
        heightCoeff = coeff;
        for (int j = j_n - 1; j >= 0; j--) {
            heightCoeff = processRow(depthMap, depthMapHeight, depthMapWidth, heightCoeff, i_n, j, tatu, transformedTatu);
        }
        return transformedTatu;
        //Bitmap result = Bitmap.createBitmap(transformedTatu, depthMapWidth, depthMapHeight, Bitmap.Config.ARGB_8888);
    }

    static int[] findNearestPixel(int[] depthMap, int depthMapHeight, int depthMapWidth) {
        int min = Integer.MAX_VALUE;
        int x = 0;
        int y = 0;
        for (int i = 0; i < depthMapWidth; i++)
            for (int j = 0; j < depthMapHeight; j++) {
                if (depthMap[i * depthMapHeight + j] < min) {
                    x = i;
                    y = j;
                    min = depthMap[i * depthMapHeight + j];
                }
            }
        return new int[]{x, y};
    }
}


