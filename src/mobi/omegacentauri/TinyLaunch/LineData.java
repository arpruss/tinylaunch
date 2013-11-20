package mobi.omegacentauri.TinyLaunch;


public class LineData {
	public AppData[] entries;
	public static final int MAX_BUTTONS = 4;
	public static final int IDs[] = { R.id.button1, R.id.button2, R.id.button3, R.id.button4 };
	
	public LineData(int numButtons) {
		entries = new AppData[numButtons];
	}
}
