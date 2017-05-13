package com.example.dudco.gopa;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.example.dudco.gopa.databinding.ActivityMainBinding;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    private ActivityMainBinding binding;
    private MapFragment mMapFragment;

    private GpsInfo gpsInfo;
    private GoogleMap map;

    private PlaceData end;
    private static Marker marker;
    private ArrayList<LatLng> polyDatas = new ArrayList<>();

    private int totalTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        permission();
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        mMapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mMapFragment.getMapAsync(this);

        gpsInfo = new GpsInfo(this);

        mSocket.connect();

        binding.btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(gpsInfo.isGetLocation()){
                    Intent intent = new Intent(MainActivity.this, SearchActivity.class);
                    intent.putExtra("lat", String.valueOf(gpsInfo.getLatitude()));
                    intent.putExtra("log", String.valueOf(gpsInfo.getLongitude()));
                    startActivityForResult(intent, 200);
                }
            }
        });

        binding.btnGo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(gpsInfo.isGetLocation()){
                    map.clear();
                    getPoly(new LatLng(gpsInfo.getLatitude(), gpsInfo.getLongitude()), new LatLng(end.getLat(), end.getLog()), new Callback() {
                        @Override
                        public void callback(PolylineOptions options, int time) {
                            totalTime = time;
                            map.addPolyline(options);
                            addMarker("현재위치", gpsInfo.getLatitude(), gpsInfo.getLongitude(), false);
                            addMarker("목적지", end.getLat(), end.getLog(), false);
                            updateCamera(gpsInfo.getLatitude(), gpsInfo.getLongitude());
//                            sendSMS(binding.editCallnum.getText().toString(), "TEST");
                            polyDatas.clear();
                            polyDatas.addAll(options.getPoints());
                            Util.startRiding(MainActivity.this);
                            emitData(end.getLat(), end.getLog(), gpsInfo.getLatitude(), gpsInfo.getLongitude(), totalTime);
//                            startNavi();
                        }
                    });
                }
            }
        });
    }
    double nowLat;
    double nowLog;

    Handler handler = new Handler();
    private void startNavi(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                int count = 0;
                int sec = 0;

                nowLat = gpsInfo.getLatitude();
                nowLog = gpsInfo.getLongitude();

                while(true){
                    try {
                        Thread.sleep(1000);
                        sec++;
                        if(sec % 3 == 0){
                            nowLat = polyDatas.get(count).latitude;
                            nowLog = polyDatas.get(count++).longitude;

                            for(int i = 0 ; i < polyDatas.size() ; i++){
                                double dist = Util.distance(polyDatas.get(i).latitude, polyDatas.get(i).longitude, nowLat, nowLog, "meter");
                                Log.d("dudco", dist+"");
                                if(dist < 50){
                                    polyDatas.remove(i);
                                }
                            }
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            map.clear();
                            addMarker(nowLat, nowLog);
                            map.addPolyline(new PolylineOptions().addAll(polyDatas));
                        }
                    });

                    emitData(end.getLat(), end.getLog(), nowLat, nowLog, totalTime - sec);
                }
            }
        }).start();
    }

    private void sendSMS(String phoneNum, String message){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(checkSelfPermission(Manifest.permission.SEND_SMS)!= PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED){
                requestPermissions(new String[]{Manifest.permission.SEND_SMS, Manifest.permission.READ_PHONE_STATE}, 200);
            }else{
                _sendSMS(phoneNum, message);
            }
        }else{
            _sendSMS(phoneNum, message);
        }
    }

    private void _sendSMS(String phoneNum, String message){
        PendingIntent intent = PendingIntent.getBroadcast(this, 0, new Intent("SENT_SMS"), 0);
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (getResultCode()){
                    case RESULT_OK:
                        Toast.makeText(context, "문자 전송이 완료되었습니다", Toast.LENGTH_SHORT).show();
                        break;
                    default:
                        break;
                }
            }
        }, new IntentFilter("SENT_SMS"));
        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage(phoneNum, null, message, intent, null);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;

        if(gpsInfo.isGetLocation()){
            double lat= gpsInfo.getLatitude();
            double log = gpsInfo.getLongitude();

            Log.d("dudco", "lat: " + lat + "       log: " + log);
            updateCamera(lat , log);
            addMarker("현재위치", lat , log, false);
        }
    }

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

                                polyDatas.add(new LatLng(lat, log));
                            }
                        }
                    }

                    totalTime = array.getJSONObject(0).getJSONObject("properties").getInt("totalTime");

                    callback.callback(new PolylineOptions().addAll(polyDatas).color(Color.RED).width(15), totalTime);
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

    private void addMarker(String title, double lat, double log, boolean isMarkerRemove){
        if(marker != null && isMarkerRemove){
            marker.remove();
        }
        marker = map.addMarker(new MarkerOptions().title(title).position(new LatLng(lat, log)));
    }

    private void addMarker(double lat, double log){
        marker.remove();
        Log.d("dudco", "remove" + ":" + lat + "," + log);
        addMarker("현재위치: " + lat + "," + log, lat, log, true);
        addMarker("목적지", end.getLat(), end.getLog(), false);
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode == RESULT_OK){
            if(requestCode == 200){
                String name = data.getStringExtra("name");
                String place = data.getStringExtra("place");
                double _lat = data.getDoubleExtra("lat",37.56461982743129);
                double _log = data.getDoubleExtra("log", 126.9823439963945);
                binding.btnSearch.setText(name);
                updateCamera(_lat, _log);
                addMarker(place, _lat, _log, true);
                end = new PlaceData(_lat, _log, name, place);
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
        void callback(PolylineOptions options, int time);
    }

    public void emitData(double elat, double elon, double dlat, double dlon, int time){
        Map<String, String> map = new HashMap<>();
        map.put("userX", String.valueOf(elon));
        map.put("userY", String.valueOf(elat));
        map.put("driverX", String.valueOf(dlon));
        map.put("driverY", String.valueOf(dlat));
        map.put("time", String.valueOf(time));
        JSONObject json = new JSONObject(map);
        mSocket.emit("location", json);
    }

    private Socket mSocket;
    {
        try {
            mSocket = IO.socket("http://soylatte.kr:3000");
        } catch (URISyntaxException e) {}
    }
}
