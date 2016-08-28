package crawler;

import java.io.IOException;
import java.util.HashMap;

public class CrawlerManager {
	public static HashMap<String, Integer> NAME_TO_ROOMID = new HashMap<>();
	static {
		NAME_TO_ROOMID.put("上帝视角", 485118);
		NAME_TO_ROOMID.put("娱乐区", 485120);
		NAME_TO_ROOMID.put("客厅", 476247);
		NAME_TO_ROOMID.put("厨房", 476249);
		NAME_TO_ROOMID.put("隐形黑衣人", 476253);

		NAME_TO_ROOMID.put("谭盐盐", 472245);
		NAME_TO_ROOMID.put("李林蔚", 462053);
		NAME_TO_ROOMID.put("曹婉瑾", 473073);
		NAME_TO_ROOMID.put("陈姝君", 472691);
		NAME_TO_ROOMID.put("小小蕾 ", 353622);
		NAME_TO_ROOMID.put("李元一", 473695);
		NAME_TO_ROOMID.put("陈海沛", 471687);
		NAME_TO_ROOMID.put("贝依霖", 472909);
		NAME_TO_ROOMID.put("蒋雪菲", 472021);
		NAME_TO_ROOMID.put("万穗", 472792);
	};

	public static void main(String[] args) throws IOException {
		for (String roomName : NAME_TO_ROOMID.keySet()) {
			Crawler c = new Crawler(roomName, NAME_TO_ROOMID.get(roomName));
			c.start();
		}
	}
}
