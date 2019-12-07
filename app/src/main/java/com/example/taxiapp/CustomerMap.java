package com.example.taxiapp;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class CustomerMap extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    Location currentLocation;
    FusedLocationProviderClient fusedLocationProviderClient;
    private static final int REQUEST_CODE = 101;
    LocationRequest locationRequest;
    Button btn_logout, btn_request;
    LatLng pickupLocation;
    private int searchRadius = 1;
    private boolean isDriverFound = false;
    private String nearestDriverId;
    DatabaseReference reference;
    DatabaseReference nearestDriverLocation;
    private Marker driverMarker;


    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_map);

        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setDefaultFontPath("fonts/Arkhip_font.ttf")
                .setFontAttrId(R.attr.fontPath).build());

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        startLocationUpdates();
        getLastLocation();

        reference = FirebaseDatabase.getInstance().getReference("DriversAvaliable");

        btn_logout = (Button) findViewById(R.id.btn_logout);
        btn_logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showQuitDialog();
            }
        });

        btn_request = (Button) findViewById(R.id.btn_call_taxi);
        btn_request.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("CustomerRequest");

                GeoFire geoFire = new GeoFire(ref);
                geoFire.setLocation(userId, new GeoLocation(currentLocation.getLatitude(), currentLocation.getLongitude()), new GeoFire.CompletionListener() {
                    @Override
                    public void onComplete(String key, DatabaseError error) {
                        if (error != null) {
                            Toast.makeText(CustomerMap.this,"There was an error saving the location to GeoFire: " + error, Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(CustomerMap.this,"Your location was saved successfully", Toast.LENGTH_LONG).show();
                        }
                    }
                });

                pickupLocation = new  LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
                mMap.addMarker(new MarkerOptions().position(pickupLocation).icon(BitmapDescriptorFactory.fromResource(R.drawable.passenger)).title("ЗАБЕРИТЕ ОТСЮДА"));

                btn_request.setText("МЫ ИЩЕМ ВОДИТЕЛЯ ...");

                gettingNearestTaxi();
            }
        });
    }

    private void gettingNearestTaxi(){

        GeoFire geoFire = new GeoFire(reference);
        GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(currentLocation.getLatitude(), currentLocation.getLongitude()), searchRadius);

        geoQuery.removeAllListeners();

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if (!isDriverFound){
                    isDriverFound = true;
                    nearestDriverId = key;

                    getNearestDriverLocation();
                }
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {

                if (!isDriverFound) {
                    searchRadius++;
                    gettingNearestTaxi();
                }

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }

    private void getNearestDriverLocation() {
        btn_request.setText(" Получаем локацию водителя ...");

        nearestDriverLocation = FirebaseDatabase.getInstance().getReference("DriversAvaliable")
        .child(nearestDriverId).child("l");

        nearestDriverLocation.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    List<Object> driverLocationParametres = (List<Object>) dataSnapshot.getValue();

                    double latitude = 0;
                    double longitude = 0;

                    if (driverLocationParametres.get(0) != null){
                        latitude = Double.parseDouble(driverLocationParametres.get(0).toString());
                    }

                    if (driverLocationParametres.get(1) != null){
                        longitude = Double.parseDouble(driverLocationParametres.get(1).toString());
                    }

                    LatLng driverLatLng = new LatLng(latitude, longitude);

                    if (driverMarker != null){
                        driverMarker.remove();
                    }

                    Location driverLocation = new Location("");
                    driverLocation.setLongitude(longitude);
                    driverLocation.setLatitude(latitude);

                    double distanceToDriver = driverLocation.distanceTo(currentLocation);

                    btn_request.setText("Растояние до водителя: " + distanceToDriver + " m");

                    driverMarker = mMap.addMarker(new MarkerOptions().position(driverLatLng)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.taxi_car)).title("Ваш водитель находится здесь"));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(CustomerMap.this,databaseError.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showQuitDialog (){
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("ВЫ УВЕРЕНЫ?");

        dialog.setPositiveButton("ДА", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(CustomerMap.this, CustomerActivity.class));
            }
        });

        dialog.setNegativeButton("НЕТ",     new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void startLocationUpdates () {

        locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(4000);
        locationRequest.setFastestInterval(2000);

        fusedLocationProviderClient.requestLocationUpdates(locationRequest, new LocationCallback(){
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
            }
        }, getMainLooper());
    }

    private void getLastLocation() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)!= PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE);

            return;
        }

        Task<Location> task = fusedLocationProviderClient.getLastLocation();
        task.addOnSuccessListener(  new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {

                currentLocation = location;

                SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map2);

                mapFragment.getMapAsync(CustomerMap.this);
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;

        LatLng latLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));

        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == REQUEST_CODE){
            if (grantResults.length >0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                getLastLocation();
            }
        }
    }
}