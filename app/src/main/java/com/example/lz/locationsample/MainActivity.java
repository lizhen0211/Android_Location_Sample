package com.example.lz.locationsample;

import android.app.Activity;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Iterator;

public class MainActivity extends Activity {

    private static final int MSG_TYPE_LOCATION_CHANGE = 1;

    private Button btn;

    private TextView fixSatellitesTextView;

    private TextView lastKnownLocationTextView;

    private TextView locationProviderTextView;

    private ArrayAdapter<String> adapter;

    private LocationProxy locationProxy;

    private Location currentBestLocation;

    private String provider;

    private ArrayList<String> locationArrayList = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initUIComponent();
        locationProxy = new LocationProxy(MainActivity.this);
        locationProxy.getLocationManager().addGpsStatusListener(gpsStatusListener);
    }

    @Override
    protected void onStart() {
        super.onStart();

        specifyProvider();

//        Criteria criteria = new Criteria();
//        provider = locationProxy.getLocationManager().getBestProvider(criteria, false);
        locationProxy.requestLocationUpdates(provider,
                LocationProxy.MIN_TIME_BW_UPDATES_DEFAULT * 0,
                LocationProxy.MIN_DISTANCE_CHANGE_FOR_UPDATES_DEFAULT,
                locationListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        locationProviderTextView.setText(locationProviderTextView.getHint() + provider);
    }

    private void specifyProvider() {
        if (locationProxy.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            provider = LocationManager.GPS_PROVIDER;
        } else {
            provider = LocationManager.NETWORK_PROVIDER;
        }
    }

    private void initUIComponent() {
        ListView locationListView = (ListView) findViewById(R.id.location_list);
        adapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_expandable_list_item_1,
                locationArrayList);
        locationListView.setAdapter(adapter);
        lastKnownLocationTextView = (TextView) findViewById(R.id.last_known_location);
        fixSatellitesTextView = (TextView) findViewById(R.id.fix_satellites);
        locationProviderTextView = (TextView) findViewById(R.id.location_provider);
        btn = (Button) findViewById(R.id.get_last_known_location_btn);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Location lastKnownLocation = locationProxy.getLastKnownLocation(provider);
                lastKnownLocationTextView.setText(lastKnownLocationTextView.getHint() + (lastKnownLocation == null ? null : lastKnownLocation.toString()));
            }
        });
    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_TYPE_LOCATION_CHANGE:
                    if (msg.obj instanceof Location) {
                        Location location = (Location) msg.obj;
                        locationArrayList.add(location.toString());
                        adapter.notifyDataSetChanged();
                    }
                    break;
            }
        }
    };


    GpsStatus.Listener gpsStatusListener = new GpsStatus.Listener() {
        @Override
        public void onGpsStatusChanged(int event) {
            switch (event) {
                case GpsStatus.GPS_EVENT_FIRST_FIX:
                    //Toast.makeText(MainActivity.this, "GPS_EVENT_FIRST_FIX", Toast.LENGTH_SHORT).show();
                    break;
                case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                    GpsStatus gpsStatus = locationProxy.getLocationManager().getGpsStatus(null);
                    Iterable<GpsSatellite> satellites = gpsStatus.getSatellites();
                    Iterator<GpsSatellite> iterator = satellites.iterator();
                    int satelliteCount = 0;
                    while (iterator.hasNext()) {
                        GpsSatellite gpsSatellite = iterator.next();
                        if (gpsSatellite.usedInFix()) {
                            satelliteCount++;
                        }
                    }
                    fixSatellitesTextView.setText(fixSatellitesTextView.getHint().toString() + satelliteCount + "颗");
                    break;
                case GpsStatus.GPS_EVENT_STARTED:
                    //Toast.makeText(MainActivity.this, "GPS_EVENT_STARTED", Toast.LENGTH_SHORT).show();
                    break;
                case GpsStatus.GPS_EVENT_STOPPED:
                    //Toast.makeText(MainActivity.this, "GPS_EVENT_STOPPED", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    // Define a listener that responds to location updates
    LocationListener locationListener = new LocationListener() {
        public void onLocationChanged(Location location) {
            // Called when a new location is found by the network location provider.
            //makeUseOfNewLocation(location);
            if (locationProxy.isBetterLocation(location, currentBestLocation)) {
                currentBestLocation = location;
                Message message = handler.obtainMessage();
                message.what = MSG_TYPE_LOCATION_CHANGE;
                message.obj = location;
                handler.sendMessage(message);
            }
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
            switch (status) {
                case LocationProvider.AVAILABLE:
                    Toast.makeText(MainActivity.this, "onStatusChanged:available", Toast.LENGTH_SHORT).show();
                    break;
                case LocationProvider.OUT_OF_SERVICE:
                    Toast.makeText(MainActivity.this, "onStatusChanged:out of service", Toast.LENGTH_SHORT).show();
                    break;
                case LocationProvider.TEMPORARILY_UNAVAILABLE:
                    Toast.makeText(MainActivity.this, "onStatusChanged:temporarily unavailable", Toast.LENGTH_SHORT).show();
                    break;
            }

        }

        public void onProviderEnabled(String provider) {
            Toast.makeText(MainActivity.this, "onProviderEnabled", Toast.LENGTH_SHORT).show();
        }

        public void onProviderDisabled(String provider) {
            Toast.makeText(MainActivity.this, "onProviderDisabled", Toast.LENGTH_SHORT).show();
        }
    };


    @Override
    protected void onPause() {
        super.onPause();
        locationProxy.removeUpdates(locationListener);
    }
}
