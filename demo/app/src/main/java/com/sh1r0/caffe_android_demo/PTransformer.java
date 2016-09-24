package com.sh1r0.caffe_android_demo;

import android.graphics.Bitmap;

/**
 * Created by ag3r on 9/25/2016.
 */
public class PTransformer {
    public static int[] makeTatu(int[] depthMap, int depthMapWidth, int depthMapHeight, Bitmap tatu) {
        int c_i = tatu.getWidth()/2;
        int c_j = tatu.getHeight()/2;
        int[] result = new int[tatu.getHeight()*tatu.getWidth()];
        int[] vectorMapX = new int[tatu.getHeight()*tatu.getWidth()];
        int[] vectorMapY = new int[tatu.getHeight()*tatu.getWidth()];
        for (int i = 0; i < tatu.getWidth(); i++) {
            for (int j = 0; j < tatu.getHeight(); j++) {
                vectorMapX[i*tatu.getHeight() + j] = x
            }
        }
        return null;
    }

    public static void processRow() {

    }

    public static int[] transformPixels(int[] pixels) {

    }
}
