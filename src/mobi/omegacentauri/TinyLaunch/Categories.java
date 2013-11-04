package mobi.omegacentauri.TinyLaunch;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.client.utils.URLEncodedUtils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class Categories {
	Context context;
	public static final String ALL = "All";
	public static final String UNCLASSIFIED = "Unclassified";
	public static final String HIDDEN = "Hidden";
	private String curCategory;
	private ArrayList<String> names;
	private Map<String,ArrayList<AppData>> categories;
	private Map<String,AppData> map;
	private SharedPreferences options;
	
	@SuppressWarnings("deprecation")
	public Categories(Context context, Map<String,AppData> map) {
		this.context = context;
    	options = PreferenceManager.getDefaultSharedPreferences(context);
		this.map = map;
		names = new ArrayList<String>();
		names.add(ALL);
		names.add(HIDDEN);
		names.add(UNCLASSIFIED);
		categories = new HashMap<String,ArrayList<AppData>>();
		for (File f : context.getFilesDir().listFiles()) {
			String n = f.getName();
			if (n.endsWith(".cat")) {
				String name = n.substring(0, n.length()-4);
				name = URLDecoder.decode(name);
				if (isEditable(name))
					categories.put(name, getEntries(f));
				if (isCustom(name))
					names.add(name);
			}
		}
		
		for (String n : names) {
			if (null == categories.get(n))
				categories.put(n, new ArrayList<AppData>());
		}
		
		sortNames();
		curCategory = options.getString(Options.PREF_CATEGORY, ALL);
	}
	
	public void setCurCategory(String category) {
		curCategory = category;
		options.edit().putString(Options.PREF_CATEGORY, category).commit();
	}
	
	public String getCategory() {
		return curCategory;
	}
	
	private ArrayList<AppData> getEntries(File f) {
		ArrayList<AppData> data = new ArrayList<AppData>();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(f));
			
			String d;
			
			while (null != (d=reader.readLine())) {
				d = d.trim();
				if (d.length()>0) {
					AppData a = map.get(d);
					if (a != null)
						data.add(a);
				}
			}
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
		}
		
		return data;
	}

	@SuppressWarnings("deprecation")
	private File catPath(String category) {
		return new File(context.getFilesDir()+"/"+URLEncoder.encode(category)+".cat");
	}

	public ArrayList<String> getCategories() {
		return names;
	}
	
	public void removeFromCategory(String cat, AppData a) {
		if (!isEditable(cat))
			return;
		ArrayList<AppData> data = categories.get(cat);
		if (data != null) {
			data.remove(a);				
			putEntries(catPath(cat), data);
		}
	}
	
	public void addToCategory(String cat, AppData a) {
		if (!isEditable(cat))
			return;
		
		ArrayList<AppData> data = categories.get(cat);
		if (data != null) {
			Log.d("TinyLaunch", "adding "+a.name);
			data.add(a);	
			putEntries(catPath(cat), data);
		}
	}
	
	public void removeCategory() {
		if (!isCustom(curCategory))
			return;
		catPath(curCategory).delete();
		categories.remove(curCategory);
		names.remove(curCategory);
		setCurCategory(ALL);
	}
	
	public void cleanCategories() {
		for (String c: names) {
			if (!isEditable(c))
				continue;
			
			ArrayList<AppData> data = categories.get(c);
			
			if (data == null) 
				continue; // should not happen
			
			boolean dirty = false;
			
			for (AppData a : data) {
				if (!map.containsValue(a)) {
					data.remove(a);
					dirty = true;
				}
			}
			
			if (dirty) 
				putEntries(catPath(c), data);
		}
	}
	
	public boolean addCategory(String c) {
		Log.v("TinyLaunch", "adding "+c);
		if (names.contains(c)) {
			Log.v("TinyLaunch", "already used "+c);
			return false;
		}
		try {
			catPath(c).createNewFile();
		} catch (IOException e) {
			return false;
		}
		categories.put(c, new ArrayList<AppData>());
		names.add(c);
		sortNames();
		return true;
	}
	
	private void putEntries(File file, ArrayList<AppData> data) {
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(file));

			for (AppData a : data) 
				writer.write(a.component + "\n");
		} catch (IOException e) {
		}

		if (writer != null) {
			try {
				writer.close();
			} catch (IOException e) {
			}		
		}
	}

	public ArrayList<AppData> filterApps(Map<String,AppData> map) {
		ArrayList<AppData> data = new ArrayList<AppData>();
		
		if (!isEditable(curCategory)) {
			data.addAll(map.values());
			if (curCategory == UNCLASSIFIED) {
				for (ArrayList<AppData> c : categories.values())
					data.removeAll(c);
			}
			else {
				ArrayList<AppData> c = categories.get(HIDDEN);
				if (c != null)
					data.removeAll(c);
			}
		}
		else {
			ArrayList<AppData> c = categories.get(curCategory);
			//Log.v("TinyLaunch", "filtering via "+curCategory+" "+c.size());
			if (c != null) {
				data.addAll(c);
			}
		}
		
		Collections.sort(data, AppData.NameComparator);
		
		return data;		
	}
	
	public void sortNames() {
		Collections.sort(names, new Comparator<String>(){
			@Override
			public int compare(String lhs, String rhs) {
				if (lhs.equals(ALL)) {
					if (rhs.equals(ALL))
						return 0;
					else
						return -1;
				}
				else if (lhs.equals(HIDDEN)) {
					if (rhs.equals(HIDDEN)) 
						return 0;
					else
						return 1;
				}
				else if (lhs.equals(UNCLASSIFIED)) {
					if (rhs.equals(UNCLASSIFIED)) 
						return 0;
					else if (rhs.equals(HIDDEN))
						return -1;
					else
						return 1;
				}
				else if (rhs.equals(ALL)) {
					return 1;
				}
				else if (rhs.equals(HIDDEN)) {
					return -1;
				}
				else if (rhs.equals(UNCLASSIFIED)) {
					return -1;
				}
				return lhs.compareToIgnoreCase(rhs);
			}});
	}

	public ArrayList<String> getEditableCategories() {
		ArrayList<String> customNames = new ArrayList<String>();
		for (String s : names)
			if (isEditable(s))
				customNames.add(s);
		return customNames;
	}
	
	public boolean isCustom(String c) {
		return ! c.equals(ALL) && ! c.equals(UNCLASSIFIED) && ! c.equals(HIDDEN);
	}

	public boolean isEditable(String c) {
		return ! c.equals(ALL) && ! c.equals(UNCLASSIFIED);
	}

	public boolean in(AppData item, String cat) {
		return categories.get(cat).contains(item);
	}

	public String getCurCategory() {
		// TODO Auto-generated method stub
		return curCategory;
	}

	public boolean renameCategory(String c) {
		if (c.equals(curCategory))
			return true;
		
		if (names.contains(c)) {
			Log.v("TinyLaunch", "already used "+c);
			return false;
		}
		
		if (!catPath(curCategory).renameTo(catPath(c)))
			return false;

		ArrayList<AppData> d = categories.get(curCategory);
		categories.remove(curCategory);
		categories.put(c, d);
		names.remove(curCategory);
		names.add(c);
		sortNames();
		setCurCategory(c);
		return true;
	}
}
