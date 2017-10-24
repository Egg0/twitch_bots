package com.cavariux.twitchirc.Core;
import java.io.IOException;
import com.cavariux.twitchirc.Chat.Channel;


public class Main {
	static final int LEVEL_MAX = 8;
	static final int PER_USER = 1;
	static final String GREETING = "MarioMakerBot connected. Max levels set to " + LEVEL_MAX +
									" and max per viewer is " + PER_USER + ".";
	
	public static void main(String[] args) {
		runMMBot(LEVEL_MAX, PER_USER);
		//runSmashBot(5);
		
	}
	
	public static void runMMBot(int level_max, int per_user) {
		MMBot bot = new MMBot(level_max, per_user);
		bot.connect();
		Channel channel = bot.joinChannel("eggdozer");
		bot.sendMessage(GREETING, channel);
		bot.start();
	}
	
	public static void runSmashBot(int queue_max) {
		SmashBot bot = new SmashBot(queue_max);
		bot.connect();
		Channel channel = bot.joinChannel("eggdozer");
		bot.sendMessage("SmashBot connected. Queue length set to " + queue_max + ".", channel);
		bot.start();
	}
}
