package it.polimi.middleware.jms.server;

import java.util.ArrayList;

import it.polimi.middleware.jms.Constants;
import it.polimi.middleware.jms.Utils;
import it.polimi.middleware.jms.server.model.IdDistributor;
import it.polimi.middleware.jms.server.model.User;
import it.polimi.middleware.jms.server.model.message.GeneralMessage;
import it.polimi.middleware.jms.server.model.message.ImageMessage;
import it.polimi.middleware.jms.server.model.message.MessageProperty;

import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.Topic;
import javax.naming.NamingException;

public class UserHandler implements Runnable {
	private boolean isConnected;
	private int userId;
	private User user;
	private IdDistributor messageIdDistributor;
	private JMSContext jmsContext;
	private Queue queueFromUser;
	private Topic userMessageTopic;
	private Topic userImageTopic;
	private JMSConsumer jmsConsumer;
	private JMSProducer jmsProducer;
	//private long lastInteractionTime;
	
	public UserHandler(JMSContext jmsContext, User user, IdDistributor messageIdDistributor) {
		this.jmsContext = jmsContext;
		this.userId = user.getUserId();
		this.user = user;
		this.messageIdDistributor = messageIdDistributor;
		isConnected = true;
		//lastInteractionTime = System.currentTimeMillis();
	}
	
	/*
	 * SETUP METHODS
	 */

	private void setup() throws NamingException {
		createUserDestinations();
		jmsProducer = jmsContext.createProducer();
	}
	
	private void createUserDestinations() {
		userMessageTopic = jmsContext.createTopic(Constants.TOPIC_USER_MESSAGES_PREFIX + userId);
		userImageTopic = jmsContext.createTopic(Constants.TOPIC_USER_IMAGES_PREFIX + userId);
		queueFromUser = jmsContext.createQueue(Constants.QUEUE_FROM_USER_PREFIX + userId);
		jmsConsumer = jmsContext.createConsumer(queueFromUser);
	}
	
	/*
	 * HANDLER METHODS
	 */
	
	@Override
	public void run() {
		try {
			setup();
			System.out.println("UH" + userId + ": user handler started.");
			while(isConnected /*&& ((System.currentTimeMillis() - lastInteractionTime) <= Constants.SERVER_LOGIN_TIMEOUT_INTERVAL)*/) {
				synchronized(this) {
					Message msg  = jmsConsumer.receiveNoWait();
					if(msg != null) {
						System.out.println("UH" + userId + ": message received.");
						try {
							GeneralMessage message = msg.getBody(GeneralMessage.class);
							if(message.getType() == Constants.MESSAGE_ONLY_TEXT) {
								processTextMessage(message, msg.getJMSTimestamp());
								System.out.println("UH" + userId + ": text message processed.");
							} else {
								processMessageWithImage(message, msg.getJMSTimestamp());
								System.out.println("UH" + userId + ": text/image message processed.");
							}
						} catch (JMSException e) {
							e.printStackTrace();
						}
						//lastInteractionTime = System.currentTimeMillis();
					}
					Thread.sleep(100);
				}
			}
			jmsConsumer.close();
			user.setLogged(false);
			System.out.println("UH" + userId + ": user handler terminated.");
		} catch (NamingException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void processTextMessage(GeneralMessage message, long timestamp) {
		ArrayList<MessageProperty> properties = new ArrayList<MessageProperty>();
		properties.add(new MessageProperty(Constants.PROPERTY_NAME_USER_ID, "" + userId));
		properties.add(new MessageProperty(Constants.PROPERTY_NAME_MESSAGE_TIMESTAMP, "" + timestamp));
		try {
			Utils.sendMessage(messageIdDistributor, jmsContext, message, jmsProducer, userMessageTopic, properties, null);
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}
	
	private void processMessageWithImage(GeneralMessage message, long timestamp) {
		byte[] thumbnail = Utils.createImageThumbnail(message.getImage(), message.getImageExtension());
		GeneralMessage messageWithThumbnail = new GeneralMessage(userId, message.getUsername(), message.getText(), message.getImageExtension(), thumbnail);
		ImageMessage imageMessage = new ImageMessage(userId, message.getImageExtension(), message.getImage());
		//create properties for messages
		ArrayList<MessageProperty> messageProperties = new ArrayList<MessageProperty>();
		messageProperties.add(new MessageProperty(Constants.PROPERTY_NAME_USER_ID, "" + userId));
		messageProperties.add(new MessageProperty(Constants.PROPERTY_NAME_MESSAGE_TIMESTAMP, "" + timestamp));
		ArrayList<MessageProperty> imageProperties = new ArrayList<MessageProperty>();
		imageProperties.add(new MessageProperty(Constants.PROPERTY_NAME_USER_ID, "" + userId));
		imageProperties.add(new MessageProperty(Constants.PROPERTY_NAME_MESSAGE_TIMESTAMP, "" + timestamp));
		//send messages
		try {
			String imageMessageId = Utils.sendMessage(messageIdDistributor, jmsContext, imageMessage, jmsProducer, userImageTopic, imageProperties, null);
			messageProperties.add(new MessageProperty(Constants.PROPERTY_NAME_IMAGE_MESSAGE_ID, imageMessageId));
			Utils.sendMessage(messageIdDistributor, jmsContext, messageWithThumbnail, jmsProducer, userMessageTopic, messageProperties, null);
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}

	public synchronized void setConnected(boolean isConnected) {
		this.isConnected = isConnected;
	}
}
