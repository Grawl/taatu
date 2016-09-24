package com.sh1r0.caffe_android_demo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;

/**
 * Created by ag3r on 9/24/2016.
 */
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
        int[] pixels = new int[tatuPixelsLength * coeff];
        tatu.getPixels(pixels, 0, 0, i * coeff, j_n * coeff, tatuPixelsLength, coeff);
        int d_p = transformPixels(pixels);
        transformedTatu[i * depthMapHeight + j_n] = d_p;
        return tatuPixelsLength;
    }

    static int processRow(int[] depthMap, int depthMapHeight, int depthMapWidth, int heightCoeff,
                           int i_n, int j, Bitmap tatu, int[] transformedTatu) {
        int[] pixels = new int[heightCoeff * heightCoeff];
        tatu.getPixels(pixels, 0, 0, i_n * heightCoeff, j * heightCoeff, heightCoeff, heightCoeff);
        int d_p = transformPixels(pixels);
        transformedTatu[i_n * depthMapHeight + j] = d_p;
        int smallestDepth = depthMap[i_n * depthMapWidth + j];
        for (int i = i_n + 1; i < depthMapWidth; i++) {
            processPixel(depthMap, depthMapWidth, depthMapHeight, smallestDepth, i, j, heightCoeff, tatu, transformedTatu);
            smallestDepth = depthMap[j * depthMapWidth + i];
        }
        smallestDepth = depthMap[i_n * depthMapWidth + j];
        for (int i = i_n - 1; i >= 0; i--) {
            processPixel(depthMap, depthMapWidth, depthMapHeight, smallestDepth, i, j, heightCoeff, tatu, transformedTatu);
            smallestDepth = depthMap[j * depthMapWidth + i];
        }
        if (j != depthMapHeight - 1)
            return processPixel(depthMap, depthMapWidth, depthMapHeight, smallestDepth, i_n, j, heightCoeff, tatu, transformedTatu);
        else
            return 0;
    }

    public static int[] makeTatu(String tatuPath, int[] depthMap, int depthMapHeight, int depthMapWidth, float tatuSize) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap tatu = BitmapFactory.decodeFile(tatuPath, options);
        int[] transformedTatu = new int[depthMapHeight*depthMapWidth];
        int coeff = tatu.getHeight()/depthMapHeight;
        int[] nearestPixel = findNearestPixel(depthMap);
        int i_n = nearestPixel[0];
        int j_n = nearestPixel[1];
        int heightCoeff = coeff;
        for (int j = j_n; j < depthMapHeight; j++) {
            heightCoeff = processRow(depthMap, depthMapHeight, depthMapWidth, heightCoeff, i_n, j, tatu, transformedTatu);
        }
        heightCoeff = coeff;
        for (int j = j_n - 1; j >= 0; j--) {
            heightCoeff = processRow(depthMap, depthMapHeight, depthMapWidth, heightCoeff, i_n, j, tatu, transformedTatu);
        }
        return transformedTatu;
        //Bitmap result = Bitmap.createBitmap(transformedTatu, depthMapWidth, depthMapHeight, Bitmap.Config.ARGB_8888);
    }

    static int[] findNearestPixel(int[] depthMap) {
        return new int[]{1,1};
    }
}


