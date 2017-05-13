package it.polimi.middleware.jms.server;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;

import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.naming.NamingException;

import it.polimi.middleware.jms.Constants;
import it.polimi.middleware.jms.Utils;
import it.polimi.middleware.jms.server.model.IdDistributor;
import it.polimi.middleware.jms.server.model.User;
import it.polimi.middleware.jms.server.model.message.GeneralMessage;
import it.polimi.middleware.jms.server.model.message.ImageMessage;
import it.polimi.middleware.jms.server.model.message.MessageProperty;
import it.polimi.middleware.jms.server.model.message.RequestMessage;
import it.polimi.middleware.jms.server.model.message.ResponseMessage;

public class ServerInstance implements Runnable {
	private int SERVER_INSTANCE_NUMBER;
	private IdDistributor userIdDistributor;
	private IdDistributor messageIdDistributor;
	private HashMap<Integer, User> usersMapId;
	private HashMap<String, User> usersMapUsername;
	private HashMap<Integer, UserQueueDaemon> daemonsMap;
	private JMSContext jmsContext;
	private JMSConsumer jmsConsumer;
	private JMSProducer jmsProducer;
	private Queue requestsQueue;
	
	public ServerInstance(int SERVER_INSTANCE_NUMBER, JMSContext jmsContext,
			IdDistributor userIdDistributor, IdDistributor messageIdDistributor, HashMap<Integer, User> usersMapId,
			HashMap<String, User> usersMapUsername,
			HashMap<Integer, UserQueueDaemon> daemonsMap) {
		this.SERVER_INSTANCE_NUMBER = SERVER_INSTANCE_NUMBER;
		this.jmsContext = jmsContext;
		this.userIdDistributor = userIdDistributor;
		this.messageIdDistributor = messageIdDistributor;
		this.usersMapId = usersMapId;
		this.usersMapUsername = usersMapUsername;
		this.daemonsMap = daemonsMap;
	}
	
	private void startServer() throws NamingException {
		requestsQueue = jmsContext.createQueue(Constants.QUEUE_REQUESTS_NAME);
		jmsConsumer = jmsContext.createConsumer(requestsQueue);
		jmsProducer = jmsContext.createProducer();
	}

	@Override
	public void run() {
		try {
			startServer();
			System.out.println("SI" + SERVER_INSTANCE_NUMBER + ": server instance started.");
			while(true) {
				Message msg = jmsConsumer.receive();
				onMessage(msg);
			}
		} catch (NamingException e1) {
			e1.printStackTrace();
		}
	}

