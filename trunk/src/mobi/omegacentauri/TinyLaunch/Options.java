package mobi.omegacentauri.TinyLaunch;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class Options extends PreferenceActivity {
	public final static String PREF_TILE = "tile";
	public final static String PREF_ICONS = "icons";
	public final static String PREF_PREV_ICONS = "prevIcons";
	public static final String PREF_CATEGORY = "category";
	public static final String PREF_DIRTY = "dirty";
	public static final String PREF_PORTRAIT = "portrait";
	public static final String PREF_LIGHT = "light";
	public static final String PREF_PREV_LIGHT = "prevLight";
	
	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		
		addPreferencesFromResource(R.xml.options);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Options.PREF_PORTRAIT, false))
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		else
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
		

	}
	
	@Override
	public void onStop() {
		super.onStop();
	}
}
