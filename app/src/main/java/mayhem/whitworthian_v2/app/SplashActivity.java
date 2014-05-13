package mayhem.whitworthian_v2.app;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;

/** This is the SplashActivity.
 *  Includes the following functionality:
 *  -Retrieves article data from RSS feeds
 *  -Opens a Top News Article List
 *
 *  Contains the following class variables:
 *      app_Articles:       ArrayList containing all article data
 *      NUM_GENRES:         The total number of genres -- HARDCODED
 *      urls:               An array of URLs from which to obtain data through RSS
 *      alert:              A dialog that tells the user something went bad
 *      my_Progress_Bar:    The scroll wheel that tells the user that load is occuring
 *      my_Progress_Text:   Gives user idea that progress is being made on loading data
 *      locked:             Ensures that only one loading thread can exist at a time.
 */
public class SplashActivity extends ActionBarActivity {
    private ArrayList<Article> app_Articles;
    private final int NUM_GENRES = 5;
    private final URL urls[] = new URL[NUM_GENRES];
    private ProgressBar my_Progress_Bar;
    private TextView my_Progress_Text;
    private boolean locked;


    /* Creates the layout, fills the urls array, and fetches all data from thewhitworthian.com */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        locked = false;

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }

        app_Articles = null;
        fill_URLs(); // fill url array
    }

    /* Inflates options menu without functionality */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.splash, menu);
        return true;
    }

    /*Fills the URL string with all appropriate feeds */
    private void fill_URLs() {
        try{
            String[] url_String = getResources().getStringArray(R.array.news_urls);
            for(int i = 0; i < url_String.length; i++) {
                urls[i] = new URL(url_String[i]);
            }
        }
        catch (Exception bad) {
            Toast.makeText(getApplicationContext(),
                    String.format("A non-fatal error occurred! \nCode: 6d617968656d-0037"),
                    Toast.LENGTH_LONG).show();
        }
    }

    /*Opens up a background AsyncTask which fetches all of the data from the website */
    private class FetchArticlesTask extends AsyncTask<URL, Integer, ArrayList<Article>> {
        /*doInBackground is where the action happens, connection is made here, and data is
         * collected.
         */
        //TODO: Fix crash on loss of internet connectivity.
        //TODO: Try to make the data collection and storing cleaner/more efficient
        @Override
        protected ArrayList<Article> doInBackground(URL... urls) {
            RssHandler new_Parser = new RssHandler(getApplicationContext());
            ArrayList<Article> arrays[] = new ArrayList[NUM_GENRES];
            for (int i = 0; i < NUM_GENRES; i++) { // loop through all feeds
                try {
                    if (is_Network_Connected()) {
                        //Setup for connection
                        InputStream input = urls[i].openStream();
                        new_Parser.parse(input);
                        new_Parser.getArticleList(); //store the data.
                        publishProgress(new Integer[]{i+1});
                        if (i == 0) {
                            new_Parser.mark_Top();
                        }
                    } else {
                        return null;
                    }
                } catch (Exception bad) {
                    Toast.makeText(getApplicationContext(),
                            String.format("Failed to retrieve articles! \nCode: 6d617968656d-0038"),
                            Toast.LENGTH_LONG).show();
                }
            }
            publishProgress(new Integer[]{NUM_GENRES+1});

            return new_Parser.getArticleList();
        }

        /*Check to see if connected to a network*/
        private boolean is_Network_Connected() {
            final ConnectivityManager conMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            final NetworkInfo activeNetwork = conMgr.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.getState() == NetworkInfo.State.CONNECTED;
        }




        /* Updates load text on splash page */
        @Override
        protected void onProgressUpdate(Integer... progress) {
            switch(progress[0]) {
                case 0: update_Progress(getResources().getString(R.string.get_top));
                    break;
                case 1: update_Progress(getResources().getString(R.string.get_news));
                    break;
                case 2: update_Progress(getResources().getString(R.string.get_sports));
                    break;
                case 3: update_Progress(getResources().getString(R.string.get_opinion));
                    break;
                case 4: update_Progress(getResources().getString(R.string.get_ac));
                    break;
                case 5: update_Progress(getResources().getString(R.string.cleaning_data));
                    break;
            }
        }

        /* After articles are gathered, this opens up the Top News article list*/
        @Override
        protected void onPostExecute(ArrayList<Article> result) {
            super.onPostExecute(result);

            if(result==null) {
                runOnUiThread(new Runnable() {

                    public void run() {
                        Toast.makeText(getApplicationContext(), "Internet Connection Failed.", Toast.LENGTH_SHORT).show();
                        update_Progress(getResources().getString(R.string.connection_fail));
                        hide_Progress();
                    }
                });
                locked = false;
                return;
            }

            try{
                app_Articles = result;
                Intent article_List = new Intent(SplashActivity.this, ArticleListActivity.class);
                article_List.putExtra("this_Genre", "Top News");
                article_List.putParcelableArrayListExtra("my_Articles", app_Articles);
                article_List.putExtra("first_Instance", true);
                startActivity(article_List);

                // close this activity
                finish();
            } catch(Exception bad) {
                Toast.makeText(getApplicationContext(),
                        String.format("A non-fatal error occured! \nCode: 6d617968656d-0040"),
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    /*Handles item menu click */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch(item.getItemId()){
            case R.id.action_settings:
                return true;
            case R.id.action_refresh:
                try {
                    if (!(locked)) {
                        locked = true;
                        my_Progress_Bar.setVisibility(View.VISIBLE);
                        update_Progress(getResources().getString(R.string.load_text));
                        new FetchArticlesTask().execute(this.urls);
                    }
                    return true;
                } catch(Exception bad) {
                    Toast.makeText(getApplicationContext(),
                            String.format("A non-fatal error occured! \nCode: 6d617968656d-0041"),
                            Toast.LENGTH_LONG).show();
                }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /*Initialize the progress bar & text */
    public void init_Progress_Bar(View view) {
        try{
            my_Progress_Bar = (ProgressBar) view.findViewById(R.id.news_Load_Bar);
            my_Progress_Text = (TextView) view.findViewById(R.id.progress_Text);
        } catch (Exception bad) {
            Toast.makeText(getApplicationContext(),
                    String.format("A non-fatal error occured! \nCode: 6d617968656d-0042"),
                    Toast.LENGTH_LONG).show();
        }
    }

    /*Hide progress bar */
    public void hide_Progress() {
        my_Progress_Bar.setVisibility(View.INVISIBLE);
    }

    /*Update text in progress textview */
    public void update_Progress(String update){
        if (my_Progress_Text != null) {
            my_Progress_Text.setText(update);
        }
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_splash, container, false);

            init_Progress_Bar(rootView); //Initialize progress bar
            if (!(locked)) {
                locked = true;
                new FetchArticlesTask().execute(urls); // fetch data
            }

            return rootView;
        }
    }

}
