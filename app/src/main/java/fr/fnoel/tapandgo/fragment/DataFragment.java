package fr.fnoel.tapandgo.fragment;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import fr.fnoel.tapandgo.R;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import org.json.JSONArray;
import org.json.JSONException;

/**
 * A simple {@link Fragment} subclass. Activities that contain this fragment must implement the {@link
 * DataFragment.OnFragmentInteractionListener} interface to handle interaction events. Use the {@link
 * DataFragment#getInstance} factory method to create an instance of this fragment.
 */
public class DataFragment extends Fragment {

  private static final String TAG = "DataFragment";

  private static final String API_KEY = "API_KEY";

  private static final String URL = "https://api.jcdecaux.com/vls/v1/stations?contract=Nantes&apiKey=";
  private OnFragmentInteractionListener mListener;
  private DownloadTask mDownloadTask;

  public DataFragment() {
  }

  /**
   * Use this factory method to create a new instance of this fragment using the provided parameters.
   *
   * @return A new instance of fragment DataFragment.
   */
  public static DataFragment getInstance(FragmentManager fragmentManager) {
    DataFragment fragment = new DataFragment();
    fragmentManager.beginTransaction().add(fragment, TAG).commit();
    return fragment;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setRetainInstance(true);
  }


  @Override
  public void onAttach(Context context) {
    super.onAttach(context);

    if (context instanceof OnFragmentInteractionListener) {
      mListener = (OnFragmentInteractionListener) context;
    } else {
      throw new RuntimeException(context.toString() + " must implement OnFragmentInteractionListener");
    }
  }

  @Override
  public void onDetach() {
    super.onDetach();
    mListener = null;
  }

  public void startFetching(String api_key) {
    mDownloadTask = new DownloadTask(this);
    mDownloadTask.execute(URL.concat(api_key));
  }

  public void cancelFetching() {
    if (mDownloadTask != null) {
      mDownloadTask.cancel(true);
    }
  }


  /**
   * This interface must be implemented by activities that contain this fragment to allow an interaction in this
   * fragment to be communicated to the activity and potentially other fragments contained in that activity. <p> See the
   * Android Training lesson <a href= "http://developer.android.com/training/basics/fragments/communicating.html"
   * >Communicating with Other Fragments</a> for more information.
   */
  public interface OnFragmentInteractionListener {

    void onFragmentInteraction(String json);
  }

  /**
   * Implementation of AsyncTask designed to fetch data from the network.
   */
  private static class DownloadTask extends AsyncTask<String, Integer, DownloadTask.Result> {

    private DataFragment fragment;

    DownloadTask(DataFragment fragment) {
      this.fragment = fragment;
    }

    /**
     * Wrapper class that serves as a union of a result value and an exception. When the download task has completed,
     * either the result value or exception can be a non-null value. This allows you to pass exceptions to the UI thread
     * that were thrown during doInBackground().
     */
    class Result {

      private String mResultValue;
      private Exception mException;

      private Result(String resultValue) {
        mResultValue = resultValue;
      }

      private Result(Exception exception) {
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
          result = new Result(resultString);
        } catch (Exception e) {
          result = new Result(e);
        }
      }
      return result;
    }

    private String downloadUrl(URL url) {
      StringBuilder result = new StringBuilder();

      try (InputStreamReader isr = new InputStreamReader(url.openStream())) {
        try (BufferedReader br = new BufferedReader(isr)) {
          String line;
          while ((line = br.readLine()) != null) {
            result.append(line + "\n");
          }
        }
      } catch (IOException e) {
        e.printStackTrace();
      }

      return result.toString();
    }


    /**
     * Updates the DownloadCallback with the result.
     */
    @Override
    protected void onPostExecute(Result result) {
      if (result != null && fragment != null && result.mResultValue != null) {
        fragment.mListener.onFragmentInteraction(result.mResultValue);
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
