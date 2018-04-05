package fr.fnoel.tapandgo.activities;

import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.DirectionsApi;
import com.google.maps.GeoApiContext;
import com.google.maps.PendingResult.Callback;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.DirectionsRoute;
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
  private static final String CURRENT_STATION = "CURRENT_STATION";
  private JSONObject currentStation;
  GeoApiContext context;
  private BottomSheetBehavior mBottomSheetBehavior1;
  private Handler handler;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_maps);
    SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
        .findFragmentById(R.id.map);
    mapFragment.getMapAsync(this);

    handler = new Handler();

    context = new GeoApiContext.Builder().apiKey(getResources().getString(R.string.google_position_key)).build();

    View bottomSheet = findViewById(R.id.bottom_sheet1);
    mBottomSheetBehavior1 = BottomSheetBehavior.from(bottomSheet);
    mBottomSheetBehavior1.setHideable(true);
    mBottomSheetBehavior1.setPeekHeight(300);
    mBottomSheetBehavior1.setState(BottomSheetBehavior.STATE_HIDDEN);

    String api_key = getResources().getString(R.string.jcdecaux_key);
    DataFragment mDataFragment = DataFragment.getInstance(getSupportFragmentManager());
    mDataFragment.startFetching(api_key);

    // GOTO FILTER VIEW
    // 		startActivity(new Intent(this, MapsActivity.class));
  }

  @Override
  protected void onSaveInstanceState(final Bundle outState) {
    super.onSaveInstanceState(outState);
    if (currentStation != null) {
      outState.putString(CURRENT_STATION, currentStation.toString());
    }
  }

  @Override
  protected void onRestoreInstanceState(final Bundle savedInstanceState) {
    super.onRestoreInstanceState(savedInstanceState);
    try {
      currentStation = new JSONObject(savedInstanceState.getString(CURRENT_STATION));
      displayStationDetails();
    } catch (JSONException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void onMapReady(GoogleMap googleMap) {
    mMap = googleMap;
    LatLng nantes = new LatLng(47.214617, -1.549949);
    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(nantes, 13.0f));

    mMap.setOnMarkerClickListener(onMarkerClickListener);
    mMap.setOnMapClickListener(new OnMapClickListener() {
      @Override
      public void onMapClick(LatLng latLng) {
        mBottomSheetBehavior1.setState(BottomSheetBehavior.STATE_HIDDEN);
      }
    });
  }

  private OnMarkerClickListener onMarkerClickListener = new OnMarkerClickListener() {
    @Override
    public boolean onMarkerClick(Marker marker) {
      currentStation = (JSONObject) marker.getTag();
      return currentStation != null && displayStationDetails();
    }
  };

  private boolean displayStationDetails() {
    Log.i(TAG, "onMarkerClick: " + currentStation.toString());
    String name;
    String address;
    boolean acceptBanking;
    boolean isOpen;
    Integer totalStands;
    Integer availableBikes;
    Integer availableStands;

    findViewById(R.id.station_distance_detail).setVisibility(View.INVISIBLE);

    try {
      name = currentStation.getString("name").replaceFirst("[0-9]{5}\\s*-\\s*", "");
      address = currentStation.getString("address").replaceFirst(".*-\\s*", "");
      acceptBanking = currentStation.getBoolean("banking");
      isOpen = currentStation.getString("status").equals("OPEN");
      totalStands = currentStation.getInt("bike_stands");
      availableBikes = currentStation.getInt("available_bikes");
      availableStands = currentStation.getInt("available_bike_stands");
    } catch (JSONException e) {
      Log.e(TAG,
          "onMarkerClick: Error lors de la récupération des informations de la station depuis les données de l'api", e);
      return false;
    }

    // TODO Corriger les coordonnées utilisées
    DirectionsApi.newRequest(context).origin("rue de chypre nantes").destination(address)
        .setCallback(directionCallback);

    if (mBottomSheetBehavior1.getState() != BottomSheetBehavior.STATE_EXPANDED) {
      mBottomSheetBehavior1.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    ((TextView) findViewById(R.id.station_name)).setText(name);
    ((TextView) findViewById(R.id.station_address)).setText(address);
    if (acceptBanking) {
      findViewById(R.id.station_banking).setVisibility(View.VISIBLE);
    } else {
      findViewById(R.id.station_banking).setVisibility(View.INVISIBLE);
    }
    if (isOpen) {
      findViewById(R.id.station_open).setVisibility(View.VISIBLE);
      findViewById(R.id.station_close).setVisibility(View.INVISIBLE);

      ((TextView) findViewById(R.id.station_available_bikes))
          .setText(getString(R.string.station_stats, availableBikes, totalStands));
      ((TextView) findViewById(R.id.station_available_stands))
          .setText(getString(R.string.station_stats, availableStands, totalStands));
    } else {
      findViewById(R.id.station_open).setVisibility(View.INVISIBLE);
      findViewById(R.id.station_close).setVisibility(View.VISIBLE);
    }

    return true;
  }

  private Callback<DirectionsResult> directionCallback = new Callback<DirectionsResult>() {
    @Override
    public void onResult(DirectionsResult result) {
      if (result.routes.length > 0 && result.routes[0].legs.length > 0) {
        handler.post(() -> displayStationDistance(result.routes[0].legs[0].duration.humanReadable));
      }
    }

    @Override
    public void onFailure(Throwable e) {

    }
  };

  private void displayStationDistance(String duration) {
    findViewById(R.id.station_distance_detail).setVisibility(View.VISIBLE);
    ((TextView) findViewById(R.id.station_eta)).setText(duration);
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
