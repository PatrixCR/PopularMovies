package rembang.patrix.movies;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DetailActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Intent intent = getIntent();
        MainActivity.Movie movie = intent.getParcelableExtra(getString(R.string.extra_main_intent_detail));

        TextView titleView = (TextView) findViewById(R.id.activity_detail_title);
        TextView ratingView = (TextView) findViewById(R.id.activity_detail_rating);
        TextView releaseDateView = (TextView) findViewById(R.id.activity_detail_release_date);
        TextView synopsisView = (TextView) findViewById(R.id.activity_detail_synopsis);
        ImageView posterView = (ImageView) findViewById(R.id.activity_detail_poster);

        titleView.setText(movie.getOriginalTitle());
        ratingView.setText("" + movie.getRating());
        String humanFriendlyReleaseDate = null;
        try {
            Date releaseDate = new SimpleDateFormat("yyyy-MM-dd").parse(movie.getReleaseDate());
            humanFriendlyReleaseDate = new SimpleDateFormat("d MMMM yyyy").format(releaseDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        releaseDateView.setText(humanFriendlyReleaseDate);
        synopsisView.setText(movie.getSynopsis());
        Picasso.with(this).load(movie.getPosterUri()).into(posterView);
    }

}
