package amirz.dynamicstune.settings;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;

public class EditTextPreference extends android.preference.EditTextPreference {
    public EditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public CharSequence getSummary() {
        CharSequence summary = super.getSummary();
        if (TextUtils.isEmpty(summary)) {
            return getText();
        }
        return getText() + " " + summary;
    }
}
