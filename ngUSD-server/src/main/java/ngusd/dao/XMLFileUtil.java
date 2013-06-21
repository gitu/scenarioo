package ngusd.dao;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

public class XMLFileUtil {
	
	public static void marshal(final Object object,
			final File destFile, Class<?>... classesToBind) {
		JAXBContext contextObj;
		try {
			
			classesToBind = appendClass(classesToBind, HashMap.class);
			contextObj = JAXBContext.newInstance(classesToBind);
			
			Marshaller marshallerObj = contextObj.createMarshaller();
			marshallerObj.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			marshallerObj.marshal(object, new FileOutputStream(destFile));
		} catch (Exception e) {
			throw new RuntimeException("could not marshall " + destFile.getAbsolutePath(), e);
		}
		
	}
	
	/**
	 * Read the given file from all subdirectories in passed srcDir and
	 * unmarshall these files
	 */
	public static <T> List<T> unmarshallListOfFilesFromSubdirs(final File directory, final String filename,
			final Class<T> targetClass, final Class<?>... classesToBind) {
		List<T> result = new ArrayList<T>();
		if (!directory.exists() || !directory.isDirectory()) {
			throw new ResourceNotFoundException(directory.getAbsolutePath());
		}
		for (File subDir : directory.listFiles()) {
			if (subDir.isDirectory()) {
				File file = new File(subDir, filename);
				if (file.exists()) {
					result.add(unmarshal(file, targetClass, classesToBind));
				}
			}
		}
		return result;
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T unmarshal(final File srcFile, final Class<T> targetClass, Class<?>... classesToBind) {
		JAXBContext contextObj;
		if (!srcFile.exists()) {
			throw new ResourceNotFoundException(srcFile.getAbsolutePath());
		}
		try {
			classesToBind = appendClass(classesToBind, targetClass);
			classesToBind = appendClass(classesToBind, HashMap.class);
			contextObj = JAXBContext.newInstance(classesToBind);
			Unmarshaller unmarshallerObj = contextObj.createUnmarshaller();
			return (T) unmarshallerObj.unmarshal(srcFile);
		} catch (Exception e) {
			throw new RuntimeException("Could not unmarshall " + srcFile.getAbsolutePath(), e);
		}
	}
	
	private static Class<?>[] appendClass(Class<?>[] classesToBind, final Class<?> additionalClass) {
		classesToBind = Arrays.copyOf(classesToBind, classesToBind.length + 1);
		classesToBind[classesToBind.length - 1] = additionalClass;
		return classesToBind;
	}
	
}