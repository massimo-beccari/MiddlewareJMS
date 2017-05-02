package it.polimi.middleware.jms;

import it.polimi.middleware.jms.model.User;
import it.polimi.middleware.jms.model.message.GeneralMessage;

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
	private User user;
	private Context initialContext;
	private JMSContext jmsContext;
	private Queue userIncomingQueue;
	private Queue userReceiveQueue;
	private Topic userTopic;
	private JMSConsumer jmsConsumer;
	private JMSProducer jmsProducer;
	
	public UserHandler(User user) {
		this.user = user;
		try {
			setup();
		} catch (NamingException e) {
			e.printStackTrace();
		}
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
		jmsContext.setClientID(user.getUserId() + "");
	}
	
	private void createUserDestinations() {
		userTopic = jmsContext.createTopic(Constants.TOPIC_USER_PREFIX + user.getUserId());
		userIncomingQueue = jmsContext.createQueue(Constants.QUEUE_FROM_USER_PREFIX + user.getUserId());
		userReceiveQueue = jmsContext.createQueue(Constants.QUEUE_TO_USER_PREFIX + user.getUserId());
		jmsConsumer = jmsContext.createConsumer(userIncomingQueue);
	}
	
	/*
	 * HANDLER METHODS
	 */
	
	@Override
	public void run() {
		while(isConnected) {
			Message message = jmsConsumer.receive();
			//TODO
		}
	}
	
	private void postMessage(GeneralMessage message) {
		//ArrayList<MessageProperty> messageProperties = new ArrayList<MessageProperty>();
		try {
			Utils.sendMessage(jmsContext, message, jmsProducer, userTopic, null);
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}

	private void subscribe(int followedUserId) throws NamingException {
		Topic subscribeTopic = (Topic) initialContext.lookup(Constants.TOPIC_USER_PREFIX + followedUserId);
		String subscriptionName = Constants.TOPIC_SUBSCRIPTION_PREFIX + "_" + user.getUserId() + "_" + followedUserId;
	}
	
	private void unsubscribe(int followedUserId) {
		jmsContext.unsubscribe(Constants.TOPIC_SUBSCRIPTION_PREFIX + "_" + user.getUserId() + "_" + followedUserId);
	}
}
