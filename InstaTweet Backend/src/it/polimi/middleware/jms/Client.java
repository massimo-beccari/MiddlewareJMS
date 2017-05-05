package it.polimi.middleware.jms;

import it.polimi.middleware.jms.model.MyTime;
import it.polimi.middleware.jms.model.message.GeneralMessage;
import it.polimi.middleware.jms.model.message.ImageMessage;
import it.polimi.middleware.jms.model.message.RequestMessage;
import it.polimi.middleware.jms.model.message.ResponseMessage;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.jms.ConnectionFactory;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.Message;
import javax.jms.Queue;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.swing.JFrame;
import javax.swing.JPanel;

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
		requestsQueue = jmsContext.createQueue(Constants.QUEUE_REQUESTS_NAME);
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
			Utils.sendMessage(null, jmsContext, request, jmsProducer, requestsQueue, null, responseQueue);
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
			Utils.sendMessage(null, jmsContext, request, jmsProducer, requestsQueue, null, responseQueue);
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
		uploadQueue = jmsContext.createQueue(Constants.QUEUE_FROM_USER_PREFIX + userId);
		messageQueue = jmsContext.createQueue(Constants.QUEUE_GET_USER_PREFIX + userId);
	}
	
	private void follow(String followedUsername) {
		if(logged) {
			ArrayList<String> params = new ArrayList<String>();
			params.add(followedUsername);
			RequestMessage request = new RequestMessage(userId, Constants.REQUEST_FOLLOW, params);
			try {
				Utils.sendMessage(null, jmsContext, request, jmsProducer, requestsQueue, null, responseQueue);
				Message msg = responseConsumer.receive();
				ResponseMessage response = msg.getBody(ResponseMessage.class);
				if(response.getResponseCode() == Constants.RESPONSE_OK) {
					System.out.println("Response: OK.");
				} else
					System.out.println("Response: ERROR: " + response.getResponseInfo());
			} catch (JMSException e) {
				e.printStackTrace();
			}
		} else
			System.out.println("Error: you are not logged in. First login to the system.");
	}
	
	private void post(String text, String imageFilePath) {
		if(logged) {
			try {
				String extension = null;
				byte[] image = null;
				if(imageFilePath != null) {
					extension = imageFilePath.substring(imageFilePath.lastIndexOf(".") + 1);
					RandomAccessFile imageFile = new RandomAccessFile(imageFilePath, "r");
					image = new byte[(int) imageFile.length()];
					imageFile.readFully(image);
					imageFile.close();
				}
				GeneralMessage message = new GeneralMessage(userId, text, extension, image);
				Utils.sendMessage(null, jmsContext, message, jmsProducer, uploadQueue, null, null);
			} catch (FileNotFoundException e) {
				System.out.println("Error: file " + imageFilePath + " not found.");
			} catch (IOException e) {
				e.printStackTrace();
			} catch (JMSException e) {
				e.printStackTrace();
			}
		} else
			System.out.println("Error: you are not logged in. First login to the system.");
	}

	private void manageGetNew() {
		if(logged) {
			ArrayList<String> params = new ArrayList<String>();
			params.add(Constants.REQUEST_GET_ALL_NEW + "");
			RequestMessage request = new RequestMessage(userId, Constants.REQUEST_GET, params);
			try {
				Utils.sendMessage(null, jmsContext, request, jmsProducer, requestsQueue, null, responseQueue);
				Message msg = responseConsumer.receive();
				ResponseMessage response = msg.getBody(ResponseMessage.class);
				if(response.getResponseCode() == Constants.RESPONSE_OK) {
					System.out.println("Response: OK. Getting new messages...");
					getMessagesFromQueue(1);
				} else if(response.getResponseCode() == Constants.RESPONSE_WARNING)
					System.out.println("Response: WARNING: " + response.getResponseInfo());
				else
					System.out.println("Response: ERROR: " + response.getResponseInfo());
			} catch (JMSException e) {
				e.printStackTrace();
			}
		} else
			System.out.println("Error: you are not logged in. First login to the system.");
	}

	private void manageGetInterval(long i, long j) {
		if(logged) {
			ArrayList<String> params = new ArrayList<String>();
			params.add(Constants.REQUEST_GET_FROM_I_TO_J + "");
			params.add(i + "");
			params.add(j + "");
			RequestMessage request = new RequestMessage(userId, Constants.REQUEST_GET, params);
			try {
				Utils.sendMessage(null, jmsContext, request, jmsProducer, requestsQueue, null, responseQueue);
				Message msg = responseConsumer.receive();
				ResponseMessage response = msg.getBody(ResponseMessage.class);
				if(response.getResponseCode() == Constants.RESPONSE_OK) {
					System.out.println("Response: OK. Getting messages...");
					getMessagesFromQueue(1);
				} else if(response.getResponseCode() == Constants.RESPONSE_WARNING)
					System.out.println("Response: WARNING: " + response.getResponseInfo());
				else
					System.out.println("Response: ERROR: " + response.getResponseInfo());
			} catch (JMSException e) {
				e.printStackTrace();
			}
		} else
			System.out.println("Error: you are not logged in. First login to the system.");
	}

	private void manageGetImage(String messageId) {
		if(logged) {
			ArrayList<String> params = new ArrayList<String>();
			params.add(Constants.REQUEST_GET_IMAGE + "");
			params.add(messageId);
			RequestMessage request = new RequestMessage(userId, Constants.REQUEST_GET, params);
			try {
				Utils.sendMessage(null, jmsContext, request, jmsProducer, requestsQueue, null, responseQueue);
				Message msg = responseConsumer.receive();
				ResponseMessage response = msg.getBody(ResponseMessage.class);
				if(response.getResponseCode() == Constants.RESPONSE_OK) {
					System.out.println("Response: OK. Getting image...");
					getMessagesFromQueue(2);
				} else
					System.out.println("Response: ERROR: " + response.getResponseInfo());
			} catch (JMSException e) {
				e.printStackTrace();
			}
		} else
			System.out.println("Error: you are not logged in. First login to the system.");
	}

	private void getMessagesFromQueue(int type) {
		JMSConsumer messageConsumer = jmsContext.createConsumer(messageQueue);
		if(type == 1) {
			//get general messages
			GeneralMessage message;
			Message msg = messageConsumer.receive();
			while(msg != null) {
				try {
					message = msg.getBody(GeneralMessage.class);
					switch(message.getType()) {
					case Constants.MESSAGE_ONLY_TEXT:
						displayTextMessage(msg.getStringProperty(Constants.PROPERTY_MESSAGE_ID), message.getText());
						break;
						
					case Constants.MESSAGE_ONLY_IMAGE:
						displayImageMessage(message.getImage(), message.getImageExtension());
						break;
						
					case Constants.MESSAGE_TEXT_AND_IMAGE:
						//TODO
						System.out.println("IMG_ID "+msg.getStringProperty(Constants.PROPERTY_IMAGE_MESSAGE_ID));
						displayTextMessage(msg.getStringProperty(Constants.PROPERTY_MESSAGE_ID), message.getText());
						displayImageMessage(message.getImage(), message.getImageExtension());
						break;
					}
				} catch (JMSException e) {
					e.printStackTrace();
				}
				msg = messageConsumer.receiveNoWait();
			}
		} else {
			//get image message
			ImageMessage message;
			Message msg = messageConsumer.receive();
			try {
				message = msg.getBody(ImageMessage.class);
				displayImageMessage(message.getImage(), message.getExtension());
			} catch (JMSException e) {
				e.printStackTrace();
			}
		}
	}

	private void displayTextMessage(String messageId, String message) {
		System.out.println("MSG_ID: " + messageId);
		System.out.println("MESSAGE: " + message);
		System.out.println("");
	}

	private void displayImageMessage(byte[] image, String extension) {
		try {
			BufferedImage img = ImageIO.read(new ByteArrayInputStream(image));
			JFrame window = new JFrame();
			@SuppressWarnings("serial")
			JPanel panel = new JPanel() {
				 @Override
				 protected void paintComponent(Graphics g) {
					 super.paintComponent(g);
				     g.drawImage(img, 0, 0, img.getWidth(), img.getHeight(), this);           
				 }
			};
			window.setContentPane(panel);
			window.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			window.setSize(img.getWidth(), img.getHeight());
			window.setResizable(false);
			window.setVisible(true);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void displayUserInfo() {
		if(logged) {
			System.out.println("You are logged as id: " + userId + ", username: " + username);
		} else
			System.out.println("Error: you are not logged in. First login to the system.");
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
				System.out.println("Commands:\nregister\nlogin\nfollow\npost\nget\nmy identity");
				command = bufferedReader.readLine();
				if(command.equalsIgnoreCase("exit")) {
					client.close();
					System.exit(0);
				} else if(command.equalsIgnoreCase("register")) {
					String username, password;
					System.out.println(" Username:");
					username = bufferedReader.readLine();
					System.out.println(" Password:");
					password = bufferedReader.readLine();
					client.register(username, password);
				} else if(command.equalsIgnoreCase("login")) {
					String username, password;
					System.out.println(" Username:");
					username = bufferedReader.readLine();
					System.out.println(" Password:");
					password = bufferedReader.readLine();
					client.login(username, password);
				} else if(command.equalsIgnoreCase("follow")) {
					String followedUsername;
					System.out.println(" Followed user username:");
					followedUsername = bufferedReader.readLine();
					client.follow(followedUsername);
				} else if(command.equalsIgnoreCase("post")) {
					String text, imageFilePath;
					System.out.println(" Text (or \"null\"):");
					text = bufferedReader.readLine();
					System.out.println(" Image file path (or \"null\"):");
					imageFilePath = bufferedReader.readLine();
					if(text.equals("null"))
						text = null;
					if(imageFilePath.equals("null"))
						imageFilePath = null;
					client.post(text, imageFilePath);
				} else if(command.equalsIgnoreCase("get")) {
					String getType;
					System.out.println(" What to get:\n new - get new messages\n interval - messages in a time interval\n"
							+ " image - full size image of a message");
					getType = bufferedReader.readLine();
					if(getType.equalsIgnoreCase("new")) {
						client.manageGetNew();
					} else if(getType.equalsIgnoreCase("interval")) {
						String i, j;
						System.out.println(" Get message from:");
						i = bufferedReader.readLine();
						System.out.println(" to:");
						j = bufferedReader.readLine();
						client.manageGetInterval((new MyTime(i)).getTime(), (new MyTime(j)).getTime());
					} else if(getType.equalsIgnoreCase("image")) {
						String messageId;
						System.out.println(" Get image of message with id:");
						messageId = bufferedReader.readLine();
						client.manageGetImage(messageId);
					} else
						System.out.println(" Wrong choice.");
				} else if(command.equalsIgnoreCase("my identity")) {
					client.displayUserInfo();
				} else {
					System.out.println("Invalid command.");
				}
			} catch (IOException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		}
	}
}
