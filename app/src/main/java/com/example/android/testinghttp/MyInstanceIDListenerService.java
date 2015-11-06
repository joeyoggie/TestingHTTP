package com.example.android.testinghttp;

import android.app.IntentService;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.gcm.GcmPubSub;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class MyInstanceIDListenerService extends IntentService {

    private static final String TAG = "MyInstanceIDListenerService";
    private static final String[] TOPICS = {"global"};

    String userName;
    String name;
    String phoneNumber;
    String deviceID;

    public MyInstanceIDListenerService()
    {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        //SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        //Get the passed info from the MainActivity.java when it called the MyInstanceIDListenerService.java service
        userName = intent.getStringExtra("userName");
        name = intent.getStringExtra("name");
        phoneNumber = intent.getStringExtra("phoneNumber");
        deviceID = intent.getStringExtra("deviceID");

        //Get a registration ID for this device and send it to the server along with the user-submitted info above
        try {
            // [START register_for_gcm]
            // Initially this call goes out to the network to retrieve the token, subsequent calls
            // are local.
            // R.string.gcm_defaultSenderId (the Sender ID) is typically derived from google-services.json.
            // See https://developers.google.com/cloud-messaging/android/start for details on this file.
            // [START get_token]
            InstanceID instanceID = InstanceID.getInstance(this);
            String token = instanceID.getToken(getString(R.string.gcm_defaultSenderId),
                    GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
            // [END get_token]
            Log.i("TOKEN", "GCM Registration Token: " + token);

            //Implement this method to send the registration info to your app's servers.
            sendRegistrationToServer(token);

            // Subscribe to topic channels
            subscribeTopics(token);

            // You should store a boolean that indicates whether the generated token has been
            // sent to your server. If the boolean is false, send the token to your server,
            // otherwise your server should have already received the token.
            //sharedPreferences.edit().putBoolean(QuickstartPreferences.SENT_TOKEN_TO_SERVER, true).apply();
            // [END register_for_gcm]
        } catch (Exception e) {
            Log.d("ERROR!!", "Failed to complete token refresh", e);
            // If an exception happens while fetching the new token or updating our registration data
            // on a third-party server, this ensures that we'll attempt the update at a later time.
            //sharedPreferences.edit().putBoolean(QuickstartPreferences.SENT_TOKEN_TO_SERVER, false).apply();
        }
        // Notify UI that registration has completed, so the progress indicator can be hidden.
        //Intent registrationComplete = new Intent(QuickstartPreferences.REGISTRATION_COMPLETE);
        //LocalBroadcastManager.getInstance(this).sendBroadcast(registrationComplete);
    }


    //Send the registration info to our server
    private void sendRegistrationToServer(String token) {

        downloadThread d = new downloadThread();

        //Send the info in a background thread
        d.execute("http://192.168.1.44:8080/MyFirstServlet/HelloWorld?userName="+URLEncoder.encode(userName)+"&name="+URLEncoder.encode(name)+"&phoneNumber="+URLEncoder.encode(phoneNumber)+"&deviceID="+URLEncoder.encode(deviceID)+"&regID="+URLEncoder.encode(token));
    }

    /**
     * Subscribe to any GCM topics of interest, as defined by the TOPICS constant.
     *
     * @param token GCM token
     * @throws IOException if unable to reach the GCM PubSub service
     */
    // [START subscribe_topics]
    private void subscribeTopics(String token) throws IOException {
        GcmPubSub pubSub = GcmPubSub.getInstance(this);
        for (String topic : TOPICS) {
            pubSub.subscribe(token, "/topics/" + topic, null);
        }
    }
    // [END subscribe_topics]



    //AsyncTask that will handle the HTTP connections in a background thread
    public class downloadThread extends AsyncTask<String,Void,String> {

        protected String doInBackground(String... urls)
        {
            String result = null;
            try {
                result = downloadUrl(urls[0]);
            }
            catch (IOException e)
            {

            }
            return result;
        }

        protected void onPostExecute(String result)
        {
            //Send the server response to MainActivity to display it to the user in contentTextView textbox

            Intent intent = new Intent("broadcastIntent");
            intent.putExtra("response", result);
            LocalBroadcastManager.getInstance(MyInstanceIDListenerService.this).sendBroadcast(intent);
        }

        private String downloadUrl(String myurl) throws IOException {
            InputStream is = null;
            try {
                URL url = new URL(myurl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(10000);
                conn.setConnectTimeout(15000);
                conn.setRequestMethod("GET");
                conn.setDoInput(true);

                conn.connect();
                int response = conn.getResponseCode();
                Log.d("REGTOKEN", "The response is: " + response);

                is = conn.getInputStream();

                int len = 500;
                String result = null;

                Reader reader = new InputStreamReader(is, "UTF-8");
                char[] buffer = new char[len];
                reader.read(buffer);
                result = new String(buffer);

                return result;

            } finally {
                if (is != null) {
                    is.close();
                }
            }
        }
    }

}
