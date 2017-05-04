package it.polimi.middleware.jms;

import it.polimi.middleware.jms.model.message.GeneralMessage;
import it.polimi.middleware.jms.model.message.RequestMessage;
import it.polimi.middleware.jms.model.message.ResponseMessage;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.util.ArrayList;

import javax.jms.ConnectionFactory;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.Message;
import javax.jms.Queue;
import javax.naming.Context;
import javax.naming.NamingException;

public class Client {
	private Context initialContext;
	private JMSContext jmsContext;
	private JMSProducer jmsProducer;
	private JMSConsumer responseConsumer;
	private Queue requestsQueue;
	private Queue responseQueue;
	private Queue uploadQueue;
	private Queue messageQueue;
	private boolean logged;
	private int userId;
	private String username;
	
	public Client() {
		logged = false;
		userId = Constants.UNREGISTERED_USER_ID;
		try {
			setup();
		} catch (NamingException e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * SETUP METHOD
	 */

	private void setup() throws NamingException {
		initialContext = Utils.getContext();
		createSession();
	}
	
	private void createSession() throws NamingException {
		jmsContext = ((ConnectionFactory) initialContext.lookup("java:comp/DefaultJMSConnectionFactory")).createContext();
		if(logged)
			jmsContext.setClientID("CLIENT_" + userId);
		requestsQueue = (Queue) initialContext.lookup(Constants.QUEUE_REQUESTS_NAME);
		responseQueue = jmsContext.createTemporaryQueue();
		responseConsumer = jmsContext.createConsumer(responseQueue);
		jmsProducer = jmsContext.createProducer();
	}
	
	/*
	 * CLIENT METHODS
	 */
	
	private void register(String username, String password) {
		ArrayList<String> params = new ArrayList<String>();
		params.add(username);
		params.add(password);
		RequestMessage request = new RequestMessage(userId, Constants.REQUEST_REGISTER, params);
		try {
			Utils.sendMessage(jmsContext, request, jmsProducer, requestsQueue, null, responseQueue);
			Message msg = responseConsumer.receive();
			ResponseMessage response = msg.getBody(ResponseMessage.class);
			if(response.getResponseCode() == Constants.RESPONSE_OK) {
				logged = true;
				this.username = username;
				this.userId = Integer.parseInt(response.getResponseInfo());
				System.out.println("Response: OK. Your id: " + response.getResponseInfo());
				setupQueues();
			} else
				System.out.println("Response: ERROR: " + response.getResponseInfo());
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}
	
	private void login(String username, String password) {
		ArrayList<String> params = new ArrayList<String>();
		params.add(username);
		params.add(password);
		RequestMessage request = new RequestMessage(userId, Constants.REQUEST_LOGIN, params);
		try {
			Utils.sendMessage(jmsContext, request, jmsProducer, requestsQueue, null, responseQueue);
			Message msg = responseConsumer.receive();
			ResponseMessage response = msg.getBody(ResponseMessage.class);
			if(response.getResponseCode() == Constants.RESPONSE_OK) {
				logged = true;
				this.username = username;
				this.userId = Integer.parseInt(response.getResponseInfo());
				//close old unidentified session and create a new one with client id
				jmsContext.close();
				createSession();
				System.out.println("Response: OK. Your id: " + response.getResponseInfo());
				setupQueues();
			} else
				System.out.println("Response: ERROR: " + response.getResponseInfo());
		} catch (JMSException e) {
			e.printStackTrace();
		} catch (NamingException e) {
			e.printStackTrace();
		}
	}
	
	private void setupQueues() {
		try {
			uploadQueue = (Queue) initialContext.lookup(Constants.QUEUE_FROM_USER_PREFIX + userId);
			messageQueue = (Queue) initialContext.lookup(Constants.QUEUE_GET_USER_PREFIX + userId);
		} catch (NamingException e) {
			e.printStackTrace();
		}
	}
	
	private void follow(String followedUsername) {
		if(logged) {
			ArrayList<String> params = new ArrayList<String>();
			params.add(followedUsername);
			RequestMessage request = new RequestMessage(userId, Constants.REQUEST_FOLLOW, params);
			try {
				Utils.sendMessage(jmsContext, request, jmsProducer, requestsQueue, null, responseQueue);
				Message msg = responseConsumer.receive();
				ResponseMessage response = msg.getBody(ResponseMessage.class);
				if(response.getResponseCode() == Constants.RESPONSE_OK) {
					System.out.println("Response: OK.");
				} else
					System.out.println("Response: ERROR: " + response.getResponseInfo());
			} catch (JMSException e) {
				e.printStackTrace();
			}
		} else {
			System.out.println("Error: you are not logged in. First login to the system.");
		}
	}
	
	private void post(String text, String imageFilePath) {
		if(logged) {
			String extension = imageFilePath.substring(imageFilePath.lastIndexOf(".") + 1);
			try {
				RandomAccessFile imageFile = new RandomAccessFile(imageFilePath, "r");
				byte[] image = new byte[(int) imageFile.length()];
				imageFile.readFully(image);
				imageFile.close();
				GeneralMessage message = new GeneralMessage(userId, text, extension, image);
				Utils.sendMessage(jmsContext, message, jmsProducer, uploadQueue, null, null);
			} catch (FileNotFoundException e) {
				System.out.println("Error: file " + imageFilePath + " not found.");
			} catch (IOException e) {
				e.printStackTrace();
			} catch (JMSException e) {
				e.printStackTrace();
			}
		} else {
			System.out.println("Error: you are not logged in. First login to the system.");
		}
	}

	private void displayUserInfo() {
		if(logged) {
			System.out.println("You are logged as id: " + userId + ", username: " + username);
		} else {
			System.out.println("Error: you are not logged in. First login to the system.");
		}
	}
	
	private void close() {
		jmsContext.close();
	}
	
	public static void main(String[] args) {
		Client client = new Client();
		String command;
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
		while (true) {
			try {
				System.out.println("Commands:\nregister\nlogin\nfollow\npost\nread\nmy identity");
				command = bufferedReader.readLine();
				if(command.equalsIgnoreCase("exit")) {
					client.close();
					System.exit(0);
				} else if(command.equalsIgnoreCase("register")) {
					String username, password;
					System.out.println("Username:");
					username = bufferedReader.readLine();
					System.out.println("Password:");
					password = bufferedReader.readLine();
					client.register(username, password);
				} else if(command.equalsIgnoreCase("login")) {
					String username, password;
					System.out.println("Username:");
					username = bufferedReader.readLine();
					System.out.println("Password:");
					password = bufferedReader.readLine();
					client.login(username, password);
				} else if(command.equalsIgnoreCase("follow")) {
					String followedUsername;
					System.out.println("Followed user username:");
					followedUsername = bufferedReader.readLine();
					client.follow(followedUsername);
				} else if(command.equalsIgnoreCase("post")) {
					String text, imageFilePath;
					System.out.println("Text:");
					text = bufferedReader.readLine();
					System.out.println("Image file path:");
					imageFilePath = bufferedReader.readLine();
					client.post(text, imageFilePath);
				} else if(command.equalsIgnoreCase("read")) {
					
				} else if(command.equalsIgnoreCase("my identity")) {
					client.displayUserInfo();
				} else {
					System.out.println("Invalid command.");
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
	}
}
