package com.schulz_kittler.florian.devil_or_saint;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.iid.InstanceID;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
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
 * Activity for the face tracker app.  This app detects and recognizes faces and draws face filter overlays over them.
 * If enough positive votes are present, a halo is drawn above the head.
 * If enough negative votes are present, devil horns are drawn onto the forehead.
 * After the vote it is possible to provide further information about the face to earn credits.
 */
@SuppressWarnings("deprecation")
public final class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private CameraSource mCameraSource = null;
    private FaceDetector detector;
    private CameraSourcePreview mPreview;
    private GraphicOverlay mGraphicOverlay;
    private TableLayout lcontacts;
    private Button bDevil;
    private Button bSaint;
    private Button bDone;
    private Button bEdit;
    private String rsFaceID;    // ID of the detected Face
    private String instanceID;  // randomly generated ID. Identifies the App running on a device. Resets if device is factory reseted.
    private String checkContact;
    private int editUpdated;    // variable used to check if the edit button was clicked before
    private int cBackOrientation;
    private int cFrontOrientation;
    private int currentOrientation; // orientation to rotate the image correctly
    private int numberCameras;
    private int cameraFacing;
    private boolean lockPress;  // disables the functions to switch camera and take photo
    private boolean autoFaceDetection;  // enable or disable the function to automatically take a picture if a face is detected
    private EditText tbName;
    private EditText tbAdresse;
    private EditText tbTel;
    private FaceGraphic fGraphic;
    private FaceGraphic pFaceGraphic;

    private static final int RC_HANDLE_GMS = 9001;
    private static final int REQUEST_CAMERA_STORAGE_PERM = 2;

    /**
     * Initializes the UI, sets the visibility of all buttons and layouts,
     * checks the orientation of the cameras and initiates the creation of a face detector.
     */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.activity_main);

        // inits the Layouts, Textboxes and Buttons
        mPreview = (CameraSourcePreview) findViewById(R.id.preview);
        mGraphicOverlay = (GraphicOverlay) findViewById(R.id.faceOverlay);
        lcontacts = (TableLayout) findViewById(R.id.lcontact);
        tbName = (EditText) findViewById(R.id.tbname);
        tbAdresse = (EditText) findViewById(R.id.tbadresse);
        tbTel = (EditText) findViewById(R.id.tbtel);
        bDevil = (Button) findViewById(R.id.btnDevil);
        bSaint = (Button) findViewById(R.id.btnSaint);
        bDone = (Button) findViewById(R.id.btnDone);
        bEdit = (Button) findViewById(R.id.btnEdit);

        lcontacts.setVisibility(View.GONE);

        bDevil.setEnabled(false);
        bDevil.setClickable(false);
        bSaint.setEnabled(false);
        bSaint.setClickable(false);
        bDone.setClickable(false);
        bEdit.setClickable(false);

        editUpdated = 0;
        lockPress = false;
        autoFaceDetection = false;  // disables or enables the automatic photo feature
        cameraFacing = CameraSource.CAMERA_FACING_BACK; // starts the camera with the back facing camera

        mGraphicOverlay.setOnLongClickListener(new View.OnLongClickListener() {
            /**
             * switches the camera between facing back and facing front
             */
            @Override
            public boolean onLongClick(View v) {
                if (!lockPress && numberCameras > 1) {
                    mCameraSource.release();
                    detector.release();
                    if(cameraFacing == CameraSource.CAMERA_FACING_BACK) {
                        cameraFacing = CameraSource.CAMERA_FACING_FRONT;
                        currentOrientation = cFrontOrientation;
                    } else {
                        cameraFacing = CameraSource.CAMERA_FACING_BACK;
                        currentOrientation = cBackOrientation;
                    }
                    createCameraSource(cameraFacing);
                    startCameraSource();
                }
                Log.d(TAG, "Camera switched!");
                return true;
            }
        });

        // locks the device in portrait mode
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // get the correct orientation of the back facing camera
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(0, info);
        cBackOrientation = getCorrectCameraOrientation(info);

        // if a front facing camera exists, get the correct orientation
        numberCameras = Camera.getNumberOfCameras();
        if (numberCameras > 1) {
            Camera.getCameraInfo(1, info);
            cFrontOrientation = getCorrectCameraOrientation(info);
            Log.d(TAG, "BackOrientation: " + cBackOrientation + ", FrontOrientation: " + cFrontOrientation);
        }

        // sets the current camera orientation
        currentOrientation = cBackOrientation;

        // change this variable to simulate a vote from another device e.g.: instanceID = "abcdefghij2";
        instanceID = InstanceID.getInstance(getApplicationContext()).getId();

        // Check for the camera permission before accessing the camera.  If the
        // permission is not granted yet, request permission.
        // creates and starts camera afterwards.
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            createCameraSource(cameraFacing);
        } else {
            requestAllPermissions();
        }
    }

    /**
     * Resets all the variables needed to restart the lifecycle of the App
     */
    public void restartLifeCycle() {
        // reset Buttons to default
        bDevil.setBackgroundColor(Color.argb(255,255,68,68));
        bSaint.setBackgroundColor(Color.argb(255, 255, 187, 51));

        bDevil.setEnabled(false);
        bSaint.setEnabled(false);
        bDevil.setClickable(false);
        bSaint.setClickable(false);

        bDone.setClickable(false);
        bDone.setVisibility(Button.INVISIBLE);
        bEdit.setClickable(false);
        bEdit.setVisibility(View.INVISIBLE);

        // Clears Textboxes
        tbName.setText("");
        tbAdresse.setText("");
        tbTel.setText("");
        lcontacts.setVisibility(View.GONE);

        editUpdated = 0;
        lockPress = false;

        // removes Face filter
        if (fGraphic != null) {
            fGraphic.setDevilOrSaint(0);
        }
        // starts the camera
        onResume();
    }

    /**
     * Checks the camera orientation with the provided Parameter and returns the degrees to correctly rotate the image.
     * @param info the properties of the provided camera
     * @return  the degrees to correctly rotate the image if a photo has been taken.
     */
    public int getCorrectCameraOrientation(Camera.CameraInfo info) {

        int rotation = this.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;

        switch(rotation){
            case Surface.ROTATION_0:
                degrees = 0;
                break;

            case Surface.ROTATION_90:
                degrees = 90;
                break;

            case Surface.ROTATION_180:
                degrees = 180;
                break;

            case Surface.ROTATION_270:
                degrees = 270;
                break;

        }

        int result;
        if(info.facing==Camera.CameraInfo.CAMERA_FACING_FRONT){
            result = (info.orientation + degrees) % 360;
        }else{
            result = (info.orientation - degrees + 360) % 360;
        }

        return result;
    }

    /**
     * Takes a picture, rotates it and tries to find a face in it.
     * If a face is detected the image is sent to the server and is processed to recognize a face.
     *
     * @param view the view of the App
     */
    public void onClickPicture(View view) {
        if(!lockPress) {
            mCameraSource.takePicture(null, new CameraSource.PictureCallback() {
                @Override
                public void onPictureTaken(byte[] bytes) {
                    lockPress = true;   // prevents the user to snap multiple pictures

                    Log.d(TAG, "Manual picture taken.");
                    Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    onPause();  // pauses the camera

                    if (currentOrientation != 0.0f) {   // checks if the image needs to be rotated
                        int width = bmp.getWidth();
                        int height = bmp.getHeight();

                        Matrix matrix = new Matrix();
                        matrix.postRotate(currentOrientation);

                        bmp = Bitmap.createBitmap(bmp, 0, 0, width, height, matrix, true);  // rotates image
                    }

                    // starts a face detector in accuracy mode and searchs for all Landmarks in the face
                    FaceDetector faceDetector = new FaceDetector.Builder((Context) MainActivity.this)
                            .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                            .setMode(FaceDetector.ACCURATE_MODE)
                            .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                            .setTrackingEnabled(true)
                            .build();

                    Bitmap tmp = Bitmap.createScaledBitmap(bmp, 480, 640, false);   // downsizes the image for the detector for faster processing

                    Frame frame = new Frame.Builder().setBitmap(tmp).build();

                    SparseArray<Face> faces = faceDetector.detect(frame);   // returns detected faces
                    tmp.recycle();

                    // Checks if a face is detected. If no face is detected shows an error message and restarts the camera
                    if (faces.size() < 1) {
                        onResume();
                        Toast.makeText((Context) MainActivity.this, "Error: No Face detected!", Toast.LENGTH_SHORT).show();
                        lockPress = false;
                        return;
                    }

                    Face biggest = null;
                    boolean leftEye = false;
                    boolean rightEye = false;

                    // Checks all detected faces for the needed Landmarks and return the biggest face in the image
                    for (int i = 0; i < faces.size(); ++i) {
                        Face face = faces.valueAt(i);
                        for (Landmark landmark : face.getLandmarks()) {
                            if(landmark.getType() == Landmark.LEFT_EYE) {
                                leftEye = true;
                            }
                            if(landmark.getType() == Landmark.RIGHT_EYE) {
                                rightEye = true;
                            }
                        }
                        if (leftEye && rightEye) {
                            if (biggest != null) {
                                if (face.getHeight() > biggest.getHeight()) {
                                    biggest = face;
                                }
                            } else {
                                biggest = face;
                            }
                        }
                        leftEye = false;
                        rightEye = false;
                    }

                    faceDetector.release();

                    // If no suitable face is detected, returns an error message and restarts the camera
                    if(biggest == null) {
                        onResume();
                        Toast.makeText((Context) MainActivity.this, "Error: No Face detected!", Toast.LENGTH_SHORT).show();
                        lockPress = false;
                        return;
                    }

                    mGraphicOverlay.add(pFaceGraphic);

                    // AsyncTask is started to send the image to the server and get a response
                    new UploadFileToServer(MainActivity.this, pFaceGraphic, biggest, instanceID).execute(bmp);

                    // in the meantime the catured image is shown to the user
                    Bitmap tmp2 = bmp.copy(Bitmap.Config.RGB_565,true);
                    Canvas canvas = new Canvas(tmp2);
                    mPreview.getSurfaceView().draw(canvas);

                }
            });
        }
    }

    /**
     * Enables or disables the Buttons and sets the previously selected button
     * @param vote can only have three different values:
     *             0 = face not rated previously
     *             1 = face rated with devil previously
     *             2 = face rated with saint previously
     */
    public void changeButtonStatus(Integer vote) {
        if (vote == 0) {
            // enables the two buttons to take a vote
            bDevil.setEnabled(true);
            bSaint.setEnabled(true);
            bDevil.setClickable(true);
            bSaint.setClickable(true);

            bDone.setClickable(false);
            bDone.setVisibility(Button.INVISIBLE);
            bEdit.setClickable(false);
            bEdit.setVisibility(View.INVISIBLE);

            bDevil.setBackgroundColor(Color.argb(255,255,68,68));
            bSaint.setBackgroundColor(Color.argb(255, 255, 187, 51));
        } else if (vote == 1) {
            // disable buttons if previously voted
            bDevil.setEnabled(false);
            bSaint.setEnabled(false);
            bDevil.setClickable(false);
            bSaint.setClickable(false);

            bDone.setClickable(true);
            bDone.setVisibility(Button.VISIBLE);
            bEdit.setClickable(true);
            bEdit.setVisibility(View.VISIBLE);

            // last choice is highlighted
            bDevil.setBackgroundColor(Color.argb(255,255,68,68));
            bSaint.setBackgroundColor(Color.GRAY);
        } else if (vote == 2) {
            // disable buttons if previously voted
            bDevil.setEnabled(false);
            bSaint.setEnabled(false);
            bDevil.setClickable(false);
            bSaint.setClickable(false);

            bDone.setClickable(true);
            bDone.setVisibility(Button.VISIBLE);
            bEdit.setClickable(true);
            bEdit.setVisibility(View.VISIBLE);

            // last choice is highlighted
            bDevil.setBackgroundColor(Color.GRAY);
            bSaint.setBackgroundColor(Color.argb(255, 255, 187, 51));
        } else {
            Log.e(TAG, "Error: Vote returned wrong value.");
        }
    }

    /**
     * Handles the requesting of the camera permission.  This includes
     * showing a "Snackbar" message of why the permission is needed then
     * sending the request.
     */
    private void requestAllPermissions() {
        Log.w(TAG, "Camera permission is not granted. Requesting permission");

        final String[] permissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.INTERNET};

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA) || !ActivityCompat.shouldShowRequestPermissionRationale(this,
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
            case REQUEST_CAMERA_STORAGE_PERM: {
                Map<String, Integer> perms = new HashMap<String, Integer>();
                perms.put(Manifest.permission.CAMERA, PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.INTERNET, PackageManager.PERMISSION_GRANTED);

                for(int i = 0; i < permissions.length; i++) {
                    perms.put(permissions[i], grantResults[i]);
                }

                if(perms.get(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                        && perms.get(Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED) {

                    createCameraSource(cameraFacing);
                } else {

                    Toast.makeText(MainActivity.this, "Some Permission is Denied", Toast.LENGTH_SHORT).show();
                }
            }
            default: {
                Log.e(TAG, "Got unexpected permission result: " + requestCode);
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            }
        }
    }

    /**
     * Creates and starts the camera.
     */
    private void createCameraSource(int facing) {

        Context context = getApplicationContext();
        detector = new FaceDetector.Builder(context)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .setMode(FaceDetector.ACCURATE_MODE)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .setMinFaceSize(0.2f) //faces bigger than 35% of Image are detected; use setMinFaceSize() instead
                .setTrackingEnabled(true)
                .build();

        detector.setProcessor(new LargestFaceFocusingProcessor(detector, new FaceTracker()));

        if (!detector.isOperational()) {
            // Note: The first time that an app using face API is installed on a device, GMS will
            // download a native library to the device in order to do detection.  Usually this
            // completes before the app is run for the first time.  But if that download has not yet
            // completed, then the above call will not detect any faces.
            Log.w(TAG, "Face detector dependencies are not yet available.");
        }

        mCameraSource = new CameraSource.Builder(context, detector)
                .setRequestedPreviewSize(640, 480)
                .setFacing(facing)
                .setRequestedFps(30.0f)
                .build();

        // Shows a dialog to give instructions.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Tap Screen to take photo.\nLong Press to switch Camera.")
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {}
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    /**
     * Starts or restarts the camera source, if it exists.  If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private void startCameraSource() {

        // checks if the device has play services available.
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                getApplicationContext());
        if (code != ConnectionResult.SUCCESS) {
            Dialog dlg =
                    GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS);
            dlg.show();
        }

        // checks if the CamerSource exists and starts it
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
     * Closes the image and resumes the Camera to detect the next face.
     * If further contact infos are present, sends them to the server.
     * @param view
     */
    public void onClickDone(View view) {
        String txtName = tbName.getText().toString();
        String txtAdresse = tbAdresse.getText().toString();
        String txtTel = tbTel.getText().toString();

        String changedContact = txtName + txtAdresse + txtTel;

        if(!TextUtils.isEmpty(txtName) || !TextUtils.isEmpty(txtAdresse) || !TextUtils.isEmpty(txtTel)) {
            if (!changedContact.equals(checkContact)) {
                new UploadVote(MainActivity.this, rsFaceID, txtName, txtAdresse, txtTel, instanceID).execute();
            }
        }

        restartLifeCycle();
    }

    /**
     * Opens the Panel to edit the Information of the Face.
     * @param view
     */
    public void onClickEdit(View view) {
        if (editUpdated == 0) { // if the button is pressed form the first time, retrieves all available contact infos
            new UploadVote(MainActivity.this, rsFaceID, "", "", "", "").execute();
            editUpdated = 1;    // contact infos are retrieved once for every face
        }
        // sets the visibility of the contact info area
        if(lcontacts.getVisibility() == View.GONE) {
            lcontacts.setVisibility(View.VISIBLE);
        } else {
            lcontacts.setVisibility(View.GONE);
        }
    }

    /**
     * The recognized face ID and a FaceGraphic is passed to the MainActivity
     * @param faceID a random ID inside a string.
     */
    public void setButtonVariable(String faceID, FaceGraphic fg) {
        rsFaceID = faceID;
        fGraphic = fg;
    }

    /**
     * Sets the Contact info of the recognized Face
     * @param name  //Name of the recognized Face
     * @param adresse   //Addresse of the recognized Face
     * @param tel   //Telephone number of the recognized Face
     */
    public void setContact(String name, String adresse, String tel) {
        tbName.setText(name);
        tbAdresse.setText(adresse);
        tbTel.setText(tel);
        checkContact = name + adresse + tel;
    }

    /**
     * Setter for the provided FaceGraphic. Needed to update the face filter overlay.
     * @param f
     */
    public void setFaceGraphic(FaceGraphic f) {
        pFaceGraphic = f;
    }

    /**
     * This class manages detected Faces.
     * Adds new faces and removes them if they disappear.
     */
    public class FaceTracker extends Tracker<Face> {
        private GraphicOverlay mOverlay;
        private FaceGraphic mFaceGraphic;
        private int frameCount = 0;

        public FaceTracker() {
            mOverlay = mGraphicOverlay;
            mFaceGraphic = new FaceGraphic(mOverlay, getApplicationContext());
            MainActivity.this.setFaceGraphic(mFaceGraphic);
        }

        /**
         *
         * @param faceId The ID of the newly detected face.
         * @param item The detected face item.
         */
        @Override
        public void onNewItem(int faceId, Face item) {
            mGraphicOverlay.setId(faceId);
        }

        /**
         * Is called if the face has updated in the current Frame.
         *
         * @param detections Detection result containing both detected faces and the associated frame metadata.
         * @param face The detected face.
         */
        @Override
        public void onUpdate(Detector.Detections<Face> detections, Face face) {
            if (autoFaceDetection) {
                boolean leftEye = false;
                boolean rightEye = false;

                //checks if the left and right Eye Landmark are detected
                for (Landmark landmark : face.getLandmarks()) {
                    if(landmark.getType() == Landmark.LEFT_EYE) {
                        leftEye = true;
                    }
                    if(landmark.getType() == Landmark.RIGHT_EYE) {
                        rightEye = true;
                    }
                }

                if (leftEye && rightEye) {
                    if (frameCount > 6) {   //waits 6 frames for a better bounding box of the face and to focus the camera correctly
                        mOverlay.add(mFaceGraphic);
                        takePicture(face);
                        mFaceGraphic.updateFace(face);
                        frameCount = 0;
                    } else {
                        try {
                            TimeUnit.MILLISECONDS.sleep(10);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        frameCount++;
                    }
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
         * Takes a picture of the current Frame and uploads it to the Server.
         * The Camera is paused and the Image is shown on screen, waiting on the response
         * of the server.
         *
         * @param face The detected Face with Landmarks
         */
        public void takePicture(final Face face) {
            mCameraSource.takePicture(null, new CameraSource.PictureCallback() {
                @Override
                public void onPictureTaken(byte[] bytes) {
                    lockPress = true;

                    //saves picture byte array to a bitmap
                    Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

                    bmp = rotateClockBitmap(bmp, currentOrientation);

                    new UploadFileToServer(MainActivity.this, mFaceGraphic, face, instanceID).execute(bmp);

                    Bitmap tmp2 = bmp.copy(Bitmap.Config.RGB_565,true);
                    onPause();
                    Canvas canvas = new Canvas(tmp2);
                    mPreview.getSurfaceView().draw(canvas);

                }
            });
        }

        /**
         * Rotates a bitmap in clockwise direction.
         * @param original  Bitmap of a picture taken by the camera.
         * @param degrees   degrees by which the picture is rotated clockwise.
         * @return          rotated bitmap is returned.
         */
        public Bitmap rotateClockBitmap(Bitmap original, float degrees) {
            if (degrees != 0.0f) {
                int width = original.getWidth();
                int height = original.getHeight();

                Matrix matrix = new Matrix();
                matrix.postRotate(degrees);

                original = Bitmap.createBitmap(original, 0, 0, width, height, matrix, true);
            }
            return original;
        }
    }
}