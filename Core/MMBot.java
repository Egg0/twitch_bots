// Mario Maker Bot. Features adding, removing, and a resizable queue.
// User commands: !add [Level-ID], !removefront, !removeback, !queue, !info, !check, !current
// Mod commands: !get, !clear, !close, !open, !update [maxLevels] [maxPerUser]

package com.cavariux.twitchirc.Core;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.cavariux.twitchirc.Chat.Channel;
import com.cavariux.twitchirc.Chat.User;
import com.cavariux.twitchirc.Core.TwitchBot;

import javafx.util.Pair;

public class MMBot extends TwitchBot {
	private ArrayList<Pair<String, String>> ids;
	private HashMap<String, Integer
	> userLevelCount;
	private int maxLevels, maxLevelsPerUser;
	private Pair<String, String> current;
	private boolean closed;
	private HashSet<String> mods; // moderators who can use special commands
	
	public MMBot(int length, int levels) {
		this.setUsername("YolkRobo");
		this.setOauth_Key("oauth:0n9aimtqjy072ivbxhuc8p16yaiczl");
		this.ids = new ArrayList<Pair<String, String>>(length);
		this.userLevelCount = new HashMap<String, Integer>();
		this.maxLevels = length;
		this.maxLevelsPerUser = levels;
		this.current = new Pair<String, String>(null, null);
		this.closed = false;
		mods = new HashSet<String>();
		initialize(mods);
	}
		
	@Override
	public void onCommand(User user, Channel channel, String command) {
		command = command.toLowerCase();
		// Help commands - work regardless of queue open/closed
		// Queue info
		if (command.equals("queue")) {
			handleQueue(channel);
		// Help
		} else if (command.startsWith("help") || command.startsWith("info")) {
			help(user, channel, command);
		} else if (command.startsWith("check")) {
			handleCheck(user, channel, command);
		} else if (command.startsWith("current")) {
			if (current.getKey() != null) {
				this.sendMessage(current.getKey() + "'s level: https://supermariomakerbookmark.nintendo.net/courses/" + current.getValue(), channel);
			} else {
				this.sendMessage("There is no level being played currently.", channel);
			}
		// Removing a level
		} else if (command.startsWith("remove")) {
			handleRemove(user, channel, command);
		// Adding a level: Check first if closed (though I can override it)
		} else if (command.startsWith("add")) {
			if (closed && !user.toString().equals("eggdozer")) {
				this.sendMessage("The queue is closed right now.", channel);
				return;
			}
			handleAdd(user, channel, command);
		// My commands
		} else if (mods.contains(user.toString())) {
			if (command.equals("get")) {
				handleGet(user, channel, command);
			} else if (command.equals("clear")) {
				ids.clear();
				userLevelCount.clear();
				current = new Pair<String, String>(null, null);
				handleQueue(channel);
			} else if (command.equals("close")) {
				this.sendMessage("The queue is closed! No more levels can be added.", channel);
				closed = true;
			} else if (command.equals("open")) {
				this.sendMessage("The queue is open again. !add to submit levels!", channel);
				closed = false;
			} else if (command.startsWith("update")) {
				handleUpdate(channel, command);
			}
		}
	}

	private void handleUpdate(Channel channel, String command) {
		if (command.equals("update")) {
			this.sendMessage("Usage: !update [maxLevels] [maxPerUser]", channel);
			return;
		}
		String[] parts = command.split(" ");
		maxLevels = Integer.parseInt(parts[1]);
		maxLevelsPerUser = Integer.parseInt(parts[2]);
		handleQueue(channel);
	}

	// Try to add a level
	private void handleAdd(User user, Channel channel, String message) {
		// Usage info
		if (message.equals("add")) {
			this.sendMessage("Usage: !add [level-code] (No brackets)", channel);
		} else if (ids.size() >= maxLevels) {
			this.sendMessage("The queue is full! Wait until one is completed.", channel);
		} else if (checkLevels(user, channel, message)) {
			// Try to extract a level code
			String[] arr = message.split(" ");
			if (arr.length != 2) {
				this.sendMessage("Usage: !add [level-code] (No brackets)", channel);
				return;
			}
			// There is a string in there, pattern matching for validity
			String code = arr[1].toUpperCase();
			Pattern p = Pattern.compile("(([A-Z]|[0-9]){4}-){3}([A-Z]|[0-9]){4}");
			Matcher m = p.matcher(code);
			if (!m.matches()) {
				this.sendMessage("Invalid level code. Be sure to include dashes.", channel);
			} else {
				ids.add(new Pair<String, String>(user.toString(), code));
				if (!userLevelCount.containsKey(user.toString())) {
					userLevelCount.put(user.toString(), 1);
				} else {  // Add the code, making it 1 if not there or prev + 1 if there.
					userLevelCount.put(user.toString(), userLevelCount.get(user.toString()) + 1);
				}
				this.sendMessage("Added " + code + " to the queue at position " + ids.size() + 
							". You have " + userLevelCount.get(user.toString()) + "/" + maxLevelsPerUser + " levels queued." , channel);
			}
		}
	}
	
