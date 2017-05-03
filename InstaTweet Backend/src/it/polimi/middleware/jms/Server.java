package it.polimi.middleware.jms;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.naming.Context;
import javax.naming.NamingException;

import it.polimi.middleware.jms.model.IdDistributor;
import it.polimi.middleware.jms.model.User;
import it.polimi.middleware.jms.model.message.RequestMessage;
import it.polimi.middleware.jms.model.message.ResponseMessage;

public class Server implements MessageListener {
	private IdDistributor userIdDistributor;
	private HashMap<Integer, User> usersMapId;
	private HashMap<String, User> usersMapUsername;
	private HashMap<Integer, UserQueueDaemon> daemonsMap;
	private Context initialContext;
	private JMSContext jmsContext;
	private JMSProducer jmsProducer;
	private Queue requestsQueue;
	
	public Server() {
		userIdDistributor = new IdDistributor(1);
		usersMapId = new HashMap<Integer, User>();
		usersMapUsername = new HashMap<String, User>();
		daemonsMap = new HashMap<Integer, UserQueueDaemon>();
	}
	
	public void startServer() throws NamingException {
		createContexts();
		requestsQueue = jmsContext.createQueue(Constants.QUEUE_REQUESTS_NAME);
		jmsContext.createConsumer(requestsQueue).setMessageListener(this);
		jmsProducer = jmsContext.createProducer();
	}
	
	private void createContexts() throws NamingException {
		initialContext = Utils.getContext();
		jmsContext = ((ConnectionFactory) initialContext.lookup("java:comp/DefaultJMSConnectionFactory")).createContext();
		jmsContext.setClientID("0");
	}

	public static void main(String[] args) {
		Server server = new Server();
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
		try {
			server.startServer();
			bufferedReader.readLine();
		} catch (NamingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onMessage(Message message) {
		try {
			RequestMessage request = message.getBody(RequestMessage.class);
			Queue responseQueue = (Queue) message.getJMSReplyTo();
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
			if(!(usersMapUsername.containsKey(username))) {
				userId = userIdDistributor.getNewId();
				User newUser = new User(userId, username, password);
				usersMapId.put(userId, newUser);
				usersMapUsername.put(username, newUser);
				ResponseMessage response = new ResponseMessage(Constants.RESPONSE_OK, "" + userId);
				jmsProducer.send(responseQueue, response);
				runHandler(userId);
				runDaemon(userId);
			} else {
				ResponseMessage response = new ResponseMessage(Constants.RESPONSE_ERROR, Constants.RESPONSE_INFO_USER_ALREADY_EXISTS);
				jmsProducer.send(responseQueue, response);
			}
		} else {
			ResponseMessage response = new ResponseMessage(Constants.RESPONSE_ERROR, Constants.RESPONSE_INFO_BAD_PARAMS);
			jmsProducer.send(responseQueue, response);
		}
	}

	private void manageLogin(RequestMessage request, Queue responseQueue) {
		String username, password;
		ArrayList<String> params = request.getRequestParams();
		if(params != null && params.size() == 2) {
			username = params.get(0);
			password = params.get(1);
			User user = usersMapUsername.get(username);
			if(user != null && password.equals(user.getPassword())) {
				ResponseMessage response = new ResponseMessage(Constants.RESPONSE_OK, "" + user.getUserId());
				jmsProducer.send(responseQueue, response);
				runHandler(user.getUserId());
			} else {
				ResponseMessage response = new ResponseMessage(Constants.RESPONSE_ERROR, Constants.RESPONSE_INFO_WRONG_AUTHENTICATION);
				jmsProducer.send(responseQueue, response);
			}
		} else {
			ResponseMessage response = new ResponseMessage(Constants.RESPONSE_ERROR, Constants.RESPONSE_INFO_BAD_PARAMS);
			jmsProducer.send(responseQueue, response);
		}
	}
	
	private void runHandler(int userId) {
		UserHandler handler = new UserHandler(userId);
		Thread t = new Thread(handler);
		t.start();
	}
	
	private void runDaemon(int userId) {
		UserQueueDaemon daemon = new UserQueueDaemon(userId);
		daemonsMap.put(userId, daemon);
		Thread t = new Thread(daemon);
		t.start();
	}
	
	private void manageFollow(RequestMessage request, Queue responseQueue) {
		ArrayList<String> params = request.getRequestParams();
		if(params != null && params.size() == 1) {
			String followedUsername = params.get(0);
			if(usersMapUsername.containsKey(followedUsername) && !(usersMapId.get(request.getUserId()).getUsername().equals(followedUsername))) {
				UserQueueDaemon daemon = daemonsMap.get(request.getUserId());
				synchronized(daemon) {
					daemon.setNewSubscription(true);
					try {
						daemon.wait();
						daemon.addSubscription(usersMapUsername.get(followedUsername).getUserId());
						daemon.setNewSubscription(false);
						daemon.notify();
					} catch (InterruptedException e) {
						e.printStackTrace();
					} catch (NamingException e) {
						e.printStackTrace();
					}
				}
				ResponseMessage response = new ResponseMessage(Constants.RESPONSE_OK, null);
				jmsProducer.send(responseQueue, response);
			} else {
				ResponseMessage response = new ResponseMessage(Constants.RESPONSE_ERROR, Constants.RESPONSE_INFO_WRONG_USERNAME);
				jmsProducer.send(responseQueue, response);
			}
		} else {
			ResponseMessage response = new ResponseMessage(Constants.RESPONSE_ERROR, Constants.RESPONSE_INFO_BAD_PARAMS);
			jmsProducer.send(responseQueue, response);
		}
	}
}
