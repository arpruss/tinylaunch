package mobi.omegacentauri.TinyLaunch;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;

public class Categories {
	Context context;
	public static final String ALL = "All";
	public static final String UNCLASSIFIED = "Unclassified";
	private String curCategory;
	private ArrayList<String> names;
	private Map<String,ArrayList<AppData>> categories;
	private Map<String,AppData> map;
	
	public Categories(Context context, Map<String,AppData> map, String startingCategory) {
		this.context = context;
		this.map = map;
		names = new ArrayList<String>();
		names.add(ALL);
		names.add(UNCLASSIFIED);
		categories = new HashMap<String,ArrayList<AppData>>();
		for (File f : context.getFilesDir().listFiles()) {
			String n = f.getName();
			if (n.endsWith(".cat")) {
				String name = n.substring(0, n.length()-4);
				if (name.equals(ALL) || name.equals(UNCLASSIFIED))
					continue;
				names.add(name);
				categories.put(name, getEntries(f));
			}
		}
		
		sortNames();
		setCategory(startingCategory);
	}
	
	public void setCategory(String category) {
		curCategory = category;
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

	private File catPath(String category) {
		return new File(context.getFilesDir()+"/"+category+".cat");
	}

	public ArrayList<String> getCategories() {
		return names;
	}
	
	public void removeFromCategory(String cat, AppData a) {
		if (cat == ALL || cat == UNCLASSIFIED)
			return;
		ArrayList<AppData> data = categories.get(cat);
		if (data != null) {
			data.remove(a);				
			putEntries(catPath(cat), data);
		}
	}
	
	public void addToCategory(String cat, AppData a) {
		if (cat == ALL || cat == UNCLASSIFIED)
			return;
		ArrayList<AppData> data = categories.get(cat);
		if (data != null) {
			data.add(a);				
			putEntries(catPath(cat), data);
		}
	}
	
	public void removeCategory() {
		if (curCategory == ALL || curCategory == UNCLASSIFIED)
			return;
		catPath(curCategory).delete();
		categories.remove(curCategory);
		names.remove(curCategory);
		setCategory(ALL);
	}
	
	public void cleanCategories() {
		for (String c: names) {
			if (c == ALL || c == UNCLASSIFIED)
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
	
	public void addCategory(String c) {
		if (names.contains(c)) 
			return;
		categories.put(c, new ArrayList<AppData>());
		names.add(c);
		sortNames();
		try {
			catPath(c).createNewFile();
		} catch (IOException e) {
		}
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
		
		if (curCategory == ALL || curCategory == UNCLASSIFIED) {
			data.addAll(map.values());
			if (curCategory == UNCLASSIFIED) {
				for (ArrayList<AppData> c : categories.values())
					data.removeAll(c);
			}
		}
		else {
			ArrayList<AppData> c = categories.get(curCategory);
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
				else if (lhs.equals(UNCLASSIFIED)) {
					if (rhs.equals(UNCLASSIFIED)) 
						return 0;
					else
						return 1;
				}
				return lhs.compareToIgnoreCase(rhs);
			}});
	}
}
