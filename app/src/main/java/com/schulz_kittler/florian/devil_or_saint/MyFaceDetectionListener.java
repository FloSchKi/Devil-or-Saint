package com.schulz_kittler.florian.devil_or_saint;

import android.hardware.Camera;
import android.hardware.camera2.params.Face;
import android.util.Log;

/**
 * Created by Schulz on 21.06.2017.
 */

public class MyFaceDetectionListener implements Camera.FaceDetectionListener {

    @Override
    public void onFaceDetection(Camera.Face[] faces, Camera camera) {
        if (faces.length > 0){
            Log.d("FaceDetection", "face detected: "+ faces.length +
                    " Face 1 Location X: " + faces[0].rect.centerX() +
                    "Y: " + faces[0].rect.centerY() );
        }
    }


}
