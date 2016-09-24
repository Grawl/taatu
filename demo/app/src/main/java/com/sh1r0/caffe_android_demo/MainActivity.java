package com.sh1r0.caffe_android_demo;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.sh1r0.caffe_android_lib.CaffeMobile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;


public class MainActivity extends Activity implements CNNListener {
    private static final String LOG_TAG = "MainActivity";
    private static final int REQUEST_IMAGE_CAPTURE = 100;
    private static final int REQUEST_IMAGE_SELECT = 200;
    public static final int MEDIA_TYPE_IMAGE = 1;
    private static String[] IMAGENET_CLASSES;

    private Button btnCamera;
    private Button btnSelect;
    private ImageView ivCaptured;
    private TextView tvLabel;
    private Uri fileUri;
    private ProgressDialog dialog;
    private Bitmap bmp;
    private CaffeMobile caffeMobile;
    File sdcard = Environment.getExternalStorageDirectory();
    String modelDir = sdcard.getAbsolutePath() + "/caffe_mobile/our_model/";
    String modelProto = modelDir + "model_norm_abs_100k.prototxt";
    String modelBinary = modelDir + "model_norm_abs_100k.caffemodel";

    static {
        System.loadLibrary("caffe");
        System.loadLibrary("caffe_jni");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ivCaptured = (ImageView) findViewById(R.id.ivCaptured);
        tvLabel = (TextView) findViewById(R.id.tvLabel);

        btnCamera = (Button) findViewById(R.id.btnCamera);
        btnCamera.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                initPrediction();
                fileUri = getOutputMediaFileUri(MEDIA_TYPE_IMAGE);
                Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                i.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
                startActivityForResult(i, REQUEST_IMAGE_CAPTURE);
            }
        });

        btnSelect = (Button) findViewById(R.id.btnSelect);
        btnSelect.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                initPrediction();
                Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(i, REQUEST_IMAGE_SELECT);
            }
        });

        // TODO: implement a splash screen(?
        caffeMobile = new CaffeMobile();
        caffeMobile.setNumThreads(4);
        caffeMobile.loadModel(modelProto, modelBinary);
        float[] meanValues = {127, 127, 127};
        caffeMobile.setMean(meanValues);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if ((requestCode == REQUEST_IMAGE_CAPTURE || requestCode == REQUEST_IMAGE_SELECT) && resultCode == RESULT_OK) {
            String imgPath;

            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                imgPath = fileUri.getPath();
            } else {
                Uri selectedImage = data.getData();
                String[] filePathColumn = {MediaStore.Images.Media.DATA};
                Cursor cursor = MainActivity.this.getContentResolver().query(selectedImage, filePathColumn, null, null, null);
                cursor.moveToFirst();
                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                imgPath = cursor.getString(columnIndex);
                cursor.close();
            }

            bmp = BitmapFactory.decodeFile(imgPath);
            Log.d(LOG_TAG, imgPath);
            Log.d(LOG_TAG, String.valueOf(bmp.getHeight()));
            Log.d(LOG_TAG, String.valueOf(bmp.getWidth()));

            dialog = ProgressDialog.show(MainActivity.this, "Predicting...", "Wait for one sec...", true);

            CNNTask cnnTask = new CNNTask(MainActivity.this);
            cnnTask.execute(imgPath);
        } else {
            btnCamera.setEnabled(true);
            btnSelect.setEnabled(true);
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void initPrediction() {
        btnCamera.setEnabled(false);
        btnSelect.setEnabled(false);
        tvLabel.setText("");
    }

    private class CNNTask extends AsyncTask<String, Void, int[]> {
        private CNNListener listener;
        private long startTime;

        public CNNTask(CNNListener listener) {
            this.listener = listener;
        }

        @Override
        protected int[] doInBackground(String... strings) {
            startTime = SystemClock.uptimeMillis();
            Bitmap bitmap = BitmapFactory.decodeFile(strings[0]);
            Bitmap scaled = Bitmap.createScaledBitmap(bitmap, 298, 218, false);
            scaled.setHasAlpha(false);
            byte[] array = getImagePixels(scaled);
            float[][] predicted = caffeMobile.extractFeatures(array, 74, 54, "depth-refine");
            int[] result = new int[predicted[0].length];
            for (int i = 0; i < result.length; i++) {
                result[i] = ((int)(predicted[0][i]*255));
                //result[i] = 0xff << 24 | ((int)(predicted[0][i]*255)) << 16 |  ((int)(predicted[0][i]*255)) << 8 | ((int)(predicted[0][i]*255));
            }
            Bitmap tatu = BitmapFactory.decodeResource(((Activity)this.listener).getResources(), R.drawable.tatu1);
            tatu = Bitmap.createScaledBitmap(tatu, 54*2, 74*2, false);
            return Transformer.makeTatu(tatu, result, 74, 54, 2.0f);
            //return result;
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

        @Override
        protected void onPostExecute(int[] result) {
            Log.i(LOG_TAG, String.format("elapsed wall time: %d ms", SystemClock.uptimeMillis() - startTime));
            listener.onTaskCompleted(result);
            super.onPostExecute(result);
        }
    }

    @Override
    public void onTaskCompleted(int[] result) {
        //ivCaptured.setImageBitmap(bmp);
        Bitmap bitmap = Bitmap.createBitmap(result, 74, 54, Bitmap.Config.ARGB_8888);
        ivCaptured.setScaleType(ImageView.ScaleType.FIT_CENTER);
        ivCaptured.setImageBitmap(bitmap);

        //tvLabel.setText(IMAGENET_CLASSES[result]);
        btnCamera.setEnabled(true);
        btnSelect.setEnabled(true);

        if (dialog != null) {
            dialog.dismiss();
        }
    }

    /**
     * Create a file Uri for saving an image or video
     */
    private static Uri getOutputMediaFileUri(int type) {
        return Uri.fromFile(getOutputMediaFile(type));
    }

    /**
     * Create a File for saving an image or video
     */
    private static File getOutputMediaFile(int type) {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "Caffe-Android-Demo");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_" + timeStamp + ".jpg");
        } else {
            return null;
        }

        return mediaFile;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
