package it.polimi.middleware.jms.server.model.message;

public class MessageProperty {
	private String propertyName;
	private String propertyValue;
	
	public MessageProperty(String propertyName, String propertyValue) {
		this.propertyName = propertyName;
		this.propertyValue = propertyValue;
	}

	public String getPropertyName() {
		return propertyName;
	}

	public void setPropertyName(String propertyName) {
		this.propertyName = propertyName;
	}

	public String getPropertyValue() {
		return propertyValue;
	}

	public void setPropertyValue(String propertyValue) {
		this.propertyValue = propertyValue;
	}
	
	@Override
	public String toString() {
		return "{\"" + propertyName  + "\", \"" + propertyValue + "\"}";
	}
}
