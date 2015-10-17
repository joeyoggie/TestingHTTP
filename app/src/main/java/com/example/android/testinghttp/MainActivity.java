package com.example.android.testinghttp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;


public class MainActivity extends ActionBarActivity {

    TextView statusTextView;
    TextView contentTextView;
    ImageView imageView;
    boolean lowRes = true;
    boolean highRes = false;

    int responseCodeG;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusTextView = (TextView) findViewById(R.id.status_text_view);
        contentTextView = (TextView) findViewById(R.id.content_text_view);
        imageView = (ImageView) findViewById(R.id.image_view);

    }

    public void low_res_select(View view)
    {
        lowRes = true;
        highRes = false;
    }

    public void high_res_select(View view)
    {
        lowRes = false;
        highRes = true;
    }

    public void download(View view)
    {
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = connMgr.getActiveNetworkInfo();

        if(netInfo != null && netInfo.isConnected())
        {
            downloadThread download = new downloadThread();
            if(highRes == true)
            {
                download.execute("http://minimemes.net/wp-content/uploads/2013/04/me_gusta1.png");
            }
            else
            {
                download.execute("https://pbs.twimg.com/profile_images/1644359398/14-me-gusta-22vmrft.png");
            }
        }
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

    public class downloadThread extends AsyncTask<String,Void,Bitmap>{

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
                conn.setReadTimeout(10000 /* milliseconds */);
                conn.setConnectTimeout(15000 /* milliseconds */);
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
    }
}
