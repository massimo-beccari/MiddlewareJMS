package it.polimi.middleware.jms;

import java.util.ArrayList;

import it.polimi.middleware.jms.model.IdDistributor;
import it.polimi.middleware.jms.model.message.GeneralMessage;
import it.polimi.middleware.jms.model.message.ImageMessage;
import it.polimi.middleware.jms.model.message.MessageProperty;

import javax.jms.ConnectionFactory;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.Topic;
import javax.naming.Context;
import javax.naming.NamingException;

public class UserHandler implements Runnable {
	private boolean isConnected;
	private int userId;
	private IdDistributor messageIdDistributor;
	private Context initialContext;
	private JMSContext jmsContext;
	private Queue queueFromUser;
	private Topic userMessageTopic;
	private Topic userImageTopic;
	private JMSConsumer jmsConsumer;
	private JMSProducer jmsProducer;
	
	public UserHandler(int userId, IdDistributor messageIdDistributor) {
		this.userId = userId;
		this.messageIdDistributor = messageIdDistributor;
		isConnected = true;
	}
	
	/*
	 * SETUP METHODS
	 */

	private void setup() throws NamingException {
		createContexts();
		createUserDestinations();
		jmsProducer = jmsContext.createProducer();
	}
	
	private void createContexts() throws NamingException {
		initialContext = Utils.getContext();
		jmsContext = ((ConnectionFactory) initialContext.lookup("java:comp/DefaultJMSConnectionFactory")).createContext();
		jmsContext.setClientID("HANDLER_" + userId);
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
			while(isConnected) {
				Message msg  = jmsConsumer.receive();
				System.out.println("UH" + userId + ": message received.");
				try {
					GeneralMessage message = msg.getBody(GeneralMessage.class);
					if(message.getType() == Constants.MESSAGE_ONLY_TEXT) {
						processTextMessage(message);
						System.out.println("UH" + userId + ": text message processed.");
					} else {
						processMessageWithImage(message);
						System.out.println("UH" + userId + ": text/image message processed.");
					}
				} catch (JMSException e) {
					e.printStackTrace();
				}
			}
		} catch (NamingException e) {
			e.printStackTrace();
		}
	}

	private void processTextMessage(GeneralMessage message) {
		ArrayList<MessageProperty> properties = new ArrayList<MessageProperty>();
		properties.add(new MessageProperty(Constants.PROPERTY_USER_ID, "" + userId));
		try {
			Utils.sendMessage(messageIdDistributor, jmsContext, message, jmsProducer, userMessageTopic, properties, null);
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}
	
	private void processMessageWithImage(GeneralMessage message) {
		byte[] thumbnail = Utils.createImageThumbnail(message.getImage(), message.getImageExtension());
		GeneralMessage messageWithThumbnail = new GeneralMessage(userId, message.getText(), message.getImageExtension(), thumbnail);
		ImageMessage imageMessage = new ImageMessage(userId, message.getImageExtension(), message.getImage());
		//create properties for messages
		ArrayList<MessageProperty> messageProperties = new ArrayList<MessageProperty>();
		messageProperties.add(new MessageProperty(Constants.PROPERTY_USER_ID, "" + userId));
		ArrayList<MessageProperty> imageProperties = new ArrayList<MessageProperty>();
		imageProperties.add(new MessageProperty(Constants.PROPERTY_USER_ID, "" + userId));
		//send messages
		try {
			String imageMessageId = Utils.sendMessage(messageIdDistributor, jmsContext, imageMessage, jmsProducer, userImageTopic, imageProperties, null);
			messageProperties.add(new MessageProperty(Constants.PROPERTY_IMAGE_MESSAGE_ID, imageMessageId));
			Utils.sendMessage(messageIdDistributor, jmsContext, messageWithThumbnail, jmsProducer, userMessageTopic, messageProperties, null);
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}
}
