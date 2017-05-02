package it.polimi.middleware.jms;

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
	private HashMap<String, User> usersMap;
	private Context initialContext;
	private JMSContext jmsContext;
	private JMSProducer jmsProducer;
	private Queue requestsQueue;
	
	public Server() {
		userIdDistributor = new IdDistributor(1);
		usersMap = new HashMap<String, User>();
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
		try {
			server.startServer();
		} catch (NamingException e) {
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
			if(!(usersMap.containsKey(username))) {
				userId = userIdDistributor.getNewId();
				User newUser = new User(userId, username, password);
				usersMap.put(username, newUser);
				ResponseMessage response = new ResponseMessage(Constants.RESPONSE_OK, null);
				jmsProducer.send(responseQueue, response);
				runHandler(newUser);
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
			User user = usersMap.get(username);
			if(user != null && password.equals(user.getPassword())) {
				ResponseMessage response = new ResponseMessage(Constants.RESPONSE_OK, null);
				jmsProducer.send(responseQueue, response);
				runHandler(user);
			} else {
				ResponseMessage response = new ResponseMessage(Constants.RESPONSE_ERROR, Constants.RESPONSE_INFO_WRONG_AUTHENTICATION);
				jmsProducer.send(responseQueue, response);
			}
		} else {
			ResponseMessage response = new ResponseMessage(Constants.RESPONSE_ERROR, Constants.RESPONSE_INFO_BAD_PARAMS);
			jmsProducer.send(responseQueue, response);
		}
	}
	
	private void runHandler(User user) {
		UserHandler handler = new UserHandler(user);
		Thread t = new Thread(handler);
		t.start();
	}
}
