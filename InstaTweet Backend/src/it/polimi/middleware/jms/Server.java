package it.polimi.middleware.jms;

import it.polimi.middleware.jms.model.IdDistributor;
import it.polimi.middleware.jms.model.User;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.naming.Context;
import javax.naming.NamingException;

public class Server {
	private int currentNumberOfServerInstances;
	private IdDistributor userIdDistributor;
	private HashMap<Integer, User> usersMapId;
	private HashMap<String, User> usersMapUsername;
	private HashMap<Integer, UserQueueDaemon> daemonsMap;
	private ExecutorService executor;
	private Context initialContext;
	private JMSContext jmsContext;
	private Queue requestsQueue;
	private QueueBrowser browser;
	private ArrayList<ServerInstance> serverInstances;
	
	private Server() {
		currentNumberOfServerInstances = 0;
		userIdDistributor = new IdDistributor(1);
		usersMapId = new HashMap<Integer, User>();
		usersMapUsername = new HashMap<String, User>();
		daemonsMap = new HashMap<Integer, UserQueueDaemon>();
		executor = Executors.newCachedThreadPool();
		serverInstances = new ArrayList<ServerInstance>();
	}
	
	private void startServer() throws NamingException {
		createContexts();
		requestsQueue = (Queue) initialContext.lookup(Constants.QUEUE_REQUESTS_NAME);
		browser = jmsContext.createBrowser(requestsQueue);
		System.out.println("SRV: server started.");
		startNewServerInstance();
		loop();
	}
	
	private void createContexts() throws NamingException {
		initialContext = Utils.getContext();
		jmsContext = ((ConnectionFactory) initialContext.lookup("java:comp/DefaultJMSConnectionFactory")).createContext();
		jmsContext.setClientID("SERVER");
	}
	
	private void startNewServerInstance() {
		ServerInstance firstInstance = new ServerInstance(currentNumberOfServerInstances,
				userIdDistributor, usersMapId, usersMapUsername, daemonsMap);
		currentNumberOfServerInstances++;
		serverInstances.add(firstInstance);
		executor.submit(firstInstance);
		System.out.println("SRV: new server instance launched. (" + currentNumberOfServerInstances + ")");
	}
	
	private void loop() {
		while(true) {
			if(getNumberOfMessages() > Constants.SERVER_MAX_LOAD_THRESOLD)
				startNewServerInstance();
			try {
				Thread.sleep(Constants.SERVER_CHECK_LOAD_TIME_INTERVAL);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	private int getNumberOfMessages() {
		int n = 0;
		Enumeration<Message> messages;
		try {
			messages = browser.getEnumeration();
			while(messages.hasMoreElements()) {
				messages.nextElement();
				n++;
			}
		} catch (JMSException e) {
			e.printStackTrace();
		}
		return n;
	}

	public static void main(String[] args) {
		Server server = new Server();
		try {
			server.startServer();
		} catch (NamingException e) {
			e.printStackTrace();
		}
	}
}
