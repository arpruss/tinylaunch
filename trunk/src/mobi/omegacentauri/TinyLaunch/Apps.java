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
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
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
	private Spinner spin;
	public static final int ICONS_PER_LINE = 4;

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

	public void loadList(boolean cleanCategory) {
		ArrayList<AppData> data = new ArrayList<AppData>(); 
		MyCache.read(this, GetApps.CACHE_NAME, data);
		loadList(data, cleanCategory);
	}

	private Map<String, AppData> makeMap(ArrayList<AppData> data) {
		Map<String, AppData> map = new HashMap<String, AppData>();

		for (AppData a : data)
			map.put(a.component, a);
		return map;
	}

	public void loadList(ArrayList<AppData> data, boolean cleanCategory) {
		loadList(makeMap(data), cleanCategory);
	}

	public void loadList(Map<String,AppData> map, boolean cleanCategory) {
		this.map = map;

		if (categories == null) {
			categories = new Categories(this, map);			
		}
		else {
			categories.setMap(map);
		}

		if (cleanCategory)
			categories.cleanCategories();

		loadFilteredApps();
		setSpinner();		
	}

	public void setSpinner() {
		spin = (Spinner)findViewById(R.id.category);
		final ArrayList<String> cats = categories.getCategories();
		ArrayAdapter<String> aa = new ArrayAdapter<String>(this, 
				android.R.layout.simple_spinner_item,
				cats);
		//		aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		aa.setDropDownViewResource(R.layout.spinner_item);
		spin.setAdapter(aa);
		String cur = categories.getCurCategory();
		int pos = -1;
		for (int i = 0 ; i < cats.size(); i++ )
			if ( cats.get(i).equals(cur)) {
				pos = i;
				break;
			}

		spin.setSelection(pos);

		spin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int catNum, long arg3) {
				categories.setCurCategory(cats.get(catNum));
				loadFilteredApps();
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});	
	}

	public void loadFilteredApps() {		
		curCatData = categories.filterApps(map);

		if (options.getBoolean(Options.PREF_TILE, false))
			makeTileList();
		else
			makeSimpleList();
	}
	
	private void launch(AppData a) {
		Intent i = new Intent(Intent.ACTION_MAIN);
		i.addCategory(Intent.CATEGORY_LAUNCHER);
		i.setComponent(ComponentName.unflattenFromString(
				a.component));
		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(i);
	}

	private void makeTileList() {
		final ArrayList<LineData> lines = new ArrayList<LineData>();
		for (int i = 0 ; i < curCatData.size(); i += ICONS_PER_LINE) {
			LineData l = new LineData(ICONS_PER_LINE);
			lines.add(l);
			for (int j = 0 ; j < ICONS_PER_LINE ; j++) {
				if (i+j < curCatData.size())
					l.entries[j] = curCatData.get(i+j);
				else
					l.entries[j] = null;
			}
		}
			
			
		ArrayAdapter<LineData> adapter = 
				new ArrayAdapter<LineData>(this, 
						R.layout.iconline, 
						lines) {
			
			@Override
			public boolean isEnabled(int position) {
				return false;
			}

			public View getView(int position, View convertView, ViewGroup parent) {
				View v;				

				if (convertView == null) {
					v = View.inflate(Apps.this, R.layout.iconline, null);
				}
				else {
					v = convertView;
				}
				
				final boolean icons = options.getBoolean(Options.PREF_ICONS, false);
				final LineData l = lines.get(position);
				
				for (int i = 0; i < l.entries.length; i++) {
					View entryView = v.findViewById(LineData.IDs[i]);
					final AppData a = l.entries[i];
					
					if (a == null) {
						entryView.setVisibility(View.INVISIBLE);
					}
					else {
						entryView.setVisibility(View.VISIBLE);
						TextView tv = (TextView)entryView.findViewById(R.id.text);
						tv.setText(a.name);
						ImageView img = (ImageView)entryView.findViewById(R.id.icon);
						if (icons) {
							File iconFile = MyCache.getIconFile(Apps.this, a.component);

							if (iconFile.exists()) {
								try {
									img.setImageDrawable(Drawable.createFromStream(
											new FileInputStream(iconFile), null));
								} catch (Exception e) {
									Log.e("TinyLaunch", ""+e);
									img.setVisibility(View.INVISIBLE);
								}
								img.setVisibility(View.VISIBLE);
							}
							else {
								img.setVisibility(View.INVISIBLE);
							}
						}
						else {
							img.setVisibility(View.GONE);
						}
						View.OnClickListener listener = new View.OnClickListener(){

							@Override
							public void onClick(View arg0) {
								launch(a);
							}

							};
						entryView.setOnClickListener(listener);					
					}
				}
				for (int i = l.entries.length ; i < LineData.MAX_BUTTONS ; i++) {
					v.findViewById(LineData.IDs[i]).setVisibility(View.GONE);
				}
				
				return v;
			}			

		};

		list.setAdapter(adapter);
	}
	
	private void makeSimpleList() {
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
					File iconFile = MyCache.getIconFile(Apps.this, a.component);

					if (iconFile.exists()) {
						try {
							img.setImageDrawable(Drawable.createFromStream(
									new FileInputStream(iconFile), null));
						} catch (Exception e) {
							Log.e("TinyLaunch", ""+e);
							img.setVisibility(View.INVISIBLE);
						}
						img.setVisibility(View.VISIBLE);
					}
					else {
						img.setVisibility(View.INVISIBLE);
					}
				}
				else {
					img.setVisibility(View.GONE);
				}

				// TODO: icon
				return v;
			}			

		};

		list.setAdapter(adapter);

		final ArrayAdapter<AppData> adapterSaved = adapter;

		list.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View v, int position,
					long id) {
				launch(adapterSaved.getItem(position));
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
		builder.setNeutralButton("Uninstall", new OnClickListener(){
			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				Uri uri = Uri.parse("package:"+ComponentName.unflattenFromString(
						item.component).getPackageName());
				startActivity(new Intent(Intent.ACTION_DELETE, uri));
				options.edit().putBoolean(Options.PREF_DIRTY, true).commit();
			}});

		ArrayList<String> editableCategories =  categories.getEditableCategories();
		final int nCategories = editableCategories.size();
		final int position = list.getFirstVisiblePosition();

		if (nCategories > 0) {
			final String[] editableCategoryNames = new String[nCategories];
			editableCategories.toArray(editableCategoryNames);
			final boolean[] checked = new boolean[nCategories];			

			for (int i = 0; i < nCategories ; i++) {
				checked[i] = categories.in(item, editableCategoryNames[i]);
			}

			final boolean[] oldChecked = checked.clone();

			builder.setMultiChoiceItems(editableCategoryNames, checked, 
					new DialogInterface.OnMultiChoiceClickListener() {							
				@Override
				public void onClick(DialogInterface dialog, int which, boolean isChecked) {
					Log.v("TinyLaunch", "setting "+item.name+" to "+isChecked);
					//						if (isChecked) 
					//							categories.addToCategory(customCategoryNames[which], item);
					//						else
					//							categories.removeFromCategory(customCategoryNames[which], item);
				}
			}
					);
			builder.setPositiveButton("OK", new OnClickListener(){
				@Override
				public void onClick(DialogInterface arg0, int arg1) {
					for (int i = 0 ; i < nCategories ; i++) {
						if (checked[i] && ! oldChecked[i])
							categories.addToCategory(editableCategoryNames[i], item);
						else if (!checked[i] && oldChecked[i])
							categories.removeFromCategory(editableCategoryNames[i], item);
					}
					loadFilteredApps();
					list.setSelectionFromTop(position, 0);
				}});
		}
		builder.create().show();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		/* if (keyCode == KeyEvent.KEYCODE_HOME) {
			categories.setCurCategory(Categories.ALL);
			loadFilteredApps();
			setSpinner();
			return true;
		}
		else */ if (keyCode == KeyEvent.KEYCODE_BACK) {
			categories.prevCategory();
			loadFilteredApps();
			setSpinner();
			return true;
		}
		return false;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);


		options = PreferenceManager.getDefaultSharedPreferences(this);
		packageManager = getPackageManager();

		setContentView(R.layout.apps);

		//       getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD);

		list = (ListView)findViewById(R.id.apps);

		res = getResources();

		categories = null;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		//    	case R.id.scan:
		//    		(new GetApps(this, false)).execute();
		//    		return true;
		case R.id.full_scan:
			(new GetApps(this, true)).execute();
			return true;
		case R.id.options:
			startActivity(new Intent(this, Options.class));
			return true;
		case R.id.new_category:
			newCategory();
			return true;
		case R.id.rename_category:
			renameCategory();
			return true;
		case R.id.delete_category:
			categories.removeCategory();
			loadFilteredApps();
			return true;
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

	private void renameCategory() {
		if (! categories.isCustom(categories.getCurCategory()))
			return;

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Rename category");
		builder.setMessage("Edit name of category:");
		final EditText inputBox = new EditText(this);
		inputBox.setText(categories.getCurCategory());
		builder.setView(inputBox);
		builder.setPositiveButton("OK", 
				new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				if (categories.renameCategory(inputBox.getText().toString())) 
					setSpinner();
				else
					Toast.makeText(Apps.this, "Name already in use", Toast.LENGTH_LONG).show();    				
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

		Log.v("TinyLaunch", "onResume");

		loadList(false);
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

		if (needReload || map.size() == 0 || options.getBoolean(Options.PREF_DIRTY, true)) {
			Log.v("TinyLaunch", "scan");
			(new GetApps(this, false)).execute();
		}
	}

}

