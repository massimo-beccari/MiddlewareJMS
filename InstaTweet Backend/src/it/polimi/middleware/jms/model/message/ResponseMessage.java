package it.polimi.middleware.jms.model.message;

import java.io.Serializable;

public class ResponseMessage implements Serializable {
	private static final long serialVersionUID = 1L;
	private int responseCode;
	private String responseInfo;
	
	public ResponseMessage(int responseCode, String responseInfo) {
		this.responseCode = responseCode;
		this.responseInfo = responseInfo;
	}

	public int getResponseCode() {
		return responseCode;
	}

	public String getResponseInfo() {
		return responseInfo;
	}
}
