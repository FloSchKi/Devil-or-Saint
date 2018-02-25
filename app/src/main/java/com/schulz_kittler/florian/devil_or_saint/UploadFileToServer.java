package com.schulz_kittler.florian.devil_or_saint;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.vision.face.Face;

import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.StringBody;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

/**
 * Created by Schulz on 12.02.2018.
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

    @Override
    protected void onPreExecute() {
        mDialog = new ProgressDialog(mainContext);
        mDialog.setMessage("Processing Image...");
        mDialog.show();
    }

    @Override
    protected String doInBackground(Bitmap... image) {
        String resultStr = null;
        Bitmap bit = image[0];

        try {
            URL url = new URL("http://schulz.pythonanywhere.com/bafacerec");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Connection", "Keep-Alive");

            Log.d(TAG, "**AsyncTask** - MultipartEntity wird erstellt.");
            MultipartEntity entity = new MultipartEntity(
                    HttpMultipartMode.BROWSER_COMPATIBLE);

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            //bit.compress(Bitmap.CompressFormat.JPEG, 100, bos);
            bit.compress(Bitmap.CompressFormat.PNG, 0, bos);
            byte[] data = bos.toByteArray();
            ByteArrayBody bab = new ByteArrayBody(data, "face.png");
            entity.addPart("face", bab);
            entity.addPart("iid", new StringBody(id));
            Log.d(TAG, "**AsyncTask** - Bild wurde zum POST hinzugefügt.");

            conn.addRequestProperty("Content-length", entity.getContentLength() + "");
            conn.addRequestProperty(entity.getContentType().getName(), entity.getContentType().getValue());

            OutputStream os = conn.getOutputStream();
            entity.writeTo(conn.getOutputStream());
            Log.d(TAG, "**AsyncTask** - POST wurde gesendet.");
            os.close();
            conn.connect();

            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                Log.d(TAG, "**AsyncTask** - HTTP_OK");
                return readStream(conn.getInputStream());
            } else {
                Log.d(TAG, "**AsyncTask** - HTTP_CODE: " + conn.getResponseCode() + "| Message: " + conn.getResponseMessage());
                return "Error HTTP_CODE";
            }

            /*Scanner sc = new Scanner(conn.getInputStream());
            resultStr = sc.nextLine();

            sc.close();*/

        } catch (Exception e) {
            e.printStackTrace();
            // Error during Server connection
        }

        return resultStr;
    }

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

    @Override
    protected void onPostExecute(String result) {
        int dos = 0;
        int vote = 3;
        int credits = 0;
        boolean error = false;
        String faceID = "";
        if (result == null) {
            Log.e(TAG, "**AsyncTask/onPostExecute** - Result contained Null");
            try {
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            error = true;
        }
        else if (result.contains("Error")) {
            Log.e(TAG, "**AsyncTask/onPostExecute** - Result contained Error: " + result);
            try {
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            error = true;
        } else {
            try {
                String[] output = result.split(", ");
                dos = Integer.parseInt(output[2]);
                vote = Integer.parseInt(output[1]);
                faceID = output[0];
                credits = Integer.parseInt(output[3]);

                MainActivity main = (MainActivity) mainContext;
                main.setButtonVariable(faceID, fGraphic);
                fGraphic.setDevilOrSaint(dos);
                fGraphic.setCredits(credits);
                fGraphic.setCreditsVisible(true);
                fGraphic.updateFace(face);
                main.changeButtonStatus(vote);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
        mDialog.dismiss();
        if(error) {
            Toast.makeText(mainContext, "An Error occured! Please try again.", Toast.LENGTH_SHORT).show();
            MainActivity main = (MainActivity) mainContext;
            main.restartLifeCycle();
        }
        Log.d(TAG, "**AsyncTask/onPostExecute** - Antwort erhalten: " + result); // + ", " + String.valueOf(myNum));
    }
}
