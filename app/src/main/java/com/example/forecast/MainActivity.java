package com.example.forecast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.ServerError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.squareup.picasso.provider.PicassoProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private RelativeLayout homeRl;
    private ProgressBar loadingPB;
    private TextView cityTV, temperatureTv, conditionTV;
    private TextInputEditText cityEdt;
    private ImageView backIv, iconIV, searchIV;
    private RecyclerView weatherRv;
    private ArrayList<weatherRVModal> weatherRVModalArrayList;
    private weatherRVAdapter weatherRVAdapter;

    FusedLocationProviderClient mfusedLocationClient;
    LocationManager locationManager;
    LocationListener locationListener;
    int PERMISSION_CODE = 44;
    private String cityName;

    final long MIN_TIME = 5000;
    final float MIN_DISTANCE = 1000;
    String Location_provider = LocationManager.GPS_PROVIDER;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
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

        weatherRVModalArrayList = new ArrayList<weatherRVModal>();
        weatherRVAdapter = new weatherRVAdapter(this, weatherRVModalArrayList);
        weatherRv.setAdapter(weatherRVAdapter);

        mfusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getLocation();
        } else {
            Toast.makeText(this, "Your device does not support location API", Toast.LENGTH_SHORT).show();
        }

        searchIV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String city = cityEdt.getText().toString();

                if (city.isEmpty()) {

                    Toast.makeText(MainActivity.this, "please enter city name", Toast.LENGTH_SHORT).show();
                } else {

                    cityTV.setText(cityName);
                    getWeatherInfo(city);
                }
            }
        });

    }

    @SuppressLint("MissingPermission")
    private void getLocation() {

        if (checkPermissions()) {
            // check if location is enabled
            if (isLocationEnabled()) {
                mfusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location == null) {
                            requestNewLocationData();
                        } else {
                            Toast.makeText(MainActivity.this, "latitude" + location.getLatitude() + "logitude" + location.getLongitude(), Toast.LENGTH_SHORT).show();
//                            cityName = "Porbandar";
                            cityName = getCityName(location.getLongitude(), location.getLatitude());

                            getWeatherInfo(cityName);
                        }
                    }
                });
            } else {
                Toast.makeText(this, "Please turn on" + " your location...", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        } else {
            // if permissions aren't available,
            // request for permissions
            requestPermissions();
        }
    }
    @SuppressLint("MissingPermission")
    private void requestNewLocationData() {

        // Initializing LocationRequest
        // object with appropriate methods
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(5);
        mLocationRequest.setFastestInterval(0);
        mLocationRequest.setNumUpdates(1);

        // setting LocationRequest
        // on FusedLocationClient
        mfusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mfusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
    }

    private LocationCallback mLocationCallback = new LocationCallback() {

        @Override
        public void onLocationResult(LocationResult locationResult) {
            Location mLastLocation = locationResult.getLastLocation();

            Toast.makeText(MainActivity.this,"latitude"+ mLastLocation.getLatitude()+"logitude"+mLastLocation.getLongitude(),Toast.LENGTH_SHORT);
            cityName = getCityName(mLastLocation.getLongitude(), mLastLocation.getLatitude());
            getWeatherInfo(cityName);
        }
    };

    // method to check for permissions
    private boolean checkPermissions() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        // If we want background location
        // on Android 10.0 and higher,
        // use:
        // ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    // method to request for permissions
    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_CODE  );
    }

    // method to check
    // if location is enabled
    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }
    // If everything is alright then
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == PERMISSION_CODE){

            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){

                Toast.makeText(this,"Permission granted",Toast.LENGTH_SHORT);
                getLocation();
            }
            else{

                Toast.makeText(this,"Please provide the permissions",Toast.LENGTH_SHORT);
            }
        }
    }
    @Override
    public void onResume() {
        super.onResume();
        if (checkPermissions()) {
            getLocation();
        }
    }

    private String getCityName(double longitude, double latitude){

        String cityName = "Not Found";
        //Toast.makeText(MainActivity.this,"latitude"+ latitude+"logitude"+longitude,Toast.LENGTH_SHORT);
        Geocoder gcd = new Geocoder(getBaseContext(), Locale.getDefault());

        try{

            List<Address> addresses = gcd.getFromLocation(latitude,longitude,1);

                if(addresses != null){

                    String city = addresses.get(0).getLocality();
                    if(city != null && !city.equals("")){
                        cityName = city;
                    }
                    else{
                        Toast.makeText(this,"City not found,",Toast.LENGTH_LONG).show();
                    }

                }

        }catch (IOException e){

            e.printStackTrace();
        }
        return cityName;
    }
    private void getWeatherInfo(String cityName){

        String URL = "https://api.weatherapi.com/v1/forecast.json?key=792b6257df8e46bdad754017210309&q=" + cityName + "&days=1&aqi=no&alerts=no";
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
                    Toast.makeText(MainActivity.this, "Something went wrong fetching weather", Toast.LENGTH_SHORT).show();
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