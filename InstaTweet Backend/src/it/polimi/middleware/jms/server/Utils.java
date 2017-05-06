package it.polimi.middleware.jms.server;

import it.polimi.middleware.jms.server.model.IdDistributor;
import it.polimi.middleware.jms.server.model.message.MessageInterface;
import it.polimi.middleware.jms.server.model.message.MessageProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.jms.Destination;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

public class Utils {
	
	public static Context getContext() throws NamingException {
		return getContext("localhost:3700");
	}

	public static Context getContext(String url) throws NamingException {
		Properties props = new Properties();
		props.setProperty("java.naming.factory.initial", "com.sun.enterprise.naming.SerialInitContextFactory");
		props.setProperty("java.naming.factory.url.pkgs", "com.sun.enterprise.naming");
		props.setProperty("java.naming.provider.url", "iiop://" + url);
		return new InitialContext(props);
	}
	
	public static String sendMessage(IdDistributor messageIdDistributor, JMSContext jmsContext, MessageInterface message, JMSProducer jmsProducer, Destination destination, List<MessageProperty> properties, Queue responseQueue) throws JMSException {
		ObjectMessage objMessage = jmsContext.createObjectMessage();
		objMessage.setObject(message);
		String messageId = null;
		if(messageIdDistributor != null) {
			int mId = messageIdDistributor.getNewId();
			messageId = Constants.PROPERTY_VALUE_MESSAGE_ID_PREFIX + mId;
			if(properties == null)
				properties = new ArrayList<MessageProperty>();
			properties.add(new MessageProperty(Constants.PROPERTY_NAME_MESSAGE_ID, messageId));
		}
		if(properties != null) {
			for(MessageProperty mp : properties)
				objMessage.setStringProperty(mp.getPropertyName(), mp.getPropertyValue());
		}
		
		//DEBUG
		if(properties != null) {
			System.out.print("MESSAGE PROPERTIES: ");
			for(MessageProperty mp : properties)
				System.out.print(mp.toString() + " ");
			System.out.println("");
		}
		//END_DEBUG
		
		if(responseQueue != null)
			objMessage.setJMSReplyTo(responseQueue);
		jmsProducer.send(destination, objMessage);
			
		return messageId;
	}
	
	public static byte[] createImageThumbnail(byte[] image, String extension) {
		byte[] thumbnail = image.clone();
		//TODO
		return thumbnail;
	}
}
