package mobi.omegacentauri.TinyLaunch;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mobi.omegacentauri.TinyLaunch.R;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Toast;

public class Apps extends Activity {
	Categories categories;
	ArrayList<AppData> curCatData;
	ArrayAdapter<AppData> adapter;
	ListView list;
	Resources res;
	Map<String,AppData> map;
	public SharedPreferences options;
	final static String PREF_APPS = "apps";
	private PackageManager packageManager;
	
	private void message(String title, String msg) {
		AlertDialog alertDialog = new AlertDialog.Builder(this).create();

		alertDialog.setTitle(title);
		alertDialog.setMessage(msg);
		alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, 
				getResources().getText(R.string.ok), 
				new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {} });
		alertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
			public void onCancel(DialogInterface dialog) {} });
		alertDialog.show();

	}

	public void loadList() {
		ArrayList<AppData> data = new ArrayList<AppData>(); 
		MyCache.read(this, GetApps.CACHE_NAME, data);
		loadList(data);
	}
	
	private Map<String, AppData> makeMap(ArrayList<AppData> data) {
		Map<String, AppData> map = new HashMap<String, AppData>();
		
		for (AppData a : data)
			map.put(a.component, a);
		return map;
	}

	public void loadList(ArrayList<AppData> data) {
		loadList(makeMap(data));
	}

	public void loadList(Map<String,AppData> map) {
		this.map = map;
			
		if (categories == null) {
			categories = new Categories(this, map, Categories.ALL);			
		}
		
		loadFilteredApps();
	}
	
	public void loadFilteredApps() {		
		curCatData = categories.filterApps(map);

		ArrayAdapter<AppData> adapter = 
			new ArrayAdapter<AppData>(this, 
					R.layout.onelinenocheck, 
					curCatData) {

			public View getView(int position, View convertView, ViewGroup parent) {
				View v;				
				
				if (convertView == null) {
	                v = View.inflate(Apps.this, R.layout.onelinenocheck, null);
	            }
				else {
					v = convertView;
				}

				final AppData a = curCatData.get(position); 
				final boolean icons = options.getBoolean(Options.PREF_ICONS, false);
				
				TextView tv = (TextView)v.findViewById(R.id.text);
				tv.setText(a.name);
				ImageView img = (ImageView)v.findViewById(R.id.icon);
				if (icons) {
					img.setVisibility(View.VISIBLE);
					File iconFile = MyCache.getIconFile(Apps.this, a.component);
					
					if (iconFile.exists()) {
						try {
							img.setImageDrawable(Drawable.createFromStream(
									new FileInputStream(iconFile), null));
						} catch (Exception e) {
							Log.e("TinyLaunch", ""+e);
						}
					}
				}
				else {
					img.setVisibility(View.GONE);
				}
				
				// TODO: icon
				return v;
			}			
			
		};
		
//		adapter.sort(AppData.NameComparator);
		list.setAdapter(adapter);
		
		final ArrayAdapter<AppData> adapterSaved = adapter;
		
        list.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View v, int position,
					long id) {
				Intent i = new Intent(Intent.ACTION_MAIN);
				i.addCategory(Intent.CATEGORY_LAUNCHER);
				i.setComponent(ComponentName.unflattenFromString(
						adapterSaved.getItem(position).component));
				i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(i);
			}        	
        });		
        
        list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener(){
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View v,
					int position, long id) {
				itemEdit(adapterSaved.getItem(position));
				return false;
			}        	
        });
	}
	
	protected void itemEdit(final AppData item) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		
		builder.setTitle(item.name);
		builder.setPositiveButton("OK", new OnClickListener(){
			@Override
			public void onClick(DialogInterface arg0, int arg1) {
			}});
		builder.setNeutralButton("Uninstall", new OnClickListener(){
			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				Uri uri = Uri.parse("package:"+ComponentName.unflattenFromString(
						item.component).getPackageName());
				startActivity(new Intent(Intent.ACTION_DELETE, uri));
			}});
		
		ArrayList<String> customCategories =  categories.getCustomCategories();
		final int nCategories = customCategories.size();

		if (nCategories > 0) {
			final String[] customCategoryNames = new String[nCategories];
			customCategories.toArray(customCategoryNames);
			boolean[] checked = new boolean[nCategories];			
			
			for (int i = 0; i < nCategories ; i++) {
				checked[i] = categories.in(item.component, customCategoryNames[i]);
			}
			builder.setMultiChoiceItems(customCategoryNames, checked, 
				new DialogInterface.OnMultiChoiceClickListener() {							
					@Override
					public void onClick(DialogInterface dialog, int which, boolean isChecked) {
						Log.v("TinyLaunch", "setting "+item.name+" to "+isChecked);
						if (isChecked) 
							categories.addToCategory(customCategoryNames[which], item);
						else
							categories.removeFromCategory(customCategoryNames[which], item);
					}
			}
			);
		}
		builder.create().show();
	}

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    	options = PreferenceManager.getDefaultSharedPreferences(this);
    	packageManager = getPackageManager();
    	
        setContentView(R.layout.apps);
        
        list = (ListView)findViewById(R.id.apps);
        
        res = getResources();
        
        categories = null;
	}
	
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch(item.getItemId()) {
    	case R.id.scan:
    		(new GetApps(this, list)).execute();
    		return true;
    	case R.id.options:
    		startActivity(new Intent(this, Options.class));
    		return true;
    	case R.id.new_category:
    		newCategory();
    	default:
    		return false;
    	}
    }

    private void newCategory() {
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setTitle("New category");
    	builder.setMessage("Enter name of category:");
    	final EditText inputBox = new EditText(this);
    	builder.setView(inputBox);
    	builder.setPositiveButton("OK", 
    				new DialogInterface.OnClickListener() {
    			public void onClick(DialogInterface dialog, int which) {
    				if (!categories.addCategory(inputBox.getText().toString())) {
    					Toast.makeText(Apps.this, "Name already in use", Toast.LENGTH_LONG).show();
    				}
    			} });
    	builder.show();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
	    return true;
	}

	@Override
    public void onResume() {
    	super.onResume();

    	loadList();
    	boolean needReload = false;
    	
    	if (map.size() == 0) {
    		needReload = true;
    	}
    	else {
    		boolean icons = options.getBoolean(Options.PREF_ICONS, false);
    		if (icons != options.getBoolean(Options.PREF_PREV_ICONS, false)) {
    			if (icons)
    				needReload = true;
    			else {
    				MyCache.deleteIcons(this);
    				options.edit().putBoolean(Options.PREF_PREV_ICONS, icons).commit();
    			}
    		}
    	}
    	if (needReload || map.size() == 0)     	
    		(new GetApps(this, list)).execute();
    }

}

