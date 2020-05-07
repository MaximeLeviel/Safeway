package com.test.safeway;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.here.sdk.core.GeoCoordinates;
import com.here.sdk.core.LanguageCode;
import com.here.sdk.core.errors.InstantiationErrorException;
import com.here.sdk.search.Address;
import com.here.sdk.search.Place;
import com.here.sdk.search.SearchEngine;
import com.here.sdk.search.SearchError;
import com.here.sdk.search.SearchOptions;
import com.here.sdk.search.SuggestCallback;
import com.here.sdk.search.Suggestion;
import com.here.sdk.search.TextQuery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class Search extends AppCompatActivity {

    private static final String LOG_TAG = Search.class.getName();

    private SearchEngine searchEngine;
    private GeoCoordinates centerGeoCoordinates;
    EditText editText;

    private MyAdapter mAdapter;
    //private ReverseGeocodingEngine reverseGeocodingEngine;
    private String location;
    private List<SearchResults> searchResults;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        searchResults = new ArrayList<>();

        Bundle b = getIntent().getExtras();

        centerGeoCoordinates = new GeoCoordinates( b.getDouble("mapViewLatitude"), b.getDouble("mapViewLongitude"));


        searchResults.add(new SearchResults("No results found", "No results found", new GeoCoordinates(0, 0)));

        buildRecyclerView();


        autoSuggest();

    }

    private final SuggestCallback autosuggestCallback = new SuggestCallback() {
        @Override
        public void onSuggestCompleted(@Nullable SearchError searchError, @Nullable List<Suggestion> list) {
            if (searchError != null) {
                Log.d(LOG_TAG, "Autosuggest Error: " + searchError.name());
                return;
            }

            if (list == null || list.isEmpty()) {
                Log.d(LOG_TAG, "Autosuggest: No results found");
            } else {
                Log.d(LOG_TAG, "Autosuggest results: " + list.size());
                searchResults.clear();
                for (Suggestion autosuggestResult : list) {
                    Place place = autosuggestResult.getPlace();
                    //getAddressForCoordinates(autosuggestResult.coordinates);
                    searchResults.add(new SearchResults(place.getTitle(), place.getAddress().addressText, place.getCoordinates()));
                }

                mAdapter.update(searchResults);
            }

        }
    };

    private void autoSuggest() {
        try {
            searchEngine = new SearchEngine();
        } catch (InstantiationErrorException e) {
            throw new RuntimeException("Initialization of SearchEngine failed: " + e.error.name());
        }
        int maxItems = 5;
        SearchOptions searchOptions = new SearchOptions(LanguageCode.FR_FR, maxItems);

        /*try {
            reverseGeocodingEngine = new ReverseGeocodingEngine();
        } catch (InstantiationErrorException e) {
            new RuntimeException("Initialization of ReverseGeocodingEngine failed: " + e.error.name());
        }*/

        editText = findViewById(R.id.EditText);

        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {

                searchEngine.suggest(new TextQuery(s.toString(), centerGeoCoordinates), searchOptions, autosuggestCallback);

            }
        });
    }

    /*private void getAddressForCoordinates(GeoCoordinates geoCoordinates) {
        // By default results are localized in EN_US.
        ReverseGeocodingOptions reverseGeocodingOptions = new ReverseGeocodingOptions(LanguageCode.FR_FR);

        reverseGeocodingEngine.searchAddress(
                geoCoordinates, reverseGeocodingOptions, new ReverseGeocodingEngine.Callback() {
                    @Override
                    public void onSearchCompleted(@Nullable SearchError searchError,
                                                  @Nullable Address address) {
                        if (searchError != null || address == null) {
                            location = "No results found";
                            return;
                        }
                        location = address.addressText;
                    }
                });
    }*/

    private void buildRecyclerView(){
        RecyclerView recyclerView = findViewById(R.id.my_recycler_view);
        recyclerView. setHasFixedSize(true);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        mAdapter = new MyAdapter(searchResults);

        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(mAdapter);
        mAdapter.setOnItemClickListener(onItemClickListener);
    }

    private View.OnClickListener onItemClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            RecyclerView.ViewHolder viewHolder = (RecyclerView.ViewHolder) view.getTag();
            int position = viewHolder.getAdapterPosition();
            // viewHolder.getItemId();
            // viewHolder.getItemViewType();
            // viewHolder.itemView;
            SearchResults thisItem = searchResults.get(position);

            Intent intent = new Intent();
            intent.putExtra("Latitude", thisItem.getGeoCoordinates().latitude);
            intent.putExtra("Longitude", thisItem.getGeoCoordinates().longitude);
            setResult(2,intent);
            finish();//finishing activity

        }
    };



}
