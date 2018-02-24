package com.schulz_kittler.florian.devil_or_saint;

import android.content.Context;
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
    private String param1;
    private String param2;
    private String param3;
    private String param4;
    private Boolean urlVote;
    private Context mainContext;

    public UploadVote(String str1, String str2, String str3) {
        param1 = str1;
        param2 = str2;
        param3 = str3;
        urlVote = true;
    }

    public UploadVote(Context con, String str1, String str2, String str3, String str4) {
        mainContext = con;
        param1 = str1;
        param2 = str2;
        param3 = str3;
        param4 = str4;
        urlVote = false;
    }

    @Override
    protected String doInBackground(String... params) {
        String resultStr = null;

        try {
            URL url;
            if (urlVote) {
                url = new URL("http://schulz.pythonanywhere.com/vote");
            } else {
                url = new URL("http://schulz.pythonanywhere.com/contact");
            }
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Connection", "Keep-Alive");

            Log.d(TAG, "**Vote** - MultipartEntity wird erstellt.");
            MultipartEntity entity = new MultipartEntity(
                    HttpMultipartMode.BROWSER_COMPATIBLE);


            if (urlVote) {
                entity.addPart("vote", new StringBody(param3));
                entity.addPart("label", new StringBody(param1));
                entity.addPart("iid", new StringBody(param2));
            } else {
                entity.addPart("label", new StringBody(param1));
                entity.addPart("name", new StringBody(param2));
                entity.addPart("adresse", new StringBody(param3));
                entity.addPart("telnr", new StringBody(param4));
            }
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
        if (!result.contains("Error") && !result.contains("DBContact")) {
            if(!urlVote && !result.equals("1::2::3:")) {
                int step = 1;
                String[] strSplit = result.split(":");
                int strSize = strSplit.length;
                String name = strSplit[1];
                String adresse = strSplit[3];
                String telnr = "";

                Log.d(TAG, "strSplit.size(): " + strSize);

                if (strSize == 6) {
                    telnr = strSplit[5];
                }
                /*if (!strSplit[step].equals("2")) {
                    name = strSplit[step];
                    step = step+2;
                } else {
                    step = step+1;
                }

                if (!strSplit[step].equals("3")) {
                    adresse = strSplit[step];
                    step = step+2;
                } else {
                    step = step+1;
                }

                if((step-1) < strSize) {
                    telnr = strSplit[step];
                }*/
                MainActivity main = (MainActivity) mainContext;
                main.setContact(name, adresse, telnr);
            } else {
                Log.d(TAG, "**Vote** - Empty String returned!");
            }
        }
        Log.d(TAG, "**Vote** - Fertig! - " + result);
    }
}
