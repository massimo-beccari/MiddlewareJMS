package it.polimi.middleware.jms.client;

public class MessageHolder {
	private String messageId;
	private String imageMessageId;
	private String username;
	private String text;
	private byte[] image;
	private String time;
	
	public MessageHolder(String messageId, String imageMessageId, 
			String username, String text, byte[] image, String time) {
		super();
		this.messageId = messageId;
		this.imageMessageId = imageMessageId;
		this.username = username;
		this.text = text;
		this.image = image;
		this.time = time;
	}

	public String getMessageId() {
		return messageId;
	}

	public String getImageMessageId() {
		return imageMessageId;
	}

	public String getUsername() {
		return username;
	}

	public String getText() {
		return text;
	}

	public byte[] getImage() {
		return image;
	}

	public String getTime() {
		return time;
	}
}
