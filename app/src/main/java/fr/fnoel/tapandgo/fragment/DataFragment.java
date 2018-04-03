package fr.fnoel.tapandgo.fragment;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import fr.fnoel.tapandgo.R;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link DataFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link DataFragment#getInstance} factory method to
 * create an instance of this fragment.
 */
public class DataFragment extends Fragment {
	private static final String TAG = "DataFragment";

	private static final String LISTE_KEY = "listeStation";

	private static final String URL = "https://api.jcdecaux.com/vls/v1/stations?contract=Nantes&apiKey=";
	private String api_key;
	private OnFragmentInteractionListener mListener;
	private DownloadTask mDownloadTask;

	public DataFragment() {
		// Required empty public constructor
	}

	/**
	 * Use this factory method to create a new instance of
	 * this fragment using the provided parameters.
	 *
	 * @param listeStation liste des statios.
	 * @return A new instance of fragment DataFragment.
	 */
	public static DataFragment getInstance(FragmentManager fragmentManager, JSONArray listeStation) {
		DataFragment fragment = new DataFragment();
		Bundle args = new Bundle();
		args.putString(LISTE_KEY, listeStation.toString());
		fragmentManager.beginTransaction().add(fragment, TAG).commit();
		return fragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		setRetainInstance(true);
		super.onCreate(savedInstanceState);

		if (getArguments() != null) {
			try {
				JSONArray listeStation = new JSONArray(getArguments().getString(LISTE_KEY));
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}


	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
		if (context instanceof OnFragmentInteractionListener) {
			mListener = (OnFragmentInteractionListener) context;
		} else {
			throw new RuntimeException(context.toString()
					+ " must implement OnFragmentInteractionListener");
		}
		api_key = context.getString(R.string.jcdecaux_key);
	}

	@Override
	public void onDetach() {
		super.onDetach();
		mListener = null;
	}

	public void startFetching() {
		cancelFetching();
		mDownloadTask = new DownloadTask(this);
		mDownloadTask.execute(URL + api_key);
	}

	public void cancelFetching() {
		if (mDownloadTask != null) {
			mDownloadTask.cancel(true);
		}
	}


	/**
	 * This interface must be implemented by activities that contain this
	 * fragment to allow an interaction in this fragment to be communicated
	 * to the activity and potentially other fragments contained in that
	 * activity.
	 * <p>
	 * See the Android Training lesson <a href=
	 * "http://developer.android.com/training/basics/fragments/communicating.html"
	 * >Communicating with Other Fragments</a> for more information.
	 */
	public interface OnFragmentInteractionListener {
		void onFragmentInteraction(String json);
	}

	/**
	 * Implementation of AsyncTask designed to fetch data from the network.
	 */
	private class DownloadTask extends AsyncTask<String, Integer, DownloadTask.Result> {

		protected DataFragment fragment = null;

		DownloadTask(DataFragment fragment) {
			this.fragment = fragment;
		}

		/**
		 * Wrapper class that serves as a union of a result value and an exception. When the download
		 * task has completed, either the result value or exception can be a non-null value.
		 * This allows you to pass exceptions to the UI thread that were thrown during doInBackground().
		 */
		class Result {
			public String mResultValue;
			public Exception mException;

			public Result(String resultValue) {
				mResultValue = resultValue;
			}

			public Result(Exception exception) {
				mException = exception;
			}
		}

		/**
		 * Cancel background network operation if we do not have network connectivity.
		 */
		@Override
		protected void onPreExecute() {
//			if (fragment != null) {
//				NetworkInfo networkInfo = fragment.getActiveNetworkInfo();
//				if (networkInfo == null || !networkInfo.isConnected() ||
//						(networkInfo.getType() != ConnectivityManager.TYPE_WIFI
//								&& networkInfo.getType() != ConnectivityManager.TYPE_MOBILE)) {
//					// If no connectivity, cancel task and update Callback with null data.
//					fragment.updateFromDownload(null);
//					cancel(true);
//				}
//			}
		}

		/**
		 * Defines work to perform on the background thread.
		 */
		@Override
		protected DownloadTask.Result doInBackground(String... urls) {
			Result result = null;
			if (!isCancelled() && urls != null && urls.length > 0) {
				String urlString = urls[0];
				try {
					URL url = new URL(urlString);
					String resultString = downloadUrl(url);
					if (resultString != null) {
						result = new Result(resultString);
					} else {
						throw new IOException("No response received.");
					}
				} catch (Exception e) {
					result = new Result(e);
				}
			}
			return result;
		}

		private String downloadUrl(URL url) throws IOException {
			StringBuilder result = new StringBuilder();
			HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			connection.setReadTimeout(10000 /* milliseconds */);
			connection.setConnectTimeout(15000 /* milliseconds */);
			connection.setDoOutput(true);
			connection.connect();

			BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));

			String line;
			while ((line = br.readLine()) != null) {
				result.append(line + "\n");
			}
			br.close();
			connection.disconnect();

			return result.toString();
		}


		/**
		 * Updates the DownloadCallback with the result.
		 */
		@Override
		protected void onPostExecute(Result result) {
			if (result != null && fragment != null) {
				if (result.mException != null) {
//					fragment.updateFromDownload(result.mException.getMessage());
				} else if (result.mResultValue != null) {
					fragment.mListener.onFragmentInteraction(result.mResultValue);
				}
			}
		}

		/**
		 * Override to add special behavior for cancelled AsyncTask.
		 */
		@Override
		protected void onCancelled(Result result) {
		}
	}

}
