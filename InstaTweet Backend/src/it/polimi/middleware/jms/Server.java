package it.polimi.middleware.jms;

import java.util.HashMap;

import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSProducer;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.naming.Context;
import javax.naming.NamingException;

import it.polimi.middleware.jms.model.IdDistributor;
import it.polimi.middleware.jms.model.User;

public class Server implements MessageListener {
	private IdDistributor idDistributor;
	private HashMap<Integer, User> usersMap;
	private Context initialContext;
	private JMSContext jmsContext;
	private JMSProducer jmsProducer;
	private Queue requestsQueue;
	
	public Server() {
		idDistributor = new IdDistributor();
		usersMap = new HashMap<Integer, User>();
	}
	
	public void startServer() throws NamingException {
		createContexts();
		requestsQueue = jmsContext.createQueue(Constants.QUEUE_REQUESTS_NAME);
		jmsProducer = jmsContext.createProducer();
	}
	
	private void createContexts() throws NamingException {
		initialContext = Utils.getContext();
		jmsContext = ((ConnectionFactory) initialContext.lookup("java:comp/DefaultJMSConnectionFactory")).createContext();
		jmsContext.setClientID("0");
	}

	public static void main(String[] args) {
		Server server = new Server();
		try {
			server.startServer();
		} catch (NamingException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onMessage(Message message) {
		
	}
}
