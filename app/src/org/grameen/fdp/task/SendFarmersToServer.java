package org.grameen.fdp.task;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import org.grameen.fdp.R;
import org.grameen.fdp.object.RealFarmer;
import org.grameen.fdp.utility.Callbacks;
import org.grameen.fdp.utility.Constants;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Created by aangjnr on 05/03/2018.
 */

public class SendFarmersToServer extends AsyncTask<Void, Void, String> {

    SharedPreferences prefs;
    private String URL_STRING;
    String REQUEST_TYPE;
    public static final String TAG = SendFarmersToServer.class.getSimpleName();
    public static Callbacks.NetworkActivityCompleteListener networkActivityCompleteListener;

    public SendFarmersToServer(String url){

        this.URL_STRING = url;
        //this.REQUEST_TYPE = requestType;

    }


   /* public static void onNetworkActivityComplete(Callbacks.NetworkActivityCompleteListener listener){
        networkActivityCompleteListener = listener;
    }

    public static void removeOnNetworkActivityComplete(){
        networkActivityCompleteListener = null;
    }
*/

    @Override
    protected String doInBackground(Void... params) {
        // These two need to be declared outside the try/catch
        // so that they can be closed in the finally block.
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;






        // Will contain the raw JSON response as a string.
        String forecastJsonStr = null;

        try {
            // Construct the URL_STRING for the OpenWeatherMap query





            URL URL = new URL(URL_STRING);

            urlConnection = (HttpURLConnection) URL.openConnection();
            urlConnection.setRequestMethod("POST");
            urlConnection.setDoOutput(true);



            urlConnection.connect();


            InputStream inputStream;


            int status = urlConnection.getResponseCode();

            Log.i(TAG, "STATUS = " + status);
            if (status != HttpsURLConnection.HTTP_OK)
                inputStream = urlConnection.getErrorStream();
            else
                inputStream = urlConnection.getInputStream();




            // Read the input stream into a String
            StringBuilder buffer = new StringBuilder();
            if (inputStream == null) {
                // Nothing to do.
                return null;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                // But it does make debugging a *lot* easier if you print out the completed
                // buffer for debugging.
                buffer.append(line).append("\n");
            }

            if (buffer.length() == 0) {
                // Stream was empty.  No point in parsing.
                return null;
            }

            forecastJsonStr = buffer.toString();
            return forecastJsonStr;


        } catch (IOException e) {
            Log.e("PlaceholderFragment", "Error ", e);
             return null;
        } finally{
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e("PlaceholderFragment", "Error closing stream", e);
                }
            }
        }
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);

        Log.i(TAG, "RESPONSE AFTER SENDING FARMER DATA TO SALES FORCE " + s);

        //if(networkActivityCompleteListener != null)
        //networkActivityCompleteListener.taskComplete(s);




    }


}
