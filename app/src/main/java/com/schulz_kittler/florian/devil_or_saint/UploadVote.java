package com.schulz_kittler.florian.devil_or_saint;

import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.StringBody;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Schulz on 20.02.2018.
 */

public class UploadVote extends AsyncTask<String, Void, String> {
    private static final String TAG = "UploadVote";
    private String id;
    private String predicted_label;
    private String vote;

    public UploadVote(String predicted_label, String iID, String vote) {
        id = iID;
        this.predicted_label = predicted_label;
        this.vote = vote;
    }

    @Override
    protected String doInBackground(String... params) {
        String resultStr = null;

        try {
            URL url = new URL("http://schulz.pythonanywhere.com/vote");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Connection", "Keep-Alive");

            Log.d(TAG, "**Vote** - MultipartEntity wird erstellt.");
            MultipartEntity entity = new MultipartEntity(
                    HttpMultipartMode.BROWSER_COMPATIBLE);

            entity.addPart("vote", new StringBody(vote));
            entity.addPart("label", new StringBody(predicted_label));
            entity.addPart("iid", new StringBody(id));
            Log.d(TAG, "**Vote** - Strings wurden zum POST hinzugefügt.");

            conn.addRequestProperty("Content-length", entity.getContentLength() + "");
            conn.addRequestProperty(entity.getContentType().getName(), entity.getContentType().getValue());

            OutputStream os = conn.getOutputStream();
            entity.writeTo(conn.getOutputStream());
            Log.d(TAG, "**Vote** - POST wurde gesendet.");
            os.close();
            conn.connect();

            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                Log.d(TAG, "**AsyncTask** - HTTP_OK");
                return readStream(conn.getInputStream());
            } else {
                Log.d(TAG, "**AsyncTask** - HTTP_CODE: " + conn.getResponseCode() + "| Message: " + conn.getResponseMessage());
                return "Error HTTP_CODE";
            }

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
        Log.d(TAG, "**Vote** - Fertig! - " + result);
    }
}
