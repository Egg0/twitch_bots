// A bot for Smash viewer battles. Lets users join a resizable queue.
// User commands: !add [NNID], !queue, !info, !nnid
// Mod commands: !get, !clear, !close, !open, !update [queueLength]
package com.cavariux.twitchirc.Core;
import java.util.ArrayList;
import java.util.HashSet;

import com.cavariux.twitchirc.Chat.Channel;
import com.cavariux.twitchirc.Chat.User;
import com.cavariux.twitchirc.Core.TwitchBot;

import javafx.util.Pair;

public class SmashBot extends TwitchBot {
	private ArrayList<Pair<String, String>> queue;  // The queue of users, paired with NNIDS
	private HashSet<String> userSet; // Users currently in queue
	private HashSet<String> mods; // Moderators allowed to use special commands
	private int max;
	private boolean closed;
	
	public SmashBot(int queueLength) {
		this.setUsername("YolkRobo");
		this.setOauth_Key("oauth:0n9aimtqjy072ivbxhuc8p16yaiczl");
		this.queue = new ArrayList<Pair<String, String>>(queueLength);
		this.userSet = new HashSet<String>();
		this.max = queueLength;
		this.closed = false;
		mods = new HashSet<String>();
		initialize(mods);
	}
		
	@Override
	public void onCommand(User user, Channel channel, String command) {
		command = command.toLowerCase();
		
		// Help commands - work regardless of queue open/closed
		if (command.startsWith("help") || command.startsWith("info")) {
			help(user, channel, command);
		} else if (command.equals("nnid")) {
			this.sendMessage("Eggdozer's NNID: Cheep-Cheep", channel);
		// Join only if is not closed and not me
		} else if (command.startsWith("join")) {
			if (closed && !user.toString().equals("eggdozer")) {
				this.sendMessage("The queue is closed right now.", channel);
				return;
			}
			handleJoin(user, channel, command);
		// Queue info, and user position
		} else if (command.equals("queue")) {
			handleQueue(user, channel, command);
		// My commands
		} else if (mods.contains(user.toString())) {
			if (command.equals("get")) {
				handleGet(user, channel, command);
			} else if (command.equals("clear")) {
				queue.clear();
				userSet.clear();
				this.sendMessage("Queue cleared.", channel);
			} else if (command.equals("close")) {
				this.sendMessage("The queue is closed!", channel);
				closed = true;
			} else if (command.equals("open")) {
				this.sendMessage("The queue is open. !join [NNID] to enter!", channel);
				closed = false;
			} else if (command.startsWith("update")) {
				handleUpdate(channel, command);
			}
		}
	}

	private void handleUpdate(Channel channel, String command) {
		if (command.equals("update")) {
			this.sendMessage("Usage: !update [maxQueueLength]", channel);
			return;
		}
		String[] parts = command.split(" ");
		max = Integer.parseInt(parts[1]);
		this.sendMessage("Updated max queue length to " + max, channel);
	}

	// Let a user join the queue if not full or already queued
	private void handleJoin(User user, Channel channel, String message) {
		// Usage info
		if (message.equals("join")) {
			this.sendMessage("Usage: !join [NNID]", channel);
		} else if (queue.size() >= max) {
			this.sendMessage("The queue is full! Wait until a set is completed.", channel);
		} else if (userSet.contains(user.toString())) {
			this.sendMessage("You are already in the queue!", channel);
		} else {
			// Try to extract a NNID
			String[] arr = message.split(" ");
			if (arr.length != 2) {
				this.sendMessage("Usage: !join [NNID]", channel);
				return;
			}
			// Add to queue and userset
			String nnid = arr[1];
			queue.add(new Pair<String, String>(user.toString(), nnid));
			userSet.add(user.toString());
			this.sendMessage("Added " + user + " | " + nnid + " at position " + queue.size() + "!"
					+ " The queue can hold " + (max - queue.size()) + " more players.", channel);
		}
	}

	
	// Gets the front level from the list. Also removes from userset
	private void handleGet(User user, Channel channel, String command) {
		if (queue.size() == 0) {
			this.sendMessage("The queue is empty. BibleThump", channel);
			return;
		}
		Pair<String, String> pair = queue.remove(0);
		userSet.remove(pair.getKey());
		this.sendMessage("@" + pair.getKey() + " - you're up! | NNID: " + pair.getValue(), channel);
	}

	
	// Prints info about the user's position in the queue if present
	private void handleQueue(User user, Channel channel, String message) {
		int size = queue.size();
		if (userSet.contains(user.toString())) {
			for (int i = 0; i < queue.size(); i++) {
				Pair<String, String> pair = queue.get(i);
				if (pair.getKey().equals(user.toString())) {
					this.sendMessage("You are at position " + (i + 1) + " in the queue out of " + size + " total player(s)."
										+ " The queue can hold up to " + max + ".", channel);
					return;
				}
			}
			this.sendMessage("Error: This shouldn't be reached", channel);
		} else { // Not in the set
			if (size < max) {
				this.sendMessage("The queue currently has " + queue.size() + "/" + max + " players. Use !join to enter.", channel);
			} else {
				this.sendMessage("The queue currently has the maximum of " + max + " players. Use !join once a match finishes.", channel);
			}
			
		}
	}
	
	// Print a list of commands
	private void help(User user, Channel channel, String message) {
		this.sendMessage("!join [NNID] (join the queue), !queue (get your status in the queue), !nnid (see my NNID)", channel);
	}
	
	// Fills the empty "mod" set with all the names of valid mods.
	private void initialize(HashSet<String> mods) {
		mods.add("eggdozer");
		//mods.add("jungo3");
	}
}
