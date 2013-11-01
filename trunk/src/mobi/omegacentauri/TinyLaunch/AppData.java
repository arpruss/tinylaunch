package mobi.omegacentauri.TinyLaunch;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Comparator;

import android.util.Log;

public class AppData implements Comparable<AppData> {
	public static final Comparator<AppData> NameComparator = 
		new Comparator<AppData>() {

		public int compare(AppData a, AppData b) {
//			Log.v("DoublePower", a.component+" "+b.component);
			if (a.component.startsWith(" ")) {
				if (b.component.startsWith(" ")) {
					return a.name.compareToIgnoreCase(b.name);
				}
				else {
					return -1;
				}
			}
			else if (b.component.startsWith(" ")) {
				return 1;
			}
			else {
				return a.name.compareToIgnoreCase(b.name);
			}
		}
	};
	
	public String component;
	public String name;
	private static final String COMPONENT = "C";
	private static final String NAME = "N";
	
	public AppData() {			
	}
	
	public AppData(String component, String name) {
		this.component = component;
		this.name = name;
	}
	
	public void read(BufferedReader reader) throws IOException {
		String component = reader.readLine();
		if (component == null || !component.startsWith(COMPONENT))
			throw new IOException();
		this.component = component.substring(1).trim();
		String name = reader.readLine();
		if (name == null || !name.startsWith(NAME))
			throw new IOException();
		this.name = name.substring(1).trim();
	}
	
	public void write(BufferedWriter writer) throws IOException {
		writer.write(COMPONENT+this.component+"\n");
		writer.write(NAME+this.name+"\n");			
	}

	@Override
	public int compareTo(AppData arg0) {
		return arg0.name.compareToIgnoreCase(this.name);
	}
}
