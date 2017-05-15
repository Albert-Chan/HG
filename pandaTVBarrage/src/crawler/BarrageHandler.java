package crawler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Timer;
import java.util.TimerTask;

public class BarrageHandler {

	/**
	 * the beginning of a comment {"type":
	 */
	private static final String COMMENT_HEAD = "{\"type\":";
	private static final String DATE_FORMAT_PATTERN = "yyyy-MM-dd";

	private BufferedWriter writer;
	private Socket socket;
	Timer timer = new Timer(true);

	private byte[] vestige = new byte[0];
	private int vestigeIndex = 0;
	private int vestigeLength = 0;

	public BarrageHandler(Socket socket, String roomName) throws IOException {
		this.socket = socket;
		createRecordFile(roomName);
		TimerTask task = new FileGenerationTask(roomName);
		timer.schedule(task,
				new Time(System.currentTimeMillis()).roundToDayLeftEdge() + 24 * 3600 * 1000 - System.currentTimeMillis(),
				24 * 3600 * 1000);
	}

	public void handle() throws IOException {
		InputStream inputStream = socket.getInputStream();
		byte[] readBytes = new byte[2048];
		int times = 0;
		while (true) {
			int len = inputStream.read(readBytes);
			if (len == 0) {
				try {
					System.err.println("Sleeping ...");
					Thread.sleep(1000);
					continue;
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
			}
			System.err.println("read times " + ++times);
			int totalLength = len + vestigeLength;
			byte[] bytes = new byte[totalLength];
			
			try {
				System.arraycopy(vestige, vestigeIndex, bytes, 0, vestigeLength);
				System.arraycopy(readBytes, 0, bytes, vestigeLength, len);
			} catch (ArrayIndexOutOfBoundsException e) {
				System.err.println( "vestige.length: " + vestige.length + "vestigeIndex: " + vestigeIndex + "vestigeLength: " + vestigeLength);
			}
			
			int commentStart = -1, commentEnd = -1;

			for (int i = 0; i < totalLength; i++) {
				if (i + COMMENT_HEAD.length() < totalLength
						&& COMMENT_HEAD.equals(new String(bytes, i, COMMENT_HEAD.length(), "UTF-8"))) {
					commentStart = i;
					if (commentStart < 4) {
						// should not happen
						break;
					}
					// get the comment length
					int commentLength = 0;
					for (int j = 1; j <= 4; j++) {
						commentLength |= ((bytes[commentStart - j] & 0xFF) << (8 * (j - 1)));
					}

					if (commentStart + commentLength >= totalLength) {
						if (commentEnd == -1) {
							commentEnd = 0;
						}
						break;
					}
					commentEnd = commentStart + commentLength;
					String comment = new String(bytes, commentStart, commentLength, "UTF-8");
					synchronized (writer) {
						writer.write(comment);
						writer.newLine();
					}
					System.out.println(comment);
					// omit the COMMENT_HEAD to speed up
					i += COMMENT_HEAD.length();
				}
			}

			if (commentEnd != -1) {
				vestige = bytes;
				vestigeIndex = commentEnd;
				vestigeLength = totalLength - vestigeIndex;
			}
			synchronized (writer) {
				writer.flush();
			}
		}
	}

	private void createRecordFile(String roomName) throws IOException, UnsupportedEncodingException {
		File barrageDir = new File("./barrage/");
		if (!barrageDir.exists()) {
			barrageDir.mkdirs();
		}
		File file = new File(barrageDir,
				new SimpleDateFormat(DATE_FORMAT_PATTERN).format(System.currentTimeMillis()) + "_" + roomName + ".txt");
		file.createNewFile();
		writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, true), "UTF-8"));
	}

	public void close() throws IOException {
		timer.cancel();
		socket.close();
		writer.close();
	}

	class FileGenerationTask extends TimerTask {
		private String roomName;

		public FileGenerationTask(String roomName) {
			this.roomName = roomName;
		}

		public void run() {
			synchronized (writer) {
				try {
					writer.close();
					createRecordFile(roomName);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
