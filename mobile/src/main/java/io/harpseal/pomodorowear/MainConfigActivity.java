package io.harpseal.pomodorowear;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.util.Log;

public class MainConfigActivity extends Activity {

    //ListPreference mCalendarListPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new PrefsFragement()).commit();

    }

    public static class PrefsFragement extends PreferenceFragment{
        @Override
        public void onCreate(Bundle savedInstanceState) {
            // TODO Auto-generated method stub
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);

            PreferenceScreen screen = this.getPreferenceScreen(); // "null". See onViewCreated.

            // Create the Preferences Manually - so that the key can be set programatically.
            PreferenceCategory category = new PreferenceCategory(screen.getContext());
            category.setTitle("Channel Configuration");
            screen.addPreference(category);

            CheckBoxPreference checkBoxPref = new CheckBoxPreference(screen.getContext());
            checkBoxPref.setKey("channelConfig.getName()" + "_ENABLED");
            checkBoxPref.setTitle("channelConfig.getShortname()" + "Enabled");
            checkBoxPref.setSummary("channelConfig.getDescription()");
            checkBoxPref.setChecked(true);




//            PreferenceScreen prefScreen = new PreferenceScreen(screen.getContext());
//            prefScreen.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
//                @Override
//                public boolean onPreferenceClick(Preference preference) {
//                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
//                    builder.setTitle("Create a new Tag");
//
//// Set up the input
//                    final android.widget.EditText input = new android.widget.EditText(MainActivity.this);
//// Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
//                    input.setInputType(InputType.TYPE_CLASS_TEXT);// | InputType.TYPE_TEXT_VARIATION_PASSWORD
//                    builder.setView(input);
//
//// Set up the buttons
//                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialog, int which) {
//                            //m_Text = input.getText().toString();
//
//                        }
//                    });
//                    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialog, int which) {
//                            dialog.cancel();
//                        }
//                    });
//
//                    builder.show();
//                    return false;
//                }
//            });

            category.addPreference(checkBoxPref);

            EditTextPreference editTextPref = new EditTextPreference(screen.getContext());
            editTextPref.setKey("edittext_preference");
            editTextPref.setTitle("editTextPref title");
            editTextPref.setSummary("editTextPref summary");
            editTextPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(PrefsFragement.this.getPreferenceScreen().getContext());
                    builder.setTitle("Create a new Tag");

// Set up the input
                    final android.widget.EditText input = new android.widget.EditText(PrefsFragement.this.getPreferenceScreen().getContext());
// Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
                    input.setInputType(InputType.TYPE_CLASS_TEXT);// | InputType.TYPE_TEXT_VARIATION_PASSWORD
                    builder.setView(input);

// Set up the buttons
                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //m_Text = input.getText().toString();

                        }
                    });
                    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });

                    builder.show();
                    return false;
                }
            });

            category.addPreference(editTextPref);

            Preference pref = new Preference(screen.getContext());
            pref.setKey("pref_test");
            pref.setTitle("title");
            pref.setSummary("summary");
            pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    new AlertDialog.Builder(PrefsFragement.this.getPreferenceScreen().getContext())
                            .setMessage("pref_test")
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    //m_Text = input.getText().toString();

                                }
                            })
                            .show();
                    return true;
                }
            });
            category.addPreference(pref);
        }
    }
}
