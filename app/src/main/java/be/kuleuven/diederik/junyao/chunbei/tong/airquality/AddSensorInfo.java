package be.kuleuven.diederik.junyao.chunbei.tong.airquality;

import android.app.AlertDialog;
import android.app.Service;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

public class AddSensorInfo extends AppCompatActivity implements View.OnClickListener , NavigationView.OnNavigationItemSelectedListener {

    private double latitude;
    private double longitude;
    private Data data;
    private User user;
    private Data timingData;
    private Measurement currentGT;
    private Measurement currentAG;

    private TextView lat;
    private TextView lng;

    private DrawerLayout drawerLayout;

    private EditText ID;
    private EditText Location;

    private Button add;

    AlertDialog.Builder builder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_sensor_info);

        Intent intent = getIntent();
        latitude = (double) intent.getSerializableExtra("latitude");
        longitude = (double) intent.getSerializableExtra("longitude");
        data = (Data) intent.getSerializableExtra("data");
        user = (User) intent.getSerializableExtra("user");
        timingData = (Data) intent.getSerializableExtra("timingData");
        currentGT = (Measurement)intent.getSerializableExtra("currentGT");
        currentAG = (Measurement) intent.getSerializableExtra("currentAG");

        System.out.println("user first: "+ user.getFirstName());

        lat = findViewById(R.id.add_sensor_info_latitude);
        lng = findViewById(R.id.add_sensor_info_longitude);

        ID = findViewById(R.id.add_sensor_info_sensorid);
        Location = findViewById(R.id.add_sensor_info_location);


        lat.setText("Latitude: " + latitude);
        lng.setText("Longitude: " + longitude);

        add = (Button) findViewById(R.id.add_sensor_info_add);
        add.setOnClickListener(this);

        drawerLayout = findViewById(R.id.add_sensor_map_drawer_layout_info);

        NavigationView navigationView1 = findViewById(R.id.add_sensor_info_nav_view);
        navigationView1.setNavigationItemSelectedListener(this);

        drawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
            }

            @Override
            public void onDrawerOpened(View drawerView) {
            }

            @Override
            public void onDrawerClosed(View drawerView) {
            }

            @Override
            public void onDrawerStateChanged(int newState) {
            }
        });



//        NavigationView navigationView2 = findViewById(R.id.add_sensor_map_to_menu);
//        navigationView2.setNavigationItemSelectedListener(this);


    }

    public void addSensorNode(){
        builder = new AlertDialog.Builder(AddSensorInfo.this);
        final String id = ID.getText().toString();
        final String location = Location.getText().toString();
        String serve_URL = "https://a18ee5air2.studev.groept.be/query/insertSensorNode.php?id=" + id + "&location=" + location + "&xco=" + latitude + "&yco=" + longitude;


        RequestQueue queue2 = Volley.newRequestQueue(this);

        StringRequest stringRequest = new StringRequest(Request.Method.POST, serve_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        builder.setTitle("Dear, ");
                        builder.setMessage("Put a sensor node successfully.");
                        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ID.setText("");
                                Location.setText("");
                            }
                        });
                        AlertDialog alertDialog = builder.create();
                        alertDialog.show();
                        System.out.println(response);

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(AddSensorInfo.this, "Error...",Toast.LENGTH_SHORT).show();
                error.printStackTrace();
            }
        });
        queue2.add(stringRequest);
    }

    @Override
    public void onClick(View view) {
        switch(view.getId()){
            case R.id.add_sensor_info_add:
                addSensorNode();
                break;
                default:break;
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        int id = menuItem.getItemId();
        if(id == R.id.add_sensor_map_to_map){
            Intent intent1 = new Intent(AddSensorInfo.this, SensorMap.class);
            intent1.putExtra("data", data);
            intent1.putExtra("user", user);
            intent1.putExtra("currentGT",currentGT);
            intent1.putExtra("currentAG",currentAG);
            intent1.putExtra("timingData",timingData);
            startActivity(intent1);
            finish();
        }
        else if(id == R.id.add_sensor_map_to_menu){
            Intent intent2 = new Intent(AddSensorInfo.this, Menu.class);
            intent2.putExtra("data", data);
            intent2.putExtra("user", user);
            intent2.putExtra("currentGT",currentGT);
            intent2.putExtra("currentAG",currentAG);
            intent2.putExtra("timingData",timingData);
            startActivity(intent2);
            finish();
        }
        drawerLayout.closeDrawers();
        return true;
    }
}
