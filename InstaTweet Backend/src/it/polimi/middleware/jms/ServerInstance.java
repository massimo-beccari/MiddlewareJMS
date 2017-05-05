package it.polimi.middleware.jms;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;

import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.naming.Context;
import javax.naming.NamingException;

import it.polimi.middleware.jms.model.IdDistributor;
import it.polimi.middleware.jms.model.User;
import it.polimi.middleware.jms.model.message.GeneralMessage;
import it.polimi.middleware.jms.model.message.ImageMessage;
import it.polimi.middleware.jms.model.message.MessageProperty;
import it.polimi.middleware.jms.model.message.RequestMessage;
import it.polimi.middleware.jms.model.message.ResponseMessage;

public class ServerInstance implements Runnable {
	private int SERVER_INSTANCE_NUMBER;
	private IdDistributor userIdDistributor;
	private HashMap<Integer, User> usersMapId;
	private HashMap<String, User> usersMapUsername;
	private HashMap<Integer, UserQueueDaemon> daemonsMap;
	private Context initialContext;
	private JMSContext jmsContext;
	private JMSProducer jmsProducer;
	private boolean iWait;
	private Message request;
	
	public ServerInstance(int SERVER_INSTANCE_NUMBER,
			IdDistributor userIdDistributor, HashMap<Integer, User> usersMapId,
			HashMap<String, User> usersMapUsername,
			HashMap<Integer, UserQueueDaemon> daemonsMap) {
		this.SERVER_INSTANCE_NUMBER = SERVER_INSTANCE_NUMBER;
		this.userIdDistributor = userIdDistributor;
		this.usersMapId = usersMapId;
		this.usersMapUsername = usersMapUsername;
		this.daemonsMap = daemonsMap;
		iWait = true;
	}
	
	public void startServer() throws NamingException {
		createContexts();
		jmsProducer = jmsContext.createProducer();
	}
	
	private void createContexts() throws NamingException {
		initialContext = Utils.getContext();
		jmsContext = ((ConnectionFactory) initialContext.lookup("java:comp/DefaultJMSConnectionFactory")).createContext();
		jmsContext.setClientID("SERVER_INSTANCE_" + SERVER_INSTANCE_NUMBER);
	}

	@Override
	public void run() {
		RequestMessage msg = null;
		Queue responseQueue = null;
		while(true) {
			//i wait a request and then fetch the request data
			synchronized(this) {
				try {
					while(iWait) {
						wait();
					}
					try {
						msg = request.getBody(RequestMessage.class);
						responseQueue = (Queue) request.getJMSReplyTo();
					} catch (JMSException e) {
						e.printStackTrace();
					}
				} catch(InterruptedException e) {
					e.printStackTrace();
				}
			}
			//i process the message
			onMessage(msg, responseQueue);
			iWait = true;
		}
	}

