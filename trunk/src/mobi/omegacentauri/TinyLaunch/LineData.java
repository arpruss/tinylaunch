package mobi.omegacentauri.TinyLaunch;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Comparator;

import android.util.Log;

public class LineData {
	public AppData[] entries;
	public static final int MAX_BUTTONS = 4;
	public static final int IDs[] = { R.id.button1, R.id.button2, R.id.button3, R.id.button4 };
	
	public LineData(int numButtons) {
		entries = new AppData[numButtons];
	}
}
