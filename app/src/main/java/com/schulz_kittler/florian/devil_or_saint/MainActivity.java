package com.schulz_kittler.florian.devil_or_saint;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.hardware.Camera;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.iid.InstanceID;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.Landmark;
import com.google.android.gms.vision.face.LargestFaceFocusingProcessor;
import com.schulz_kittler.florian.devil_or_saint.camera.CameraSourcePreview;
import com.schulz_kittler.florian.devil_or_saint.camera.GraphicOverlay;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Activity for the face tracker app.  This app detects faces with the rear facing camera, and draws
 * overlay graphics to indicate the position, size, and ID of each face.
 */
public final class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private CameraSource mCameraSource = null;
    private CameraSourcePreview mPreview;
    private GraphicOverlay mGraphicOverlay;
    private Button bDevil;
    private Button bSaint;
    private Button bDone;
    private String rsFaceID;
    private String instanceID;
    private FaceGraphic fGraphic;
    //private SurfaceView cspSurfaceView;
    //private CameraDevice cameraDevice;
    //private CaptureRequest.Builder captureRequestBuilder;
    //private CameraCaptureSession cameraCaptureSession;

    private static final int RC_HANDLE_GMS = 9001;
    // permission request codes need to be < 256
    private static final int REQUEST_CAMERA = 1;
    private static final int REQUEST_CAMERA_STORAGE_PERM = 2;
    //private static final String PYTHON_SERVER_URL = "http://schulz.pythonanywhere.com/bafacerec";

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static
    {
        ORIENTATIONS.append(Surface.ROTATION_0,90);
        ORIENTATIONS.append(Surface.ROTATION_90,0);
        ORIENTATIONS.append(Surface.ROTATION_180,270);
        ORIENTATIONS.append(Surface.ROTATION_270,180);
    }

    //==============================================================================================
    // Activity Methods
    //==============================================================================================

    /**
     * Initializes the UI and initiates the creation of a face detector.
     */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.activity_main);

        mPreview = (CameraSourcePreview) findViewById(R.id.preview);
        mGraphicOverlay = (GraphicOverlay) findViewById(R.id.faceOverlay);
        bDevil = (Button) findViewById(R.id.btnDevil);
        bSaint = (Button) findViewById(R.id.btnSaint);
        bDone = (Button) findViewById(R.id.btnDone);

        bDevil.setEnabled(false);
        bDevil.setClickable(false);
        bSaint.setEnabled(false);
        bSaint.setClickable(false);
        bDone.setClickable(false);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR); //Shut off screen rotation

        instanceID = InstanceID.getInstance(getApplicationContext()).getId();
        //instanceID = "abcdefghij2";

        //cspSurfaceView = mPreview.getSurfaceView();

        // Check for the camera permission before accessing the camera.  If the
        // permission is not granted yet, request permission.
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            createCameraSource();
        } else {
            requestAllPermissions();
        }
    }

    /**
     * Highlights the Devil button and sends vote to Server
     * @param view
     */
    public void onClickDevil(View view) {
        new UploadVote(rsFaceID, instanceID, "1").execute();
        changeButtonStatus(1);
    }

    /**
     * Highlights the Saint button and sends vote to Server
     * @param view
     */
    public void onClickSaint(View view) {
        new UploadVote(rsFaceID, instanceID, "2").execute();
        changeButtonStatus(2);
    }

    /**
     * Closes the image and resumes the Camera to detect the next face
     * @param view
     */
    public void onClickDone(View view) {
        restartLifeCycle();
    }

    public void restartLifeCycle() {
        //reset Buttons to default
        bDevil.setBackgroundColor(Color.argb(255,255,68,68));
        bSaint.setBackgroundColor(Color.argb(255, 255, 187, 51));

        bDevil.setEnabled(false);
        bSaint.setEnabled(false);
        bDevil.setClickable(false);
        bSaint.setClickable(false);
        bDone.setClickable(false);
        bDone.setVisibility(Button.INVISIBLE);

        fGraphic.setDevilOrSaint(0);
        onResume();
    }

    /**
     * Enables or disables the two Buttons and sets the previously selected button
     * @param vote can only have four different values:
     *             0 = face not rated previously
     *             1 = face rated with devil previously
     *             2 = face rated with saint previously
     *             3 = error
     */
    public void changeButtonStatus(Integer vote) {
        if (vote == 0) {
            bDevil.setEnabled(true);
            bSaint.setEnabled(true);
            bDevil.setClickable(true);
            bSaint.setClickable(true);

            bDone.setClickable(false);
            bDone.setVisibility(Button.INVISIBLE);

            bDevil.setBackgroundColor(Color.argb(255,255,68,68));
            bSaint.setBackgroundColor(Color.argb(255, 255, 187, 51));
        } else if (vote == 1) {
            //disable buttons if previously voted
            bDevil.setEnabled(false);
            bSaint.setEnabled(false);
            bDevil.setClickable(false);
            bSaint.setClickable(false);

            bDone.setClickable(true);
            bDone.setVisibility(Button.VISIBLE);

            //last choice is highlighted
            bDevil.setBackgroundColor(Color.argb(255,255,68,68));
            bSaint.setBackgroundColor(Color.GRAY);
        } else if (vote == 2) {
            //disable buttons if previously voted
            bDevil.setEnabled(false);
            bSaint.setEnabled(false);
            bDevil.setClickable(false);
            bSaint.setClickable(false);

            bDone.setClickable(true);
            bDone.setVisibility(Button.VISIBLE);

            //last choice is highlighted
            bDevil.setBackgroundColor(Color.GRAY);
            bSaint.setBackgroundColor(Color.argb(255, 255, 187, 51));
        } else {
            Log.e(TAG, "Error: Vote returned wrong value.");
        }
    }

    /**
     * The recognized face ID is passed to the MainActivity
     * @param faceID a random ID inside a string.
     */
    public void setButtonVariable(String faceID, FaceGraphic fg) {
        rsFaceID = faceID;
        fGraphic = fg;
    }

    /**
     * Handles the requesting of the camera permission.  This includes
     * showing a "Snackbar" message of why the permission is needed then
     * sending the request.
     */
    private void requestAllPermissions() {
        Log.w(TAG, "Camera or Storage permission is not granted. Requesting permissions");

        final String[] permissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.INTERNET};

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA) || !ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) || !ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.INTERNET)) {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CAMERA_STORAGE_PERM);
            return;
        }

        final Activity thisActivity = this;

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityCompat.requestPermissions(thisActivity, permissions,
                        REQUEST_CAMERA_STORAGE_PERM);
            }
        };

        Snackbar.make(mGraphicOverlay, R.string.permission_camera_storage_rationale,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.ok, listener)
                .show();
    }

    /**
     * Creates and starts the camera.  Note that this uses a higher resolution in comparison
     * to other detection examples to enable the barcode detector to detect small barcodes
     * at long distances.
     */
    private void createCameraSource() {

        Context context = getApplicationContext();
        FaceDetector detector = new FaceDetector.Builder(context)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .setMode(FaceDetector.ACCURATE_MODE)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .setProminentFaceOnly(true)
                .setTrackingEnabled(true)
                .build();

        detector.setProcessor(new LargestFaceFocusingProcessor(detector, new FaceTracker()));

        /*detector.setProcessor(
                new MultiProcessor.Builder<>(new GraphicFaceTrackerFactory())
                        .build());*/

        if (!detector.isOperational()) {
            // Note: The first time that an app using face API is installed on a device, GMS will
            // download a native library to the device in order to do detection.  Usually this
            // completes before the app is run for the first time.  But if that download has not yet
            // completed, then the above call will not detect any faces.
            //
            // isOperational() can be used to check if the required native library is currently
            // available.  The detector will automatically become operational once the library
            // download completes on device.
            Log.w(TAG, "Face detector dependencies are not yet available.");
        }

        mCameraSource = new CameraSource.Builder(context, detector)
                .setRequestedPreviewSize(640, 480)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setRequestedFps(30.0f)
                .build();
    }

    /**
     * Restarts the camera.
     */
    @Override
    protected void onResume() {
        super.onResume();

        startCameraSource();
    }

    /**
     * Stops the camera.
     */
    @Override
    protected void onPause() {
        super.onPause();
        mPreview.stop();
    }

    /**
     * Releases the resources associated with the camera source, the associated detector, and the
     * rest of the processing pipeline.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCameraSource != null) {
            mCameraSource.release();
        }
    }

    /**
     * Callback for the result from requesting permissions. This method
     * is invoked for every call on {@link #requestPermissions(String[], int)}.
     * Note: It is possible that the permissions request interaction
     * with the user is interrupted. In this case you will receive empty permissions
     * and results arrays which should be treated as a cancellation.
     *
     * @param requestCode  The request code passed in {@link #requestPermissions(String[], int)}.
     * @param permissions  The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *                     which is either {@link PackageManager#PERMISSION_GRANTED}
     *                     or {@link PackageManager#PERMISSION_DENIED}. Never null.
     * @see #requestPermissions(String[], int)
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch(requestCode) {
            /*case REQUEST_CAMERA: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length != 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission granted
                    Log.d(TAG, "Storage permission granted!");
                } else {

                    // permission denied
                    Log.d(TAG, "Storage permission denied!");
                }
                return;
            }*/
            case REQUEST_CAMERA_STORAGE_PERM: {
                Map<String, Integer> perms = new HashMap<String, Integer>();
                perms.put(Manifest.permission.CAMERA, PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.WRITE_EXTERNAL_STORAGE, PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.INTERNET, PackageManager.PERMISSION_GRANTED);

                for(int i = 0; i < permissions.length; i++) {
                    perms.put(permissions[i], grantResults[i]);
                }

                if(perms.get(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                        && perms.get(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                        && perms.get(Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED) {

                    createCameraSource();
                } else {

                    Toast.makeText(MainActivity.this, "Some Permission is Denied", Toast.LENGTH_SHORT).show();
                }

                /*if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Camera permission granted - initialize the camera source");
                    // we have permission, so create the camerasource
                    createCameraSource();
                } else {
                    Log.e(TAG, "Permission not granted: results len = " + grantResults.length +
                            " Result code = " + (grantResults.length > 0 ? grantResults[0] : "(empty)"));

                    DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            finish();
                        }
                    };

                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Face Tracker sample")
                            .setMessage(R.string.no_camera_permission)
                            .setPositiveButton(R.string.ok, listener)
                            .show();
                }
                return;*/
            }
            default: {
                Log.d(TAG, "Got unexpected permission result: " + requestCode);
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            }
        }
    }

    //==============================================================================================
    // Camera Source Preview
    //==============================================================================================

    /**
     * Starts or restarts the camera source, if it exists.  If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private void startCameraSource() {

        // check that the device has play services available.
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                getApplicationContext());
        if (code != ConnectionResult.SUCCESS) {
            Dialog dlg =
                    GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS);
            dlg.show();
        }

        if (mCameraSource != null) {
            try {
                mPreview.start(mCameraSource, mGraphicOverlay);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                mCameraSource.release();
                mCameraSource = null;
            }
        }
    }

    /*private void SaveImage(Bitmap finalBitmap) {

        String root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString();
        File myDir = new File(root + "/Devil_or_Saint");
        myDir.mkdirs();
        Random generator = new Random();
        int n = 10000;
        n = generator.nextInt(n);
        String fname = "DoS-"+ n +".jpg";
        File file = new File (myDir, fname);
        if (file.exists ()) file.delete ();
        try {
            FileOutputStream out = new FileOutputStream(file);
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public  boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.v(TAG,"Permission is granted");
                return true;
            } else {

                Log.v(TAG,"Permission is revoked");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2);
                return false;
            }
        }
        else { //permission is automatically granted on sdk<23 upon installation
            Log.v(TAG,"Permission is granted");
            return true;
        }
    }*/

    //==============================================================================================
    // My Face Tracker
    //==============================================================================================

    public class FaceTracker extends Tracker<Face> {
        private GraphicOverlay mOverlay = mGraphicOverlay;
        private FaceGraphic mFaceGraphic = new FaceGraphic(mOverlay, getApplicationContext());
        private int countA = 0;

        @Override
        public void onNewItem(int faceId, Face item) {
            mGraphicOverlay.setId(faceId);
        }

        @Override
        public void onUpdate(Detector.Detections<Face> detections, Face face) {
            boolean leftEye = false;
            boolean rightEye = false;
            for (Landmark landmark : face.getLandmarks()) {
                if(landmark.getType() == Landmark.LEFT_EYE) {
                    leftEye = true;
                }
                if(landmark.getType() == Landmark.RIGHT_EYE) {
                    rightEye = true;
                }
            }

            if (leftEye && rightEye) {
                if (countA > 6) {
                    mOverlay.add(mFaceGraphic);
                    takePicture(face);
                    //mFaceGraphic.setDevilOrSaint(1);
                    mFaceGraphic.updateFace(face);
                    countA = 0;
                } else {
                    try {
                        TimeUnit.MILLISECONDS.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    countA++;
                }
            }
        }

        /**
         * Hide the graphic when the corresponding face was not detected.  This can happen for
         * intermediate frames temporarily (e.g., if the face was momentarily blocked from
         * view).
         */
        @Override
        public void onMissing(FaceDetector.Detections<Face> detectionResults) {
            mOverlay.remove(mFaceGraphic);
        }

        /**
         * Called when the face is assumed to be gone for good. Remove the graphic annotation from
         * the overlay.
         */
        @Override
        public void onDone() {
            mOverlay.remove(mFaceGraphic);
        }

        /**
         * Takes a picture of the current Frame.
         *
         * @param face The detected Face with Landmarks
         */
        public void takePicture(final Face face) {
            mCameraSource.takePicture(null, new CameraSource.PictureCallback() {
                @Override
                public void onPictureTaken(byte[] bytes) {
                    Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

                    bmp = rotateClockBitmap(bmp, 90);
                    //Bitmap grayFace = grayFaceBitmap(bmp, face);
                    //Log.d("BITMAP", bmp.getWidth() + "x" + bmp.getHeight());

                    new UploadFileToServer(MainActivity.this, mFaceGraphic, face, instanceID).execute(bmp);

                    Bitmap tmp2 = bmp.copy(Bitmap.Config.RGB_565,true);
                    onPause();
                    Canvas canvas = new Canvas(tmp2);
                    mPreview.getSurfaceView().draw(canvas);

                }
            });
        }

        /**
         *
         * @param original  Bitmap of a picture taken by the camera.
         * @param degrees   degrees by which the picture is rotated clockwise.
         * @return          rotated bitmap is returned.
         */
        public Bitmap rotateClockBitmap(Bitmap original, float degrees) {
            int width = original.getWidth();
            int height = original.getHeight();

            Matrix matrix = new Matrix();
            matrix.postRotate(degrees);

            //Bitmap scaledBitmap = Bitmap.createScaledBitmap(original, width, height, true);
            original = Bitmap.createBitmap(original, 0, 0, width, height, matrix, true);
            /*Canvas canvas = new Canvas(rotatedBitmap);
            canvas.drawBitmap(original, 0.0f, 0.0f, null);*/

            return original;
        }

        /*public Bitmap grayFaceBitmap(Bitmap original, Face face) {
            float bitWidth = original.getWidth();
            float bitHeight = original.getHeight();
            int dispWidth = 480;
            int dispHeight = 640;

            float scaleX = bitWidth/dispWidth;
            float scaleY = bitHeight/dispHeight;
            float widthScaled = face.getWidth()*scaleX;
            float heightScaled = face.getHeight()*scaleY;
            int x = Math.round(face.getPosition().x);
            int y = Math.round(face.getPosition().y);
            int wCrop = 0;
            int hCrop = 0;
            if (x < 0) {
                wCrop = Math.round(widthScaled - x);
                x=0;
            } else {
                wCrop = Math.round(widthScaled);
            }
            if (y < 0) {
                hCrop = Math.round(heightScaled - y);
                y=0;
            } else {
                hCrop = Math.round(heightScaled);
            }

            Log.d("BITMAP", "GrayFace --- x: " + x + " y: " + y + " scaled: width x height: " + widthScaled + "x" + heightScaled);
            Bitmap grayFaceImage = Bitmap.createBitmap(original, x, y, wCrop, hCrop);

            Bitmap grayFaceImage = Bitmap.createBitmap(original.getWidth(), original.getHeight(), Bitmap.Config.RGB_565);
            Canvas c = new Canvas(grayFaceImage);
            Paint paint = new Paint();
            ColorMatrix cm = new ColorMatrix();
            cm.setSaturation(0);
            ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
            paint.setColorFilter(f);
            c.drawBitmap(original, 0, 0, paint);

            return grayFaceImage;
        }*/
    }

    //==============================================================================================
    // Graphic Face Tracker
    //==============================================================================================

    /**
     * Factory for creating a face tracker to be associated with a new face.  The multiprocessor
     * uses this factory to create face trackers as needed -- one for each individual.
     */
   /* private class GraphicFaceTrackerFactory implements MultiProcessor.Factory<Face> {
        @Override
        public Tracker<Face> create(Face face) {
            return new GraphicFaceTracker(mGraphicOverlay);
        }
    }

    *//**
     * Face tracker for each detected individual. This maintains a face graphic within the app's
     * associated face overlay.
     *//*
    private class GraphicFaceTracker extends Tracker<Face> {
        private GraphicOverlay mOverlay;
        private FaceGraphic mFaceGraphic;

        GraphicFaceTracker(GraphicOverlay overlay) {
            mOverlay = overlay;
            mFaceGraphic = new FaceGraphic(overlay, getApplicationContext());
        }

        *//**
         * Start tracking the detected face instance within the face overlay.
         *//*
        @Override
        public void onNewItem(int faceId, Face item) {
            mFaceGraphic.setId(faceId);
        }

        *//**
         * Update the position/characteristics of the face within the overlay.
         *//*
        @Override
        public void onUpdate(FaceDetector.Detections<Face> detectionResults, Face face) {
            mOverlay.add(mFaceGraphic);
            mFaceGraphic.updateFace(face);
        }

        *//**
         * Hide the graphic when the corresponding face was not detected.  This can happen for
         * intermediate frames temporarily (e.g., if the face was momentarily blocked from
         * view).
         *//*
        @Override
        public void onMissing(FaceDetector.Detections<Face> detectionResults) {
            mOverlay.remove(mFaceGraphic);
        }

        *//**
         * Called when the face is assumed to be gone for good. Remove the graphic annotation from
         * the overlay.
         *//*
        @Override
        public void onDone() {
            mOverlay.remove(mFaceGraphic);
        }
    }*/
}