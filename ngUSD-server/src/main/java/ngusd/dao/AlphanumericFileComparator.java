package ngusd.dao;

import java.io.File;
import java.text.Collator;
import java.util.Comparator;

public class AlphanumericFileComparator implements Comparator<File> {
	
	Collator collator = Collator.getInstance();
	
	@Override
	public int compare(final File f1, final File f2) {
		return collator.compare(f1.getName(), f2.getName());
	}
	
}
