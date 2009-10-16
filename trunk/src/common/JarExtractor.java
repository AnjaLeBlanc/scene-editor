package common;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public class JarExtractor {
	private Hashtable<String, Integer> m_sizes = new Hashtable<String, Integer>();
	private Hashtable<String, byte[]> m_jarContents = new Hashtable<String, byte[]>();
	private String m_jarFileName;

	public JarExtractor(String jarFileName) {
		this.m_jarFileName = jarFileName;
		initialize();
	}

	public boolean hasImageData(String name) {
		return m_jarContents.containsKey(name);
	}
	
	public byte[] getImageData(String name) {
		return (byte[]) m_jarContents.get(name);
	}

	private void initialize() {
		ZipFile zf = null;
		try {
			// let's get the sizes.
			zf = new ZipFile(m_jarFileName);
			Enumeration<? extends ZipEntry> e = zf.entries();
			while (e.hasMoreElements()) {
				ZipEntry ze = (ZipEntry) e.nextElement();
				m_sizes.put(ze.getName(), new Integer((int) ze.getSize()));
			}
			zf.close();
			zf = null;
			// put resources into m_jarContents
			FileInputStream fis = new FileInputStream(m_jarFileName);
			BufferedInputStream bis = new BufferedInputStream(fis);
			ZipInputStream zis = new ZipInputStream(bis);
			ZipEntry ze = null;
			while ((ze = zis.getNextEntry()) != null) {
				if (ze.isDirectory()) {
					continue;
				}
				int size = (int) ze.getSize();
				if (size == -1) {
					size = ((Integer) m_sizes.get(ze.getName())).intValue();
				}
				byte[] b = new byte[(int) size];
				int rb = 0;
				int chunk = 0;
				while (((int) size - rb) > 0) {
					chunk = zis.read(b, rb, (int) size - rb);
					if (chunk == -1) {
						break;
					}
					rb += chunk;
				}
				m_jarContents.put(ze.getName(), b);
			}
			zis.close();
		} catch (NullPointerException e) {/* handle exception */
		} catch (FileNotFoundException e) {/* handle exception */
		} catch (IOException e) {/* handle exception */
		}
	}
}