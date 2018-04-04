package fr.fnoel.tapandgo.activities;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import fr.fnoel.tapandgo.R;
import fr.fnoel.tapandgo.fragment.DataFragment;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
    DataFragment.OnFragmentInteractionListener {

  private static final String TAG = "MAPS_ACTIVITY";
  private GoogleMap mMap;
  private JSONArray listeStation = new JSONArray();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_maps);
    SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
        .findFragmentById(R.id.map);
    mapFragment.getMapAsync(this);

    String api_key = getResources().getString(R.string.jcdecaux_key);
    DataFragment mDataFragment = DataFragment.getInstance(getSupportFragmentManager());
    mDataFragment.startFetching(api_key);

    // GOTO FILTER VIEW
    // 		startActivity(new Intent(this, MapsActivity.class));
  }


  @Override
  public void onMapReady(GoogleMap googleMap) {
    mMap = googleMap;
    LatLng nantes = new LatLng(47.214617, -1.549949);
    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(nantes, 13.0f));

    mMap.setOnMarkerClickListener(new OnMarkerClickListener() {
      @Override
      public boolean onMarkerClick(Marker marker) {
        JSONObject station = (JSONObject) marker.getTag();
        Log.i(TAG, "onMarkerClick: " + station);
        // TODO Externaliser listener / afficher bottom sheet
        return false;
      }
    });
  }


  @Override
  public void onFragmentInteraction(String json) {
    try {
      listeStation = new JSONArray(json);
      initLocation();
    } catch (JSONException e) {
      e.printStackTrace();
    }
  }

  private void initLocation() throws JSONException {
    for (int i = 0; i < listeStation.length(); i++) {
      JSONObject station = listeStation.getJSONObject(i);
      JSONObject pos = station.getJSONObject("position");
      LatLng coordStation = new LatLng(pos.getDouble("lat"), pos.getDouble("lng"));

      String status = station.getString("status");
      int availableBike = station.getInt("available_bikes");

      if ("OPEN".equals(status)) {
        if (availableBike == 0) {
          mMap.addMarker(new MarkerOptions().position(coordStation)
              .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))).setTag(station);
        } else {
          mMap.addMarker(new MarkerOptions().position(coordStation)
              .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))).setTag(station);
        }
      } else {
        mMap.addMarker(new MarkerOptions().position(coordStation)
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE))).setTag(station);
      }
    }
  }

}