	// Returns true if the user's levels is less than the max allowed, false otherwise
	private boolean checkLevels(User user, Channel channel, String message) {
		if (!userLevelCount.containsKey(user.toString())) {
			return true;
		} else {
			int count = userLevelCount.get(user.toString());
			if (count >= maxLevelsPerUser) {
				this.sendMessage("You already have " + maxLevelsPerUser + " levels in queue."
						+ " !remove one or wait until your other levels are played.", channel);
				return false;
			} else {
				return true;
			}
		}
	}
	
	// Gets the front level from the list. Also updates userLevelCount appropriately,
	// Removing the entry if it would be reduced to 0.
	private void handleGet(User user, Channel channel, String command) {
		if (ids.size() == 0) {
			this.sendMessage("The queue is empty. BibleThump", channel);
			current = new Pair<String, String>(null, null);
			return;
		}
		Pair<String, String> pair = ids.remove(0);
		current = pair;
		this.sendMessage(pair.getKey() + "'s level: https://supermariomakerbookmark.nintendo.net/courses/" + pair.getValue(), channel);
		int newCount = userLevelCount.get(pair.getKey()) - 1;
		// Add back only if still > 0
		if (newCount > 0) {
			userLevelCount.put(pair.getKey(), newCount);
		} else {
			userLevelCount.remove(pair.getKey());
		}
	}

	// Checks if there is a level to remove, and remove it if so. 
	// Can remove from the front or back of the queue - see options
	private void handleRemove(User user, Channel channel, String message) {
		// Print usage
		if (!message.equals("removefront") && !message.equals("removeback")) {
			this.sendMessage("Usage: !removefront or !removeback to remove your front-most or back-most level from the queue.", channel);
			return;
		} 
		if (!userLevelCount.containsKey(user.toString())) {
			this.sendMessage("You don't have any levels to remove.", channel);
		} else if (message.equals("removefront")){
			for (int i = 0; i < ids.size(); i++) {
				Pair<String, String> pair = ids.get(i);
				if (pair.getKey().equals(user.toString())) {
					ids.remove(i);
					this.sendMessage("Removed " + pair.getValue() + " from the queue.", channel);
					int newCount = userLevelCount.get(pair.getKey()) - 1;
					// Add back only if still > 0
					if (newCount > 0) {
						userLevelCount.put(pair.getKey(), newCount);
					} else {
						userLevelCount.remove(pair.getKey());
					}
					return;
				}
			}
			this.sendMessage("Unexpected error", channel);	
		} else { //remove back
			for (int i = ids.size() - 1; i >= 0; i--) {
				Pair<String, String> pair = ids.get(i);
				if (pair.getKey().equals(user.toString())) {
					ids.remove(i);
					this.sendMessage("Removed " + pair.getValue() + " from the queue.", channel);
					int newCount = userLevelCount.get(pair.getKey()) - 1;
					// Add back only if still > 0
					if (newCount > 0) {
						userLevelCount.put(pair.getKey(), newCount);
					} else {
						userLevelCount.remove(pair.getKey());
					}
					return;
				}
			}
			this.sendMessage("Unexpected error", channel);
		}
	}
	
	// Prints all the queue slots of the user's levels.
	private void handleCheck(User user, Channel channel, String message) {
		if (!userLevelCount.containsKey(user.toString())) {
			this.sendMessage("You have 0 queued levels. Add one with !add.", channel);
		} else {
			int count = userLevelCount.get(user.toString());
			int[] positions = new int[count];
			int curIndex = 0;
			for (int i = 0; i < ids.size(); i++) {
				Pair<String, String> pair = ids.get(i);
				if (pair.getKey().equals(user.toString())) {
					positions[curIndex] = i + 1;
					curIndex++;
				}
			}
			this.sendMessage("You currently have " + count + " level(s) out of " + maxLevelsPerUser + " max queued at positions: " + 
							 Arrays.toString(positions), channel);	
		}
	}
	
	private void handleQueue(Channel channel) {
		int size = ids.size();
		if (size < maxLevels) {
			this.sendMessage("There are " + size + "/" + maxLevels + " levels in queue, with " + maxLevelsPerUser + " max per user."
					+ " Type !check to see the position of your levels in the queue!", channel);
		} else {
			this.sendMessage("There are " + size + "/" + maxLevels + " levels in queue. "
					+ "Wait until one is completed before adding more.", channel);
		}
	}
	
	// Print a list of commands
	private void help(User user, Channel channel, String message) {
		this.sendMessage("!add (add a level to the queue) -- !remove (remove one of your levels - type for more info) "
				+ "-- !queue (queue info) -- !check (your queued levels) -- !current (info about current level)", channel);
	}
	
	// Fills the empty "mod" set with all the names of valid mods.
	private void initialize(HashSet<String> mods) {
		mods.add("eggdozer");
		//mods.add("jungo3");
	}
}
