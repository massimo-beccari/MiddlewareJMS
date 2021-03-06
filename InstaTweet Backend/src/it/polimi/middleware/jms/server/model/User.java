package it.polimi.middleware.jms.server.model;

import java.util.ArrayList;
import java.util.Iterator;

public class User {
	private int userId;
	private String username;
	private String password;
	private ArrayList<Integer> followedUsers;
	private long lastMessageReadTimestamp;
	private boolean isLogged;
	
	public User(int userId, String username, String password) {
		this.userId = userId;
		this.username = username;
		this.password = password;
		followedUsers = new ArrayList<Integer>();
		lastMessageReadTimestamp = 0;
		isLogged = true;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public int getUserId() {
		return userId;
	}

	public String getUsername() {
		return username;
	}
	
	public synchronized ArrayList<Integer> getFollowedUsers() {
		 ArrayList<Integer> copy = new ArrayList<Integer>();
		 copy.addAll(followedUsers);
		 return copy;
	}
	
	public synchronized void addFollowed(int userId) {
		followedUsers.add(userId);
	}
	
	public synchronized void removeFollowed(int userId) {
		for(int i = 0; i < followedUsers.size(); i++)
			if(followedUsers.get(i) == userId)
				followedUsers.remove(i);
	}
	
	public synchronized boolean getIfFollowing(int userId) {
		boolean following = false;
		Iterator<Integer> it;
		for(it = followedUsers.iterator(); !following && it.hasNext(); )
			if(it.next().intValue() == userId)
				following = true;
		return following;
	}
	
	public synchronized long getLastMessageReadTimestamp() {
		return lastMessageReadTimestamp;
	}
	
	public synchronized void setLastMessageReadTimestamp(long lastMessageReadTimestamp) {
		this.lastMessageReadTimestamp = lastMessageReadTimestamp;
	}

	public synchronized boolean isLogged() {
		return isLogged;
	}

	public synchronized void setLogged(boolean isLogged) {
		this.isLogged = isLogged;
	}
}
