package fr.fnoel.tapandgo.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.DirectionsApi;
import com.google.maps.GeoApiContext;
import com.google.maps.PendingResult.Callback;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.TravelMode;
import fr.fnoel.tapandgo.R;
import fr.fnoel.tapandgo.fragment.DataFragment;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Activité principale affichant la carte et les marqueurs indiquant les positions des stations.
 */
public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
    DataFragment.OnFragmentInteractionListener {

  private static final String KEY_CAMERA_POSITION = "camera_position";
  private static final String TAG = "MAPS_ACTIVITY";
  private static final String CURRENT_STATION = "CURRENT_STATION";
  private GeoApiContext context;
  private GoogleMap mMap;
  private JSONArray listeStation = new JSONArray();
  private JSONObject currentStation;
  private BottomSheetBehavior mBottomSheetBehavior1;
  private Handler handler;
  private CameraPosition mapPositionState;
  private FusedLocationProviderClient mFusedLocationClient;
  private ArrayList<Marker> markers;
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
  /**
   * Callback appelée lors d'un clic sur un marqueur.<br/> Si le marqueur est associé à une station, on appelle la
   * méthode qui affichera les informations de cette station.
   */
  private OnMarkerClickListener onMarkerClickListener = new OnMarkerClickListener() {
    @Override
    public boolean onMarkerClick(Marker marker) {
      currentStation = (JSONObject) marker.getTag();
      return currentStation != null && displayStationDetails();
    }
  };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_maps);
    SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
    mapFragment.getMapAsync(this);

    handler = new Handler();

    context = new GeoApiContext.Builder().apiKey(getResources().getString(R.string.google_position_key)).build();

    mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

    View bottomSheet = findViewById(R.id.bottom_sheet1);
    mBottomSheetBehavior1 = BottomSheetBehavior.from(bottomSheet);
    mBottomSheetBehavior1.setHideable(true);
    mBottomSheetBehavior1.setPeekHeight(300);
    mBottomSheetBehavior1.setState(BottomSheetBehavior.STATE_HIDDEN);

    // Création d'une instance du fragment utilisé pour récupérer la liste des stations
    String api_key = getResources().getString(R.string.jcdecaux_key);
    DataFragment mDataFragment = DataFragment.getInstance(getSupportFragmentManager());
    mDataFragment.startFetching(api_key);

    // On restaure l'état de la camera en cas de recréation de l'activité
    if (savedInstanceState != null) {
      mapPositionState = savedInstanceState.getParcelable(KEY_CAMERA_POSITION);
    }
  }

  @Override
  protected void onSaveInstanceState(final Bundle outState) {
    if (currentStation != null) {
      outState.putString(CURRENT_STATION, currentStation.toString());
    }
    if (mMap != null) {
      outState.putParcelable(KEY_CAMERA_POSITION, mMap.getCameraPosition());
    }
    super.onSaveInstanceState(outState);
  }

  @Override
  protected void onRestoreInstanceState(final Bundle savedInstanceState) {
    super.onRestoreInstanceState(savedInstanceState);
    String savedStation = savedInstanceState.getString(CURRENT_STATION);

    if (savedStation != null) {
      try {
        currentStation = new JSONObject(savedStation);
        displayStationDetails();
      } catch (JSONException e) {
        e.printStackTrace();
      }
    }
  }

  @Override
  public void onMapReady(GoogleMap googleMap) {
    mMap = googleMap;

    // Initialisation de la carte sur Nantes
    LatLng nantes = new LatLng(47.214617, -1.549949);
    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(nantes, 13.0f));

    mMap.setOnMarkerClickListener(onMarkerClickListener);

    // Lors d'un clic sur la carte et pas sur un marqueur, on cache le BottomSheet
    mMap.setOnMapClickListener((LatLng latLng) -> mBottomSheetBehavior1.setState(BottomSheetBehavior.STATE_HIDDEN));

    // On vérifie que l'application possède l'autorisation d'accéder à la position de l'utilisateur,
    // si c'est le cas alors on active l'affichage de sa position sur la carte
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        == PackageManager.PERMISSION_GRANTED) {
      mMap.setMyLocationEnabled(true);
    }

    // Dans le cas d'un changement d'état de l'activité,
    // on récupère l'état de la caméra sauvegardé avant le changement et on restaure cet état
    if (mapPositionState != null) {
      mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mapPositionState.target, mapPositionState.zoom));
    }

  }

  private boolean displayStationDetails() {
    Log.i(TAG, "onMarkerClick: " + currentStation.toString());
    String name;
    String address;
    boolean acceptBanking;
    boolean isOpen;
    Integer totalStands;
    Integer availableBikes;
    Integer availableStands;
    double destinationLat;
    double destinationLng;

    findViewById(R.id.station_distance_detail).setVisibility(View.INVISIBLE);

    try {
      name = currentStation.getString("name").replaceFirst("[0-9]{5}\\s*-\\s*", "");
      address = currentStation.getString("address").replaceFirst(".*-\\s*", "");
      acceptBanking = currentStation.getBoolean("banking");
      isOpen = currentStation.getString("status").equals("OPEN");
      totalStands = currentStation.getInt("bike_stands");
      availableBikes = currentStation.getInt("available_bikes");
      availableStands = currentStation.getInt("available_bike_stands");
      destinationLat = currentStation.getJSONObject("position").getDouble("lat");
      destinationLng = currentStation.getJSONObject("position").getDouble("lng");
    } catch (JSONException e) {
      Log.e(TAG,
          "onMarkerClick: Error lors de la récupération des informations de la station depuis les données de l'api", e);
      return false;
    }

    com.google.maps.model.LatLng destination = new com.google.maps.model.LatLng(destinationLat, destinationLng);
    if (mMap != null) {
      mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(destinationLat, destinationLng), 15.0f));
    }

    try {
      mFusedLocationClient.getLastLocation()
          .addOnSuccessListener(this, (Location location) -> {
            if (location != null) {
              com.google.maps.model.LatLng orign = new com.google.maps.model.LatLng(location.getLatitude(),
                  location.getLongitude());
              DirectionsApi.newRequest(context).origin(orign).destination(destination).mode(TravelMode.WALKING)
                  .setCallback(directionCallback);
            }
          });
    } catch (SecurityException e) {
      Log.e(TAG, "displayStationDetails: Permission de géolocalisation non accordée", e);
    }

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

  /**
   * Initialise les marqueurs de la carte à partir de la liste des stations récupérée.
   *
   * @throws JSONException en cas d'erreur de lecture des objets json
   */
  private void initLocation() throws JSONException {
    markers = new ArrayList<>();
    for (int i = 0; i < listeStation.length(); i++) {
      JSONObject station = listeStation.getJSONObject(i);
      JSONObject pos = station.getJSONObject("position");
      LatLng coordStation = new LatLng(pos.getDouble("lat"), pos.getDouble("lng"));

      String status = station.getString("status");
      int availableBike = station.getInt("available_bikes");

      Marker marker;
      if ("OPEN".equals(status)) {
        if (availableBike == 0) {
          marker = mMap.addMarker(new MarkerOptions().position(coordStation)
              .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)));
          marker.setTag(station);
        } else {
          marker = mMap.addMarker(new MarkerOptions().position(coordStation)
              .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
          marker.setTag(station);
        }
      } else {
        marker = mMap.addMarker(new MarkerOptions().position(coordStation)
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        marker.setTag(station);
      }
      markers.add(marker);
    }
  }
}
