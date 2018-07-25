package amirz.dynamicstune.database;

import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;
import android.util.Pair;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import amirz.dynamicstune.Algorithm;

public class MeasureDB {
    private static final String TAG = "MeasureDB";

    private static class Entry implements BaseColumns {
        private static final String TABLE_NAME = "measurement";

        private static final String COL_PACKAGE = "package_name";
        private static final String COL_COMPONENT = "component_name";
        private static final String COL_BOOST = "boost";
        private static final String COL_FRAMES = "frames";
        private static final String COL_JANKS = "janks";
        private static final String COL_PERC_90 = "percentile_90";
        private static final String COL_PERC_95 = "percentile_95";
        private static final String COL_PERC_99 = "percentile_99";
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
            Entry._ID,
            Entry.COL_BOOST,
            Entry.COL_FRAMES,
            Entry.COL_JANKS,
            Entry.COL_PERC_90,
            Entry.COL_PERC_95,
            Entry.COL_PERC_99
    };

    private static final int COMPONENT_MAX_RESULTS = 75;
    private static final int COMPONENT_RESULT_TARGET = 50;

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
        List<Algorithm.Measurement> results = select(Entry.COL_COMPONENT + " = ?",
                new String[] { componentName.flattenToString() });

        if (results.size() >= COMPONENT_MAX_RESULTS) {
            deleteObsoleteResults(componentName);
        }

        return results;
    }

    public List<Algorithm.Measurement> select(String packageName) {
        return select(Entry.COL_PACKAGE + " = ?",
                new String[] { packageName });
    }

    private List<Algorithm.Measurement> select(String where, String[] args) {
        List<Algorithm.Measurement> results = new ArrayList<>();
        if (!mClosed) {
            SQLiteDatabase db = mHelper.getReadableDatabase();

            try (Cursor cursor = db.query(Entry.TABLE_NAME, RESULT_PROJECTION, where, args,
                    null, null, null
            )) {
                while (cursor.moveToNext()) {
                    results.add(readMeasurement(cursor).second);
                }
            }
        }
        return results;
    }

    public void close() {
        mClosed = true;
        mHelper.close();
    }

    private Pair<Long, Algorithm.Measurement> readMeasurement(Cursor cursor) {
        Algorithm.Measurement measurement = new Algorithm.Measurement(
                cursor.getInt(cursor.getColumnIndexOrThrow(Entry.COL_BOOST)));
        measurement.total = cursor.getInt(cursor.getColumnIndexOrThrow(Entry.COL_FRAMES));
        measurement.janky = cursor.getInt(cursor.getColumnIndexOrThrow(Entry.COL_JANKS));
        measurement.perc90 = cursor.getInt(cursor.getColumnIndexOrThrow(Entry.COL_PERC_90));
        measurement.perc95 = cursor.getInt(cursor.getColumnIndexOrThrow(Entry.COL_PERC_95));
        measurement.perc99 = cursor.getInt(cursor.getColumnIndexOrThrow(Entry.COL_PERC_99));
        return new Pair<>(cursor.getLong(cursor.getColumnIndexOrThrow(Entry._ID)), measurement);
    }

    private void deleteObsoleteResults(ComponentName componentName) {
        SQLiteDatabase db = mHelper.getWritableDatabase();

        Set<Long> removeIds = new HashSet<>();

        int i = 0;
        try (Cursor cursor = db.query(Entry.TABLE_NAME, RESULT_PROJECTION,
                Entry.COL_COMPONENT + " = ?", new String[] { componentName.flattenToString() },
                null, null, Entry._ID + " DESC"
        )) {
            while (cursor.moveToNext()) {
                if (i++ >= COMPONENT_RESULT_TARGET) {
                    removeIds.add(readMeasurement(cursor).first);
                }
            }
        }

        for (long removeId : removeIds) {
            db.delete(Entry.TABLE_NAME, Entry._ID + " = ?",
                    new String[] { String.valueOf(removeId) });
        }

        if (removeIds.size() > 0) {
            Log.e(TAG, "Cleared " + removeIds.size() +
                    " results for " + componentName.flattenToString());
        }
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