	public void onMessage(Message msg) {
		try {
			RequestMessage request = msg.getBody(RequestMessage.class);
			Queue responseQueue = (Queue) msg.getJMSReplyTo();
			System.out.println("SI" + SERVER_INSTANCE_NUMBER + ": requests received.");
			//find the user of the request
			User user;
			synchronized(usersMapUsername) {
				user = usersMapId.get(request.getUserId());
			}
			//process request
			switch(request.getRequestCode()) {
			
			case Constants.REQUEST_REGISTER:
				if(user == null) {
					System.out.println("SI" + SERVER_INSTANCE_NUMBER + ": processing registration...");
					manageRegistration(request, responseQueue);
					System.out.println("SI" + SERVER_INSTANCE_NUMBER + ": registration processed.");
				} else if(user.isLogged()) {
					ResponseMessage response = new ResponseMessage(Constants.RESPONSE_ERROR, Constants.RESPONSE_INFO_USER_ALREADY_AUTHENTICATED);
					jmsProducer.send(responseQueue, response);
				} else {
					ResponseMessage response = new ResponseMessage(Constants.RESPONSE_ERROR, Constants.RESPONSE_INFO_BAD_ID);
					jmsProducer.send(responseQueue, response);
				}
				break;
				
			case Constants.REQUEST_LOGIN:
				if(user == null) {
					System.out.println("SI" + SERVER_INSTANCE_NUMBER + ": processing login...");
					manageLogin(request, responseQueue);
					System.out.println("SI" + SERVER_INSTANCE_NUMBER + ": login processed.");
				} else if(user.isLogged()) {
					ResponseMessage response = new ResponseMessage(Constants.RESPONSE_ERROR, Constants.RESPONSE_INFO_USER_ALREADY_AUTHENTICATED);
					jmsProducer.send(responseQueue, response);
				} else {
					ResponseMessage response = new ResponseMessage(Constants.RESPONSE_ERROR, Constants.RESPONSE_INFO_BAD_ID);
					jmsProducer.send(responseQueue, response);
				}
				break;
				
			case Constants.REQUEST_UNREGISTER:
				if(user != null && user.isLogged()) {
					
				} else {
					sendNoLoggedResponse(responseQueue);
					System.out.println("SI" + SERVER_INSTANCE_NUMBER + ": REQUEST RECEIVED FROM INAVLID USER.");
				}
				break;
				
			case Constants.REQUEST_LOGOUT:
				if(user != null && user.isLogged()) {
					
				} else {
					sendNoLoggedResponse(responseQueue);
					System.out.println("SI" + SERVER_INSTANCE_NUMBER + ": REQUEST RECEIVED FROM INAVLID USER.");
				}
				break;
				
			case Constants.REQUEST_FOLLOW:
				if(user != null && user.isLogged()) {
					System.out.println("SI" + SERVER_INSTANCE_NUMBER + ": processing follow...");
					manageFollow(request, responseQueue);
					System.out.println("SI" + SERVER_INSTANCE_NUMBER + ": follow processed.");
				} else {
					sendNoLoggedResponse(responseQueue);
					System.out.println("SI" + SERVER_INSTANCE_NUMBER + ": REQUEST RECEIVED FROM INAVLID USER.");
				}
				break;
				
			case Constants.REQUEST_UNFOLLOW:
				if(user != null && user.isLogged()) {
					System.out.println("SI" + SERVER_INSTANCE_NUMBER + ": processing unfollow...");
					manageUnfollow(request, responseQueue);
					System.out.println("SI" + SERVER_INSTANCE_NUMBER + ": unfollow processed.");
				} else {
					sendNoLoggedResponse(responseQueue);
					System.out.println("SI" + SERVER_INSTANCE_NUMBER + ": REQUEST RECEIVED FROM INAVLID USER.");
				}
				break;	
				
			case Constants.REQUEST_GET:
				if(user != null && user.isLogged()) {
					System.out.println("SI" + SERVER_INSTANCE_NUMBER + ": processing get...");
					manageGet(request, responseQueue);
					System.out.println("SI" + SERVER_INSTANCE_NUMBER + ": get processed.");
				} else {
					sendNoLoggedResponse(responseQueue);
					System.out.println("SI" + SERVER_INSTANCE_NUMBER + ": REQUEST RECEIVED FROM INAVLID USER.");
				}
				break;
			}
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}

	private void manageRegistration(RequestMessage request, Queue responseQueue) {
		int userId;
		String username, password;
		ArrayList<String> params = request.getRequestParams();
		if(params != null && params.size() == 2) {
			username = params.get(0);
			password = params.get(1);
			boolean userArleadyExists;
			synchronized(usersMapUsername) {
				userArleadyExists = usersMapUsername.containsKey(username);
			}
			if(!userArleadyExists) {
				userId = userIdDistributor.getNewId();
				User newUser = new User(userId, username, password);
				synchronized(usersMapId) {
					usersMapId.put(userId, newUser);
				}
				synchronized(usersMapUsername) {
					usersMapUsername.put(username, newUser);
				}
				sendOkResponse(responseQueue, "" + userId);
				runHandler(userId);
				runDaemon(userId);
			} else {
				ResponseMessage response = new ResponseMessage(Constants.RESPONSE_ERROR, Constants.RESPONSE_INFO_USER_ALREADY_EXISTS);
				try {
					Utils.sendMessage(null, jmsContext, response, jmsProducer, responseQueue, null, null);
				} catch (JMSException e) {
					e.printStackTrace();
				}
			}
		} else
			sendBadParamsResponse(responseQueue);
	}

	private void manageLogin(RequestMessage request, Queue responseQueue) {
		String username, password;
		ArrayList<String> params = request.getRequestParams();
		if(params != null && params.size() == 2) {
			username = params.get(0);
			password = params.get(1);
			User user;
			synchronized(usersMapUsername) {
				user = usersMapUsername.get(username);
			}
			if(user != null && password.equals(user.getPassword())) {
				sendOkResponse(responseQueue, "" + user.getUserId());
				user.setLogged(true);
				runHandler(user.getUserId());
			} else {
				ResponseMessage response = new ResponseMessage(Constants.RESPONSE_ERROR, Constants.RESPONSE_INFO_WRONG_AUTHENTICATION);
				try {
					Utils.sendMessage(null, jmsContext, response, jmsProducer, responseQueue, null, null);
				} catch (JMSException e) {
					e.printStackTrace();
				}
			}
		} else
			sendBadParamsResponse(responseQueue);
	}
	
	private void runHandler(int userId) {
		UserHandler handler = new UserHandler(jmsContext, userId, messageIdDistributor);
		Thread t = new Thread(handler);
		t.start();
	}
	
	private void runDaemon(int userId) {
		UserQueueDaemon daemon = new UserQueueDaemon(userId, messageIdDistributor);
		synchronized(daemonsMap) {
			daemonsMap.put(userId, daemon);
		}
		Thread t = new Thread(daemon);
		t.start();
	}
	
	private void manageFollow(RequestMessage request, Queue responseQueue) {
		ArrayList<String> params = request.getRequestParams();
		if(params != null && params.size() == 1) {
			String followedUsername = params.get(0);
			User user;
			synchronized(usersMapId) {
				user = usersMapId.get(request.getUserId());
			}
			boolean followedUserExists;
			synchronized(usersMapUsername) {
				followedUserExists = usersMapUsername.containsKey(followedUsername);
			}
			if(followedUserExists && !(user.getUsername().equals(followedUsername))) {
				User followedUser;
				synchronized(usersMapUsername) {
					followedUser = usersMapUsername.get(followedUsername);
				}
				int followedUserId = followedUser.getUserId();
				if(!user.getIfFollowing(followedUserId)) {
					UserQueueDaemon daemon;
					synchronized(daemonsMap) {
						daemon = daemonsMap.get(request.getUserId());
					}
					synchronized(daemon) {
						daemon.setNewSubscription(true);
						try {
							daemon.wait();
							daemon.addSubscription(followedUserId);
							user.addFollowed(followedUserId);
							daemon.setNewSubscription(false);
							daemon.notify();
						} catch (InterruptedException e) {
							e.printStackTrace();
						} catch (NamingException e) {
							e.printStackTrace();
						}
					}
					sendOkResponse(responseQueue, null);
				} else {
					ResponseMessage response = new ResponseMessage(Constants.RESPONSE_ERROR, Constants.RESPONSE_INFO_ALREADY_FOLLOWING);
					try {
						Utils.sendMessage(null, jmsContext, response, jmsProducer, responseQueue, null, null);
					} catch (JMSException e) {
						e.printStackTrace();
					}
				}
			} else {
				ResponseMessage response = new ResponseMessage(Constants.RESPONSE_ERROR, Constants.RESPONSE_INFO_WRONG_USERNAME);
				try {
					Utils.sendMessage(null, jmsContext, response, jmsProducer, responseQueue, null, null);
				} catch (JMSException e) {
					e.printStackTrace();
				}
			}
		} else
			sendBadParamsResponse(responseQueue);
	}
	
	private void manageUnfollow(RequestMessage request, Queue responseQueue) {
		ArrayList<String> params = request.getRequestParams();
		if(params != null && params.size() == 1) {
			String followedUsername = params.get(0);
			User user;
			synchronized(usersMapId) {
				user = usersMapId.get(request.getUserId());
			}
			boolean followedUserExists;
			synchronized(usersMapUsername) {
				followedUserExists = usersMapUsername.containsKey(followedUsername);
			}
			if(followedUserExists && !(user.getUsername().equals(followedUsername))) {
				User followedUser;
				synchronized(usersMapUsername) {
					followedUser = usersMapUsername.get(followedUsername);
				}
				int followedUserId = followedUser.getUserId();
				if(user.getIfFollowing(followedUserId)) {
					UserQueueDaemon daemon;
					synchronized(daemonsMap) {
						daemon = daemonsMap.get(request.getUserId());
					}
					synchronized(daemon) {
						daemon.setNewSubscription(true);
						try {
							daemon.wait();
							daemon.removeSubscription(followedUserId);
							user.removeFollowed(followedUserId);
							daemon.setNewSubscription(false);
							daemon.notify();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					sendOkResponse(responseQueue, null);
				} else {
					ResponseMessage response = new ResponseMessage(Constants.RESPONSE_ERROR, Constants.RESPONSE_INFO_NOT_FOLLOWING);
					try {
						Utils.sendMessage(null, jmsContext, response, jmsProducer, responseQueue, null, null);
					} catch (JMSException e) {
						e.printStackTrace();
					}
				}
			} else {
				ResponseMessage response = new ResponseMessage(Constants.RESPONSE_ERROR, Constants.RESPONSE_INFO_WRONG_USERNAME);
				try {
					Utils.sendMessage(null, jmsContext, response, jmsProducer, responseQueue, null, null);
				} catch (JMSException e) {
					e.printStackTrace();
				}
			}
		} else
			sendBadParamsResponse(responseQueue);
	}
	
	private void manageGet(RequestMessage request, Queue responseQueue) {
		ArrayList<String> params = request.getRequestParams();
		if(params != null && params.size() > 0) {
			int getCode = Integer.parseInt(params.get(0));
			switch(getCode) {
			
			case Constants.REQUEST_GET_ALL_NEW:
				manageGetAllNew(request.getUserId(), responseQueue);
				break;
				
			case Constants.REQUEST_GET_FROM_I_TO_J:
				if(params.size() == 3) {
					long i, j;
					i = Long.parseLong(params.get(1));
					j = Long.parseLong(params.get(2));
					manageGetFromIToJ(request.getUserId(), responseQueue, i, j);
				} else
					sendBadParamsResponse(responseQueue);
				break;
				
			case Constants.REQUEST_GET_IMAGE:
				if(params.size() == 2) {
					String imageMessageId = params.get(1);
					manageGetImage(request.getUserId(), responseQueue, imageMessageId);
				} else
					sendBadParamsResponse(responseQueue);
			}
		} else
			sendBadParamsResponse(responseQueue);
	}
	
	private void manageGetAllNew(int userId, Queue responseQueue) {
		try {
			Queue userMessagesQueue, destinationQueue;
			userMessagesQueue = jmsContext.createQueue(Constants.QUEUE_TO_USER_MESSAGES_PREFIX + userId);
			destinationQueue = jmsContext.createQueue(Constants.QUEUE_GET_USER_PREFIX + userId);
			QueueBrowser browser = jmsContext.createBrowser(userMessagesQueue);
			@SuppressWarnings("unchecked")
			Enumeration<Message> messages = browser.getEnumeration();
			User user;
			synchronized(usersMapId) {
				user = usersMapId.get(userId);
			}
			long lastMessageReadTimestamp = user.getLastMessageReadTimestamp();
			long newTimestamp = lastMessageReadTimestamp;
			//send new messages
			while(messages.hasMoreElements()) {
				Message msg = messages.nextElement();
				GeneralMessage message = msg.getBody(GeneralMessage.class);
				if(msg.getJMSTimestamp() > lastMessageReadTimestamp) {
					//set properties
					ArrayList<MessageProperty> messageProperties = new ArrayList<MessageProperty>();
					@SuppressWarnings("unchecked")
					Enumeration<String> propertyNames = msg.getPropertyNames();
					while(propertyNames.hasMoreElements()) {
						String propertyName = propertyNames.nextElement();
						messageProperties.add(new MessageProperty(propertyName, msg.getStringProperty(propertyName)));
					}
					Utils.sendMessage(null, jmsContext, message, jmsProducer, destinationQueue, messageProperties, null);
					newTimestamp = msg.getJMSTimestamp();
				}
			}
			user.setLastMessageReadTimestamp(newTimestamp);
			//send response
			if(lastMessageReadTimestamp != newTimestamp)
				sendOkResponse(responseQueue, null);
			else {
				ResponseMessage response = new ResponseMessage(Constants.RESPONSE_WARNING, Constants.RESPONSE_INFO_NO_NEW_MESSAGES);
				jmsProducer.send(responseQueue, response);
			}
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}

	private void manageGetFromIToJ(int userId, Queue responseQueue, long i, long j) {
		if(j >= i) {
			try {
				Queue userMessagesQueue, destinationQueue;
				boolean iSentSomething = false;
				userMessagesQueue = jmsContext.createQueue(Constants.QUEUE_TO_USER_MESSAGES_PREFIX + userId);
				destinationQueue = jmsContext.createQueue(Constants.QUEUE_GET_USER_PREFIX + userId);
				QueueBrowser browser = jmsContext.createBrowser(userMessagesQueue);
				@SuppressWarnings("unchecked")
				Enumeration<Message> messages = browser.getEnumeration();
				//send messages
				while(messages.hasMoreElements()) {
					Message msg = messages.nextElement();
					GeneralMessage message = msg.getBody(GeneralMessage.class);
					if(msg.getJMSTimestamp() >= i && msg.getJMSTimestamp() <= j) {
						//set properties
						ArrayList<MessageProperty> messageProperties = new ArrayList<MessageProperty>();
						@SuppressWarnings("unchecked")
						Enumeration<String> propertyNames = msg.getPropertyNames();
						while(propertyNames.hasMoreElements()) {
							String propertyName = propertyNames.nextElement();
							messageProperties.add(new MessageProperty(propertyName, msg.getStringProperty(propertyName)));
						}
						Utils.sendMessage(null, jmsContext, message, jmsProducer, destinationQueue, messageProperties, null);
						iSentSomething = true;
					}
				}
				//send response
				if(iSentSomething)
					sendOkResponse(responseQueue, null);
				else {
					ResponseMessage response = new ResponseMessage(Constants.RESPONSE_WARNING, Constants.RESPONSE_INFO_NO_NEW_MESSAGES);
					jmsProducer.send(responseQueue, response);
				}
			} catch (JMSException e) {
				e.printStackTrace();
			}
		} else
			sendBadParamsResponse(responseQueue);
	}
	
	private void manageGetImage(int userId, Queue responseQueue, String imageMessageId) {
		Queue userImagesQueue, destinationQueue;
		try {
			userImagesQueue = jmsContext.createQueue(Constants.QUEUE_TO_USER_IMAGES_PREFIX + userId);
			destinationQueue = jmsContext.createQueue(Constants.QUEUE_GET_USER_PREFIX + userId);
			String selector = Constants.PROPERTY_NAME_MESSAGE_ID + " IS NOT NULL AND " + Constants.PROPERTY_NAME_MESSAGE_ID + " = \'" + imageMessageId + "\'";
			QueueBrowser browser = jmsContext.createBrowser(userImagesQueue, selector);
			@SuppressWarnings("unchecked")
			Enumeration<Message> messages = browser.getEnumeration();
			Message msg = null;
			if(messages.hasMoreElements()) {
				msg = messages.nextElement();
				ImageMessage message = msg.getBody(ImageMessage.class);
				//set properties
				ArrayList<MessageProperty> messageProperties = new ArrayList<MessageProperty>();
				@SuppressWarnings("unchecked")
				Enumeration<String> propertyNames = msg.getPropertyNames();
				while(propertyNames.hasMoreElements()) {
					String propertyName = propertyNames.nextElement();
					messageProperties.add(new MessageProperty(propertyName, msg.getStringProperty(propertyName)));
				}
				Utils.sendMessage(null, jmsContext, message, jmsProducer, destinationQueue, messageProperties, null);
				sendOkResponse(responseQueue, null);
			} else {
				ResponseMessage response = new ResponseMessage(Constants.RESPONSE_WARNING, Constants.RESPONSE_INFO_NO_NEW_MESSAGES);
				jmsProducer.send(responseQueue, response);
			}
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}
	
	private void sendOkResponse(Queue responseQueue, String info) {
		ResponseMessage response = new ResponseMessage(Constants.RESPONSE_OK, info);
		try {
			Utils.sendMessage(null, jmsContext, response, jmsProducer, responseQueue, null, null);
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}

	private void sendBadParamsResponse(Queue responseQueue) {
		ResponseMessage response = new ResponseMessage(Constants.RESPONSE_ERROR, Constants.RESPONSE_INFO_BAD_PARAMS);
		try {
			Utils.sendMessage(null, jmsContext, response, jmsProducer, responseQueue, null, null);
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}
	
	private void sendNoLoggedResponse(Queue responseQueue) {
		ResponseMessage response = new ResponseMessage(Constants.RESPONSE_ERROR, Constants.RESPONSE_INFO_USER_NOT_AUTHENTICATED);
		try {
			Utils.sendMessage(null, jmsContext, response, jmsProducer, responseQueue, null, null);
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}

	public int getSERVER_INSTANCE_NUMBER() {
		return SERVER_INSTANCE_NUMBER;
	}
}
