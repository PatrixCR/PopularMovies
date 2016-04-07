package rembang.patrix.movies;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends ActionBarActivity {

    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    private MovieImageArrayAdapter movieAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        movieAdapter = new MovieImageArrayAdapter(this, new ArrayList<Movie>());
        GridView moviesGrid = (GridView) findViewById(R.id.movies_grid);
        moviesGrid.setAdapter(movieAdapter);

        moviesGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(MainActivity.this, DetailActivity.class);
                intent.putExtra(getString(R.string.extra_main_intent_detail), movieAdapter.getItem(position));
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        String sortBy = PreferenceManager.getDefaultSharedPreferences(this).getString(
                getString(R.string.sort_setting_key),
                getString(R.string.sort_setting_default));
        (new FetchMoviesTask()).execute(sortBy);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public static class Movie implements Parcelable {
        private Uri posterUri;
        private String originalTitle;
        private String synopsis;
        private double rating;
        private String releaseDate;

        public Movie(Uri posterUri, String originalTitle, String synopsis, double rating, String releaseDate) {
            this.posterUri = posterUri;
            this.originalTitle = originalTitle;
            this.synopsis = synopsis;
            this.rating = rating;
            this.releaseDate = releaseDate;
        }

        public String getOriginalTitle() {
            return originalTitle;
        }

        public String getSynopsis() {
            return synopsis;
        }

        public double getRating() {
            return rating;
        }

        public String getReleaseDate() {
            return releaseDate;
        }

        public Uri getPosterUri() {
            return posterUri;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(originalTitle);
            dest.writeString(synopsis);
            dest.writeDouble(rating);
            dest.writeString(releaseDate);
            dest.writeParcelable(posterUri, flags);
        }

        public static final Parcelable.Creator<Movie> CREATOR = new Parcelable.Creator<Movie>() {
            @Override
            public Movie createFromParcel(Parcel source) {
                return new Movie(source);
            }

            @Override
            public Movie[] newArray(int size) {
                return new Movie[size];
            }
        };

        private Movie(Parcel in) {
            originalTitle = in.readString();
            synopsis = in.readString();
            rating = in.readDouble();
            releaseDate = in.readString();
            posterUri = in.readParcelable(Uri.class.getClassLoader());
        }
    }

    private static class MovieImageArrayAdapter extends ArrayAdapter<Movie> {
        private List<Movie> movies;
        private Context context;

        public MovieImageArrayAdapter(Context context, List<Movie> movies) {
            super(context, 0, movies);

            this.context = context;
            this.movies = movies;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView imageView;
            if (convertView == null) {
                // if it's not recycled, initialize some attributes
                imageView = new ImageView(context);
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            } else {
                imageView = (ImageView) convertView;
            }

            Picasso.with(context).load(movies.get(position).getPosterUri()).into(imageView);
            return imageView;
        }
    }

    class FetchMoviesTask extends AsyncTask<String, Void, Movie[]> {
        @Override
        protected void onPostExecute(Movie[] movies) {
            if (movies != null) {
                movieAdapter.clear();
                for (Movie m : movies) {
                    movieAdapter.add(m);
                }
            }
        }

        @Override
        protected Movie[] doInBackground(String... params) {
            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String jsonStr = null;
            Movie[] movies = null;

            try {
                // Construct the URL for the OpenWeatherMap query
                // Possible parameters are available at OWM's forecast API page, at
                // http://openweathermap.org/API#forecast
                Uri uri = (new Uri.Builder()).scheme("http")
                        .path("//api.themoviedb.org/3/movie/" + params[0])
                        .appendQueryParameter("api_key", "ae55fd49beb3560cb2cd89a39bae6fbc")
                        .build();

                URL url = new URL(uri.toString());

                // Create the request to OpenWeatherMap, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    jsonStr = null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    jsonStr = null;
                }
                jsonStr = buffer.toString();
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                // If the code didn't successfully get the weather data, there's no point in attempting
                // to parse it.
                jsonStr = null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }

            try {
                if (jsonStr != null) {
                    movies = parse(jsonStr);
                }
            } catch (JSONException e) {
                Log.e(LOG_TAG, "Error ", e);
            }

            return movies;
        }

        private Movie[] parse(String jsonStr) throws JSONException {
            JSONObject data = new JSONObject(jsonStr);
            JSONArray results = data.getJSONArray("results");
            Movie[] movies = new Movie[results.length()];

            for (int i = 0; i < results.length(); i++) {
                JSONObject result = results.getJSONObject(i);
                movies[i] = new Movie(Uri.parse("http://image.tmdb.org/t/p/w185" + result.getString("poster_path")),
                        result.getString("original_title"), result.getString("overview"),
                        result.getDouble("vote_average"), result.getString("release_date"));
            }
            return movies;
        }
    }
}