	public void onMessage(RequestMessage request, Queue responseQueue) {
		switch(request.getRequestCode()) {
		
		case Constants.REQUEST_REGISTER:
			manageRegistration(request, responseQueue);
			break;
			
		case Constants.REQUEST_LOGIN:
			manageLogin(request, responseQueue);
			break;
			
		case Constants.REQUEST_UNREGISTER:
			
			break;
			
		case Constants.REQUEST_LOGOUT:
			
			break;
			
		case Constants.REQUEST_FOLLOW:
			manageFollow(request, responseQueue);
			break;
			
		case Constants.REQUEST_GET:
			manageGet(request, responseQueue);
			break;
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
				jmsProducer.send(responseQueue, response);
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
				runHandler(user.getUserId());
			} else {
				ResponseMessage response = new ResponseMessage(Constants.RESPONSE_ERROR, Constants.RESPONSE_INFO_WRONG_AUTHENTICATION);
				jmsProducer.send(responseQueue, response);
			}
		} else
			sendBadParamsResponse(responseQueue);
	}
	
	private void runHandler(int userId) {
		UserHandler handler = new UserHandler(userId);
		Thread t = new Thread(handler);
		t.start();
	}
	
	private void runDaemon(int userId) {
		UserQueueDaemon daemon = new UserQueueDaemon(userId);
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
				UserQueueDaemon daemon;
				synchronized(daemonsMap) {
					daemon = daemonsMap.get(request.getUserId());
				}
				synchronized(daemon) {
					daemon.setNewSubscription(true);
					try {
						daemon.wait();
						User followedUser;
						synchronized(usersMapUsername) {
							followedUser = usersMapUsername.get(followedUsername);
						}
						int followedUserId = followedUser.getUserId();
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
				ResponseMessage response = new ResponseMessage(Constants.RESPONSE_ERROR, Constants.RESPONSE_INFO_WRONG_USERNAME);
				jmsProducer.send(responseQueue, response);
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
					int imageMessageId = Integer.parseInt(params.get(1));
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
			userMessagesQueue = (Queue) initialContext.lookup(Constants.QUEUE_TO_USER_MESSAGES_PREFIX + userId);
			destinationQueue = (Queue) initialContext.lookup(Constants.QUEUE_GET_USER_PREFIX + userId);
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
					ArrayList<MessageProperty> messageProperties = new ArrayList<MessageProperty>();
					messageProperties.add(new MessageProperty(Constants.PROPERTY_IMAGE_MESSAGE_ID, msg.getStringProperty(Constants.PROPERTY_IMAGE_MESSAGE_ID)));
					Utils.sendMessage(jmsContext, message, jmsProducer, destinationQueue, null, null);
					newTimestamp = msg.getJMSTimestamp();
				}
			}
			user.setLastMessageReadTimestamp(newTimestamp);
			//send response
			if(lastMessageReadTimestamp != newTimestamp)
				sendOkResponse(responseQueue, null);
			else
				sendOkResponse(responseQueue, Constants.RESPONSE_INFO_NO_NEW_MESSAGES);
		} catch (NamingException e) {
			e.printStackTrace();
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}

	private void manageGetFromIToJ(int userId, Queue responseQueue, long i, long j) {
		if(j < i) {
			try {
				Queue userMessagesQueue, destinationQueue;
				boolean iSentSomething = false;
				userMessagesQueue = (Queue) initialContext.lookup(Constants.QUEUE_TO_USER_MESSAGES_PREFIX + userId);
				destinationQueue = (Queue) initialContext.lookup(Constants.QUEUE_GET_USER_PREFIX + userId);
				QueueBrowser browser = jmsContext.createBrowser(userMessagesQueue);
				@SuppressWarnings("unchecked")
				Enumeration<Message> messages = browser.getEnumeration();
				//send new messages
				while(messages.hasMoreElements()) {
					Message msg = messages.nextElement();
					GeneralMessage message = msg.getBody(GeneralMessage.class);
					if(msg.getJMSTimestamp() >= i && msg.getJMSTimestamp() <= j) {
						ArrayList<MessageProperty> messageProperties = new ArrayList<MessageProperty>();
						messageProperties.add(new MessageProperty(Constants.PROPERTY_IMAGE_MESSAGE_ID, msg.getStringProperty(Constants.PROPERTY_IMAGE_MESSAGE_ID)));
						Utils.sendMessage(jmsContext, message, jmsProducer, destinationQueue, null, null);
						iSentSomething = true;
					}
				}
				//send response
				if(iSentSomething)
					sendOkResponse(responseQueue, null);
				else
					sendOkResponse(responseQueue, Constants.RESPONSE_INFO_NO_MESSAGES);
			} catch (NamingException e) {
				e.printStackTrace();
			} catch (JMSException e) {
				e.printStackTrace();
			}
		} else
			sendBadParamsResponse(responseQueue);
	}
	
	private void manageGetImage(int userId, Queue responseQueue, int imageMessageId) {
		Queue userImagesQueue, destinationQueue;
		try {
			userImagesQueue = (Queue) initialContext.lookup(Constants.QUEUE_TO_USER_IMAGES_PREFIX + userId);
			destinationQueue = (Queue) initialContext.lookup(Constants.QUEUE_GET_USER_PREFIX + userId);
			String selector = "JMSMessageID IS NOT NULL AND JMSMessageID = " + imageMessageId;
			QueueBrowser browser = jmsContext.createBrowser(userImagesQueue, selector);
			@SuppressWarnings("unchecked")
			Enumeration<Message> messages = browser.getEnumeration();
			Message msg = null;
			if(messages.hasMoreElements())
				msg = messages.nextElement();
			ImageMessage message = msg.getBody(ImageMessage.class);
			Utils.sendMessage(jmsContext, message, jmsProducer, destinationQueue, null, null);
			sendOkResponse(responseQueue, null);
		} catch (NamingException e) {
			e.printStackTrace();
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}
	
	private void sendOkResponse(Queue responseQueue, String info) {
		ResponseMessage response = new ResponseMessage(Constants.RESPONSE_OK, info);
		jmsProducer.send(responseQueue, response);
	}

	private void sendBadParamsResponse(Queue responseQueue) {
		ResponseMessage response = new ResponseMessage(Constants.RESPONSE_ERROR, Constants.RESPONSE_INFO_BAD_PARAMS);
		jmsProducer.send(responseQueue, response);
	}
	
	public void setNoWait() {
		iWait = false;
	}
	
	public void setRequest(Message request) {
		this.request = request;
	}
	
	public boolean getWait() {
		return iWait;
	}
}
