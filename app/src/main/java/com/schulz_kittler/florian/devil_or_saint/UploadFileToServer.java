package com.schulz_kittler.florian.devil_or_saint;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;

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

/**
 * Created by Schulz on 12.02.2018.
 */

class UploadFileToServer extends AsyncTask<Bitmap, Void, String> {
    private static final String TAG = "UploadFileToServer";

    @Override
    protected String doInBackground(Bitmap... image) {
        /*String attachmentName = "bitmap";
        String attachmentFileName = "bitmap.bmp";
        String crlf = "\r\n";
        String twoHyphens = "--";
        String boundary =  "*****";*/
        String resultStr = null;
        Bitmap bit = image[0];

        /*try {
            Log.d(TAG, "Async Try Statement");
            HttpURLConnection httpUrlConnection = null;
            URL url = new URL("http://schulz.pythonanywhere.com/bafacerec");
            httpUrlConnection = (HttpURLConnection) url.openConnection();
            httpUrlConnection.setUseCaches(false);
            httpUrlConnection.setDoOutput(true);

            Log.d(TAG, "**!!** - 1");
            httpUrlConnection.setRequestMethod("POST");
            httpUrlConnection.setRequestProperty("Connection", "Keep-Alive");
            httpUrlConnection.setRequestProperty("Cache-Control", "no-cache");
            httpUrlConnection.setRequestProperty(
                    "Content-Type", "multipart/form-data;boundary=" + boundary);

            Log.d(TAG, "**!!** - 2");
            DataOutputStream request = new DataOutputStream(
                    httpUrlConnection.getOutputStream());

            request.writeBytes(twoHyphens + boundary + crlf);
            request.writeBytes("Content-Disposition: form-data; name=\"" +
                    attachmentName + "\";filename=\"" +
                    attachmentFileName + "\"" + crlf);
            request.writeBytes(crlf);

            Log.d(TAG, "**!!** - 3");
            //I want to send only 8 bit black & white bitmaps
            byte[] pixels = new byte[bit.getWidth() * bit.getHeight()];
            for (int i = 0; i < bit.getWidth(); ++i) {
                for (int j = 0; j < bit.getHeight(); ++j) {
                    //we're interested only in the MSB of the first byte,
                    //since the other 3 bytes are identical for B&W images
                    pixels[i + j] = (byte) ((bit.getPixel(i, j) & 0x80) >> 7);
                }
            }
            request.write(pixels);
            Log.d(TAG, "**!!** - 3,5");
            request.writeBytes(crlf);
            request.writeBytes(twoHyphens + boundary +
                    twoHyphens + crlf);

            Log.d(TAG, "**!!** - 4");
            request.flush();
            request.close();

            InputStream responseStream = new
                    BufferedInputStream(httpUrlConnection.getInputStream());

            BufferedReader responseStreamReader =
                    new BufferedReader(new InputStreamReader(responseStream));

            String line = "";
            StringBuilder stringBuilder = new StringBuilder();

            while ((line = responseStreamReader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
            responseStreamReader.close();

            resultStr = stringBuilder.toString();
            Log.d(TAG, "Response: " + resultStr);

            responseStream.close();
            httpUrlConnection.disconnect();

        } catch (IOException e) {
            e.printStackTrace();
        }*/

        /*try {
            Log.d(TAG, "Async Try Statement");
            URL url = new URL("http://schulz.pythonanywhere.com/bafacerec");
            HttpURLConnection c = (HttpURLConnection) url.openConnection();
            c.setDoInput(true);
            c.setRequestMethod("POST");
            c.setDoOutput(true);
            c.connect();
            Log.d(TAG, "Async After Connect");
            OutputStream output = c.getOutputStream();
            bit.compress(Bitmap.CompressFormat.JPEG, 100, output);
            output.close();
            Log.d(TAG, "Async POST done!");
            Scanner result = new Scanner(c.getResponseMessage());
            String response = result.nextLine();
            Log.d(TAG, "Async " + response);
            result.close();
            resultStr = response;
        } catch (IOException e) {
            Log.e(TAG, "Error uploading image", e);
        }*/


        try {
            URL url = new URL("http://schulz.pythonanywhere.com/bafacerec");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Connection", "Keep-Alive");

            Log.d(TAG, "**AsyncTask** - MultipartEntity wird erstellt.");
            MultipartEntity entity = new MultipartEntity(
                    HttpMultipartMode.BROWSER_COMPATIBLE);

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bit.compress(Bitmap.CompressFormat.JPEG, 100, bos);
            byte[] data = bos.toByteArray();
            ByteArrayBody bab = new ByteArrayBody(data, "face.jpg");
            entity.addPart("face", bab);
            Log.d(TAG, "**AsyncTask** - Bild wurde zum POST hinzugefügt.");

            conn.addRequestProperty("Content-length", entity.getContentLength() + "");
            conn.addRequestProperty(entity.getContentType().getName(), entity.getContentType().getValue());

            OutputStream os = conn.getOutputStream();
            entity.writeTo(conn.getOutputStream());
            Log.d(TAG, "**AsyncTask** - POST wurde gesendet.");
            os.close();
            conn.connect();

            Scanner sc = new Scanner(conn.getInputStream());
            resultStr = sc.nextLine();

            //resultStr = conn.getResponseMessage();

            sc.close();

        } catch (Exception e) {
            e.printStackTrace();
            // Error during Server connection
        }

        return resultStr;
    }

    @Override
    protected void onPostExecute(String result) {
        Log.d(TAG, "**AsyncTask/onPostExecute** - Antwort erhalten: " + result);
    }
}
