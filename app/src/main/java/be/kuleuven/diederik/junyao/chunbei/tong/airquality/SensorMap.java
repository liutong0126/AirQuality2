package be.kuleuven.diederik.junyao.chunbei.tong.airquality;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class SensorMap

    extends
    AppCompatActivity
    implements
    OnMapReadyCallback,
    GoogleApiClient.ConnectionCallbacks,
    GoogleApiClient.OnConnectionFailedListener,
    GoogleMap.OnMarkerClickListener,
    LocationListener,
    GoogleMap.OnInfoWindowClickListener,
    NavigationView.OnNavigationItemSelectedListener
{

    private GoogleMap mMap;
    private GoogleApiClient googleApiClient;
    private LocationRequest locationRequest;
    private static final int Request_User_Location_Code = 99;
    private ArrayList markers = new ArrayList<Marker>();
    private Data data;
    private Data timingData;
    private Marker marker;
    private Sensor sensorOfMarker;
    private User user;
    private Set<Measurement> currentV = new HashSet<>();

    private Measurement currentGT = new Measurement(0,0,null,"groupt");
    private Measurement currentAG = new Measurement(0,0,null,"agora");
    private Measurement currentTest = new Measurement(0,0,null,"test");

    private double[] gtCurrent = new double[2];
    private double[] agCurrent = new double[2];
    private String gtDate;
    private String agDate;
    private String testDate;

    private DrawerLayout drawerLayout;
    private LatLng cameraPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor_map);

        checkUserLocationPermission();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        Intent intent = getIntent();
        data = (Data) intent.getSerializableExtra("data");
        user = (User) intent.getSerializableExtra("user");
        timingData = (Data) intent.getSerializableExtra("timingData");

        System.out.println("user first: "+ user.getFirstName());

        getCurrentValue("groupt");
        getCurrentValue("agora");
        getCurrentValue("test");

        drawerLayout = findViewById(R.id.sensor_map_drawer_layout);

        NavigationView navigationView = findViewById(R.id.sensor_map_nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        drawerLayout.addDrawerListener(new DrawerLayout.DrawerListener(){
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {}
            @Override
            public void onDrawerOpened(View drawerView) {}
            @Override
            public void onDrawerClosed(View drawerView) {}
            @Override
            public void onDrawerStateChanged(int newState) {}
        });
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem menuItem) {
        int id = menuItem.getItemId();
        if (id == R.id.sensor_map_refresh) {
            getCurrentValue("groupt");
            getCurrentValue("agora");
            getCurrentValue("test");
            System.out.println("gt PM: "+gtCurrent[0]+" gt CO: "+gtCurrent[1]);
            addSensors();
            Toast.makeText(SensorMap.this,"Successfully refreshed!",Toast.LENGTH_SHORT).show();
        }else if(id == R.id.sensor_map_goBack){
            Intent intent = new Intent(SensorMap.this, Menu.class);
            intent.putExtra("data", data);
            intent.putExtra("user", user);
            intent.putExtra("timingData",timingData);
            intent.putExtra("currentGT",currentGT);
            intent.putExtra("currentAG",currentAG);
            startActivity(intent);
            finish();
        }
        else if(id == R.id.sensor_map_add_sensor){
            Intent intent = new Intent(SensorMap.this, AddSensorMap.class);
            intent.putExtra("latitude", cameraPosition.latitude);
            intent.putExtra("longitude", cameraPosition.longitude);
            intent.putExtra("data", data);
            intent.putExtra("user", user);
            intent.putExtra("timingData",timingData);
            intent.putExtra("currentGT",currentGT);
            intent.putExtra("currentAG",currentAG);
            startActivity(intent);
            finish();
            //Toast.makeText(SensorMap.this,"Click on the map to place a new sensor node!",Toast.LENGTH_SHORT).show();
        }

        //menuItem.setChecked(true);
        // close drawer when item is tapped
        drawerLayout.closeDrawers();

        return true;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnInfoWindowClickListener(this);
        addSensors();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            buildGoogleApiClient();
            mMap.setMyLocationEnabled(true);
            return;
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        try {
            InfoWindowData infoWindowData=(InfoWindowData)marker.getTag();
            sensorOfMarker = infoWindowData.getSensor();
        } catch (NullPointerException e) {
        }
        return false;
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        Intent intent2 = new Intent (SensorMap.this, NewReport.class);
        intent2.putExtra("data", data);
        intent2.putExtra("user", user);
        intent2.putExtra("timingData", timingData);
        InfoWindowData infoWindowData=(InfoWindowData)marker.getTag();
        sensorOfMarker = infoWindowData.getSensor();
        intent2.putExtra("sensor", sensorOfMarker);
        startActivity(intent2);
        finish();
    }

    public boolean checkUserLocationPermission(){
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){

            if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.ACCESS_FINE_LOCATION)){
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},Request_User_Location_Code);
            }
            else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},Request_User_Location_Code);}
            return false;
        }
        else{return true;}
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch(requestCode){
            case Request_User_Location_Code:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED) {
                        if(googleApiClient==null){buildGoogleApiClient();}
                        mMap.setMyLocationEnabled(true);
                    }
                }
                else{Toast.makeText(this,"Permission Denied...",Toast.LENGTH_SHORT).show();}
                break;
        }

    }

    protected synchronized void buildGoogleApiClient(){
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        googleApiClient.connect();
    }

    @Override
    public void onLocationChanged(Location location){

        LatLng latLng = new LatLng(location.getLatitude(),location.getLongitude());

        CameraPosition newCamPos = new CameraPosition(latLng, 14.0f, mMap.getCameraPosition().tilt, mMap.getCameraPosition().bearing);
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(newCamPos), 1000, null);

        if(googleApiClient!=null){LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient,this);}
        cameraPosition = latLng;
    }

    private void getCurrentValue(final String location){
        final String loc=location;
        final Date[] dates = new Date[2];
        final double[] values=new double[2];
        String url = "https://a18ee5air2.studev.groept.be/query/readCurrent.php?location=" + location;
        RequestQueue queue = Volley.newRequestQueue(SensorMap.this);

        System.out.println("Get CurrentValue starts");

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            System.out.println("Into response!");
                            JSONArray jarr = new JSONArray(response);
                            for (int i = 0; i < jarr.length(); i++) {
                                JSONObject jobj = jarr.getJSONObject(i);
                                double currentPM = jobj.getDouble("valuePM");
                                double currentCo=jobj.getDouble("valueCO");
                                String day = jobj.getString("timeStamps");
                                Date date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(day);
                                Measurement m = new Measurement(currentCo,currentPM, date,loc);
                                currentV.add(m);
                                if(location.equals("groupt")){
                                    gtCurrent[0] = currentPM;
                                    gtCurrent[1] = currentCo;
                                    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
                                    gtDate = dateFormat.format(date);
                                    System.out.println("current date: " +gtDate);

                                    currentGT = m;
                                }
                                else if(location.equals("agora")){
                                    agCurrent[0] = currentPM;
                                    agCurrent[1] = currentCo;
                                    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
                                    agDate = dateFormat.format(date);
                                    System.out.println("current date: " +agDate);

                                    currentAG = m;
                                }
                                else if(location.equals("test")){
                                    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
                                    testDate = dateFormat.format(date);
                                    currentTest = m;
                                }
                                values[0]=currentPM;
                                values[1]=currentCo;
                                System.out.println("current date: " +day);
                            }
                            System.out.println("End of response!");
                            System.out.println("currentPM: "+values[0]);
                            System.out.println("currentCO: "+values[1]);


                        } catch (JSONException e) {
                            System.out.println(e);
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(SensorMap.this, "Error...", Toast.LENGTH_SHORT).show();
                error.printStackTrace();
            }
        });
        queue.add(stringRequest);

    }



    private void addSensors(){

        ArrayList sensors = data.getSensors();
        if(!sensors.isEmpty()){
            Iterator<Sensor> it = sensors.iterator();
            while(it.hasNext()){

                Sensor currentSensor = it.next();

                LatLng latlng = new LatLng(currentSensor.getXcoordinate(),currentSensor.getYcoordinate());//latitude is xcoordinate, longitude is ycoordinate

                marker = findMarkerInListByPosition(latlng);

                if(marker!=null){
                    markers.remove(marker);
                    marker.remove();
                }

                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.position(latlng).icon(BitmapDescriptorFactory.defaultMarker(143));

                InfoWindowData info = new InfoWindowData();
                info.setLocation(currentSensor.getLocation());

                //next part needs improvement -> so it is applicable to a random amount of sensors
                if(currentSensor.getLocation().equals("groupt")){
                    info.setPmValue("PM value: " + currentGT.getPmValue());
                    info.setCoValue("CO value: " + currentGT.getCoValue());

                    if(currentGT.getPmValue()<=30 && currentGT.getCoValue()<=1){
                        markerOptions.position(latlng).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                    }
                    else if((currentGT.getPmValue()>30 && currentGT.getPmValue()<=60) || (currentGT.getCoValue()>1 && currentGT.getCoValue()<=2)){
                        markerOptions.position(latlng).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
                    }
                    else if((currentGT.getPmValue()>60 && currentGT.getPmValue()<=90) || (currentGT.getCoValue()>2 && currentGT.getCoValue()<=10)){
                        markerOptions.position(latlng).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW));
                    }
                    else if((currentGT.getPmValue()>90 && currentGT.getPmValue()<=120) || (currentGT.getCoValue()>10 && currentGT.getCoValue()<=17)){
                        markerOptions.position(latlng).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));
                    }
                    else if((currentGT.getPmValue()>120 && currentGT.getPmValue()<=250) || (currentGT.getCoValue()>17 && currentGT.getCoValue()<=34)){
                        markerOptions.position(latlng).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                    }
                    else if(currentGT.getPmValue()>250 || currentGT.getCoValue()>34){
                        markerOptions.position(latlng).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET));
                    }
