package com.schulz_kittler.florian.devil_or_saint;

/**
 * Copyright 2018 Florian Schulz-Kittler
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.vision.face.Face;

import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.StringBody;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * This AsyncTask is used to send an image to the python server and handle the response accordingly
 */
class UploadFileToServer extends AsyncTask<Bitmap, Void, String> {
    private static final String TAG = "UploadFileToServer";
    private Context mainContext;
    private ProgressDialog mDialog;
    private FaceGraphic fGraphic;
    private Face face;
    private String id;

    public UploadFileToServer(Context context, FaceGraphic faceG, Face faceIn, String iID) {
        mainContext = context;
        fGraphic = faceG;
        face = faceIn;
        id = iID;
    }

    /**
     * Shows a Dialog while the Background task is running
     */
    @Override
    protected void onPreExecute() {
        mDialog = new ProgressDialog(mainContext);
        mDialog.setMessage("Processing Image...");
        mDialog.show();
    }

    /**
     * Converts the image and sends it with the device ID to the server.
     * The response is passed the onPostExecute.
     *
     * @param image the image which is sent to the server
     * @return the response from the server
     */
    @Override
    protected String doInBackground(Bitmap... image) {
        Bitmap bit = image[0];

        try {
            URL url = new URL("http://schulz.pythonanywhere.com/bafacerec");    // the addresse of the image recognition python server
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Connection", "Keep-Alive");

            Log.d(TAG, "**AsyncTask** - MultipartEntity wird erstellt.");
            // creates a MultiPartEntity
            MultipartEntity entity = new MultipartEntity(
                    HttpMultipartMode.BROWSER_COMPATIBLE);

            // converts the image to a png
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bit.compress(Bitmap.CompressFormat.PNG, 0, bos);
            byte[] data = bos.toByteArray();

            // sets the names of the image and the ID of the device and adds it to the MultiPartEntity
            ByteArrayBody bab = new ByteArrayBody(data, "face.png");
            entity.addPart("face", bab);
            entity.addPart("iid", new StringBody(id));
            Log.d(TAG, "**AsyncTask** - Bild wurde zum POST hinzugefügt.");

            conn.addRequestProperty("Content-length", entity.getContentLength() + "");
            conn.addRequestProperty(entity.getContentType().getName(), entity.getContentType().getValue());

            // sends the data to the python server
            OutputStream os = conn.getOutputStream();
            entity.writeTo(conn.getOutputStream());
            Log.d(TAG, "**AsyncTask** - POST wurde gesendet.");
            os.close();
            conn.connect();

            // checks if the no errors occured and reads the response of the server
            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                Log.d(TAG, "**AsyncTask** - HTTP_OK");
                return readStream(conn.getInputStream());
            } else {
                Log.d(TAG, "**AsyncTask** - HTTP_CODE: " + conn.getResponseCode() + "| Message: " + conn.getResponseMessage());
                return "Error HTTP_CODE";
            }

        } catch (Exception e) {
            e.printStackTrace();    // Error during Server connection
        }
        // needed to avoid syntax errors
        return null;
    }

    /**
     * Reads the server response and returns the String.
     *
     * @param in the InputStream of the response
     * @return a well formatted String
     */
    private String readStream(InputStream in) {
        BufferedReader reader = null;
        StringBuilder builder = new StringBuilder();
        try {
            reader = new BufferedReader(new InputStreamReader(in));
            String line = "";
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return builder.toString();
    }

    /**
     * Checks the response of the server for errors and sets the face filter, vote and credits of the user.
     *
     * @param result the response of the server
     */
    @Override
    protected void onPostExecute(String result) {
        int dos = 0;
        int vote = 3;
        int credits = 0;
        boolean error = false;  // indicates if an error occured
        boolean faceError = false; // indicates if no face was detected by the server
        String faceID = "";

        // checks the response for errors
        if (result == null) {
            Log.e(TAG, "**AsyncTask/onPostExecute** - Result contained Null");
            error = true;
        }
        else if (result.contains("Error")) {
            Log.e(TAG, "**AsyncTask/onPostExecute** - Result contained Error: " + result);
            if (result.contains("a face")) {
                faceError = true;
            }
            error = true;
        } else {
            try {
                // splits the String to retrieve all Informations
                String[] output = result.split(", ");
                dos = Integer.parseInt(output[2]);
                vote = Integer.parseInt(output[1]);
                faceID = output[0];
                credits = Integer.parseInt(output[3]);

                MainActivity main = (MainActivity) mainContext;
                main.setButtonVariable(faceID, fGraphic);

                // sets and shows the face filter, credits and vote
                fGraphic.setDevilOrSaint(dos);
                fGraphic.setCredits(credits);
                fGraphic.setCreditsVisible(true);
                fGraphic.updateFace(face);
                main.changeButtonStatus(vote);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }

        // removes the processing Dialog
        mDialog.dismiss();

        // if an error occurred, it is shown to the user and the LifeCycle is restarted
        if(error) {
            if(faceError) {
                Toast.makeText(mainContext, "Error: Server didn't detect a face! Please try again.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(mainContext, "An Error occured! Please try again.", Toast.LENGTH_SHORT).show();
            }
            MainActivity main = (MainActivity) mainContext;
            main.restartLifeCycle();
        }
        Log.d(TAG, "**AsyncTask/onPostExecute** - Antwort erhalten: " + result);
    }
}
