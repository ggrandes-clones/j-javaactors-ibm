package com.ibm.actor.test;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.ibm.actor.ActorManager;
import com.ibm.actor.DefaultActorManager;
import com.ibm.actor.DefaultMessage;
import com.ibm.actor.Message;

/**
 * An actor that does a simplified "Virus" (actually files withmultiple text lines) 
 * scan of a directory tree.
 * 
 * @author BFEIGENB
 *
 */
public class VirusScanActor extends TestableActor {

	@Override
	protected void runBody() {
		// logger.trace("TestActor:%s runBody: %s", getName(), this);
		DefaultActorTest.sleeper(1);
		DefaultMessage m = new DefaultMessage("init", null);
		getManager().send(m, null, this);
	}

	@Override
	protected void loopBody(Message m) {
		try {
			// logger.trace("TestActor:%s loopBody %s: %s", getName(), m, this);
			DefaultActorTest.sleeper(1);
			String subject = m.getSubject();
			DefaultActorManager manager = (DefaultActorManager) getManager();
			if ("scanDir".equals(subject)) {
				String dir = (String) m.getData();
				File[] list = new File(dir).listFiles();
				logger.trace("Scanning directory %s...", dir);
				if (manager.getCategorySize(getCategory()) < 50) {
					createVirusScanActor(manager);
				}
				for (File file : list) {
					if (file.isDirectory()) {
						DefaultMessage dm = new DefaultMessage("scanDir", file.getCanonicalPath());
						manager.send(dm, this, this.getClass().getSimpleName());
					} else if (file.isFile() && file.getName().toLowerCase().endsWith(".txt")) {
						DefaultMessage dm = new DefaultMessage("scanFile", file.getCanonicalPath());
						manager.send(dm, this, getCategoryName());
					}
				}
			} else if ("scanFile".equals(subject)) {
				String file = (String) m.getData();
				File xfile = new File(file);
				String xpath = xfile.getCanonicalPath();
				logger.trace("Scaning file %s...", xpath);
				byte[] ba = readBinaryFileContents(xfile);
				// TODO: check contents here is real app
				DefaultActorTest.sleeper(1);
				//logger.trace("**** Size %s:  %d", xpath, ba.length);
				int count = 0;
				for(byte b: ba) {
					if(b == (byte)'\n') {
						count ++;
					}
				}
				if (count > 10) {
					DefaultMessage dm = new DefaultMessage("virusFound", new Object[] {xpath, "more than 10 returns found"});
					manager.send(dm, this, getCategoryName());
				} 
				ba = null;
			} else if ("virusFound".equals(subject)) {
				Object[] params = (Object[]) m.getData();
				String file = params.length > 0 ? (String) params[0] : null;
				String message = params.length > 1 ? (String) params[1] : null;
				if (file != null) {
					File xfile = new File(file);
					String xpath = xfile.getCanonicalPath();
					logger.trace("**** suspect file; %s: %s", message, xpath);
				} 
			} else if ("init".equals(subject)) {
				String startPath = (String) m.getData();
				if (startPath != null) {
					DefaultMessage dm = new DefaultMessage("scanDir", startPath);
					manager.send(dm, this, getCategoryName());
				}
			} else {
				logger.warning("VirusScanActor:%s loopBody unknown subject: %s", getName(), subject);
			}
		} catch (IOException e) {
			logger.error("VirusScanActor exception", e);
		}
	}

	/** Read a file. */
	public static byte[] readBinaryFileContents(String fileName)
			throws IOException {
		return readBinaryFileContents(new File((fileName)));
	}

	/** Read a file. */
	public static byte[] readBinaryFileContents(File file) throws IOException {
		BufferedInputStream bis = new BufferedInputStream(new FileInputStream(
				file));
		try {
			return readBinaryStreamContents(bis);
		} finally {
			bis.close();
		}
	}

	/** Read a stream. */
	public static byte[] readBinaryStreamContents(InputStream is)
			throws IOException {
		return streamToByteArray(is);
	}

	/** Get the bytes from an input stream. */
	public static byte[] streamToByteArray(InputStream is) throws IOException {
		if (!(is instanceof BufferedInputStream)) {
			is = new BufferedInputStream(is);
		}
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] buffer = new byte[4 * 1024];
		for (int count = is.read(buffer); count >= 0; count = is.read(buffer)) {
			baos.write(buffer, 0, count);
		}
		return baos.toByteArray();
	}

	public static VirusScanActor createVirusScanActor(ActorManager manager) {
		VirusScanActor res = (VirusScanActor) manager.createAndStartActor(VirusScanActor.class, getActorName());
		res.setCategory(getCategoryName());
		return res;
	}

	public static String getCategoryName() {
		return VirusScanActor.class.getSimpleName();
	}

	protected static int actorCount;

	protected static String getActorName() {
		return getCategoryName() + '_' + actorCount++;
	}

}