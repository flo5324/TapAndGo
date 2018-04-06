package fr.fnoel.tapandgo.fragment;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

/**
 * Fragment utilisé pour effectuer des opérations réseaux afin d'éviter de bloquer le thread principal.<br/> Ce fragment
 * est utilisé pour récupérer la liste des stations et les renvoyé à l'activité principale ensuite.<br/> Pour utiliser
 * ce fragment, il faut d'abord en récupérer une instance en utilisant {@link DataFragment#getInstance}
 */
public class DataFragment extends Fragment {

  private static final String TAG = "DataFragment";
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
    // On indique que ce fragment sera persistant afin que les opérations réseaux ne soient pas stoppées en exécution
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
    cancelFetching();
    mListener = null;
  }

  /**
   * Méthode permettant de créer l'{@link AsyncTask} qui récupèrera la liste des stations.
   *
   * @param api_key clé de l'API
   */
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
   * Interface permettant de communiquer avec l'activité pour renvoyer le résultat de l'appel à l'API
   */
  public interface OnFragmentInteractionListener {

    void onFragmentInteraction(String json);
  }

  /**
   * AsyncTask est le thread qui fera les appels réseaux sans bloquer le thread principal.<br/> Il prend en entrée l'url
   * de l'API et renvoie un wrapper contenant la liste des stations.
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

    @Override
    protected void onPreExecute() {
    }

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

    private String downloadUrl(URL url) throws IOException {
      StringBuilder result = new StringBuilder();

      try (InputStreamReader isr = new InputStreamReader(url.openStream())) {
        try (BufferedReader br = new BufferedReader(isr)) {
          String line;
          while ((line = br.readLine()) != null) {
            result.append(line).append("\n");
          }
        }
      }

      return result.toString();
    }


    /**
     * Méthode appelée une fois que la récupération est terminée.<br/> Elle utilise l'interface {@link
     * OnFragmentInteractionListener} pour envoyer le résultat à l'activité principale.
     */
    @Override
    protected void onPostExecute(Result result) {
      if (result != null && fragment != null && result.mResultValue != null) {
        fragment.mListener.onFragmentInteraction(result.mResultValue);
      }
    }

    @Override
    protected void onCancelled(Result result) {
    }
  }

}