//                    info.setPmValue("PM value: " + gtCurrent[0]);
//                    info.setCoValue("CO value: " + gtCurrent[1]);
                    info.setDate("Last update time: "+gtDate);
                }else if(currentSensor.getLocation().equals("agora")){
                    info.setPmValue("PM value: " + agCurrent[0]);
                    info.setCoValue("CO value: " + agCurrent[1]);


                    if(currentAG.getPmValue()<=30 && currentAG.getCoValue()<=1){
                        markerOptions.position(latlng).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                    }
                    else if((currentAG.getPmValue()>30 && currentAG.getPmValue()<=60) || (currentAG.getCoValue()>1 && currentAG.getCoValue()<=2)){
                        markerOptions.position(latlng).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
                    }
                    else if((currentAG.getPmValue()>60 && currentAG.getPmValue()<=90) || (currentAG.getCoValue()>2 && currentAG.getCoValue()<=10)){
                        markerOptions.position(latlng).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW));
                    }
                    else if((currentAG.getPmValue()>90 && currentAG.getPmValue()<=120) || (currentAG.getCoValue()>10 && currentAG.getCoValue()<=17)){
                        markerOptions.position(latlng).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));
                    }
                    else if((currentAG.getPmValue()>120 && currentAG.getPmValue()<=250) || (currentAG.getCoValue()>17 && currentAG.getCoValue()<=34)){
                        markerOptions.position(latlng).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                    }
                    else if(currentAG.getPmValue()>250 || currentAG.getCoValue()>34){
                        markerOptions.position(latlng).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET));
                    }

                    info.setDate("Last update time: " + agDate);
                }
                else if(currentSensor.getLocation().equals("test")){
                    info.setPmValue("PM value: " + currentTest.getPmValue());
                    info.setCoValue("CO value: " + currentTest.getCoValue());

                    if(currentTest.getPmValue()<=30 && currentTest.getCoValue()<=1){
                        markerOptions.position(latlng).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                    }
                    else if((currentTest.getPmValue()>30 && currentTest.getPmValue()<=60) || (currentTest.getCoValue()>1 && currentTest.getCoValue()<=2)){
                        markerOptions.position(latlng).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
                    }
                    else if((currentTest.getPmValue()>60 && currentTest.getPmValue()<=90) || (currentTest.getCoValue()>2 && currentTest.getCoValue()<=10)){
                        markerOptions.position(latlng).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW));
                    }
                    else if((currentTest.getPmValue()>90 && currentTest.getPmValue()<=120) || (currentTest.getCoValue()>10 && currentTest.getCoValue()<=17)){
                        markerOptions.position(latlng).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));
                    }
                    else if((currentTest.getPmValue()>120 && currentTest.getPmValue()<=250) || (currentTest.getCoValue()>17 && currentTest.getCoValue()<=34)){
                        markerOptions.position(latlng).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                    }
                    else if(currentTest.getPmValue()>250 || currentTest.getCoValue()>34){
                        markerOptions.position(latlng).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET));
                    }
//                    info.setPmValue("PM value: " + gtCurrent[0]);
//                    info.setCoValue("CO value: " + gtCurrent[1]);
                    info.setDate("Last update time: "+testDate);
                }
                info.setSensor(currentSensor);

                CustomInfoWindow customInfoWindow = new CustomInfoWindow(this);
                mMap.setInfoWindowAdapter(customInfoWindow);

                marker = mMap.addMarker(markerOptions);
                marker.setTag(info);
                markers.add(marker);
            }
        }
    }

    private Marker findMarkerInListByPosition(LatLng latlng) {
        Iterator<Marker> ite = markers.iterator();
        while(ite.hasNext()){
            Marker currentMarker = ite.next();
            if(currentMarker.getPosition().longitude==latlng.longitude && currentMarker.getPosition().latitude==latlng.latitude){
                return currentMarker;
            }
        }
        return null;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(1100);
        locationRequest.setFastestInterval(1100);
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)== PackageManager.PERMISSION_GRANTED){
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);}
    }

    @Override
    public void onConnectionSuspended(int i){}

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult){}
}


