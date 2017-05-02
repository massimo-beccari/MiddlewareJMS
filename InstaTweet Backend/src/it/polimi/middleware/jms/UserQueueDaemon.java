package it.polimi.middleware.jms;

import java.util.ArrayList;
import java.util.Enumeration;

import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Queue;

import it.polimi.middleware.jms.model.message.GeneralMessage;
import it.polimi.middleware.jms.model.MessageProperty;

public class UserQueueDaemon implements MessageListener {
	private JMSContext jmsContext;
	private Queue userMessagesQueue;
	private JMSProducer jmsProducer;

	public UserQueueDaemon(JMSContext jmsContext, Queue userMessagesQueue, JMSProducer jmsProducer) {
		this.jmsContext = jmsContext;
		this.userMessagesQueue = userMessagesQueue;
		this.jmsProducer = jmsProducer;
	}

	@Override
	public void onMessage(Message message) {
		try {
			GeneralMessage msg = message.getBody(GeneralMessage.class);
			//set properties
			@SuppressWarnings("unchecked")
			Enumeration<String> propertyNames = message.getPropertyNames();
			ArrayList<MessageProperty> messageProperties = new ArrayList<MessageProperty>();
			while(propertyNames.hasMoreElements()) {
				String propertyName = propertyNames.nextElement();
				messageProperties.add(new MessageProperty(propertyName, message.getStringProperty(propertyName)));
			}
			//forward message to queue
			Utils.sendMessage(jmsContext, msg, jmsProducer, userMessagesQueue, messageProperties);
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}
}
