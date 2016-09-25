package com.sh1r0.caffe_android_demo;

import android.graphics.Bitmap;

/**
 * Created by shiro on 2014/9/22.
 */
public interface CNNListener {
    void onTaskCompleted(Bitmap result);
}
