package com.olson.whereis;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.provider.Settings;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.text.DateFormat;
import java.util.Date;

public class BlankActivity extends FragmentActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener, OnMapReadyCallback {

    public static String TAG = "BlankActivity";
    public static GoogleApiClient mGoogleApiClient;
    public static Location mLastLocation;
    public static Location mCurrentLocation;
    public static String mLastUpdateTime;
    public static boolean mRequestingLocationUpdates;

    public static LocationRequest mLocationRequest;

    private Context context;
    private TextView mLastUpdateTimeTextView;
    private TextView mLatitudeTextView;
    private TextView mLongitudeTextView;
    private CheckBox mRequestingLocationCheckBox;
    private GoogleMap mGoogleMap;
    public static StringBuilder sb;

    private static final String REQUESTING_LOCATION_UPDATES_KEY = "REQUESTING_LOCATION_UPDATES_KEY";
    private static final String LOCATION_KEY = "LOCATION_KEY";
    private static final String LAST_UPDATED_TIME_STRING_KEY = "LAST_UPDATED_TIME_STRING_KEY";

    private static boolean isAirplaneModeOn(Context context) {
        return Settings.System.getInt(context.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
    }

    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
        mGoogleApiClient.connect();
//        mGoogleMap = ((MapView) findViewById(R.id.mapView)).getMapAsync(this);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //super.onStart();
    }

    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this.getBaseContext();
        setContentView(R.layout.activity_blank);

        mLocationRequest = LocationRequest.create().setInterval(10000);
        mLatitudeTextView = (TextView) findViewById(R.id.mLatitudeTextView);
        mLongitudeTextView = (TextView) findViewById(R.id.mLongitudeTextView);
        mLastUpdateTimeTextView = (TextView) findViewById(R.id.mLastUpdateTimeTextView);
        mRequestingLocationCheckBox = (CheckBox) findViewById(R.id.mRequestingLocationUpdates);

        updateValuesFromBundle(savedInstanceState);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
        Log.d(TAG, "Created the client: " + mGoogleApiClient);
    }

    protected void startLocationUpdates() {
        if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION)) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        } else {
            Log.i(TAG, "Location Services cannot poll for location updates: Permissions.");
        }
    }
    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    void getLastLocation(View view) throws SecurityException {
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        Log.d(TAG, "mLastLocation="+mLastLocation);
        sb = new StringBuilder();
        if (mLastLocation != null) {
            sb.append(String.valueOf(mLastLocation.getLatitude()));
            sb.append("; ");
            sb.append(String.valueOf(mLastLocation.getLongitude()));
            TextView locationInformation = (TextView) findViewById(R.id.locationInformation);
            locationInformation.setText(sb.toString());

            onLocationChanged(mLastLocation);

            SmsManager smsManager = SmsManager.getDefault();
			//							twitter = 40404; msg other account.s
            smsManager.sendTextMessage("", null, "" + sb.toString(), null, null);

            Log.d(TAG, sb.toString());
        } else {
            Log.d(TAG, "no last location found.");
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) throws SecurityException {

        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        sb = new StringBuilder();
        if (mLastLocation != null) {
            sb.append(String.valueOf(mLastLocation.getLatitude()));
            sb.append("; ");
            sb.append(String.valueOf(mLastLocation.getLongitude()));
        }

        if (mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    public void onLocationChanged(Location location) {
        mCurrentLocation = location;
        mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());

        LatLng currentLL = new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
        mGoogleMap.addMarker(new MarkerOptions().position(currentLL).title("Marker"));
        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLL, 10));
        mGoogleMap.setMaxZoomPreference(10);
        mGoogleMap.setMinZoomPreference(10);
        updateUI();
    }

    private void updateUI() {
        mLatitudeTextView.setText(String.valueOf(mCurrentLocation.getLatitude()));
        mLongitudeTextView.setText(String.valueOf(mCurrentLocation.getLongitude()));
        mLastUpdateTimeTextView.setText(mLastUpdateTime);
        mRequestingLocationCheckBox.setChecked(mRequestingLocationUpdates);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mGoogleApiClient.isConnected() && !mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;
        try {
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

            if (null != mLastLocation) {
                LatLng currentLL = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());

                mGoogleMap.addMarker(new MarkerOptions().position(currentLL).title("Marker"));
                mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(currentLL));
            } else {
                Toast.makeText(getBaseContext(), "Could not pull current location information.", Toast.LENGTH_SHORT);
            }
        } catch (SecurityException se) {
            Log.d(TAG, "SecurityException: " + se);
        }
    }

    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean(REQUESTING_LOCATION_UPDATES_KEY, mRequestingLocationUpdates);
        savedInstanceState.putParcelable(LOCATION_KEY, mCurrentLocation);
        savedInstanceState.putString(LAST_UPDATED_TIME_STRING_KEY, mLastUpdateTime);
        super.onSaveInstanceState(savedInstanceState);
    }

    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            if (savedInstanceState.keySet().contains(REQUESTING_LOCATION_UPDATES_KEY)) {
                mRequestingLocationUpdates = savedInstanceState.getBoolean(REQUESTING_LOCATION_UPDATES_KEY);
//                setButtonsEnabledState();
            }
            if (savedInstanceState.keySet().contains(LOCATION_KEY)) {
                mCurrentLocation = savedInstanceState.getParcelable(LOCATION_KEY);
            }
            if (savedInstanceState.keySet().contains(LAST_UPDATED_TIME_STRING_KEY)) {
                mLastUpdateTime = savedInstanceState.getString(LAST_UPDATED_TIME_STRING_KEY);
            }
            updateUI();
        }
    }

}
