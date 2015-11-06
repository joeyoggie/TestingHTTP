package com.example.android.testinghttp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;


public class MainActivity extends ActionBarActivity {

    TextView statusTextView;
    TextView contentTextView;

    EditText enteredUserName;
    EditText enteredName;
    EditText enteredPhoneNumber;

    EditText enteredRecepient;
    EditText enteredMessage;

    String deviceID;

    int responseCodeG;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusTextView = (TextView) findViewById(R.id.status_text_view);
        contentTextView = (TextView) findViewById(R.id.content_text_view);

        enteredUserName = (EditText) findViewById(R.id.entered_username);
        enteredName = (EditText) findViewById(R.id.entered_name);
        enteredPhoneNumber = (EditText) findViewById(R.id.entered_phone_number);

        enteredRecepient = (EditText) findViewById(R.id.entered_recepient);
        enteredMessage = (EditText) findViewById(R.id.entered_message);

        //Get the unique device ID that will be stored in the database to uniquely identify this device
        TelephonyManager tm = (TelephonyManager) getBaseContext().getSystemService(Context.TELEPHONY_SERVICE);
        deviceID = tm.getSimSerialNumber().toString();

        //BroadcastReceiver that will be waiting for calls from the onPostExecute() method in MyInstanceIDListenerService.java
        //to update the contentTextView textbox with the response from the server after sending the registration info to the server
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String response = intent.getStringExtra("response");
                contentTextView.setText(response);
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver,new IntentFilter("broadcastIntent"));
    }


    //Submit the info to the server (register a new device)
    public void submitInfo(View view)
    {
        //Check for internet connectivity first
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = connMgr.getActiveNetworkInfo();

        //If there's internet connection
        if(netInfo != null && netInfo.isConnected())
        {
            //Get the info from the UI (textboxes)
            String userName = enteredUserName.getText().toString();
            String name = enteredName.getText().toString();
            String phoneNumber = enteredPhoneNumber.getText().toString();

            //Pass the info to the MyInstanceIDListenerService.java service that will send the info to the server along
            //with the registration ID token
            Intent reg = new Intent(this, MyInstanceIDListenerService.class);
            reg.putExtra("userName", userName);
            reg.putExtra("name", name);
            reg.putExtra("phoneNumber", phoneNumber);
            reg.putExtra("deviceID", deviceID);
            startService(reg);
        }
        //If there's no internet connection
        else
        {
            statusTextView.setText("No internet connection!");
        }
    }

    //Get the user info from the server (using the device ID)
    public void getInfo(View view)
    {
        //Check for internet connectivity first
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = connMgr.getActiveNetworkInfo();

        //If there's an internet connectioj
        if(netInfo != null && netInfo.isConnected())
        {
            downloadThread download = new downloadThread();

            //Get the info from the server in a background thread
            download.execute("http://192.168.1.44:8080/MyFirstServlet/GetInfo?deviceID=" + deviceID);
        }
        //If there's no internet connection
        else
        {
            statusTextView.setText("No internet connection!");
        }
    }


    //Send a message to the server, which will then send it to the recepient
    public void sendMessage(View view)
    {
        //Check if there's an internet connection
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = connMgr.getActiveNetworkInfo();

        //If there's an internet connection
        if(netInfo != null && netInfo.isConnected())
        {
            //Get the value of the textfields from the UI
            String senderUserName = enteredUserName.getText().toString();
            String recepientUserName = enteredRecepient.getText().toString();
            String message = enteredMessage.getText().toString();

            //Send the message info to the server in a background thread
            downloadThread download = new downloadThread();
            download.execute("http://192.168.1.44:8080/MyFirstServlet/SendNewMessage?senderUserName="+ URLEncoder.encode(senderUserName)+"&recepientUserName="+URLEncoder.encode(recepientUserName)+"&message="+URLEncoder.encode(message));
        }
        //If there's no internet connection
        else
        {
            statusTextView.setText("No internet connection!");
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


    //AsyncTask that will handle the HTTP connections in a background thread
    public class downloadThread extends AsyncTask<String,Void,String>{

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

        protected void onPreExecute()
        {
            contentTextView.setText("Loading...");
        }

        protected void onPostExecute(String result)
        {
            contentTextView.setText(result);
        }

        protected void onProgressUpdate(Void... value)
        {
            statusTextView.setText("" + responseCodeG);
            contentTextView.setText("Still loading...");
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
                Log.d("MainActivity", "The response is: " + response);
                responseCodeG = response;
                publishProgress();

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
        }//End of downloadUrl method
    }

    /*public class downloadThread extends AsyncTask<String,Void,Bitmap>{

        protected Bitmap doInBackground(String... urls)
        {
            Bitmap result = null;
            try {
                result = downloadUrl(urls[0]);
            }
            catch (IOException e)
            {
                //result = "Unable to retrieve image. URL might be invalid!";
            }
            return result;
        }

        protected void onPreExecute()
        {
            contentTextView.setText("Loading...");
        }

        protected void onPostExecute(Bitmap result)
        {
            imageView.setImageBitmap(result);
            contentTextView.setText("Done!");
        }

        protected void onProgressUpdate(Void... value)
        {
            statusTextView.setText("" + responseCodeG);
            contentTextView.setText("Still loading...");
        }


        private Bitmap downloadUrl(String myurl) throws IOException {
            InputStream is = null;
            try {
                URL url = new URL(myurl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(10000);
                conn.setConnectTimeout(15000);
                conn.setRequestMethod("GET");
                conn.setDoInput(true);
                // Starts the query
                conn.connect();
                int response = conn.getResponseCode();
                Log.d("MainActivity", "The response is: " + response);
                responseCodeG = response;
                publishProgress();
                is = conn.getInputStream();

                Bitmap bitmap = BitmapFactory.decodeStream(is);
                return bitmap;

                // Makes sure that the InputStream is closed after the app is
                // finished using it.
            } finally {
                if (is != null) {
                    is.close();
                }
            }
        }
    }*/
}
