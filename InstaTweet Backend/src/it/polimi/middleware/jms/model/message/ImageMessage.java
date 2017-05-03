package it.polimi.middleware.jms.model.message;

public class ImageMessage implements MessageInterface {
	private static final long serialVersionUID = 1L;
	private int userId;
	private byte[] image;
	
	public ImageMessage(int userId, byte[] image) {
		this.userId = userId;
		this.image = image;
	}

	public int getUserId() {
		return userId;
	}

	public byte[] getImage() {
		return image;
	}
}
