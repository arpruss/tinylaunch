package mobi.omegacentauri.TinyLaunch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class DirtinessReceiver extends BroadcastReceiver {
	
	@Override
	public void onReceive(Context context, Intent intent) {
		SharedPreferences options = PreferenceManager.getDefaultSharedPreferences(context);
		options.edit().putBoolean(Options.PREF_DIRTY, true).commit();
		//Log.v("TinyLaunch", "marked as dirty");
	}
}
