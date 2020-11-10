package com.example.assessment;

import android.Manifest;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;

import android.content.SharedPreferences;

import android.content.Intent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;


import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import android.location.LocationManager;
import android.location.LocationListener;
import android.location.Location;
import android.content.Context;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;


public class MainActivity extends AppCompatActivity implements LocationListener {

    MapView mv;

    Double latitude;
    Double longitude;

    ItemizedIconOverlay<OverlayItem> items;
    ItemizedIconOverlay.OnItemGestureListener<OverlayItem> markerGestureListener;

    OverlayItem addedPOI;

    ItemizedIconOverlay<OverlayItem> webItems;

    File file = new File((Environment.getExternalStorageDirectory().getAbsolutePath()
            + "/POI.csv"));

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        // This line sets the user agent, a requirement to download OSM maps
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));

        setContentView(R.layout.activity_main);

        mv = (MapView) findViewById(R.id.map1);

        mv.setMultiTouchControls(true);
        mv.getController().setZoom(25);

        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);

        LocationManager mgr = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        mgr.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
        mgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);

        markerGestureListener = new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>()
        {
            public boolean onItemLongPress(int i, OverlayItem item)
            {
                //split the snippet into an array including type and description
                String[] snippet = item.getSnippet().split(",");
                Toast.makeText(MainActivity.this, "Name: " + item.getTitle()
                        + "\nType: " + snippet[0] + "\nDescription: " + snippet[1], Toast.LENGTH_LONG).show();
                return true;
            }

            public boolean onItemSingleTapUp(int i, OverlayItem item)
            {
                //split the snippet into an array including type and description
                String[] snippet = item.getSnippet().split(",");
                Toast.makeText(MainActivity.this, "Name: " + item.getTitle()
                        + "\nType: " + snippet[0] + "\nDescription: " + snippet[1], Toast.LENGTH_SHORT).show();
                return true;
            }
        };

        items = new ItemizedIconOverlay<OverlayItem>(this, new ArrayList<OverlayItem>(), markerGestureListener);

        //load all markers from file

        if (file.exists()) {
            try {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                String line;

                while ((line = reader.readLine()) != null) {
                    String[] readPOI = line.split(",");
                    if (readPOI.length == 5) {
                        Double lat = Double.parseDouble(readPOI[3]);
                        Double lon = Double.parseDouble(readPOI[4]);
                        OverlayItem newItem = new OverlayItem(readPOI[0], readPOI[1] + "," + readPOI[2], new GeoPoint(lat, lon));
                        try {
                            items.addItem(newItem);
                            mv.getOverlays().add(items);
                        } catch (Exception e) {
                            new AlertDialog.Builder(this).setMessage(e.toString()).setPositiveButton("OK", null).show();
                        }
                    }
                }
                reader.close();
            } catch (Exception e) {
                new AlertDialog.Builder(this).setMessage(e.toString()).setPositiveButton("OK", null).show();
            }
        }
        else {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        LoadFromWeb lfw = new LoadFromWeb();
        lfw.execute();

        // My Location Overlay
        MyLocationNewOverlay myLocationoverlay = new MyLocationNewOverlay(mv);
        myLocationoverlay.enableMyLocation(); // not on by default
        myLocationoverlay.enableFollowLocation();
        mv.getOverlays().add(myLocationoverlay);

    }

    public void onLocationChanged(Location newLoc)
    {
        latitude = newLoc.getLatitude();
        longitude = newLoc.getLongitude();
        mv.getController().setCenter(new GeoPoint(latitude, longitude));
    }

    public void onProviderDisabled(String provider)
    {

    }

    public void onProviderEnabled(String provider)
    {

    }

    public void onStatusChanged(String provider,int status,Bundle extras)
    {

    }



    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == R.id.createPOI)
        {
            Intent intent = new Intent(this, PointOfInterestActivity.class);

            intent.putExtra("latitude", latitude);
            intent.putExtra("longitude", longitude);
            startActivityForResult(intent,0);
            return true;
        }
        else if (item.getItemId() == R.id.prefs)
        {
            Intent intent = new Intent(this, PreferencesActivity.class);
            startActivityForResult(intent,1);
            return true;
        }
        return false;
    }


    protected void onActivityResult(int requestCode,int resultCode,Intent intent)
    {
        if(requestCode==0)
        {
            if (resultCode == RESULT_OK)
            {
                Bundle extras = intent.getExtras();
                //if the data passed back isn't null
                if (extras != null) {
                    //get all data out of the bundle into separate variables
                    String name = extras.getString("name");
                    String type = extras.getString("type");
                    String desc = extras.getString("desc");
                    Double lat = extras.getDouble("lat");
                    Double lon = extras.getDouble("lon");

                    //create new overlay item including all of the details grabbed from the bundle
                    OverlayItem newItem = new OverlayItem(name, type
                            + "," + desc, new GeoPoint(lat, lon));
                    //add the new marker to the map
                    items.addItem(newItem);
                    mv.getOverlays().add(items);

                    try {
                        PrintWriter printwriter = new PrintWriter(new FileWriter(file));
                        //loop through each item in the marker list
                        for (int i = 0; i < items.size(); i++)
                        {
                            //find the data of the marker to be added

                            addedPOI = items.getItem(i);
                            double latAdd = addedPOI.getPoint().getLatitude();
                            double lonAdd = addedPOI.getPoint().getLongitude();

                            //split the snippet into an array including type and description
                            String[] snippet = addedPOI.getSnippet().split(",");

                            //save it to the csv file
                            printwriter.println(addedPOI.getTitle() + ","
                             + snippet[0] + "," + snippet[1] + "," + latAdd + ","
                             + lonAdd);

                        }
                        printwriter.close();
                        //upload only most recent poi added when the button is pressed
                        //get checkbox preference of uploading to web
                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                        boolean uploadToWeb = prefs.getBoolean("uploadToWeb", false);

                        //Do something with preference data
                        if (uploadToWeb)
                        {
                            //call async task to save to web
                            if (addedPOI != null) {
                                SaveToWeb stw = new SaveToWeb();
                                stw.execute(addedPOI);
                            }
                        }
                    }
                    catch(Exception e) {
                        new AlertDialog.Builder(this).setMessage(e.toString()).setPositiveButton("OK", null).show();
                    }

                }
            }
        }
    }

    class LoadFromWeb extends AsyncTask<Void,Void,String>
    {
        public String doInBackground(Void... unused)
        {
            HttpURLConnection conn = null;
            try
            {
                URL url = new URL("http://www.free-map.org.uk/course/mad/ws/get.php?year=19&username=user008&format=csv");
                conn = (HttpURLConnection) url.openConnection();
                InputStream in = conn.getInputStream();
                webItems = new ItemizedIconOverlay<OverlayItem>(MainActivity.this, new ArrayList<OverlayItem>(), markerGestureListener);
                if(conn.getResponseCode() == 200)
                {
                    BufferedReader br = new BufferedReader(new InputStreamReader(in));
                    String result = "", line;
                    while((line = br.readLine()) !=null)
                    {
                        String[] webPOI = line.split(",");
                        if (webPOI.length == 5) {
                            Double lat = Double.parseDouble(webPOI[4]);
                            Double lon = Double.parseDouble(webPOI[3]);
                            OverlayItem newItem = new OverlayItem(webPOI[0], webPOI[1]
                                    + "," + webPOI[2], new GeoPoint(lat, lon));
                            webItems.addItem(newItem);
                        }
                        result += line;
                        mv.getOverlays().add(webItems);
                    }
                    return result;
                }
                else
                {
                    return "HTTP ERROR: " + conn.getResponseCode();
                }
            }
            catch(IOException e)
            {
                return e.toString();
            }
            finally
            {
                if(conn!=null)
                {
                    conn.disconnect();
                }
            }
        }
    }


    class SaveToWeb extends AsyncTask<OverlayItem,Void,String>
    {
        public String doInBackground(OverlayItem... poiOverlay)
        {
            //get the most recent overlay item
            OverlayItem uploadPOI = poiOverlay[0];
            //get all info on most recent poi
            String name = uploadPOI.getTitle();

            //split snippet into type and desc
            String[] snippet = uploadPOI.getSnippet().split(",");
            String type = snippet[0];
            String desc = snippet[1];
            Double lat = uploadPOI.getPoint().getLatitude();
            Double lon = uploadPOI.getPoint().getLongitude();

            HttpURLConnection conn = null;
            try
            {
                URL url = new URL("http://www.free-map.org.uk/course/mad/ws/add.php");
                conn = (HttpURLConnection) url.openConnection();

                String postData = "username=user008&name=" + name
                        + "&type=" + type + "&description=" + desc + "&lat="
                        + lat + "&lon=" + lon + "&year=19";
                // For POST
                conn.setDoOutput(true);
                conn.setFixedLengthStreamingMode(postData.length());

                OutputStream out;
                out = conn.getOutputStream();
                out.write(postData.getBytes());
                if(conn.getResponseCode() == 200)
                {
                    InputStream in = conn.getInputStream();
                    BufferedReader br = new BufferedReader(new InputStreamReader(in));
                    String all = "", line;
                    while((line = br.readLine()) !=null) {
                        all += line;
                    }
                    return all;
                }
                else
                {
                    return "HTTP ERROR: " + conn.getResponseCode();
                }
            }
            catch(IOException e)
            {
                return e.toString();
            }
            finally
            {
                if(conn!=null)
                {
                    conn.disconnect();
                }
            }
        }
    }
}
