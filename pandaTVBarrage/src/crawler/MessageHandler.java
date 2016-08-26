package crawler;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.Socket;

public class MessageHandler {

	/**
	 * the beginning of a comment {"type":
	 */
	private static final String COMMENT_HEAD = "{\"type\":";

	private PrintWriter file;
	private Socket socket;
	private InputStream inputStream;

	private byte[] vestige = new byte[0];
	private int vestigeIndex = 0;
	private int vestigeLength = 0;

	public MessageHandler(Socket socket, String roomName) throws IOException {
		this.socket = socket;
		file = new PrintWriter("./barrage/" + roomName + ".txt");
	}

	public void handle() throws IOException {
		if (inputStream == null) {
			inputStream = socket.getInputStream();
		}
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
						commentLength |= ( (bytes[commentStart - j] & 0xFF) << (8 * (j - 1)));
					}
					commentEnd = commentStart + commentLength;
					if (commentEnd >= totalLength) {
						commentEnd = 0;
					}
					String comment = new String(bytes, commentStart, commentLength, "UTF-8");
					file.println(comment);
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
			file.flush();

		}

	}

	public void close() throws IOException {
		socket.close();
		file.close();
	}
}
