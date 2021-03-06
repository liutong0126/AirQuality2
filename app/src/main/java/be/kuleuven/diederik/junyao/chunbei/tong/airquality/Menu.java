package be.kuleuven.diederik.junyao.chunbei.tong.airquality;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.view.View;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Menu extends AppCompatActivity implements View.OnClickListener{

    private CardView mapMenu, listMenu, userInfoMenu, signOutMenu;
    private User user = new User();
    private Data data = new Data();
    private Data timingData = new Data();
    private Measurement currentGT = new Measurement(0,0,null,"groupt");
    private Measurement currentAG = new Measurement(0,0,null,"agora");

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        Intent intent = getIntent();
        user = (User) intent.getSerializableExtra("user");
        //data = (Data) intent.getSerializableExtra("data");
        //timingData = (Data) intent.getSerializableExtra("timingData");
        currentGT = (Measurement)intent.getSerializableExtra("currentGT");
        currentAG = (Measurement) intent.getSerializableExtra("currentAG");

        System.out.println("user first: "+ user.getFirstName());

        mapMenu = findViewById(R.id.menu_map);
        listMenu = findViewById(R.id.menu_list);
        userInfoMenu = findViewById(R.id.menu_userinfo);
        signOutMenu = findViewById(R.id.menu_out);

        mapMenu.setOnClickListener(this);
        listMenu.setOnClickListener(this);
        userInfoMenu.setOnClickListener(this);
        signOutMenu.setOnClickListener(this);

        getMeasurements("groupt");
        getMeasurements("agora");
        getTimingMeasurements("groupt");
        getTimingMeasurements("agora");
        getSensors();

    }

    @Override
    public void onClick(View view) {
        Intent intent;
        switch (view.getId())
        {
            case R.id.menu_out:
                intent = new Intent(this,Login.class); startActivity(intent);
                break;
            case R.id.menu_userinfo:
                intent = new Intent(this,UserInfo.class);
                intent.putExtra("user",user);
                intent.putExtra("data",data);
                intent.putExtra("timingData",timingData);
                intent.putExtra("currentGT",currentGT);
                intent.putExtra("currentAG",currentAG);
                startActivity(intent);
                break;
            case R.id.menu_map:
                intent = new Intent(this, SensorMap.class);
                intent.putExtra("user",user);
                intent.putExtra("data",data);
                intent.putExtra("timingData",timingData);
                intent.putExtra("currentGT",currentGT);
                intent.putExtra("currentAG",currentAG);
                startActivity(intent);
                break;
            case R.id.menu_list:
                intent = new Intent(this,Position.class);
                intent.putExtra("user",user);
                intent.putExtra("data",data);
                intent.putExtra("timingData",timingData);
                intent.putExtra("currentGT",currentGT);
                intent.putExtra("currentAG",currentAG);
                startActivity(intent);
                break;
            default: break;
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void getMeasurements(String location) {

        final String loc=location;

        String url = "https://a18ee5air2.studev.groept.be/query/avgWeek.php?location=" + location;
        RequestQueue queue = Volley.newRequestQueue(Menu.this);

        System.out.println("Get measurements starts");

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            System.out.println("Into response! measurement");
                            JSONArray jarr = new JSONArray(response);
                            for (int i = 0; i < jarr.length(); i++) {
                                JSONObject jobj = jarr.getJSONObject(i);
                                String day = jobj.getString("days");
                                Date date = new SimpleDateFormat("yyyy-MM-dd").parse(day);
                                Double pm_value = jobj.getDouble("avg_pm");
                                Double co_value = jobj.getDouble("avg_CO");

                                try{
                                data.addMeasurement(new Measurement(co_value,pm_value,date,loc));}
                                catch(AlreadyAddedException A){
                                    Toast.makeText(Menu.this,"Add measurement failed!",Toast.LENGTH_SHORT).show();
                                }

                                System.out.println("Date: " + date);
                                System.out.println("PM: " + pm_value);
                                System.out.println("CO: " + co_value);
                            }
                            System.out.println("End of response! measurement");

                        } catch (JSONException e) {
                            System.out.println(e);
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(Menu.this, "Error...", Toast.LENGTH_SHORT).show();
                error.printStackTrace();
            }
        });
        queue.add(stringRequest);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void getTimingMeasurements(String location) {

        final String loc=location;

        String url = "https://a18ee5air2.studev.groept.be/query/readTwenty.php?location="+location;
        RequestQueue queue = Volley.newRequestQueue(Menu.this);

        System.out.println("Get timing measurements starts");

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            System.out.println("Into response! timing");
                            JSONArray jarr = new JSONArray(response);
                            for (int i = 0; i < jarr.length(); i++) {
                                JSONObject jobj = jarr.getJSONObject(i);
                                String time = jobj.getString("timeStamps");
                                Date date = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse(time);
                                Double pm_value = jobj.getDouble("valuePM");
                                Double co_value = jobj.getDouble("valueCO");

                                try{
                                    timingData.addMeasurement(new Measurement(co_value,pm_value,date,loc));}
                                catch(AlreadyAddedException A){
                                    Toast.makeText(Menu.this,"Add measurement failed!",Toast.LENGTH_SHORT).show();
                                }

                                System.out.println("Date: " + date);
                                System.out.println("PM: " + pm_value);
                                System.out.println("CO: " + co_value);
                            }
                            System.out.println("End of response! timing");

                        } catch (JSONException e) {
                            System.out.println(e);
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(Menu.this, "Error...", Toast.LENGTH_SHORT).show();
                error.printStackTrace();
            }
        });
        queue.add(stringRequest);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void getSensors() {
        String url = "https://a18ee5air2.studev.groept.be/query/readSensorNode.php";
        RequestQueue queue = Volley.newRequestQueue(Menu.this);

        /*LocalDateTime localToday = LocalDateTime.ofInstant(today.toInstant(), ZoneId.systemDefault());
        final int dayOfToday = localToday.getDayOfMonth();
        System.out.println("Day of today: " + dayOfToday);*/

        System.out.println("Get sensors starts");

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            System.out.println("Into response! sensor");
                            JSONArray jarr = new JSONArray(response);
                            for (int i = 0; i < jarr.length(); i++) {
                                JSONObject jobj = jarr.getJSONObject(i);
                                int sensorId=jobj.getInt("idSensorNode");
                                double xcoordinate=jobj.getDouble("xCo");
                                double ycoordinate=jobj.getDouble("yCo");
                                String location=jobj.getString("location");

                                try{
                                    data.addSensor(new Sensor(sensorId,xcoordinate,ycoordinate,location));}
                                catch(AlreadyAddedException A){
                                    Toast.makeText(Menu.this,"Add sensors failed!",Toast.LENGTH_SHORT).show();
                                }

                                System.out.println("SensorId: " + sensorId);
                                System.out.println("Xcoordinate: " + xcoordinate);
                                System.out.println("Ycoordinate: " + ycoordinate);
                                System.out.println("Location: " + location);
                            }
                            System.out.println("End of response! sensor");
                        } catch (JSONException e) {
                            System.out.println(e);
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(Menu.this, "Error...", Toast.LENGTH_SHORT).show();
                error.printStackTrace();
            }
        });
        queue.add(stringRequest);
    }


}
