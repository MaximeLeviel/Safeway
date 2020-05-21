package com.test.safeway;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.here.sdk.core.Anchor2D;
import com.here.sdk.core.GeoCircle;
import com.here.sdk.core.GeoCoordinates;
import com.here.sdk.core.Metadata;
import com.here.sdk.core.Point2D;
import com.here.sdk.core.errors.InstantiationErrorException;
import com.here.sdk.gestures.TapListener;
import com.here.sdk.mapviewlite.MapCircle;
import com.here.sdk.mapviewlite.MapCircleStyle;
import com.here.sdk.mapviewlite.MapImage;
import com.here.sdk.mapviewlite.MapImageFactory;
import com.here.sdk.mapviewlite.MapMarker;
import com.here.sdk.mapviewlite.MapMarkerImageStyle;
import com.here.sdk.mapviewlite.MapScene;
import com.here.sdk.mapviewlite.MapStyle;
import com.here.sdk.mapviewlite.MapViewLite;
import com.here.sdk.mapviewlite.PickMapItemsCallback;
import com.here.sdk.mapviewlite.PickMapItemsResult;
import com.here.sdk.mapviewlite.PixelFormat;

import java.util.ArrayList;
import java.util.List;

public class ReportActivity extends AppCompatActivity {

    private static final String TAG = ReportActivity.class.getSimpleName();
    private MapViewLite mapViewR;
    private GeoCoordinates currentLocation;
    private GeoCoordinates issueLocation;
    private MapScene mapScene;
    private MapCircle mapCircle;
    private MapMarker mapMarkerPOI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        // Get a mapViewRLite instance from the layout.
        mapViewR = findViewById(R.id.map_view);
        mapViewR.onCreate(savedInstanceState);

        Bundle b = getIntent().getExtras();

        if (b != null) {
            currentLocation = new GeoCoordinates(b.getDouble("locationLatitude"), b.getDouble("locationLongitude"));
        }

        loadMapScene();
        setCurrentLocation();
        showLocation();
        addDestinationMarker();
        setTapGestureHandler();
        issueLocation = currentLocation;

        Button searchButton = findViewById(R.id.searchButton);
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ReportActivity.this, Search.class);
                GeoCoordinates centerMap = mapViewR.getCamera().getTarget();
                double latitude = centerMap.latitude;
                double longitude = centerMap.longitude;
                intent.putExtra("mapViewLatitude", latitude);
                intent.putExtra("mapViewLongitude", longitude);
                startActivityForResult(intent, 2);// Activity is started with requestCode 2
            }
        });

        Button sendButton = findViewById(R.id.sendButton);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatabaseManager databaseManager = new DatabaseManager(ReportActivity.this);
                databaseManager.addHandler(issueLocation, 6);
                //finish();
            }
        });
    }

    private void loadMapScene() {
        // Load a scene from the SDK to render the map with a map style.
        mapViewR.getMapScene().loadScene(MapStyle.NORMAL_DAY, new MapScene.LoadSceneCallback() {
            @Override
            public void onLoadScene(@Nullable MapScene.ErrorCode errorCode) {
                if (errorCode == null) {
                    mapViewR.getCamera().setTarget(new GeoCoordinates(48.872156, 2.347464));
                    mapViewR.getCamera().setZoomLevel(12);

                } else {
                    Log.d(TAG, "onLoadScene failed: " + errorCode.toString());
                }
            }
        });
    }

    private void updateLocation(){
        PlatformPositioningProvider platformPositioningProvider = new PlatformPositioningProvider(ReportActivity.this);
        platformPositioningProvider.startLocating(new PlatformPositioningProvider.PlatformLocationListener() {
            @Override
            public void onLocationUpdated(android.location.Location location) {
                currentLocation = new GeoCoordinates(location.getLatitude(), location.getLongitude());
                mapViewR.getCamera().setTarget(currentLocation);
                mapViewR.getCamera().setZoomLevel(16);
                addDestinationMarker();
            }
        });
    }

    private void showLocation(){
        if (mapCircle != null) {
            mapScene.removeMapCircle(mapCircle);
        }
        GeoCircle geoCircle = new GeoCircle(new GeoCoordinates(currentLocation.latitude, currentLocation.longitude), 30);
        MapCircleStyle mapCircleStyle = new MapCircleStyle();
        mapCircleStyle.setFillColor(0x00908AA0, PixelFormat.RGBA_8888);
        mapCircle = new MapCircle(geoCircle, mapCircleStyle);
        mapScene = mapViewR.getMapScene();
        mapScene.addMapCircle(mapCircle);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        // check if the request code is same as what is passed  here it is 2
        if(requestCode==2)
        {
            Bundle b = data.getExtras();
            if (b!= null){
                issueLocation = new GeoCoordinates(b.getDouble("Latitude"), b.getDouble("Longitude"));
                addDestinationMarker();
            }
        }
    }

    private void addDestinationMarker() {

        if(mapMarkerPOI != null){
            mapViewR.getMapScene().removeMapMarker(mapMarkerPOI);
        }

        MapImage mapImage = MapImageFactory.fromResource(ReportActivity.this.getResources(), R.drawable.poi);

        mapMarkerPOI = new MapMarker(issueLocation);

        // The bottom, middle position should point to the location.
        // By default, the anchor point is set to 0.5, 0.5.
        MapMarkerImageStyle mapMarkerImageStyle = new MapMarkerImageStyle();
        mapMarkerImageStyle.setAnchorPoint(new Anchor2D(0.5F, 1));

        mapMarkerPOI.addImage(mapImage, mapMarkerImageStyle);

        Metadata metadata = new Metadata();
        metadata.setString("key_poi", "This is a POI.");
        mapMarkerPOI.setMetadata(metadata);

        mapViewR.getMapScene().addMapMarker(mapMarkerPOI);
    }

    private void setCurrentLocation(){
        issueLocation = currentLocation;
        addDestinationMarker();
    }

    private void setTapGestureHandler() {
        mapViewR.getGestures().setTapListener(new TapListener() {
            @Override
            public void onTap(Point2D touchPoint) {
                issueLocation = mapViewR.getCamera().viewToGeoCoordinates(touchPoint);
                addDestinationMarker();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapViewR.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapViewR.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapViewR.onDestroy();
    }
}
