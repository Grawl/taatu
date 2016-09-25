package com.sh1r0.caffe_android_demo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;


import com.sh1r0.caffe_android_lib.CaffeMobile;
import com.sh1r0.caffe_android_lib.CameraPreview;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

public class SCamera extends AppCompatActivity {

    private Preview2 mPreview;
    Camera mCamera;
    int numberOfCameras;
    int cameraCurrentlyLocked;
    // The first rear facing camera
    int defaultCameraId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Hide the window title.
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        super.onCreate(savedInstanceState);


        mPreview = new Preview2(this);
        setContentView(R.layout.activity_scamera);
        FrameLayout layout = (FrameLayout)findViewById(R.id.scamera_frame);
        layout.addView(mPreview);
        //setContentView(mPreview);
        // Find the total number of cameras available
        numberOfCameras = Camera.getNumberOfCameras();

        // Find the ID of the default camera
        CameraInfo cameraInfo = new CameraInfo();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == CameraInfo.CAMERA_FACING_BACK) {
                defaultCameraId = i;
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Open the default i.e. the first rear facing camera.
        mCamera = Camera.open();
        cameraCurrentlyLocked = defaultCameraId;
        mPreview.setCamera(mCamera);
    }
    @Override
    protected void onPause() {
        super.onPause();
        // Because the Camera object is a shared resource, it's very
        // important to release it when the activity is paused.
        if (mCamera != null) {
            mPreview.setCamera(null);
            mCamera.release();
            mCamera = null;
        }
    }


}

// ----------------------------------------------------------------------
/**
 * A simple wrapper around a Camera and a SurfaceView that renders a centered preview of the Camera
 * to the surface. We need to center the SurfaceView because not all devices have cameras that
 * support preview sizes at the same aspect ratio as the device's display.
 */
class Preview2 extends ViewGroup implements SurfaceHolder.Callback, Camera.PreviewCallback, CNNListener {
    private final String TAG = "Preview";
    SurfaceView mSurfaceView;
    SurfaceHolder mHolder;
    Camera.Size mPreviewSize;
    List<Camera.Size> mSupportedPreviewSizes;
    Camera mCamera;
    private CaffeMobile caffeMobile;
    File sdcard = Environment.getExternalStorageDirectory();
    String modelDir = sdcard.getAbsolutePath() + "/caffe_mobile/our_model/";
    String modelProto = modelDir + "model_norm_abs_100k.prototxt";
    String modelBinary = modelDir + "model_norm_abs_100k.caffemodel";
    Context context;
    int counter = 0;
    static {
        System.loadLibrary("caffe");
        System.loadLibrary("caffe_jni");
    }

    Preview2(Context context) {
        super(context);
        this.context = context;
        mSurfaceView = new SurfaceView(context);
        addView(mSurfaceView);
        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = mSurfaceView.getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        caffeMobile = new CaffeMobile();
        caffeMobile.setNumThreads(4);
        caffeMobile.loadModel(modelProto, modelBinary);
        float[] meanValues = {1, 1, 1};
        caffeMobile.setMean(meanValues);
    }

    public void setCamera(Camera camera) {
        mCamera = camera;
        if (mCamera != null) {
            mCamera.setDisplayOrientation(90);
            mSupportedPreviewSizes = mCamera.getParameters().getSupportedPreviewSizes();
            requestLayout();
        }
    }

