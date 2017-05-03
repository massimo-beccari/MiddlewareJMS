package it.polimi.middleware.jms;

public final class Constants {
	public static final int UNREGISTERED_USER_ID = -1;
	
	public static final int MESSAGE_ONLY_TEXT = 1;
	public static final int MESSAGE_ONLY_IMAGE = 2;
	public static final int MESSAGE_TEXT_AND_IMAGE = 3;
	
	public static final int REQUEST_REGISTER = 1;
	public static final int REQUEST_LOGIN = 2;
	public static final int REQUEST_LOGOUT = 3;
	public static final int REQUEST_UNREGISTER = 4;
	public static final int REQUEST_FOLLOW = 5;
	
	public static final int RESPONSE_ERROR = 0;
	public static final int RESPONSE_OK = 1;
	
	public static final String RESPONSE_INFO_BAD_PARAMS = "BAD_PARAMS";
	public static final String RESPONSE_INFO_USER_ALREADY_EXISTS = "USER_ALREADY_EXISTS";
	public static final String RESPONSE_INFO_WRONG_AUTHENTICATION = "USER_DOESNT_EXISTS_OR_WRONG_PASSWORD";
	public static final String RESPONSE_INFO_WRONG_USERNAME = "USER_DOESNT_EXISTS_OR_IS_YOU";
	public static final String RESPONSE_INFO_USER_NOT_AUTHENTICATED = "USER_NOT_AUTHENTICATED";

	public static final String QUEUE_REQUESTS_NAME = "REQUESTS";
	public static final String QUEUE_RESPONSE_PREFIX = "RESPONSES_USER_";
	public static final String QUEUE_FROM_USER_PREFIX = "RAW_QUEUE_USER_";
	public static final String QUEUE_TO_USER_MESSAGES_PREFIX = "MESSAGES_USER_";
	public static final String QUEUE_TO_USER_IMAGES_PREFIX = "IMAGES_USER_";
	public static final String TOPIC_USER_MESSAGES_PREFIX = "TOPIC_MESSAGES_USER_";
	public static final String TOPIC_USER_IMAGES_PREFIX = "TOPIC_IMAGES_USER_";
	public static final String TOPIC_SUBSCRIPTION_MESSAGES_PREFIX = "SUBSCRIPTION_MESSAGES_USERS_";
	public static final String TOPIC_SUBSCRIPTION_IMAGES_PREFIX = "SUBSCRIPTION_IMAGES_USERS_";
	
	public static final String PROPERTY_USER_ID = "USER_ID";
	public static final String PROPERTY_IMAGE_MESSAGE_ID = "IMAGE_MESSAGE_ID";
}
