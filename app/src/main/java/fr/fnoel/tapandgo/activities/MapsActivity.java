package fr.fnoel.tapandgo.activities;

import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import fr.fnoel.tapandgo.fragment.DataFragment;
import fr.fnoel.tapandgo.R;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, DataFragment.OnFragmentInteractionListener {

	private GoogleMap mMap;

	private JSONArray listeStation = new JSONArray();
	private DataFragment mDataFragment;

	BitmapDescriptor closeIcon;
	BitmapDescriptor availableIcon;
	BitmapDescriptor unavailableIcon;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_maps);
		// Obtain the SupportMapFragment and get notified when the map is ready to be used.
		SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
				.findFragmentById(R.id.map);
		mapFragment.getMapAsync(this);

		closeIcon = BitmapDescriptorFactory.fromResource(R.drawable.close);
		availableIcon = BitmapDescriptorFactory.fromResource(R.drawable.available);
		unavailableIcon = BitmapDescriptorFactory.fromResource(R.drawable.unavailable);

		mDataFragment = DataFragment.getInstance(getSupportFragmentManager(), listeStation);
		if (mDataFragment != null) {
			// Execute the async download.
			mDataFragment.startFetching();
		}

		// GOTO FILTER VIEW
		// 		startActivity(new Intent(this, MapsActivity.class));
	}


	/**
	 * Manipulates the map once available.
	 * This callback is triggered when the map is ready to be used.
	 * This is where we can add markers or lines, add listeners or move the camera. In this case,
	 * we just add a marker near Sydney, Australia.
	 * If Google Play services is not installed on the device, the user will be prompted to install
	 * it inside the SupportMapFragment. This method will only be triggered once the user has
	 * installed Google Play services and returned to the app.
	 */
	@Override
	public void onMapReady(GoogleMap googleMap) {
		mMap = googleMap;
		LatLng nantes = new LatLng(47.214617, -1.549949);
		mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(nantes, 8.0f));
	}


	@Override
	public void onFragmentInteraction(String json) {
		try {
			Log.i("MainActivity", json);
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
					mMap.addMarker(new MarkerOptions().position(coordStation).icon(unavailableIcon));

				} else {
					mMap.addMarker(new MarkerOptions().position(coordStation).icon(availableIcon));
				}
			} else {
				mMap.addMarker(new MarkerOptions().position(coordStation).icon(closeIcon));
			}
		}
	}
}
