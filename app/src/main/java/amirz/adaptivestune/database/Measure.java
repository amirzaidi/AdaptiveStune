package amirz.adaptivestune.database;

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
import java.util.List;

import amirz.adaptivestune.learning.Algorithm;

import static amirz.adaptivestune.settings.Tunable.*;

// ToDo: Improve structure for testing.
public class Measure {
    private static final String TAG = "Measure";

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

    private final Helper mHelper;
    private boolean mClosed;

    public Measure(Helper helper) {
        mHelper = helper;
    }

    /**
     * Inserts a new measurement for the component into the database.
     * @param componentName The component for which this measurement was taken.
     * @param measurement The measured frame time statistics.
     * @return The row ID if insertion went successfully, or -1 if an error occurred.
     */
    public long insert(ComponentName componentName, Algorithm.Measurement measurement) {
        if (mClosed) {
            return -1;
        }

        SQLiteDatabase db = mHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(Entry.COL_PACKAGE, componentName.getPackageName());
        values.put(Entry.COL_COMPONENT, componentName.flattenToString());
        values.put(Entry.COL_BOOST, measurement.boost);
        values.put(Entry.COL_FRAMES, measurement.total);
        values.put(Entry.COL_JANKS, measurement.janky);
        values.put(Entry.COL_PERC_90, measurement.perc90);
        values.put(Entry.COL_PERC_95, measurement.perc95);
        values.put(Entry.COL_PERC_99, measurement.perc99);

        return db.insert(Entry.TABLE_NAME, null, values);
    }

    /**
     * Retrieves all measurements for the specified component.
     * Automatically removes old results when the MAX_RESULTS cap has been reached.
     * @param componentName The component for which measurements are queried.
     * @return A list of all its measurements, including potentially removed ones,
     * or null if the connection was closed.
     */
    public List<Algorithm.Measurement> select(ComponentName componentName) {
        if (mClosed) {
            return null;
        }

        SQLiteDatabase db = mHelper.getWritableDatabase();

        List<Long> ids = new ArrayList<>();
        List<Algorithm.Measurement> results = select(db, Entry.COL_COMPONENT + " = ?",
                new String[] { componentName.flattenToString() }, ids);

        if (results.size() >= DECAY_TRIGGER.get()) {
            int clearCount = ids.size() - DECAY_KEEP.get();
            for (int i = 0; i < clearCount; i++) {
                db.delete(Entry.TABLE_NAME, Entry._ID + " = ?",
                        new String[] { String.valueOf(ids.get(i)) });
            }
            Log.e(TAG, "Cleared " + clearCount + " results for " +
                    componentName.flattenToString());
        }

        return results;
    }

    private List<Algorithm.Measurement> select(SQLiteDatabase db, String where, String[] args, List<Long> idsOut) {
        List<Algorithm.Measurement> results = new ArrayList<>();
        try (Cursor cursor = db.query(Entry.TABLE_NAME, RESULT_PROJECTION, where, args,
                null, null, Entry._ID + " ASC"
        )) {
            while (cursor.moveToNext()) {
                Pair<Long, Algorithm.Measurement> pair = readMeasurement(cursor);
                if (idsOut != null) {
                    idsOut.add(pair.first);
                }
                results.add(pair.second);
            }
        }
        return results;
    }

    /**
     * Closes the connection to the database and prevents any new operations from being executed.
     */
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

    public static class Helper extends SQLiteOpenHelper {
        private static final int DATABASE_VERSION = 1;
        private static final String DATABASE_NAME = "Measure.db";

        public Helper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            recreate(db);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            recreate(db);
        }

        public void recreate(SQLiteDatabase db) {
            db.execSQL(DROP);
            db.execSQL(CREATE);
        }
    }
}
