package it.polimi.middleware.jms.model.message;

import it.polimi.middleware.jms.Constants;

public class GeneralMessage implements MessageInterface {
	private static final long serialVersionUID = 1L;
	private int userId;
	private String text;
	private byte[] image;
	private int type;
	
	public GeneralMessage(int userId, String text, byte[] image) {
		this.userId = userId;
		this.text = text;
		this.image = image;
		if(text == null)
			type = Constants.MESSAGE_ONLY_IMAGE;
		else if(image == null)
			type = Constants.MESSAGE_ONLY_TEXT;
		else
			type = Constants.MESSAGE_TEXT_AND_IMAGE;
	}

	public int getUserId() {
		return userId;
	}

	public String getText() {
		return text;
	}

	public byte[] getImage() {
		return image;
	}

	public int getType() {
		return type;
	}
}
