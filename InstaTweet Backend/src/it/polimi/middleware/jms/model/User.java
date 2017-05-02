package it.polimi.middleware.jms.model;

import java.util.ArrayList;
import java.util.Iterator;

public class User {
	private int userId;
	private String username;
	private String password;
	private ArrayList<User> followedUsers;
	
	public User(int userId, String username, String password) {
		this.userId = userId;
		this.username = username;
		this.password = password;
		followedUsers = new ArrayList<User>();
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
	
	public Iterator<User> getFollowedIterator() {
		return followedUsers.iterator();
	}
	
	public void addFollowed(User user) {
		followedUsers.add(user);
	}
}
