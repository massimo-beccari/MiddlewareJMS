package it.polimi.middleware.jms.model.message;

public class ImageMessage implements MessageInterface {
	private static final long serialVersionUID = 1L;
	private int userId;
	private String extension;
	private byte[] image;
	
	public ImageMessage(int userId, String extension, byte[] image) {
		this.userId = userId;
		this.extension = extension;
		this.image = image;
	}

	public int getUserId() {
		return userId;
	}

	public String getExtension() {
		return extension;
	}

	public byte[] getImage() {
		return image;
	}
}
