package amirz.dynamicstune.database;

import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import java.util.ArrayList;
import java.util.List;

import amirz.dynamicstune.Algorithm;

public class MeasureDB {
    public static class Entry implements BaseColumns {
        public static final String TABLE_NAME = "measurement";

        public static final String COL_PACKAGE = "package_name";
        public static final String COL_COMPONENT = "component_name";
        public static final String COL_BOOST = "boost";
        public static final String COL_FRAMES = "frames";
        public static final String COL_JANKS = "janks";
        public static final String COL_PERC_90 = "percentile_90";
        public static final String COL_PERC_95 = "percentile_95";
        public static final String COL_PERC_99 = "percentile_99";
    }

    private static final String CREATE =
            "CREATE TABLE " + Entry.TABLE_NAME + " (" +
                    Entry._ID + " INTEGER PRIMARY KEY," +
                    Entry.COL_PACKAGE + " TEXT," +
                    Entry.COL_COMPONENT + " TEXT," +
                    Entry.COL_BOOST + " INTEGER," +
                    Entry.COL_FRAMES + " INTEGER," +
                    Entry.COL_JANKS + " INTEGER," +
                    Entry.COL_PERC_90 + " INTEGER," +
                    Entry.COL_PERC_95 + " INTEGER," +
                    Entry.COL_PERC_99 + " INTEGER)";

    private static final String DROP =
            "DROP TABLE IF EXISTS " + Entry.TABLE_NAME;

    private static final String[] RESULT_PROJECTION = {
            Entry.COL_BOOST,
            Entry.COL_FRAMES,
            Entry.COL_JANKS,
            Entry.COL_PERC_90,
            Entry.COL_PERC_95,
            Entry.COL_PERC_99
    };

    private static final int RESULT_COUNT = 30;

    private final Helper mHelper;
    private boolean mClosed;

    public MeasureDB(Helper helper) {
        mHelper = helper;
    }

    public long insert(ComponentName componentName, Algorithm.Measurement info) {
        if (mClosed) {
            return -1;
        }

        SQLiteDatabase db = mHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(Entry.COL_PACKAGE, componentName.getPackageName());
        values.put(Entry.COL_COMPONENT, componentName.flattenToString());
        values.put(Entry.COL_BOOST, info.boost);
        values.put(Entry.COL_FRAMES, info.total);
        values.put(Entry.COL_JANKS, info.janky);
        values.put(Entry.COL_PERC_90, info.perc90);
        values.put(Entry.COL_PERC_95, info.perc95);
        values.put(Entry.COL_PERC_99, info.perc99);

        return db.insert(Entry.TABLE_NAME, null, values);
    }

    public List<Algorithm.Measurement> select(ComponentName componentName) {
        List<Algorithm.Measurement> results = new ArrayList<>();
        if (!mClosed) {
            SQLiteDatabase db = mHelper.getReadableDatabase();

            try (Cursor cursor = db.query(Entry.TABLE_NAME, RESULT_PROJECTION,
                    Entry.COL_COMPONENT + " = ?", new String[] { componentName.flattenToString() },
                    null, null, null
            )) {
                while (cursor.moveToNext()) {
                    results.add(readMeasurement(cursor));
                }
            }
        }
        return results;
    }


    public List<Algorithm.Measurement> select(String packageName) {
        List<Algorithm.Measurement> results = new ArrayList<>();
        if (!mClosed) {
            SQLiteDatabase db = mHelper.getReadableDatabase();

            try (Cursor cursor = db.query(Entry.TABLE_NAME, RESULT_PROJECTION,
                    Entry.COL_PACKAGE + " = ?", new String[] { packageName },
                    null, null, null
            )) {
                while (cursor.moveToNext()) {
                    results.add(readMeasurement(cursor));
                }
            }
        }
        return results;
    }

    public void clearOldResults() {
        /*if (!mClosed) {
            SQLiteDatabase db = mHelper.getWritableDatabase();
            db.execSQL("DELETE FROM " + Entry.TABLE_NAME +
                    " ORDER BY " + Entry._ID + " DESC" +
                    " LIMIT -1" +
                    " OFFSET " + RESULT_COUNT);
        }*/
    }

    public void close() {
        mClosed = true;
        mHelper.close();
    }

    private Algorithm.Measurement readMeasurement(Cursor cursor) {
        Algorithm.Measurement measurement = new Algorithm.Measurement(
                cursor.getInt(cursor.getColumnIndexOrThrow(Entry.COL_BOOST)));
        measurement.total = cursor.getInt(cursor.getColumnIndexOrThrow(Entry.COL_FRAMES));
        measurement.janky = cursor.getInt(cursor.getColumnIndexOrThrow(Entry.COL_JANKS));
        measurement.perc90 = cursor.getInt(cursor.getColumnIndexOrThrow(Entry.COL_PERC_90));
        measurement.perc95 = cursor.getInt(cursor.getColumnIndexOrThrow(Entry.COL_PERC_95));
        measurement.perc99 = cursor.getInt(cursor.getColumnIndexOrThrow(Entry.COL_PERC_99));
        return measurement;
    }

    public static class Helper extends SQLiteOpenHelper {
        private static final int DATABASE_VERSION = 1;
        private static final String DATABASE_NAME = "Measure.db";

        public Helper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL(DROP);
            onCreate(db);
        }
    }
}
