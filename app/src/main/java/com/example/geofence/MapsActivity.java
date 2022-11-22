package com.example.geofence;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.PendingIntent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback  {

    private static final String TAG = "MapsActivity";

    private GoogleMap mMap;
    private GeofencingClient geofencingClient;
    private GeofenceHelper geofenceHelper;

    private float GEOFENCE_RADIUS = 100;
    private String GEOFENCE_ID = "SOME_GEOFENCE_ID";

    private int FINE_LOCATION_ACCESS_REQUEST_CODE = 10001;
    private int BACKGROUND_LOCATION_ACCESS_REQUEST_CODE = 10002;
    ArrayList<LatLng> latLngList;
    private Button mSimulate;
    private DatabaseReference mDatabase;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        mapFragment.getMapAsync(this);
        mSimulate = (Button)findViewById(R.id.simulate);
        geofencingClient = LocationServices.getGeofencingClient(this);
        geofenceHelper = new GeofenceHelper(this);


        mSimulate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.e("","Simulate the locations");
                /*PositionSimulator positionSimulator = new PositionSimulator();
                // Read the gpx file from the device
                PositionSimulator.PlaybackError pbError = positionSimulator.startPlayback("/path_to_your_file/your_file_name.gpx");
                if(pbError != PositionSimulator.PlaybackError.NONE){
                    Log.e(TAG, pbError.toString());
                }*/

            }
        });

    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera

        LatLng bangalore = new LatLng(12.954179, 77.4989981);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(bangalore, 16));
        enableUserLocation();


        /** Get Geofences from firebase data*/
        mDatabase = FirebaseDatabase.getInstance().getReference("Geofences/"); //Getting root reference
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.e("","..data snapshot..."+dataSnapshot.getValue());
                latLngList = new ArrayList<>();
                for (final DataSnapshot childSnapshot: dataSnapshot.getChildren()){
                    //Log.e("","child snapshot"+childSnapshot.child("latitude").getValue());
                    //Log.e("","child snapshot"+childSnapshot.child("longitude").getValue());
                    double lat= (double) childSnapshot.child("latitude").getValue();
                    double log= (double) childSnapshot.child("longitude").getValue();
                    latLngList.add(new LatLng(lat, log));
                }
                //add geofences
                start();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


    }


    private void enableUserLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        } else {
            //Ask for permission
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                //We need to show user a dialog for displaying why the permission is needed and then ask for the permission...
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, FINE_LOCATION_ACCESS_REQUEST_CODE);
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, FINE_LOCATION_ACCESS_REQUEST_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == FINE_LOCATION_ACCESS_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //We have the permission
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                mMap.setMyLocationEnabled(true);
            } else {
                //We do not have the permission..

            }
        }

        if (requestCode == BACKGROUND_LOCATION_ACCESS_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //We have the permission
                Toast.makeText(this, "You can add geofences...", Toast.LENGTH_SHORT).show();
            } else {
                //We do not have the permission..
                Toast.makeText(this, "Background location access is neccessary for geofences to trigger...", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**Module to add 20+ Geofence*/
    private void start() {


        /*latLngList = new ArrayList<>();
        latLngList.add(new LatLng(12.953013054035946, 77.5417514266668));
        latLngList.add(new LatLng(12.95428866232216, 77.5438757362066));
        latLngList.add(new LatLng(12.95558517552543, 77.54565672299249));
        latLngList.add(new LatLng(12.956442543452548, 77.54752354046686));
        latLngList.add(new LatLng(12.95675621390793,77.54919723889215));
        latLngList.add(new LatLng(12.957069883968225,77.55112842938287));
        latLngList.add(new LatLng(12.957711349517467,77.55308710458465));
        latLngList.add(new LatLng(12.958464154110917,77.55514704110809));
        latLngList.add(new LatLng(12.959656090062529,77.5559409749765));
        latLngList.add(new LatLng(12.960814324441305,77.5574167738546));
        latLngList.add(new LatLng(12.961253455257907,77.5592192183126));
        latLngList.add(new LatLng(12.96156308861349,77.56126500049922));
        latLngList.add(new LatLng(12.961814019848779,77.56308890262935));
        latLngList.add(new LatLng(12.962775920574138,77.56592131534907));
        latLngList.add(new LatLng(12.963340512746937,77.5676379291186));
        latLngList.add(new LatLng(12.96411114676894,77.56951526792183));
        latLngList.add(new LatLng(12.964382986146292, 77.5717254081501));
        latLngList.add(new LatLng(12.964584624077109, 77.57370388966811));
        latLngList.add(new LatLng(12.964542802687657, 77.57591402989638));
        latLngList.add(new LatLng(12.963795326167373, 77.57798997552734));
        latLngList.add(new LatLng(12.96358621848664, 77.58015720041138));
        latLngList.add(new LatLng(12.963481664580394, 77.58189527185303));*/


        /**Database query to add the data
        DatabaseReference mRef;
        int count=1;
        for(LatLng latLng1 : latLngList) {
            count++;
           mRef= mDatabase.child("Location" + count);
           LatLong data = new LatLong(latLng1.latitude,latLng1.longitude);
           mRef.setValue(data);
        }*/




        if (Build.VERSION.SDK_INT >= 29) {
            //We need background permission
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                handleMap(latLngList);
            } else {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                    //We show a dialog and ask for permission
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, BACKGROUND_LOCATION_ACCESS_REQUEST_CODE);
                } else {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, BACKGROUND_LOCATION_ACCESS_REQUEST_CODE);
                }
            }

        } else {
            handleMap(latLngList);
        }
    }



    private void handleMap(ArrayList<LatLng> latLng) {
        //ArrayList<Geofence> geofenceList = new ArrayList<>();
        for(LatLng latLng1 : latLngList) {
           // mMap.clear();

            addMarker(latLng1);
            addCircle(latLng1, GEOFENCE_RADIUS);
            addGeofence(latLng1, GEOFENCE_RADIUS);
        }
    }

    private void addGeofence(LatLng latLng, float radius) {
        Geofence geofence = geofenceHelper.getGeofence(GEOFENCE_ID, latLng, radius, Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_DWELL | Geofence.GEOFENCE_TRANSITION_EXIT);
        GeofencingRequest geofencingRequest = geofenceHelper.getGeofencingRequest(geofence);


        PendingIntent pendingIntent = geofenceHelper.getPendingIntent();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        geofencingClient.addGeofences(geofencingRequest, pendingIntent)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.e(TAG, "onSuccess: Geofence Added...");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        String errorMessage = geofenceHelper.getErrorString(e);
                        Log.e(TAG, "onFailure: " + errorMessage);
                    }
                });
    }

    private void addMarker(LatLng latLng) {
        MarkerOptions markerOptions = new MarkerOptions().position(latLng);
        mMap.addMarker(markerOptions);
    }

    private void addCircle(LatLng latLng, float radius) {
        CircleOptions circleOptions = new CircleOptions();
        circleOptions.center(latLng);
        circleOptions.radius(radius);
        circleOptions.strokeColor(Color.argb(255, 0, 0,255));
        circleOptions.fillColor(Color.argb(64, 0, 0,255));
        circleOptions.strokeWidth(4);
        mMap.addCircle(circleOptions);
    }
}
