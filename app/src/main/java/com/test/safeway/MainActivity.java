package com.test.safeway;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import com.here.sdk.core.Anchor2D;
import com.here.sdk.core.Color;
import com.here.sdk.core.GeoCircle;
import com.here.sdk.core.GeoCoordinates;
import com.here.sdk.core.GeoPolyline;
import com.here.sdk.core.Metadata;
import com.here.sdk.core.errors.InstantiationErrorException;
import com.here.sdk.mapviewlite.MapCircle;
import com.here.sdk.mapviewlite.MapCircleStyle;
import com.here.sdk.mapviewlite.MapImage;
import com.here.sdk.mapviewlite.MapImageFactory;
import com.here.sdk.mapviewlite.MapMarker;
import com.here.sdk.mapviewlite.MapMarkerImageStyle;
import com.here.sdk.mapviewlite.MapPolyline;
import com.here.sdk.mapviewlite.MapPolylineStyle;
import com.here.sdk.mapviewlite.MapScene;
import com.here.sdk.mapviewlite.MapStyle;
import com.here.sdk.mapviewlite.MapViewLite;
import com.here.sdk.mapviewlite.PixelFormat;
import com.here.sdk.routing.CalculateRouteCallback;
import com.here.sdk.routing.OptimizationMode;
import com.here.sdk.routing.PedestrianOptions;
import com.here.sdk.routing.Route;
import com.here.sdk.routing.RouteOptions;
import com.here.sdk.routing.RouteTextOptions;
import com.here.sdk.routing.RoutingEngine;
import com.here.sdk.routing.RoutingError;
import com.here.sdk.routing.Section;
import com.here.sdk.routing.Waypoint;
import com.test.safeway.PermissionsRequestor.ResultListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static android.graphics.Color.pack;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private PermissionsRequestor permissionsRequestor;
    private MapViewLite mapView;

    private LocationManager locationManager;
    private LocationListener listener;

    private MapScene mapScene;
    private MapCircle mapCircle;
    private ImageButton button;

    private RoutingEngine routingEngine;
    private GeoCoordinates currentLocation;
    private final List<MapPolyline> mapPolylines = new ArrayList<>();
    private DatabaseManager databaseManager;

    private  GeoCoordinates destinationCoordinates;

    private List<MapMarker> mapMarkerList = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        databaseManager = new DatabaseManager(this);
        //databaseManager.addHandler(new GeoCoordinates(48.859306, 2.305487), 6);

        // Get a MapViewLite instance from the layout.
        mapView = findViewById(R.id.map_view);
        mapView.onCreate(savedInstanceState);

        handleAndroidPermissions();

        updateLocation();

        button = findViewById(R.id.searchButton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearMap();
                Intent intent = new Intent(MainActivity.this, Search.class);
                GeoCoordinates centerMap = mapView.getCamera().getTarget();
                double latitude = centerMap.latitude;
                double longitude = centerMap.longitude;
                intent.putExtra("mapViewLatitude", latitude);
                intent.putExtra("mapViewLongitude", longitude);
                startActivityForResult(intent, 2);// Activity is started with requestCode 2
                //startActivity(intent);
            }
        });

    }

    private void handleAndroidPermissions() {
        permissionsRequestor = new PermissionsRequestor(this);
        permissionsRequestor.request(new ResultListener(){

            @Override
            public void permissionsGranted() {

            }

            @Override
            public void permissionsDenied() {
                handleAndroidPermissions();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsRequestor.onRequestPermissionsResult(requestCode, grantResults);
    }

    private void loadMapScene() {
        // Load a scene from the SDK to render the map with a map style.
        mapView.getMapScene().loadScene(MapStyle.NORMAL_DAY, new MapScene.LoadSceneCallback() {
            @Override
            public void onLoadScene(@Nullable MapScene.ErrorCode errorCode) {
                if (errorCode == null) {
                    mapView.getCamera().setTarget(new GeoCoordinates(48.872156, 2.347464));
                    mapView.getCamera().setZoomLevel(12);

                } else {
                    Log.d(TAG, "onLoadScene failed: " + errorCode.toString());
                }
            }
        });
    }

    private void showLocation(Location location, MapViewLite mapView){
        if (mapCircle != null) {
            mapScene.removeMapCircle(mapCircle);
        }
        GeoCircle geoCircle = new GeoCircle(new GeoCoordinates(location.getLatitude(), location.getLongitude()), 30);
        MapCircleStyle mapCircleStyle = new MapCircleStyle();
        mapCircleStyle.setFillColor(0x00908AA0, PixelFormat.RGBA_8888);
        mapCircle = new MapCircle(geoCircle, mapCircleStyle);
        mapScene = mapView.getMapScene();
        mapScene.addMapCircle(mapCircle);
    }

    private void updateLocation(){
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        loadMapScene();

        listener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                currentLocation = new GeoCoordinates(location.getLatitude(), location.getLongitude());
                showLocation(location, mapView);
                mapView.getCamera().setTarget(currentLocation);
                mapView.getCamera().setZoomLevel(16);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {
                Intent i = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(i);
            }
        };

        //Vérifie qu'on a les autorisations. Meme si on a déjà verifié avec notre propre code dans handlepermissions, il dit qu'il y a une erreur si on n'utilise pas leur code...
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        locationManager.requestLocationUpdates("gps", 5000, 10, listener);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        // check if the request code is same as what is passed  here it is 2
        if(requestCode==2)
        {
            Bundle b = data.getExtras();
            assert b != null;
            destinationCoordinates = new GeoCoordinates( b.getDouble("Latitude"), b.getDouble("Longitude"));
            try {
                routingEngine = new RoutingEngine();
            } catch (InstantiationErrorException e) {
                new RuntimeException("Initialization of RoutingEngine failed: " + e.error.name());
            }
            addRoute(destinationCoordinates);
        }
    }

    public void addRoute(GeoCoordinates destinationCoordinates) {

        for (MapPolyline mapPolyline : mapPolylines) {
            mapView.getMapScene().removeMapPolyline(mapPolyline);
        }
        mapPolylines.clear();

        Waypoint startWaypoint = new Waypoint(currentLocation);
        Waypoint destinationWaypoint = new Waypoint(destinationCoordinates);

        List<Waypoint> waypoints = new ArrayList<>(Arrays.asList(startWaypoint, destinationWaypoint));

        routingEngine.calculateRoute(waypoints, new PedestrianOptions(new RouteOptions(OptimizationMode.FASTEST, 5, null), new RouteTextOptions()), new CalculateRouteCallback() {
                    @Override
                    public void onRouteCalculated(@Nullable RoutingError routingError, @Nullable List<Route> routes) {
                        if (routingError == null) {
                            List<List<RiskLocation>> riskLocations = new ArrayList<>();
                            for(int j = 0; j < routes.size(); j++){
                                Route route = routes.get(j);
                                riskLocations.add(new ArrayList<RiskLocation>());
                                safeRoad(route.getPolyline(), riskLocations.get(j));
                            }

                            int safest = safestRoute(riskLocations);
                            if(safest == 0){
                                showRouteDetails(routes.get(0));
                                showRouteOnMap(routes.get(0), destinationCoordinates, 0x00908AA0);
                            }
                            else{
                                showRouteDetails((routes.get(safest)));
                                showRouteOnMap(routes.get(safest), destinationCoordinates, 0x00908AA0);

                                //show fastest road in red
                                //TODO: check it doesn't appear
                                showRouteOnMap(routes.get(0), destinationCoordinates,  0x80ff0000); //255 = noir ?
                            }
                        }
                        else {
                            showDialog("Error while calculating a route:", routingError.toString());
                        }
                    }
                });
    }

    private void showRouteOnMap(Route route, GeoCoordinates destination, long color) {
        // Show route as polyline.
        GeoPolyline routeGeoPolyline;
        try {
            routeGeoPolyline = new GeoPolyline(route.getPolyline());
        } catch (InstantiationErrorException e) {
            // It should never happen that the route polyline contains less than two vertices.
            return;
        }
        MapPolylineStyle mapPolylineStyle = new MapPolylineStyle();
        mapPolylineStyle.setColor(color, PixelFormat.RGBA_8888);
        mapPolylineStyle.setWidthInPixels(10);
        MapPolyline routeMapPolyline = new MapPolyline(routeGeoPolyline, mapPolylineStyle);
        mapView.getMapScene().addMapPolyline(routeMapPolyline);
        mapPolylines.add(routeMapPolyline);

        databaseManager.getRedPoints(destination);

        addDestinationMarker();
    }

    private void showRouteDetails(Route route) {
        long estimatedTravelTimeInSeconds = route.getDurationInSeconds();
        int lengthInMeters = route.getLengthInMeters();

        String routeDetails =
                "Travel Time: " + formatTime(estimatedTravelTimeInSeconds)
                        + ", Length: " + formatLength(lengthInMeters);

        showDialog("Route Details", routeDetails);
    }

    private void showDialog(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.show();
    }

    private String formatTime(long sec) {
        long hours = sec / 3600;
        long minutes = (sec % 3600) / 60;

        return String.format(Locale.getDefault(), "%02d:%02d", hours, minutes);
    }

    private String formatLength(int meters) {
        int kilometers = meters / 1000;
        int remainingMeters = meters % 1000;

        return String.format(Locale.getDefault(), "%02d.%02d km", kilometers, remainingMeters);
    }

    private void safeRoad(List<GeoCoordinates> geoCoordinates, List<RiskLocation> IDs){
        for (int i = 0; i < geoCoordinates.size() - 1; i++){
            safeSection(geoCoordinates.get(i), geoCoordinates.get(i + 1), IDs);
        }
    }

    private void safeSection(GeoCoordinates start, GeoCoordinates end, List<RiskLocation> IDs){
        if (start.distanceTo(end) > 150){
            double latitude = (start.latitude + end.latitude)/2;
            double longitude = (start.longitude + end.longitude)/2;
            GeoCoordinates middle = new GeoCoordinates(latitude, longitude);
            safeSection(start, middle, IDs);
            safeSection(middle, end, IDs);
        }
        else{
            List itemsIDs = databaseManager.getRedPoints(start);
            for (int i = 0; i < itemsIDs.size(); i++){
                RiskLocation itemID = (RiskLocation) itemsIDs.get(i);
                if(!IDs.contains(itemID)){
                    IDs.add(itemID);
                }
            }
        }
    }

    private int safestRoute(List<List<RiskLocation>> riskLocations){
        int shortest = 0;
        for(int i = 0; i < riskLocations.size(); i++){
            if(riskLocations.get(i).size() < riskLocations.get(shortest).size()){
                shortest = i;
            }
        }
        return shortest;
    }

    private void addDestinationMarker() {
        MapImage mapImage = MapImageFactory.fromResource(MainActivity.this.getResources(), R.drawable.poi);

        MapMarker mapMarker = new MapMarker(destinationCoordinates);

        // The bottom, middle position should point to the location.
        // By default, the anchor point is set to 0.5, 0.5.
        MapMarkerImageStyle mapMarkerImageStyle = new MapMarkerImageStyle();
        mapMarkerImageStyle.setAnchorPoint(new Anchor2D(0.5F, 1));

        mapMarker.addImage(mapImage, mapMarkerImageStyle);

        Metadata metadata = new Metadata();
        metadata.setString("key_poi", "This is a POI.");
        mapMarker.setMetadata(metadata);

        mapView.getMapScene().addMapMarker(mapMarker);
        mapMarkerList.add(mapMarker);
    }

    private void addCircleMapMarker(GeoCoordinates geoCoordinates) {
        MapImage mapImage = MapImageFactory.fromResource(MainActivity.this.getResources(), R.drawable.circle);

        MapMarker mapMarker = new MapMarker(geoCoordinates);
        mapMarker.addImage(mapImage, new MapMarkerImageStyle());

        mapView.getMapScene().addMapMarker(mapMarker);
        mapMarkerList.add(mapMarker);
    }

    public void clearMap() {
        for (MapMarker mapMarker : mapMarkerList) {
            mapView.getMapScene().removeMapMarker(mapMarker);
        }
        mapMarkerList.clear();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //startLocationUpdates();
        mapView.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

}