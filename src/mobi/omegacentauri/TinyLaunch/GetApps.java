package mobi.omegacentauri.TinyLaunch;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.Log;

public class GetApps extends AsyncTask<Boolean, Integer, ArrayList<AppData>> {
	final PackageManager pm;
	final Apps	 context;
	public final static String CACHE_NAME = "apps"; 
	ProgressDialog progress;

	GetApps(Apps c) {
		this.context = c;
		pm = context.getPackageManager();

	}

//	private boolean profilable(ApplicationInfo a) {
//		return true;
//	}

	@Override
	protected ArrayList<AppData> doInBackground(Boolean... slow) {
		//		Log.v("getting", "installed");

		ArrayList<AppData> apps = new ArrayList<AppData>();

		Intent launchIntent = new Intent(Intent.ACTION_MAIN);
		launchIntent.addCategory(Intent.CATEGORY_LAUNCHER);

		boolean icons = context.options.getBoolean(Options.PREF_ICONS, true);
		Map<String,AppData> cache = new HashMap<String,AppData>();

		if (slow[0] || !icons) {
			MyCache.deleteIcons(context);
		}

		if (!slow[0]) {
			ArrayList<AppData> cacheData = new ArrayList<AppData>();
			MyCache.read(context, CACHE_NAME, cacheData);
			//			Log.v("TinyLaunch", "cache "+cacheData.size());
			for (AppData a : cacheData) {
				cache.put(a.component, a);
			}
		}

		List<ResolveInfo> list = 
				pm.queryIntentActivities(launchIntent, 0);

		String component;
		String name;
		boolean cacheValid;
		//int versionCode;

		for (int i = 0 ; i < list.size() ; i++) {
			publishProgress(i, list.size());

			ResolveInfo info = list.get(i);

			ComponentName cn = new ComponentName(info.activityInfo.packageName, 
					info.activityInfo.name);
			component = cn.flattenToString();
			name = null;
			cacheValid = false;
			//versionCode = -1;

			//			try {
			//				versionCode = pm.getPackageInfo(info.activityInfo.packageName, 0).versionCode;
			//			} catch (NameNotFoundException e1) {
			//			}

			if (!slow[0]) {
				AppData a = cache.get(component);
				if (a != null) {
					name = a.name;
					cacheValid = true;
				}
			}

			if (!cacheValid) {
				name = (String) info.activityInfo.loadLabel(pm);
				if (name == null)
					name = component;
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
			}

			apps.add(new AppData(component, name));

			if (icons) {
				File iconFile = MyCache.getIconFile(context, component);

				if (!cacheValid || !iconFile.exists()) {
					//					Log.v("TinyLaunch", "finding icon for "+name);
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
						//						Log.e("TinyLaunch", ""+e);
						iconFile.delete();
					}
				}
			}
		}

		MyCache.write(context, CACHE_NAME, apps);
		MyCache.cleanIcons(context, apps);

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

		context.loadList(data, true);
		context.options.edit().putBoolean(Options.PREF_PREV_ICONS, 
				context.options.getBoolean(Options.PREF_ICONS, true)).commit();
		context.options.edit().putBoolean(Options.PREF_DIRTY,false).commit();

		try {
			progress.dismiss();
		}
		catch (Exception e) {
		}
	}
}
