package amirz.adaptivestune.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.widget.Toast;

import amirz.adaptivestune.R;
import amirz.adaptivestune.StuneService;
import amirz.adaptivestune.Tunable;
import amirz.adaptivestune.Tweaker;
import amirz.adaptivestune.database.Measure;

import static amirz.adaptivestune.database.Settings.prefs;

public class SettingsActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setTitle(R.string.app_name);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .replace(android.R.id.content, new SettingsFragment())
                    .commit();
        }

        if (!StuneService.sIsRunning) {
            new MissingServiceDialog().show(getFragmentManager(), "missing_service");
        }
    }

    public static class MissingServiceDialog extends DialogFragment
            implements DialogInterface.OnClickListener {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();
            return new AlertDialog.Builder(context)
                    .setTitle(R.string.title_missing_service)
                    .setMessage(R.string.desc_missing_service)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(R.string.go_there, this)
                    .create();
        }

        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        }
    }

    @Override
    public void recreate() {
        finish();
        startActivity(getIntent());
    }

    public static class SettingsFragment extends PreferenceFragment
            implements Preference.OnPreferenceChangeListener {
        private Activity mContext;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mContext = getActivity();

            getPreferenceManager().setSharedPreferencesName(mContext.getPackageName());
            addPreferencesFromResource(R.xml.preferences);

            SwitchPreference inputBoostPref = (SwitchPreference) findPreference(
                    getString(R.string.pref_input_boost_enabled));
            onPreferenceChange(inputBoostPref, inputBoostPref.isChecked());
            inputBoostPref.setOnPreferenceChangeListener(this);

            findPreference(getString(R.string.pref_reset_database))
                    .setOnPreferenceClickListener(new OnResetDatabase());

            findPreference(getString(R.string.pref_reset_prefs))
                    .setOnPreferenceClickListener(new OnResetPreferences());
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            boolean enabled = (boolean) newValue;
            findPreference(getString(R.string.pref_input_boost_ms)).setEnabled(enabled);
            findPreference(getString(R.string.pref_input_boost_freq_little)).setEnabled(enabled);
            findPreference(getString(R.string.pref_input_boost_freq_big)).setEnabled(enabled);
            return true;
        }

        public class OnResetDatabase implements Preference.OnPreferenceClickListener {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                // Clears all measurements from the SQLite database.
                Measure.Helper db = new Measure.Helper(mContext);
                db.recreate(db.getWritableDatabase());
                db.close();

                Toast.makeText(mContext, R.string.on_reset_database, Toast.LENGTH_SHORT).show();
                return true;
            }
        }

        public class OnResetPreferences implements Preference.OnPreferenceClickListener {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                // Clears all previously calculated boosts and changes to settings.
                SharedPreferences prefs = prefs(mContext);
                SharedPreferences.Editor editor = prefs.edit();
                editor.clear();
                editor.apply();

                // Reload tunables and kernel values after the truncation.
                Tunable.applyAll(prefs, getResources());
                Tweaker.applyDefaultParams();

                Toast.makeText(mContext, R.string.on_reset_pref, Toast.LENGTH_SHORT).show();
                mContext.recreate();
                return true;
            }
        }
    }
}
