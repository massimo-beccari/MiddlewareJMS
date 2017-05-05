package it.polimi.middleware.jms;

import java.util.ArrayList;
import java.util.Enumeration;

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

import it.polimi.middleware.jms.model.IdDistributor;
import it.polimi.middleware.jms.model.message.GeneralMessage;
import it.polimi.middleware.jms.model.message.ImageMessage;
import it.polimi.middleware.jms.model.message.MessageProperty;

public class UserQueueDaemon implements Runnable {
	private int userId;
	@SuppressWarnings("unused")
	private IdDistributor messageIdDistributor;
	private Context initialContext;
	private JMSContext jmsContext;
	private Queue userMessagesQueue;
	private Queue userImagesQueue;
	private JMSProducer jmsProducer;
	private ArrayList<JMSConsumer> messageConsumers;
	private ArrayList<JMSConsumer> imageConsumers;
	private boolean iWait;

	public UserQueueDaemon(int userId, IdDistributor messageIdDistributor) {
		this.userId = userId;
		this.messageIdDistributor = messageIdDistributor;
		messageConsumers = new ArrayList<JMSConsumer>();
		imageConsumers = new ArrayList<JMSConsumer>();
		iWait = false;
	}
	
	/*
	 * SETUP METHODS
	 */

	private void setup() throws NamingException {
		createContexts();
		userMessagesQueue = jmsContext.createQueue(Constants.QUEUE_TO_USER_MESSAGES_PREFIX + userId);
		userImagesQueue = jmsContext.createQueue(Constants.QUEUE_TO_USER_IMAGES_PREFIX + userId);
		jmsProducer = jmsContext.createProducer();
	}
	
	private void createContexts() throws NamingException {
		initialContext = Utils.getContext();
		jmsContext = ((ConnectionFactory) initialContext.lookup("java:comp/DefaultJMSConnectionFactory")).createContext();
		jmsContext.setClientID("DAEMON_" + userId);
	}
	
	/*
	 * DAEMON METHODS
	 */
	
	@Override
	public void run() {
		try {
			setup();
			System.out.println("QD" + userId + ": queue daemon started.");
			while(true) {
				synchronized(this) {
					try {
						//if new subscription, wake server and wait it adds new subscription
						while(iWait) {
							notify();
							wait();
						}
						//check messages
						for(JMSConsumer consumer : messageConsumers) {
							Message message = consumer.receiveNoWait();
							if(message != null) {
								System.out.println("QD" + userId + ": message received.");
								onMessage(message);
								System.out.println("QD" + userId + ": message forwarded.");
							}
						}
						//check images
						for(JMSConsumer consumer : imageConsumers) {
							Message message = consumer.receiveNoWait();
							if(message != null) {
								System.out.println("QD" + userId + ": message received.");
								onImage(message);
								System.out.println("QD" + userId + ": message forwarded.");
							}
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		} catch (NamingException e) {
			e.printStackTrace();
		}
	}

	private void onMessage(Message message) {
		try {
			//set properties
			ArrayList<MessageProperty> messageProperties = new ArrayList<MessageProperty>();
			@SuppressWarnings("unchecked")
			Enumeration<String> propertyNames = message.getPropertyNames();
			while(propertyNames.hasMoreElements()) {
				String propertyName = propertyNames.nextElement();
				messageProperties.add(new MessageProperty(propertyName, message.getStringProperty(propertyName)));
			}
			//send message
			GeneralMessage msg = message.getBody(GeneralMessage.class);//forward message to queue
			Utils.sendMessage(null, jmsContext, msg, jmsProducer, userMessagesQueue, messageProperties, null);
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}
	
	private void onImage(Message message) {
		try {
			//set properties
			ArrayList<MessageProperty> messageProperties = new ArrayList<MessageProperty>();
			@SuppressWarnings("unchecked")
			Enumeration<String> propertyNames = message.getPropertyNames();
			while(propertyNames.hasMoreElements()) {
				String propertyName = propertyNames.nextElement();
				messageProperties.add(new MessageProperty(propertyName, message.getStringProperty(propertyName)));
			}
			//send message
			ImageMessage msg = message.getBody(ImageMessage.class);//forward message to queue
			Utils.sendMessage(null, jmsContext, msg, jmsProducer, userImagesQueue, messageProperties, null);
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}
	
	public void addSubscription(int followedUserId) throws NamingException {
		Topic newMessageTopic, newImageTopic;
		newMessageTopic = jmsContext.createTopic(Constants.TOPIC_USER_MESSAGES_PREFIX + followedUserId);
		newImageTopic = jmsContext.createTopic(Constants.TOPIC_USER_IMAGES_PREFIX + followedUserId);
		JMSConsumer newMessageConsumer, newImageConsumer;
		newMessageConsumer = jmsContext.createDurableConsumer(newMessageTopic, Constants.TOPIC_SUBSCRIPTION_MESSAGES_PREFIX + userId + "_" + followedUserId);
		newImageConsumer = jmsContext.createDurableConsumer(newImageTopic, Constants.TOPIC_SUBSCRIPTION_IMAGES_PREFIX + userId + "_" + followedUserId);
		messageConsumers.add(newMessageConsumer);
		imageConsumers.add(newImageConsumer);
		System.out.println("QD" + userId + ": subscription added to user " + followedUserId + ".");
	}
	
	public void setNewSubscription(boolean newSubscription) {
		this.iWait = newSubscription;
	}
}
