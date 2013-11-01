package mobi.omegacentauri.TinyLaunch;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import android.content.Context;
import android.util.Log;

public class MyCache {
	static public final int MODE_READ = 0;
	static public final int MODE_WRITE = 1;
	
	public static boolean write(Context c, String fname, 
			ArrayList<AppData> data) {
		String path = genFilename(c, fname);
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(
					path+".temp"));
			for (AppData a: data) {
				a.write(writer);
			}
			if (!new File(path+".temp").renameTo(new File(path))) {
				throw new IOException();
			}
		} catch (IOException e) {
			new File(path+".tmp").delete();
			return false;
		}
		return true;		
	}
	
	public static void read(Context c, String fname, ArrayList<AppData> data) {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(genFilename(c,fname)));
			for(;;) {
				AppData a = new AppData();
				a.read(reader);
				data.add(a);
			}
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
		}
	}

	static public String genFilename(Context c, String name) {
		File dir = c.getCacheDir();
		return dir.getPath() + "/" + name + ".MyCache"; 
	}	
}