    public void switchCamera(Camera camera) {
        System.out.println("Si entro");
        setCamera(camera);
        try {
            camera.setPreviewDisplay(mHolder);
            camera.setPreviewCallback(this);
        } catch (IOException exception) {
            Log.e(TAG, "IOException caused by setPreviewDisplay()", exception);
        }
        Camera.Parameters parameters = camera.getParameters();
        parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
        parameters.set("orientation", "portrait");
        requestLayout();
        camera.setParameters(parameters);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // We purposely disregard child measurements because act as a
        // wrapper to a SurfaceView that centers the camera preview instead
        // of stretching it.
        final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        final int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);
        setMeasuredDimension(width, height);
        if (mSupportedPreviewSizes != null) {
            mPreviewSize = getOptimalPreviewSize(mSupportedPreviewSizes, width, height);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (changed && getChildCount() > 0) {
            final View child = getChildAt(0);
            final int width = r - l;
            final int height = b - t;
            int previewWidth = width;
            int previewHeight = height;
            if (mPreviewSize != null) {
                previewWidth = mPreviewSize.width;
                previewHeight = mPreviewSize.height;
            }

            double ratio = previewWidth / previewHeight;


            System.out.println(previewWidth + "x" + previewHeight);

            System.out.println(width + "x" + height);
            // Center the child SurfaceView within the parent.

            child.layout(0, 0, width, height);
            return;

            /*
            if (width * previewHeight > height * previewWidth) {
                final int scaledChildWidth = previewWidth * height / previewHeight;
                child.layout((width - scaledChildWidth) / 2, 0,
                        (width + scaledChildWidth) / 2, height);
            } else {
                final int scaledChildHeight = previewHeight * width / previewWidth;
                child.layout(0, (height - scaledChildHeight) / 2,
                        width, (height + scaledChildHeight) / 2);
            }*/


        }
    }

    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, acquire the camera and tell it where
        // to draw.
        try {
            if (mCamera != null) {
                mCamera.setPreviewDisplay(holder);
                mCamera.setPreviewCallback(this);
            }
        } catch (IOException exception) {
            Log.e(TAG, "IOException caused by setPreviewDisplay()", exception);
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // Surface will be destroyed when we return, so stop the preview.
        if (mCamera != null) {
            mCamera.stopPreview();
        }
    }

    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) w / h;
        if (sizes == null) return null;
        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;
        int targetHeight = h;
        // Try to find an size match aspect ratio and size
        for (Camera.Size size : sizes) {
            //System.out.println(size.width + "x" + size.height);
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        //System.out.println("No good sizes");
        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return sizes.get(16);
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // Now that the size is known, set up the camera parameters and begin
        // the preview.
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
        parameters.set("orientation", "portrait");
        requestLayout();
        mCamera.setParameters(parameters);
        mCamera.startPreview();
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        counter++;
        if (counter%200 == 1) {
            CNNTask cnnTask = new CNNTask(this);
            cnnTask.execute(data);
        }
    }

    private class CNNTask extends AsyncTask<byte[], Void, Bitmap> {
        private CNNListener listener;
        private long startTime;

        public CNNTask(CNNListener listener) {
            this.listener = listener;
        }

        @Override
        protected Bitmap doInBackground(byte[]... image) {
            startTime = SystemClock.uptimeMillis();

            Preview2 preview = (Preview2)listener;

            int[] pixels = new int[preview.mPreviewSize.width * preview.mPreviewSize.height];

            decodeYUV420SP(pixels, image[0], preview.mPreviewSize.width, preview.mPreviewSize.height);

            Bitmap bitmap = Bitmap.createBitmap(pixels, preview.mPreviewSize.width, preview.mPreviewSize.height, Bitmap.Config.RGB_565);
            Bitmap scaled = Bitmap.createScaledBitmap(bitmap, 298, 216, false);

            String file_path = Environment.getExternalStorageDirectory().getAbsolutePath() +
                    "/Images";
            File dir = new File(file_path);
            if(!dir.exists())
                dir.mkdirs();
            String filename = "image" + counter + ".png";
            try {
                File file = new File(dir, "image" + counter + ".png");
                FileOutputStream fOut = new FileOutputStream(file);

                scaled.compress(Bitmap.CompressFormat.JPEG, 90, fOut);
                fOut.flush();
                fOut.close();
            }
            catch (IOException e) {
                Log.d("ANDROID_DEMO","error");
            }

            float[][] predicted = caffeMobile.extractFeatures(file_path + "/" + filename, "depth-refine");
            int[] result = new int[predicted[0].length];
            for (int i = 0; i < result.length; i++) {
                float p = predicted[0][i];
                result[i] = ((int) (predicted[0][i] * 255));
                //result[i] = 0xff << 24 | ((int)(p*255)) << 16 |  ((int)(p*255)) << 8 | ((int)(p*255));
            }
            //return Bitmap.createBitmap(result, 74, 54, Bitmap.Config.ARGB_8888);

            Bitmap tatu = BitmapFactory.decodeResource(((Preview2)listener).context.getResources(), R.drawable.tatu3);
            tatu = Bitmap.createScaledBitmap(tatu, 74 *10, 54 * 10, false);
            return PTransformer.makeTatu(tatu, result, 54, 74);
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            Log.i("CAFFE_ANDROID_DEMO", String.format("elapsed wall time: %d ms", SystemClock.uptimeMillis() - startTime));
            listener.onTaskCompleted(result);
            super.onPostExecute(result);
        }

        void decodeYUV420SP(int[] rgb, byte[] yuv420sp, int width, int height) {

            final int frameSize = width * height;

            for (int j = 0, yp = 0; j < height; j++) {       int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
                for (int i = 0; i < width; i++, yp++) {
                    int y = (0xff & ((int) yuv420sp[yp])) - 16;
                    if (y < 0)
                        y = 0;
                    if ((i & 1) == 0) {
                        v = (0xff & yuv420sp[uvp++]) - 128;
                        u = (0xff & yuv420sp[uvp++]) - 128;
                    }

                    int y1192 = 1192 * y;
                    int r = (y1192 + 1634 * v);
                    int g = (y1192 - 833 * v - 400 * u);
                    int b = (y1192 + 2066 * u);

                    if (r < 0)                  r = 0;               else if (r > 262143)
                        r = 262143;
                    if (g < 0)                  g = 0;               else if (g > 262143)
                        g = 262143;
                    if (b < 0)                  b = 0;               else if (b > 262143)
                        b = 262143;

                    rgb[yp] = 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
                }
            }
        }

        public byte[] getImagePixels(Bitmap image) {
            // calculate how many bytes our image consists of
            int bytes = image.getByteCount();

            ByteBuffer buffer = ByteBuffer.allocate(bytes); // Create a new buffer
            image.copyPixelsToBuffer(buffer); // Move the byte data to the buffer

            byte[] temp = buffer.array(); // Get the underlying array containing the data.

            byte[] pixels = new byte[(temp.length / 4) * 3]; // Allocate for 3 byte BGR

            // Copy pixels into place
            for (int i = 0; i < (temp.length / 4); i++) {
                pixels[i * 3] = temp[i * 4 + 3];     // B
                pixels[i * 3 + 1] = temp[i * 4 + 2]; // G
                pixels[i * 3 + 2] = temp[i * 4 + 1]; // R

                // Alpha is discarded
            }

            return pixels;
        }

    }

    public static Bitmap RotateBitmap(Bitmap source, float angle)
    {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    @Override
    public void onTaskCompleted(Bitmap result) {
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER;
        ImageView imageView = new ImageView(this.context);
        imageView.setLayoutParams(params);
        imageView.setImageBitmap(RotateBitmap(result, 90.0f));
        FrameLayout layout = (FrameLayout)((Activity)this.context).findViewById(R.id.scamera_frame);
        layout.addView(imageView);
    }
}
