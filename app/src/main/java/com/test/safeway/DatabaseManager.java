package com.test.safeway;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.here.sdk.core.GeoCoordinates;

import java.util.ArrayList;
import java.util.List;

public class DatabaseManager extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1; // première version de la base de donnée
    private static final String DATABASE_NAME = "Safeway.db";
    private static final String TABLE_NAME = "Risk_Zones";
    private static final String COLUMN_ID = "PlaceID";
    private static final String COLUMN_LATITUDE = "Latitude";
    private static final String COLUMN_LONGITUDE = "Longitude";
    private static final String COLUMN_NOTE = "Note";

    public DatabaseManager(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String strSql = "CREATE TABLE " + TABLE_NAME + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT not null, " // id du lieu qui va s'incrementer automatiquement a chaque nouvelle ajout de lieu. Facilite lappel
                + COLUMN_LATITUDE + " DOUBLE not null, "
                + COLUMN_LONGITUDE + " DOUBLE not null, "
                + COLUMN_NOTE + " INTEGER not null )";

        db.execSQL(strSql);
        Log.i("DATABASE", "onCreate invoked");

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String strSql = "DROP TABLE " + TABLE_NAME;
        db.execSQL(strSql);
        this.onCreate(db);
        Log.i("DATABASE", "onUpgrade invoked");

    }

    /*
    public void insertInfos(double latitude, double longitude, int note){
        String strSql = "insert into Risk_Zones (latitude, longitude, note) values ("
                +latitude + ", " + longitude+" , "+note+")";

        Log.i("DATABASE", "insertInfos invoked");
    }*/

    public void addHandler(GeoCoordinates geoCoordinates, int note) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_LATITUDE, geoCoordinates.latitude);
        values.put(COLUMN_LONGITUDE, geoCoordinates.longitude);
        values.put(COLUMN_NOTE, note);
        SQLiteDatabase db = this.getWritableDatabase();
        db.insert(TABLE_NAME, null, values);
        db.close();
    }

    public static GeoCoordinates calculateDerivedPosition(GeoCoordinates point, double range, double bearing)
    {
        double EarthRadius = 6371000; // m

        double latA = Math.toRadians(point.latitude);
        double lonA = Math.toRadians(point.longitude);
        double angularDistance = range / EarthRadius;
        double trueCourse = Math.toRadians(bearing);

        double lat = Math.asin(
                Math.sin(latA) * Math.cos(angularDistance) +
                        Math.cos(latA) * Math.sin(angularDistance)
                                * Math.cos(trueCourse));

        double dlon = Math.atan2(
                Math.sin(trueCourse) * Math.sin(angularDistance)
                        * Math.cos(latA),
                Math.cos(angularDistance) - Math.sin(latA) * Math.sin(lat));

        double lon = ((lonA + dlon + Math.PI) % (Math.PI * 2)) - Math.PI;

        lat = Math.toDegrees(lat);
        lon = Math.toDegrees(lon);

        return new GeoCoordinates(lat, lon);
    }

    public List getRedPoints(GeoCoordinates position){
        double radius = 100;
        final double mult = 1.1; // mult = 1.1; is more reliable
        GeoCoordinates p1 = calculateDerivedPosition(position, mult * radius, 0);
        GeoCoordinates p2 = calculateDerivedPosition(position, mult * radius, 90);
        GeoCoordinates p3 = calculateDerivedPosition(position, mult * radius, 180);
        GeoCoordinates p4 = calculateDerivedPosition(position, mult * radius, 270);

        String strWhere = " WHERE "
                + COLUMN_LATITUDE + " > " + String.valueOf(p3.latitude) + " AND "
                + COLUMN_LATITUDE + " < " + String.valueOf(p1.latitude) + " AND "
                + COLUMN_LONGITUDE + " < " + String.valueOf(p2.longitude) + " AND "
                + COLUMN_LONGITUDE + " > " + String.valueOf(p4.longitude);

        String query = "Select * FROM "+ TABLE_NAME + strWhere;
        Log.i("Debug:", query);

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(query, null);

        List<RiskLocation> itemIds = new ArrayList<RiskLocation>();
        while(cursor.moveToNext()) {
            int ID = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID));

            double latitude = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LATITUDE));
            double longitude = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LONGITUDE));
            GeoCoordinates geoCoordinates = new GeoCoordinates(latitude, longitude);

            int note = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_NOTE));

            itemIds.add(new RiskLocation(ID, geoCoordinates, note));
        }
        cursor.close();
        db.close();

        return itemIds;
    }

    public List<RiskLocation> getAll(){
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("Select * FROM " + TABLE_NAME, null);

        List<RiskLocation> itemIds = new ArrayList<RiskLocation>();
        while(cursor.moveToNext()) {
            int ID = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID));

            double latitude = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LATITUDE));
            double longitude = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LONGITUDE));
            GeoCoordinates geoCoordinates = new GeoCoordinates(latitude, longitude);

            int note = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_NOTE));

            itemIds.add(new RiskLocation(ID, geoCoordinates, note));
        }
        cursor.close();
        db.close();

        return itemIds;
    }


}