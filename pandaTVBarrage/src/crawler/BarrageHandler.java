package crawler;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
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

	private PrintWriter file;
	private Socket socket;
	Timer timer = new Timer(true);

	private byte[] vestige = new byte[0];
	private int vestigeIndex = 0;
	private int vestigeLength = 0;

	public BarrageHandler(Socket socket, String roomName) throws IOException {
		this.socket = socket;
		createRecordFile(roomName);
		TimerTask task = new FileGenerationTask(roomName);
		timer.schedule(task, 24 * 3600 * 1000, 24 * 3600 * 1000);
	}

	public void handle() throws IOException {
		InputStream inputStream = socket.getInputStream();
		byte[] readBytes = new byte[2048];
		while (true) {
			int len = inputStream.read(readBytes);
			if (len == 0) {
				try {
					Thread.sleep(1000);
					continue;
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			int totalLength = len + vestigeLength;
			byte[] bytes = new byte[totalLength];
			System.arraycopy(vestige, vestigeIndex, bytes, 0, vestigeLength);
			System.arraycopy(readBytes, 0, bytes, vestigeLength, len);

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
					synchronized (file) {
						file.println(comment);
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
			synchronized (file) {
				file.flush();
			}
		}
	}

	private void createRecordFile(String roomName) {
		try {
			file = new PrintWriter(
					"./barrage/" + new SimpleDateFormat(DATE_FORMAT_PATTERN).format(System.currentTimeMillis()) + "_"
							+ roomName + ".txt");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void close() throws IOException {
		timer.cancel();
		socket.close();
		file.close();
	}

	class FileGenerationTask extends TimerTask {
		private String roomName;

		public FileGenerationTask(String roomName) {
			this.roomName = roomName;
		}

		public void run() {
			synchronized (file) {
				file.close();
				createRecordFile(roomName);
			}
		}
	}
}
