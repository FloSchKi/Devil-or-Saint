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
 * This AsyncTask sends/receives vote or contact data to/from the python server.
 * If empty fields are sent to the server, it responds with the available contact data.
 * If the parameter fields contain data, they are saved on the server with the provided device ID.
 */
public class UploadVote extends AsyncTask<String, Void, String> {
    private static final String TAG = "UploadVote";
    private String param1;
    private String param2;
    private String param3;
    private String param4;
    private String paramId;
    private Boolean urlVote;
    private Context mainContext;

    /**
     * The Vote Constructor.
     */
    public UploadVote(String str1, String str2, String str3) {
        param1 = str1;
        param2 = str2;
        param3 = str3;
        urlVote = true;
    }

    /**
     * The Contact Constructor.
     */
    public UploadVote(Context con, String str1, String str2, String str3, String str4, String strId) {
        mainContext = con;
        param1 = str1;
        param2 = str2;
        param3 = str3;
        param4 = str4;
        paramId = strId;
        urlVote = false;
    }

    /**
     * Sends data to save on the server or retrieves contact information from the server.
     * @return response from the server
     */
    @Override
    protected String doInBackground(String... params) {
        try {
            // changes the addresse of the server accordingly if the input is a vote or a contact
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

            // constructs the variables of the vote/contact data, which is sent to the server
            if (urlVote) {
                entity.addPart("vote", new StringBody(param3));
                entity.addPart("label", new StringBody(param1));
                entity.addPart("iid", new StringBody(param2));
            } else {
                entity.addPart("label", new StringBody(param1));
                entity.addPart("name", new StringBody(param2));
                entity.addPart("adresse", new StringBody(param3));
                entity.addPart("telnr", new StringBody(param4));
                entity.addPart("ciid", new StringBody(paramId));
            }
            Log.d(TAG, "**Vote** - Strings wurden zum POST hinzugefügt.");

            conn.addRequestProperty("Content-length", entity.getContentLength() + "");
            conn.addRequestProperty(entity.getContentType().getName(), entity.getContentType().getValue());

            // sends the data to the python server
            OutputStream os = conn.getOutputStream();
            entity.writeTo(conn.getOutputStream());
            Log.d(TAG, "**Vote** - POST wurde gesendet.");
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
     * Checks the response of the server for errors and sets the name, addresse and telephone number of the recognized face.
     *
     * @param result the response of the server
     */
    @Override
    protected void onPostExecute(String result) {
        // checks the response for errors or if the operation was successfully executed
        if (!result.contains("Error") && !result.contains("DBContact")) {
            if(!urlVote && !result.equals("1::2::3:")) {
                int step = 1;
                // splits the String to retrieve all Informations
                String[] strSplit = result.split(":");
                int strSize = strSplit.length;
                String name = strSplit[1];
                String adresse = strSplit[3];
                String telnr = "";

                Log.d(TAG, "strSplit.size(): " + strSize);

                if (strSize == 6) {
                    telnr = strSplit[5];
                }
                MainActivity main = (MainActivity) mainContext;
                main.setContact(name, adresse, telnr);  // transfers the Strings to the MainActivity and shows them to the user
            } else if (result.contains("DBVote")) {
                Log.d(TAG, "**Vote** - Vote updated!");
            } else {
                Log.d(TAG, "**Vote** - Empty String returned!");
            }
        }
        Log.d(TAG, "**Vote** - Fertig! - " + result);
    }
}
