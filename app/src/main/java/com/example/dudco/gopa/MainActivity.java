package com.example.dudco.gopa;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.example.dudco.gopa.databinding.ActivityMainBinding;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    private ActivityMainBinding binding;
    private MapFragment mMapFragment;

    private GpsInfo gpsInfo;
    private GoogleMap map;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        permission();
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        mMapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mMapFragment.getMapAsync(this);

        gpsInfo = new GpsInfo(this);
    }



    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;

        if(gpsInfo.isGetLocation()){
            double lat= gpsInfo.getLatitude();
            double log = gpsInfo.getLongitude();

            Log.d("dudco", "lat: " + lat + "       log: " + log);
//            zoomCamera(6.0f);
            updateCamera(37.56461982743129 , 126.9823439963945);
            addMarker("현재위치", 37.56461982743129 , 126.9823439963945 );

//            getPoly(new Callback() {
//                @Override
//                public void callback(PolylineOptions options) {
//                    map.addPolyline(options);
//                }
//            });
        }
    }

    ArrayList<LatLng> datas = new ArrayList<>();
    private void getPoly(LatLng start, LatLng end, final Callback callback){
        AQuery aq = new AQuery(this);
        AjaxCallback<JSONObject> cb = new AjaxCallback<JSONObject>(){
            @Override
            public void callback(String url, JSONObject object, AjaxStatus status) {
                try {
                    JSONArray array = object.getJSONArray("features");

                    for(int i = 0; i < array.length() ; i++){
                        JSONObject json = array.getJSONObject(i);
                        JSONObject geometry = json.getJSONObject("geometry");
                        String type = geometry.getString("type");
                        if(type.equals("LineString")){
                            JSONArray coordinates = geometry.getJSONArray("coordinates");
                            for(int j = 0 ; j < coordinates.length() ; j++){
                                double lat = coordinates.getJSONArray(j).getDouble(1);
                                double log = coordinates.getJSONArray(j).getDouble(0);

                                datas.add(new LatLng(lat, log));
                            }
                        }
                    }

                    callback.callback(new PolylineOptions().addAll(datas).color(Color.RED).width(15));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        };

        Map<String, String> params = new HashMap<>();
        params.put("endX", String.valueOf(end.longitude));
        params.put("endY", String.valueOf(end.latitude));
        params.put("startX", String.valueOf(start.longitude));
        params.put("startY", String.valueOf(start.latitude));
        params.put("reqCoordType", "WGS84GEO");
        params.put("resCoordType", "WGS84GEO");

        cb.header("appKey","da86b5e7-5c3a-3999-b465-903f7c6cf4e9");
        cb.header("Content-Type", "application/x-www-form-urlencoded");
        cb.params(params);

        aq.ajax("https://apis.skplanetx.com/tmap/routes?version=1", JSONObject.class, cb);
    }

    private void updateCamera(double lat, double log){
        map.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(lat, log)));
    }

    private void zoomCamera(float zoom){
        map.animateCamera(CameraUpdateFactory.zoomTo(zoom));
    }

    private void addMarker(String title, double lat, double log){
        map.addMarker(new MarkerOptions()
                .position(new LatLng(lat, log))
                .title(title));
    }

    private void permission(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if( checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 200);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == 200){
            if(grantResults[0] != PackageManager.PERMISSION_GRANTED && grantResults[1] != PackageManager.PERMISSION_GRANTED){
                new AlertDialog.Builder(this)
                        .setTitle("권한")
                        .setMessage("권한이 허용되지 않았습니다 앱을 종료합니다.")
                        .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).show();
            }
        }
    }

    interface Callback{
        void callback(PolylineOptions options);
    }
}
