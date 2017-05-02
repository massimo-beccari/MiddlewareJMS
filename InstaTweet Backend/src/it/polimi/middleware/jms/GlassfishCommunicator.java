package it.polimi.middleware.jms;

import it.polimi.middleware.jms.model.message.GeneralMessage;

import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.Topic;
import javax.naming.Context;
import javax.naming.NamingException;

public class GlassfishCommunicator {
	private int userId;
	private Context initialContext;
	private JMSContext jmsContext;
	private Topic userTopic;
	private JMSProducer jmsProducer;
	
	public GlassfishCommunicator(int userId) {
		this.userId = userId;
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
		createUserTopic();
		jmsProducer = jmsContext.createProducer();
	}
	
	private void createContexts() throws NamingException {
		initialContext = Utils.getContext();
		jmsContext = ((ConnectionFactory) initialContext.lookup("java:comp/DefaultJMSConnectionFactory")).createContext();
		jmsContext.setClientID(userId + "");
	}
	
	private void createUserTopic() {
		userTopic = jmsContext.createTopic(Constants.TOPIC_USER_PREFIX + userId);
	}
	
	/*
	 * PUBLIC METHODS
	 */
	
	public void postMessage(GeneralMessage message) {
		//ArrayList<MessageProperty> messageProperties = new ArrayList<MessageProperty>();
		try {
			Utils.sendMessage(jmsContext, message, jmsProducer, userTopic, null);
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}

	public void subscribe(int followedUserId) throws NamingException {
		Topic subscribeTopic = (Topic) initialContext.lookup(Constants.TOPIC_USER_PREFIX + followedUserId);
		String subscriptionName = Constants.TOPIC_SUBSCRIPTION_PREFIX + "_" + userId + "_" + followedUserId;
	}
	
	public void unsubscribe(int followedUserId) {
		jmsContext.unsubscribe(Constants.TOPIC_SUBSCRIPTION_PREFIX + "_" + userId + "_" + followedUserId);
	}
}
