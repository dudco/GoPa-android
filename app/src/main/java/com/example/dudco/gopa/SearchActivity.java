package com.example.dudco.gopa;

import android.databinding.DataBindingUtil;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;

import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.example.dudco.gopa.databinding.ActivitySearchBinding;
import com.google.android.gms.location.places.Place;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class SearchActivity extends AppCompatActivity {
    private ActivitySearchBinding binding;

    private String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?radius=200000&&key=AIzaSyDgRwVaFsYKrqPNGdGgBTfVcpSgeGpQytI&language=ko&location=";
    private ArrayAdapter<String> adapter;
    private ArrayList<PlaceData> datas = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_search);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        adapter.setNotifyOnChange(true);
        binding.list.setAdapter(adapter);
        binding.list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                PlaceData data = datas.get(position);
                getIntent().putExtra("name", data.getName());
                getIntent().putExtra("place", data.getPlace());
                getIntent().putExtra("lat", data.getLat());
                getIntent().putExtra("log", data.getLog());
                setResult(RESULT_OK, getIntent());
                finish();
            }
        });

        binding.editSearch.addTextChangedListener(searchTextWatcher);
        url += getIntent().getStringExtra("lat");
        url += ",";
        url += getIntent().getStringExtra("log");
        url += "&name=";
    }

    TextWatcher searchTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {


        }

        @Override
        public void afterTextChanged(Editable s) {
            AQuery aq = new AQuery(SearchActivity.this);
            adapter.clear();
            datas.clear();
            aq.ajax(url+s.toString(), JSONObject.class, new AjaxCallback<JSONObject>(){
                @Override
                public void callback(String url, JSONObject object, AjaxStatus status) {
                    Log.d("dudco", object.toString());
                    try {
                        JSONArray arr = object.getJSONArray("results");

                        for(int i = 0 ; i < arr.length(); i++){
                            JSONObject geometry = arr.getJSONObject(i).getJSONObject("geometry");
                            double lat = geometry.getJSONObject("location").getDouble("lat");
                            double lng = geometry.getJSONObject("location").getDouble("lng");
                            String name = arr.getJSONObject(i).getString("name");
                            String place = arr.getJSONObject(i).getString("vicinity");

                            datas.add(new PlaceData(lat, lng, name, place));
                            adapter.add(name);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
            adapter.notifyDataSetChanged();
        }
    };

    class ListAdapter extends BaseAdapter {
        ArrayList<String> datas = new ArrayList<>();

        @Override
        public int getCount() {
            return datas.size();
        }

        @Override
        public Object getItem(int position) {
            return datas.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            return null;
        }
    }
}
