package crawler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

/**
 * the crawler of a given room
 */
public class Crawler extends Thread {
	String roomName;
	int roomId;

	private String serverIp;
	private int port;
	private String rid;
	private String appid;
	private String k = "1";
	private String t = "300";
	private String ts;
	private String sign;
	private String authType;

	public Crawler(String roomName, int roomId) {
		this.roomName = roomName;
		this.roomId = roomId;
	}

	public Socket connect() throws IOException {
		// phase 1
		JSONObject phase1Response = phase1();
		// phase 2
		phase2(phase1Response);
		return createSocket();
	}

	private JSONObject phase1() throws IOException {
		String url1 = "http://www.panda.tv/ajax_chatroom?roomid=" + roomId + "&_=" + System.currentTimeMillis();
		Document doc1 = Jsoup.connect(url1)
				.header("User-Agent",
						"Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2490.86 Safari/537.36")
				.ignoreContentType(true).get();
		JSONObject jsonObject1 = new JSONObject(doc1.toString().split("<body>", 2)[1].split("</body>", 2)[0]);

		int errorNo = jsonObject1.getInt("errno");
		if (errorNo != 0) {
			throw new IOException("Failed on phase 1.");
		} else {
			return jsonObject1.getJSONObject("data");
		}
	}

	private void phase2(JSONObject json) throws IOException {
		String _sign = json.getString("sign");
		String _roomid = String.valueOf(json.getLong("roomid"));
		String _rid = String.valueOf(json.getLong("rid"));
		String _ts = String.valueOf(json.getLong("ts"));

		String url2 = "http://api.homer.panda.tv/chatroom/getinfo?rid=" + _rid + "&roomid=" + _roomid + "&retry=0&sign="
				+ _sign + "&ts=" + _ts + "&_=" + System.currentTimeMillis();
		Document doc2 = Jsoup.connect(url2)
				.header("User-Agent",
						"Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2490.86 Safari/537.36")
				.ignoreContentType(true).get();
		JSONObject jsonObject2 = new JSONObject(doc2.toString().split("<body>", 2)[1].split("</body>", 2)[0]);

		int errorNo = jsonObject2.getInt("errno");
		if (errorNo != 0) {
			throw new IOException("Failed on phase 2.");
		} else {
			JSONObject j = jsonObject2.getJSONObject("data");
			rid = String.valueOf(j.getLong("rid"));
			appid = j.getString("appid");
			ts = String.valueOf(j.getLong("ts"));
			sign = j.getString("sign");
			authType = j.getString("authType");

			JSONArray chatAddrList = j.getJSONArray("chat_addr_list");
			for (Object o : chatAddrList) {
				serverIp = ((String) o).split(":", 2)[0];
				port = Integer.valueOf(((String) o).split(":", 2)[1]);
				break;
			}
		}

	}

	/**
	 * connect to the barrage server, return the socket.
	 */
	private Socket createSocket() throws IOException {
		Socket socket = new Socket(serverIp, port);
		String msg = "u:" + rid + "@" + appid + "\n" + "k:" + k + "\n" + "t:" + t + "\n" + "ts:" + ts + "\n" + "sign:"
				+ sign + "\n" + "authtype:" + authType;
		ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
		byte[] b = new byte[] { 0x00, 0x06, 0x00, 0x02, 0x00, (byte) msg.length() };
		byteArray.write(b);

		byteArray.write(msg.getBytes("UTF-8"));
		OutputStream outputStream = socket.getOutputStream();
		outputStream.write(byteArray.toByteArray());

		b = new byte[] { 0x00, 0x06, 0x00, 0x00 };
		outputStream.write(b);
		return socket;
	}

	@Override
	public void run() {
		BarrageHandler messageHandler = null;
		Timer hearBeatTimer = null;
		while (true) {
			try {
				Socket socket = connect();
				System.out.println("Connected to " + serverIp + ":" + port + ".");
				hearBeatTimer = heartBeat(socket);
				messageHandler = new BarrageHandler(socket, roomName);
				messageHandler.handle();
			} catch (IOException e) {
				e.printStackTrace();
				continue;
			} finally {
				try {
					if (messageHandler != null) {
						messageHandler.close();
					}
					if (hearBeatTimer != null) {
						hearBeatTimer.cancel();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private Timer heartBeat(Socket socket) {
		Timer timer = new Timer(true);
		TimerTask task = new HeartBeatTask(socket);
		timer.schedule(task, 60000, 60000);
		return timer;
	}
}

class HeartBeatTask extends TimerTask {
	private Socket socket;

	public HeartBeatTask(Socket socket) {
		this.socket = socket;
	}

	public void run() {
		try {
			socket.getOutputStream().write(new byte[] { 0x00, 0x06, 0x00, 0x00 });
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
