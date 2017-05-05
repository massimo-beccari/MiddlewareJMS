package it.polimi.middleware.jms;

import it.polimi.middleware.jms.model.IdDistributor;
import it.polimi.middleware.jms.model.User;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.jms.ConnectionFactory;
import javax.jms.JMSConsumer;
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
	private IdDistributor messageIdDistributor;
	private HashMap<Integer, User> usersMapId;
	private HashMap<String, User> usersMapUsername;
	private HashMap<Integer, UserQueueDaemon> daemonsMap;
	private ExecutorService executor;
	private Context initialContext;
	private JMSContext jmsContext;
	private Queue requestsQueue;
	private JMSConsumer jmsConsumer;
	private QueueBrowser browser;
	private ArrayList<ServerInstance> serverInstances;
	private long nextCheckTime;
	
	private Server() {
		currentNumberOfServerInstances = 0;
		userIdDistributor = new IdDistributor(1);
		messageIdDistributor = new IdDistributor(1);
		usersMapId = new HashMap<Integer, User>();
		usersMapUsername = new HashMap<String, User>();
		daemonsMap = new HashMap<Integer, UserQueueDaemon>();
		executor = Executors.newCachedThreadPool();
		serverInstances = new ArrayList<ServerInstance>();
	}
	
	private void startServer() throws NamingException {
		createContexts();
		requestsQueue = jmsContext.createQueue(Constants.QUEUE_REQUESTS_NAME);
		jmsConsumer = jmsContext.createConsumer(requestsQueue);
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
		currentNumberOfServerInstances++;
		ServerInstance firstInstance = new ServerInstance(currentNumberOfServerInstances,
				userIdDistributor, messageIdDistributor, usersMapId, usersMapUsername, daemonsMap);
		serverInstances.add(firstInstance);
		executor.submit(firstInstance);
		System.out.println("SRV: new server instance launched. (" + currentNumberOfServerInstances + ")");
	}
	
	private void loop() {
		Message msg;
		nextCheckTime = System.currentTimeMillis() + Constants.SERVER_CHECK_LOAD_TIME_INTERVAL;
		while(true) {
			if(System.currentTimeMillis() > nextCheckTime) {
				int n = getNumberOfMessages();
				if(n > Constants.SERVER_MAX_LOAD_THRESOLD) {
					System.out.println("SRV: requests number = " + n);
					startNewServerInstance();
				} else
					System.out.println("SRV: requests number = " + n + " (Thresold " + Constants.SERVER_MAX_LOAD_THRESOLD + ")");
				nextCheckTime = System.currentTimeMillis() + Constants.SERVER_CHECK_LOAD_TIME_INTERVAL;
			} else {
				msg = jmsConsumer.receiveNoWait();
				if(msg != null)
					onMessage(msg);
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

	public void onMessage(Message msg) {
		boolean messageProcessed = false;
		while(!messageProcessed) {
			for(ServerInstance si : serverInstances) {
				if(si.getWait()) {
					synchronized(si) {
						si.setRequest(msg);
						si.setNoWait();
						si.notify();
					}
					messageProcessed = true;
					System.out.println("SRV: request delivered to #" + si.getSERVER_INSTANCE_NUMBER() + " server instance");
				}
			}
			/*
			try {
				Thread.sleep(Constants.SERVER_CHECK_LOAD_TIME_INTERVAL);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}*/
		}
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
