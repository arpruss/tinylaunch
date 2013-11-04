package mobi.omegacentauri.TinyLaunch;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Map;

import android.widget.CheckBox;
import java.util.List;

import mobi.omegacentauri.TinyLaunch.R;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

public class GetApps extends AsyncTask<Void, Integer, ArrayList<AppData>> {
	final PackageManager pm;
	final Apps	 context;
	final ListView listView;
	public final static String CACHE_NAME = "apps"; 
	ProgressDialog progress;
	
	GetApps(Apps c, ListView lv) {
		context = c;
		pm = context.getPackageManager();
		listView = lv;
	}

	private boolean profilable(ApplicationInfo a) {
		return true;
	}

	@Override
	protected ArrayList<AppData> doInBackground(Void... c) {
		Log.v("getting", "installed");
		
		ArrayList<AppData> apps = new ArrayList<AppData>();
		
		Intent launchIntent = new Intent(Intent.ACTION_MAIN);
		launchIntent.addCategory(Intent.CATEGORY_LAUNCHER);
		
		MyCache.deleteIcons(context);

		List<ResolveInfo> list = 
			pm.queryIntentActivities(launchIntent, 0);
		
		boolean icons = context.options.getBoolean(Options.PREF_ICONS, false);
		
		for (int i = 0 ; i < list.size() ; i++) {
			publishProgress(i, list.size());
			
			ResolveInfo info = list.get(i);

			ComponentName cn = new ComponentName(info.activityInfo.packageName, 
					info.activityInfo.name);
			String component = cn.flattenToString();
			String name = (String) info.activityInfo.loadLabel(pm);
			if (name.equals("Angry Birds")) {
				if(info.activityInfo.packageName.startsWith("com.rovio.angrybirdsrio")) {
					name = name + " Rio";
				}
				else if (info.activityInfo.packageName.startsWith("com.rovio.angrybirdsseasons")) {
					name = name + " Seasons";
				}
				else if (info.activityInfo.packageName.startsWith("com.rovio.angrybirdsspace")) {
					name = name + " Space";
				}
			}
			
			apps.add(new AppData(component, name));
			
			if (icons) {
				File iconFile = MyCache.getIconFile(context, component);
				
				try {
					Drawable d = pm.getResourcesForActivity(cn)
							.getDrawable(pm.getPackageInfo(
									info.activityInfo.packageName, 
									0).applicationInfo.icon);
					if (d instanceof BitmapDrawable) {
						Bitmap bmp = ((BitmapDrawable)d).getBitmap();
						FileOutputStream out = new FileOutputStream(iconFile);
						bmp.compress(CompressFormat.PNG, 100, out);
						out.close();
					}
				} catch (Exception e) {
					Log.e("TinyLaunch", ""+e);
					iconFile.delete();
				}
			}
			
		}
		
		MyCache.write(context, CACHE_NAME, apps);
		
		publishProgress(list.size(), list.size());

		return apps;
	}
	
	@Override
	protected void onPreExecute() {
//		listView.setVisibility(View.GONE);
		progress = new ProgressDialog(context);
		progress.setCancelable(false);
		progress.setMessage("Getting applications...");
		progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		progress.setIndeterminate(true);
		progress.show();
	}
	
	protected void onProgressUpdate(Integer... p) {
		progress.setIndeterminate(false);
		progress.setMax(p[1]);
		progress.setProgress(p[0]);
	}
	
	@Override
	protected void onPostExecute(ArrayList<AppData> data) {
		
		context.loadList(data);
		context.options.edit().putBoolean(Options.PREF_PREV_ICONS, 
				context.options.getBoolean(Options.PREF_ICONS, false)).commit();
		
		progress.dismiss();
	}
}
