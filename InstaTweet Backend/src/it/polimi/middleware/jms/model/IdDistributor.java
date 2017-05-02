package it.polimi.middleware.jms.model;

public class IdDistributor {
	private int nextId;
	
	public IdDistributor(int firstId) {
		nextId = firstId;
	}

	public synchronized int getNewId() {
		int newId = nextId;
		nextId++;
		return newId;
	}
}
