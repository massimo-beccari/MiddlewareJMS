package it.polimi.middleware.jms.server.model.message;

import java.util.ArrayList;

public class RequestMessage implements MessageInterface {
	private static final long serialVersionUID = 1L;
	private int userId;
	private int requestCode;
	private ArrayList<String> requestParams;
	
	public RequestMessage(int userId, int requestCode,
			ArrayList<String> requestParams) {
		this.userId = userId;
		this.requestCode = requestCode;
		this.requestParams = requestParams;
	}

	public int getUserId() {
		return userId;
	}

	public int getRequestCode() {
		return requestCode;
	}

	public ArrayList<String> getRequestParams() {
		return requestParams;
	}
}
