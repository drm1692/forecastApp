package com.example.forecast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.textfield.TextInputEditText;
import com.squareup.picasso.provider.PicassoProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private RelativeLayout homeRl;
    private ProgressBar loadingPB;
    private TextView cityTV,temperatureTv,conditionTV;
    private TextInputEditText cityEdt;
    private ImageView backIv,iconIV,searchIV;
    private RecyclerView weatherRv;
    private ArrayList<weatherRVModal> weatherRVModalArrayList;
    private weatherRVAdapter weatherRVAdapter;

    private LocationManager locationManager;
    int PERMISSION_CODE = 1;
    private String cityName;
    public Criteria criteria;
    public String bestProvider;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        setContentView(R.layout.activity_main);

        homeRl = findViewById(R.id.RLHome);
        loadingPB = findViewById(R.id.loadingBar);
        cityTV = findViewById(R.id.TVCityName);
        temperatureTv = findViewById(R.id.TVTemperature);
        conditionTV = findViewById(R.id.TVCondition);
        cityEdt = findViewById(R.id.EDtCity);
        backIv = findViewById(R.id.bgImage);
        iconIV = findViewById(R.id.IVIcon);
        searchIV = findViewById(R.id.IVSearch);
        weatherRv = findViewById(R.id.RVWeather);

        weatherRVModalArrayList = new ArrayList<>();
        weatherRVAdapter = new weatherRVAdapter(this,weatherRVModalArrayList);
        weatherRv.setAdapter(weatherRVAdapter);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
         ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){

            ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},PERMISSION_CODE);

        }
//        criteria = new Criteria();
//        bestProvider = String.valueOf(locationManager.getBestProvider(criteria, true)).toString();

        Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
       // if(location != null) {
            cityName = getCityName(location.getLongitude(), location.getLatitude());
            getWeatherInfo(cityName);
       // }
//        else {
//
//            locationManager.requestLocationUpdates(bestProvider, 1000, 0, this);
//        }
        searchIV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String city = cityEdt.getText().toString();

                if(city.isEmpty()){

                    Toast.makeText(MainActivity.this,"please enter city name",Toast.LENGTH_SHORT).show();
                }
                else{

                    cityTV.setText(cityName);
                    getWeatherInfo(city);
                }
            }
        });

    }
//    public void onLocationChanged(Location location) {
//
//        locationManager.removeUpdates((LocationListener) this);
//        cityName = getCityName(location.getLongitude(), location.getLatitude());
//        getWeatherInfo(cityName);
//    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == PERMISSION_CODE){

            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){

                Toast.makeText(this,"Permission granted",Toast.LENGTH_SHORT);
            }
            else{

                Toast.makeText(this,"Please provide the permissions",Toast.LENGTH_SHORT);
                finish();
            }
        }
    }

    private String getCityName(double longitude, double latitude){

        String cityName = "Not Found";
        Toast.makeText(MainActivity.this,"latitude"+ latitude+"logitude"+longitude,Toast.LENGTH_SHORT);
        Geocoder gcd = new Geocoder(getBaseContext(), Locale.getDefault());

        try{

            List<Address> addresses = gcd.getFromLocation(latitude,longitude,10);
            for(Address adr: addresses){

                if(adr != null){

                    String city = adr.getLocality();
                    if(city != null && !city.equals("")){

                        cityName = city;
                    }
                    else{

                        Log.d("TAG","City not found");
                        Toast.makeText(this,"User city not found,",Toast.LENGTH_SHORT).show();
                    }

                }
            }

        }catch (IOException e){

            e.printStackTrace();
        }
        return cityName;
    }
    private void getWeatherInfo(String cityName){

        String URL = "http://api.weatherapi.com/v1/forecast.json?key=792b6257df8e46bdad754017210309&q="+cityName+"&days=1&aqi=no&alerts=no";
        cityTV.setText(cityName);
        RequestQueue requestQueue = Volley.newRequestQueue(MainActivity.this);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, URL, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                loadingPB.setVisibility(View.GONE);
                homeRl.setVisibility(View.VISIBLE);
                weatherRVModalArrayList.clear();

                try {
                    String temperature = response.getJSONObject("current").getString("temp_c");
                    temperatureTv.setText(temperature + "°C");
                    int isDay = response.getJSONObject("current").getInt("is_day");
                    String condition = response.getJSONObject("current").getJSONObject("condition").getString("text");
                    String conditionIcon = response.getJSONObject("current").getJSONObject("condition").getString("icon");
                    PicassoProvider.get().load("http:".concat(conditionIcon)).into(iconIV);
                    conditionTV.setText(condition);
                    if(isDay == 1){

                        PicassoProvider.get().load("https://png.pngtree.com/background/20210716/original/pngtree-blue-gradient-beautiful-early-morning-sky-picture-image_1344648.jpg").into(backIv);
                    }
                    else{

                        PicassoProvider.get().load("https://png.pngtree.com/background/20210714/original/pngtree-flat-wind-day-and-night-2-picture-image_1210654.jpg").into(backIv);
                    }

                    JSONObject forecastObj = response.getJSONObject("forecast");
                    JSONObject forecast0 = forecastObj.getJSONArray("forecastday").getJSONObject(0);
                    JSONArray hourArray = forecast0.getJSONArray("hour");

                    for (int i = 0; i < hourArray.length();i++){

                        JSONObject hourObj = hourArray.getJSONObject(i);
                        String time = hourObj.getString("time");
                        String temper = hourObj.getString("temp_c");
                        String img = hourObj.getJSONObject("condition").getString("icon");
                        String wind = hourObj.getString("wind_kph");

                        weatherRVModalArrayList.add(new weatherRVModal(time,temper,img,wind));
                    }
                    weatherRVAdapter.notifyDataSetChanged();

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(MainActivity.this,"Please enter valid city name",Toast.LENGTH_SHORT);
            }
        });

        requestQueue.add(jsonObjectRequest);
    }

}