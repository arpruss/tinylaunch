package mobi.omegacentauri.TinyLaunch;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Comparator;

public class AppData implements Comparable<AppData> {
	public String component;
	public String name;

//	public int versionCode;
	private static final String COMPONENT = "C";
	private static final String NAME = "N";
//	private static final String VERSION = "V";
	
	public static final Comparator<AppData> NameComparator = 
		new Comparator<AppData>() {

		public int compare(AppData a, AppData b) {
			return a.name.compareToIgnoreCase(b.name);
//			if (a.component.startsWith(" ")) {
//				if (b.component.startsWith(" ")) {
//					return a.name.compareToIgnoreCase(b.name);
//				}
//				else {
//					return -1;
//				}
//			}
//			else if (b.component.startsWith(" ")) {
//				return 1;
//			}
//			else {
//				return a.name.compareToIgnoreCase(b.name);
//			}
		}
	};
	
	public AppData() {			
	}
	
	@Override
	public boolean equals(Object a) {
		if (! (a instanceof AppData))
			return false;
		if (component == null) {
			return a == null || ((AppData)a).component == null;
		}
		return component.equals( ((AppData)a).component );
	}
	
	@Override
	public int hashCode() {
		if (component == null)
			return "NULL null NULL".hashCode();
		else
			return ("AppData:"+component).hashCode();
	}
	
	public AppData(String component, String name) {
		this.component = component;
		this.name = name;
	//	this.versionCode = versionCode;
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
//		String version = reader.readLine();
//		if (version == null || !version.startsWith(VERSION))
//			throw new IOException();
//		try {
//			versionCode = Integer.parseInt(version.substring(1).trim());
//		}
//		catch(NumberFormatException e) {
//			throw new IOException();
//		}
	}
	
	public void write(BufferedWriter writer) throws IOException {
		writer.write(COMPONENT+this.component+"\n"+
					 NAME+this.name+"\n"/*+
					 VERSION+this.versionCode+"\n"*/);
	}

	@Override
	public int compareTo(AppData arg0) {
		return arg0.name.compareToIgnoreCase(this.name);
	}
}
