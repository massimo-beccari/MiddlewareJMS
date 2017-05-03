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

import it.polimi.middleware.jms.model.message.GeneralMessage;
import it.polimi.middleware.jms.model.message.ImageMessage;
import it.polimi.middleware.jms.model.message.MessageInterface;
import it.polimi.middleware.jms.model.message.MessageProperty;

public class UserQueueDaemon implements Runnable {
	private int userId;
	private Context initialContext;
	private JMSContext jmsContext;
	private Queue userMessagesQueue;
	private Queue userImagesQueue;
	private JMSProducer jmsProducer;
	private ArrayList<JMSConsumer> subscriptionsConsumers;
	private boolean newSubscription;

	public UserQueueDaemon(int userId) {
		this.userId = userId;
		subscriptionsConsumers = new ArrayList<JMSConsumer>();
		newSubscription = false;
		try {
			setup();
		} catch (NamingException e) {
			e.printStackTrace();
		}
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
		while(true) {
			synchronized(this) {
				try {
					//if new subscription, wake server and wait it adds new subscription
					while(newSubscription) {
						notify();
						wait();
					}
					//check messages
					for(JMSConsumer consumer : subscriptionsConsumers) {
						Message message = consumer.receiveNoWait();
						if(message != null)
							onMessage(message);
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void onMessage(Message message) {
		MessageInterface msg;
		ArrayList<MessageProperty> messageProperties = new ArrayList<MessageProperty>();
		//set properties
		try {
			@SuppressWarnings("unchecked")
			Enumeration<String> propertyNames = message.getPropertyNames();
			while(propertyNames.hasMoreElements()) {
				String propertyName = propertyNames.nextElement();
				messageProperties.add(new MessageProperty(propertyName, message.getStringProperty(propertyName)));
			}
		} catch (JMSException e) {
			e.printStackTrace();
		}
		//send message
		try {
			msg = message.getBody(GeneralMessage.class);//forward message to queue
			Utils.sendMessage(jmsContext, msg, jmsProducer, userMessagesQueue, messageProperties, null);
		} catch (JMSException e) {
			//if this exception is thrown, the message is an image message
			try {
				msg = message.getBody(ImageMessage.class);//forward message to queue
				Utils.sendMessage(jmsContext, msg, jmsProducer, userImagesQueue, messageProperties, null);
			} catch (JMSException e1) {
				e1.printStackTrace();
			}
		}
	}
	
	public void addSubscription(int followedUserId) throws NamingException {
		Topic newMessageTopic, newImageTopic;
		newMessageTopic = (Topic) initialContext.lookup(Constants.TOPIC_USER_MESSAGES_PREFIX + followedUserId);
		newImageTopic = (Topic) initialContext.lookup(Constants.TOPIC_USER_IMAGES_PREFIX + followedUserId);
		JMSConsumer newMessageConsumer, newImageConsumer;
		newMessageConsumer = jmsContext.createDurableConsumer(newMessageTopic, Constants.TOPIC_SUBSCRIPTION_MESSAGES_PREFIX + userId + "_" + followedUserId);
		newImageConsumer = jmsContext.createDurableConsumer(newImageTopic, Constants.TOPIC_SUBSCRIPTION_IMAGES_PREFIX + userId + "_" + followedUserId);
		subscriptionsConsumers.add(newMessageConsumer);
		subscriptionsConsumers.add(newImageConsumer);
	}
	
	public void setNewSubscription(boolean newSubscription) {
		this.newSubscription = newSubscription;
	}
}
