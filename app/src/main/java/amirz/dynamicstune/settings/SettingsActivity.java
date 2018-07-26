package amirz.dynamicstune.settings;

import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceFragment;

import amirz.dynamicstune.R;

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
    }

    public static class SettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            getPreferenceManager().setSharedPreferencesName(getActivity().getPackageName());
            addPreferencesFromResource(R.xml.preferences);
        }
    }
}
