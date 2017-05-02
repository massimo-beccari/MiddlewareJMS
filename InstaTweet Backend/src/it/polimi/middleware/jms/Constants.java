package it.polimi.middleware.jms;

public final class Constants {
	public static final int UNREGISTERED_USER_CODE = 0;
	
	public static final int MESSAGE_ONLY_TEXT = 1;
	public static final int MESSAGE_ONLY_IMAGE = 2;
	public static final int MESSAGE_TEXT_AND_IMAGE = 3;
	
	public static final int REQUEST_REGISTER = 1;
	public static final int REQUEST_LOGIN = 2;

	public static final String QUEUE_REQUESTS_NAME = "REQUESTS";
	public static final String QUEUE_MESSAGES_NAME_PREFIX = "MESSAGES_";
	public static final String TOPIC_USER_PREFIX = "TOPIC_";
	public static final String TOPIC_SUBSCRIPTION_PREFIX = "SUBSCRIPTION_";
}
