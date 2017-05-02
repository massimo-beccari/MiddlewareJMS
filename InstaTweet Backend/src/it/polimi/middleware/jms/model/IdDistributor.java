package it.polimi.middleware.jms.model;

public class IdDistributor {
	private int nextId;
	
	public IdDistributor() {
		nextId = 1;
	}

	public synchronized int getNewId() {
		int newId = nextId;
		nextId++;
		return newId;
	}
}
