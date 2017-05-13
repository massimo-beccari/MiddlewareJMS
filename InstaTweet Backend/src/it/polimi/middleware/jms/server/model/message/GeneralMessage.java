package it.polimi.middleware.jms.server.model.message;

import it.polimi.middleware.jms.Constants;

public class GeneralMessage implements MessageInterface {
	private static final long serialVersionUID = 1L;
	private int userId;
	private String username;
	private String text;
	private String imageExtension;
	private byte[] image;
	private int type;
	
	public GeneralMessage(int userId, String username, String text, String imageExtension, byte[] image) {
		this.userId = userId;
		this.username = username;
		this.text = text;
		this.imageExtension = imageExtension;
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

	public String getUsername() {
		return username;
	}

	public String getText() {
		return text;
	}

	public String getImageExtension() {
		return imageExtension;
	}

	public byte[] getImage() {
		return image;
	}

	public int getType() {
		return type;
	}
}
