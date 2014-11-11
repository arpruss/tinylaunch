 	package mobi.omegacentauri.TinyLaunch;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
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
	//private PackageManager packageManager;
	private Spinner spin;
	public int iconsPerLine = 4;
	public static final int MIN_BUTTONS = 4;
	public static final int MAX_BUTTONS = 4;
	public int[] IDs = {R.id.button1, R.id.button2, R.id.button3, R.id.button4, R.id.button5, R.id.button6};
	public int[] spacerIDs = {0, 0, 0, 0, R.id.spacer5, R.id.spacer6};
	GetApps scanner = null;
	private OnSharedPreferenceChangeListener prefListener;
	private boolean light;
	private boolean homePressed;
	private boolean childMode;

	//	private void message(String title, String msg) {
	//		AlertDialog alertDialog = new AlertDialog.Builder(this).create();
	//
	//		alertDialog.setTitle(title);
	//		alertDialog.setMessage(msg);
	//		alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, 
	//				"OK", 
	//				new DialogInterface.OnClickListener() {
	//			public void onClick(DialogInterface dialog, int which) {} });
	//		alertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
	//			public void onCancel(DialogInterface dialog) {} });
	//		alertDialog.show();
	//
	//	}

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
		final ArrayList<String> cats = new ArrayList<String>();
		cats.addAll(categories.getCategories());
		if (childMode)
			cats.remove(Categories.HIDDEN);
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
				//Log.v("TinyLaunch", "selected from spinner");
				String newCat = cats.get(catNum);
				if (!newCat.equals(categories.getCurCategory())) {
					categories.setCurCategory(newCat);
					loadFilteredApps();
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});	
	}

	public void loadFilteredApps() {		
		//Log.v("TinyLaunch", "filtering");
		curCatData = categories.filterApps(map);
		//Log.v("TinyLaunch", "filtered");

		if (options.getBoolean(Options.PREF_TILE, true))
			makeTileList();
		else
			makeSimpleList();
		
//		list.setFastScrollEnabled(true);
		//list.setFastScrollAlwaysVisible(true);
		//Log.v("TinyLaunch", "made list");
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
		list.setAdapter(null);

		final boolean icons = options.getBoolean(Options.PREF_ICONS, true);

		final View.OnClickListener onClickListener = new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				if (arg0.getTag() instanceof AppData)
					launch((AppData)arg0.getTag());
			}

		};

		final View.OnLongClickListener onLongClickListener = new View.OnLongClickListener() {

			@Override
			public boolean onLongClick(View arg0) {
				if (arg0.getTag() instanceof AppData)
					itemEdit((AppData)arg0.getTag());
				return false;
			}

		};
		
		final boolean large = options.getBoolean(Options.PREF_LARGE, false);
		final int iconSize = (int)((large ? 64 : 48) * getResources().getDisplayMetrics().density + 0.5);

		BaseAdapter adapter = new BaseAdapter() {

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

				for (int i = 0; i < iconsPerLine ; i++) {
					View entryView = v.findViewById(IDs[i]);
					int entryPos = position * iconsPerLine + i;
					AppData a;
					if (entryPos < curCatData.size())
						a = curCatData.get(entryPos);
					else
						a= null;

					entryView.setTag(a);
					
					TextView tv = (TextView)entryView.findViewById(R.id.text);
					ImageView img = (ImageView)entryView.findViewById(R.id.icon);

					if (a == null) {
						tv.setText("");
						img.setImageResource(android.R.color.transparent);
						entryView.setVisibility(View.INVISIBLE);
						entryView.setOnClickListener(null);
						entryView.setOnLongClickListener(null);
					}
					else {
						entryView.setVisibility(View.VISIBLE);
						tv.setText(a.name);
						if (light)
							tv.setTextColor(Color.BLACK);
						if (icons) {
							if (large) {
								ViewGroup.LayoutParams p = img.getLayoutParams();
								p.width = iconSize;
								p.height = iconSize;
								img.setLayoutParams(p);
							}					

							setIcon(img, a);
						}
						else {
							img.setVisibility(View.GONE);
						}
						entryView.setOnClickListener(onClickListener);					
						entryView.setOnLongClickListener(onLongClickListener);
					}
				}

				for (int i = iconsPerLine; i < IDs.length ; i++) {
					View entryView = v.findViewById(IDs[i]);
					entryView.setVisibility(View.GONE);
					entryView.setOnClickListener(null);
					entryView.setOnLongClickListener(null);
					entryView.setTag(null);
					if (spacerIDs[i] != 0)
						v.findViewById(spacerIDs[i]).setVisibility(View.GONE);
				}

				return v;
			}

			@Override
			public int getCount() {
				return (curCatData.size() + iconsPerLine - 1)/iconsPerLine;
			}

			@Override
			public Object getItem(int position) {
				return null;
			}

			@Override
			public long getItemId(int position) {
				return 0;
			}			

		};

		list.setAdapter(adapter);
		list.setOnItemClickListener(null);
		list.setOnItemLongClickListener(null);
		if (light)
			list.setBackgroundColor(Color.WHITE);
		list.setDivider(null);
	}

	private void makeSimpleList() {
		list.setAdapter(null);

		final boolean icons = options.getBoolean(Options.PREF_ICONS, true);
		final boolean large = options.getBoolean(Options.PREF_LARGE, false);
		final int iconSize = (int)((large ? 64 : 48) * getResources().getDisplayMetrics().density + 0.5);

		ArrayAdapter<AppData> adapter = 
				new ArrayAdapter<AppData>(this, 
						R.layout.oneline, 
						curCatData) {

			public View getView(int position, View convertView, ViewGroup parent) {
				View v;				

				if (convertView == null) {
					v = View.inflate(Apps.this, R.layout.oneline, null);
				}
				else {
					v = convertView;
				}

				AppData a = curCatData.get(position); 

				TextView tv = (TextView)v.findViewById(R.id.text);
				tv.setText(a.name);
				if (light)
					tv.setTextColor(Color.BLACK);
				ImageView img = (ImageView)v.findViewById(R.id.icon);

				if (icons) {
					if (large) {
						ViewGroup.LayoutParams p = img.getLayoutParams();
						p.width = iconSize;
						p.height = iconSize;
						img.setLayoutParams(p);
					}					

					setIcon(img,a);
				}
				else {
					img.setVisibility(View.GONE);
				}

				return v;
			}			

		};

		list.setAdapter(adapter);
		if (light)
			list.setBackgroundColor(Color.WHITE);
		list.setDivider(getResources().getDrawable(
				light ? android.R.drawable.divider_horizontal_dark : android.R.drawable.divider_horizontal_bright));

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


	void setIcon(ImageView img, AppData a) {
		File iconFile = MyCache.getIconFile(Apps.this, a.component);
//		if (true /*options.getBoolean(Options.PREF_BIG_ICONS, false)*/) {
//			img.setMinimumHeight(96);
//			img.setMinimumHeight(96);
//		}

		if (iconFile.exists()) {
			try {
				img.setImageDrawable(Drawable.createFromStream(
						new FileInputStream(iconFile), null));
			} catch (Exception e) {
				//				Log.e("TinyLaunch", ""+e);
				img.setImageResource(android.R.drawable.sym_def_app_icon);
			}
		}
		else {
			img.setImageResource(android.R.drawable.sym_def_app_icon);
		}

		img.setVisibility(View.VISIBLE);
	}


	protected void itemEdit(final AppData item) {
		if (childMode)
			return;
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		builder.setTitle(item.name);
		builder.setNeutralButton("Uninstall", new OnClickListener(){
			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				Uri uri = Uri.parse("package:"+ComponentName.unflattenFromString(
						item.component).getPackageName());
				startActivity(new Intent(Intent.ACTION_DELETE, uri));
			//	options.edit().putBoolean(Options.PREF_DIRTY, false).commit();
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
					//					Log.v("TinyLaunch", "setting "+item.name+" to "+isChecked);
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
	
//	@Override
//	public boolean dispatchKeyEvent(KeyEvent event) {
//		Log.v("TinyLaunch", "key "+event);
//		if (event.getKeyCode() == KeyEvent.KEYCODE_HOME &&
//				event.getAction() == KeyEvent.ACTION_UP &&
//				event.getDownTime() < 500) {
//			categories.setCurCategory(Categories.ALL);
//			categories.clearHistory();
//			loadFilteredApps();
//			setSpinner();
//			return true;			
//		}
//		return super.dispatchKeyEvent(event);
//	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		homePressed = true;
		super.onNewIntent(intent);
	}
	
	void menu() {
		if (childMode)
			menuIfNotChild();
		else
			openOptionsMenu();
	}
	
	public void onMenuButton(View v) {
		menu();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_MENU) {
			menu();
			return true;
		}
		else if (keyCode == KeyEvent.KEYCODE_BACK) {
			categories.prevCategory();
			loadFilteredApps();
			setSpinner();
			return true;
		}
		return false;
	}
	
	@Override
	public void onStart() {
		super.onStart();
		homePressed = false;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		options.unregisterOnSharedPreferenceChangeListener(prefListener);
		prefListener = null;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		options = PreferenceManager.getDefaultSharedPreferences(this);
		
		prefListener = new OnSharedPreferenceChangeListener() {			
			@Override
			public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
					String key) {
				//Log.v("TinyLaunch", "pref change detected");
				if (key.equals(Options.PREF_DIRTY) && sharedPreferences.getBoolean(Options.PREF_DIRTY, false)) {
					if (scanner == null || scanner.getStatus() != AsyncTask.Status.RUNNING) {
						scanner = new GetApps(Apps.this);
						scanner.execute(false);
					}
				}
			}
		};
		
		options.registerOnSharedPreferenceChangeListener(prefListener);

		
		if (options.getBoolean(Options.PREF_LIGHT, false))
			setTheme(android.R.style.Theme_Light);

		setContentView(R.layout.apps);

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
			if (scanner != null && scanner.getStatus() == AsyncTask.Status.RUNNING)
				return true;
			scanner = new GetApps(this);
			scanner.execute(true);
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
		inputBox.setInputType(InputType.TYPE_CLASS_TEXT | 
				InputType.TYPE_TEXT_FLAG_CAP_WORDS);
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

	@SuppressLint("DefaultLocale")
	private void menuIfNotChild() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Child lock");
		builder.setMessage("Please type in: \"not toddler\".");
		final EditText inputBox = new EditText(this);
		inputBox.setInputType(InputType.TYPE_CLASS_TEXT);
		builder.setView(inputBox);
		builder.setPositiveButton("OK", 
				new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				if (inputBox.getText().toString().toLowerCase().equals("not toddler"))
					openOptionsMenu();
			} });
		builder.setCancelable(true);
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

		//Log.v("TinyLaunch", "onResume");
		
		childMode = options.getBoolean(Options.PREF_CHILD_MODE, false);
		iconsPerLine = Integer.parseInt(options.getString(Options.PREF_COLUMNS, "4"));
		
	    light = options.getBoolean(Options.PREF_LIGHT, false);
		if (options.getBoolean(Options.PREF_PREV_LIGHT, false) !=
			light) {
			options.edit().putBoolean(Options.PREF_PREV_LIGHT, light).commit();
			Intent i = getIntent();
			finish();
			startActivity(i);
		}
		if (options.getBoolean(Options.PREF_PORTRAIT, false))
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		else
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
		
		loadList(false);
		boolean needReload = false;
		
		if (homePressed) {
			homePressed = false;
		
			if (categories != null) {
				if (categories.haveCategory(Categories.HOME) && 
						! categories.getCurCategory().equals(Categories.HOME)) {
					categories.setCurCategory(Categories.HOME);
				}
				else {
					categories.setCurCategory(Categories.ALL);
					categories.clearHistory();
				}
				loadFilteredApps();
				setSpinner();
			}
		}

		if (map.size() == 0) {
			needReload = true;
		}
		else {
			boolean icons = options.getBoolean(Options.PREF_ICONS, true);
			if (icons != options.getBoolean(Options.PREF_PREV_ICONS, true)) {
				if (icons)
					needReload = true;
				else {
					MyCache.deleteIcons(this);
					options.edit().putBoolean(Options.PREF_PREV_ICONS, icons).commit();
				}
			}
		}

		if (needReload || map.size() == 0 || options.getBoolean(Options.PREF_DIRTY, false)) {
			//			Log.v("TinyLaunch", "scan");
			if (scanner == null || scanner.getStatus() != Status.RUNNING) {
				scanner = new GetApps(this);
				scanner.execute(false);
			}
		}
	}

}

