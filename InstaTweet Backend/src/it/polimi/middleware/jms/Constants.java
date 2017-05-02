package it.polimi.middleware.jms;

public final class Constants {
	public static final int UNREGISTERED_USER_CODE = 0;
	
	public static final int MESSAGE_ONLY_TEXT = 1;
	public static final int MESSAGE_ONLY_IMAGE = 2;
	public static final int MESSAGE_TEXT_AND_IMAGE = 3;
	
	public static final int REQUEST_REGISTER = 1;
	public static final int REQUEST_LOGIN = 2;
	
	public static final int RESPONSE_ERROR = 0;
	public static final int RESPONSE_OK = 1;
	
	public static final String RESPONSE_INFO_BAD_PARAMS = "BAD_PARAMS";
	public static final String RESPONSE_INFO_USER_ALREADY_EXISTS = "USER_ALREADY_EXISTS";
	public static final String RESPONSE_INFO_WRONG_AUTHENTICATION = "USER_DOESNT_EXISTS_OR_WRONG_PASSWORD";

	public static final String QUEUE_REQUESTS_NAME = "REQUESTS";
	public static final String QUEUE_FROM_USER_PREFIX = "RAW_QUEUE_USER_";
	public static final String QUEUE_TO_USER_PREFIX = "MESSAGES_";
	public static final String TOPIC_USER_PREFIX = "TOPIC_USER_";
	public static final String TOPIC_SUBSCRIPTION_PREFIX = "SUBSCRIPTION_USERS_";
}
