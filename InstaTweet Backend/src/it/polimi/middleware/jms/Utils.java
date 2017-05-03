package it.polimi.middleware.jms;

import it.polimi.middleware.jms.model.message.MessageInterface;
import it.polimi.middleware.jms.model.message.MessageProperty;

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
		Properties props = new Properties();
		props.setProperty("java.naming.factory.initial", "com.sun.enterprise.naming.SerialInitContextFactory");
		props.setProperty("java.naming.factory.url.pkgs", "com.sun.enterprise.naming");
		props.setProperty("java.naming.provider.url", "iiop://localhost:3700");
		return new InitialContext(props);
	}
	
	public static String sendMessage(JMSContext jmsContext, MessageInterface message, JMSProducer jmsProducer, Destination destination, List<MessageProperty> properties, Queue responseQueue) throws JMSException {
		ObjectMessage objMessage = jmsContext.createObjectMessage();
		objMessage.setObject(message);
		if(properties != null)
			for(MessageProperty mp : properties)
				objMessage.setStringProperty(mp.getPropertyName(), mp.getPropertyValue());
		if(responseQueue != null)
			objMessage.setJMSReplyTo(responseQueue);
		jmsProducer.send(destination, objMessage);
		return objMessage.getJMSMessageID();
	}
	
	public static byte[] createImageThumbnail(byte[] image, String extension) {
		byte[] thumbnail = image.clone();
		//TODO
		return thumbnail;
	}
}
