package com.example.ihyelan.opencvuse;

import android.annotation.TargetApi;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
//import androidx.support.annotation.NonNull;
//import android.support.v4.content.ContextCompat;
//import android.support.v7.app.AlertDialog;
//import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;


public class MainActivity extends AppCompatActivity
        implements CameraBridgeViewBase.CvCameraViewListener2, View.OnTouchListener {

    private static final String TAG = "opencv";
    private CameraBridgeViewBase mOpenCvCameraView;

    private Mat matInput;
    private Mat matResult;
    private boolean bSaveThisFrame = false;
    private int cameraNumber = 0;

    private int filterIndex = 0;
    private int totalFilterSize = 4;

    static final int PERMISSIONS_REQUEST_CODE = 1000;
    String[] PERMISSIONS = {"android.permission.CAMERA", "android.permission.WRITE_EXTERNAL_STORAGE"};

    private static final int SWIPE_THRESHOLD = 100;
    private static final int SWIPE_VELOCITY_THRESHOLD = 100;

    public native void ConvertRGBtoRGB(long matAddrInput, long matAddrResult);

    public native void ConvertRGBtoGray(long matAddrInput, long matAddrResult);

    public native void ConvertRGBtoSepia(long matAddrInput, long matAddrResult);

    public native void ConvertRGBtoSharpen(long matAddrInput, long matAddrResult);


    static {
        System.loadLibrary("opencv_java4");
        System.loadLibrary("native-lib");
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    mOpenCvCameraView.enableView();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //퍼미션 상태 확인
            if (!hasPermissions(PERMISSIONS)) {
                //퍼미션 허가 안되어있다면 사용자에게 요청
                requestPermissions(PERMISSIONS, PERMISSIONS_REQUEST_CODE);
            }
        }

        AdView mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        List<String> testDeviceIds = Arrays.asList("30257C08C20362ACDB1737F1A0FEAB9E");
        RequestConfiguration configuration =
                new RequestConfiguration.Builder().setTestDeviceIds(testDeviceIds).build();
        MobileAds.setRequestConfiguration(configuration);

        mOpenCvCameraView = findViewById(R.id.activity_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setCameraIndex(cameraNumber); // front-camera(1),  back-camera(0)
        mOpenCvCameraView.setOnTouchListener(this);

        mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);

        Button mCameraChangeButton = findViewById(R.id.camera_change_btn);
        Button mCameraShotButton = findViewById(R.id.camera_shot_btn);

        mCameraChangeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (cameraNumber == 1) cameraNumber = 0;
                else cameraNumber = 1;
                mOpenCvCameraView.setCameraIndex(cameraNumber);
                mOpenCvCameraView.disableView();
                mOpenCvCameraView.enableView();
            }
        });

        mCameraShotButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bSaveThisFrame = true;
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "onResume :: Internal OpenCV library not found.");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "onResum :: OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();

        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
    }

    @Override
    public void onCameraViewStopped() {
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        matInput = inputFrame.rgba();
        if (cameraNumber == 1) {
            Core.flip(matInput, matInput, 1);
        }

        //if ( matResult != null ) matResult.release(); fix 2018. 8. 18
        if (matResult == null)
            matResult = new Mat(matInput.rows(), matInput.cols(), matInput.type());

        if (filterIndex == 0) {
            ConvertRGBtoRGB(matInput.getNativeObjAddr(), matResult.getNativeObjAddr());
        }
        else if (filterIndex == 1) {
            ConvertRGBtoSepia(matInput.getNativeObjAddr(), matResult.getNativeObjAddr());
        }
        else if (filterIndex == 2) {
            ConvertRGBtoGray(matInput.getNativeObjAddr(), matResult.getNativeObjAddr());
        } else {
            ConvertRGBtoSharpen(matInput.getNativeObjAddr(), matResult.getNativeObjAddr());
        }

        if (bSaveThisFrame) {
            Bitmap bmp = null;
            try {
                Imgproc.cvtColor(matResult, matResult, Imgproc.COLOR_RGB2RGBA, 4);

                Matrix matrix = new Matrix();
                matrix.postRotate(90);
                bmp = Bitmap.createBitmap(matResult.cols(), matResult.rows(), Bitmap.Config.ARGB_8888);

                Matrix m = new Matrix();
                m.setRotate(90, (float) bmp.getWidth() / 2, (float) bmp.getHeight() / 2);
                Utils.matToBitmap(matResult, bmp);
                bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), m, true);

            } catch (CvException e) {
                Log.d("Exception", e.getMessage());
            }
            savePNGImageToGallery(bmp, this, "RanFilter_" + System.currentTimeMillis() + ".png");
            bSaveThisFrame = false;
        }
        return matResult;
    }


    private boolean hasPermissions(String[] permissions) {
        int result;

        //스트링 배열에 있는 퍼미션들의 허가 상태 여부 확인
        for (String perms : permissions) {
            result = ContextCompat.checkSelfPermission(this, perms);
            if (result == PackageManager.PERMISSION_DENIED) {
                //허가 안된 퍼미션 발견
                return false;
            }
        }
        //모든 퍼미션이 허가되었음
        return true;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {

            case PERMISSIONS_REQUEST_CODE:
                if (grantResults.length > 0) {
                    boolean cameraPermissionAccepted = grantResults[0]
                            == PackageManager.PERMISSION_GRANTED;

                    if (!cameraPermissionAccepted)
                        showDialogForPermission("앱을 실행하려면 퍼미션을 허가하셔야합니다.");
                }
                break;
        }
    }


    @TargetApi(Build.VERSION_CODES.M)
    private void showDialogForPermission(String msg) {

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("알림");
        builder.setMessage(msg);
        builder.setCancelable(false);
        builder.setPositiveButton("예", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                requestPermissions(PERMISSIONS, PERMISSIONS_REQUEST_CODE);
            }
        });
        builder.setNegativeButton("아니오", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {
                finish();
            }
        });
        builder.create().show();
    }

    // Save the processed image as a PNG file on the SD card and shown in the Android Gallery.
    protected void savePNGImageToGallery(Bitmap bmp, Context context, String baseFilename) {
        try {
            // Get the file path to the SD card.
            String baseFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath() + "/";
            File file = new File(baseFolder + baseFilename);
            Log.i(TAG, "Saving the processed image to file [" + file.getAbsolutePath() + "]");

            // Open the file.
            OutputStream out = new BufferedOutputStream(new FileOutputStream(file));
            // Save the image file as PNG.
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();    // Make sure it is saved to file soon, because we are about to add it to the Gallery.
            out.close();

            // Add the PNG file to the Android Gallery.
            ContentValues image = new ContentValues();

            image.put(MediaStore.Images.Media.TITLE, baseFilename);
            image.put(MediaStore.Images.Media.DISPLAY_NAME, baseFilename);
            image.put(MediaStore.Images.Media.DESCRIPTION, "Processed by the Cartoonifier App");
            image.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis()); // Milliseconds since 1970 UTC.
            image.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            image.put(MediaStore.Images.Media.ORIENTATION, 0);
            image.put(MediaStore.Images.Media.DATA, file.getAbsolutePath());
            Uri result = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, image);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        Log.i(TAG, "onTouch down event");
//        bSaveThisFrame = true;
        filterIndex = (filterIndex + 1) % totalFilterSize;
        return false;
    }


//    @Override
//    public boolean onDown(MotionEvent e) {
//        return false;
//    }
//    @Override
//    public void onShowPress(MotionEvent e) {
//
//    }
//
//    @Override
//    public boolean onSingleTapUp(MotionEvent e) {
//        return false;
//    }
//
//    @Override
//    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
//        return false;
//    }
//
//    @Override
//    public void onLongPress(MotionEvent e) {
//
//    }
//
//    @Override
//    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
//        boolean result = false;
//        float diffY = e2.getY() - e1.getY();
//        float diffX = e2.getX() - e1.getX();
//        if (Math.abs(diffX) > Math.abs(diffY)) {
//            if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
//                if (diffX > 0) {
//                    Log.i("ranran", "ranran!!");
//                    //onSwipeRight();
//                } else {
//                    //onSwipeLeft();
//                }
//            }
//        }
//        return false;
//    }

}