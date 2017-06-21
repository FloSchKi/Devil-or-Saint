package com.schulz_kittler.florian.devil_or_saint;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private CameraPreview mCameraPreview;
    private Camera camera;

    private FrameLayout cameraPreviewLayout;
    private ImageView capturedImageHolder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        cameraPreviewLayout = (FrameLayout)findViewById(R.id.camera_preview);
        capturedImageHolder = (ImageView)findViewById(R.id.captured_image);

        if (checkCameraHardware(getApplicationContext()) >= 1) {
            camera = getCameraInstance();
            mCameraPreview = new CameraPreview(MainActivity.this, camera);
            cameraPreviewLayout.addView(mCameraPreview);
//            camera.setFaceDetectionListener(new MyFaceDetectionListener());

        } else {
            AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
            alertDialog.setTitle("Error");
            alertDialog.setMessage("This Application needs a camera to work!");
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Exit",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            intent.putExtra("EXIT", true);
                            startActivity(intent);
                            if (getIntent().getExtras() != null && getIntent().getExtras().getBoolean("EXIT", false)) {
                                finish();
                            }
                        }
                    });
            alertDialog.show();
        }
       /*FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });*/
    }

    /** Get an instance of the Camera Object. */
    private Camera getCameraInstance(){
        Camera myCamera = null;
        try {
            myCamera = Camera.open();   //attempt to get a Camera instance
        } catch (Exception e) {
            Log.d(TAG, "Error getting camera instance: " + e.getMessage());
        }
        return myCamera;                //returns null if camera is unavailable
    }

    Camera.PictureCallback pictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            if(bitmap==null){
                Toast.makeText(MainActivity.this, "Captured image is empty", Toast.LENGTH_LONG).show();
                return;
            }
            capturedImageHolder.setImageBitmap(scaleDownBitmapImage(bitmap, 300, 200 ));
        }
    };

    private Bitmap scaleDownBitmapImage(Bitmap bitmap, int newWidth, int newHeight){
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
        return resizedBitmap;
    }

    /** Check if this device has a camera */
    private int checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            // this device has a camera
            return Camera.getNumberOfCameras();
        } else {
            // no camera on this device
            return 0;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
