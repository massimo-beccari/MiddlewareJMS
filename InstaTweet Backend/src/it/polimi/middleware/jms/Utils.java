package it.polimi.middleware.jms;

import it.polimi.middleware.jms.client.MessageViewer;
import it.polimi.middleware.jms.server.model.IdDistributor;
import it.polimi.middleware.jms.server.model.message.MessageInterface;
import it.polimi.middleware.jms.server.model.message.MessageProperty;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

import javax.imageio.ImageIO;
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
		byte[] thumbnail = null;
		BufferedImage img, thumbImg;
		try {
			img = ImageIO.read(new ByteArrayInputStream(image));
			//calculate scale
			float scale;
			if(img.getWidth() > img.getHeight())
				scale = Constants.THUMBNAIL_MAX_DIMENSION/((float)img.getWidth());
			else
				scale = Constants.THUMBNAIL_MAX_DIMENSION/((float)img.getHeight());
			System.out.println(scale + " " + (int) (img.getWidth()*scale) + " " + (int) (img.getHeight()*scale));
			//create thumbnail image
			thumbImg = new BufferedImage((int) (img.getWidth()*scale), (int) (img.getHeight()*scale), BufferedImage.TYPE_INT_ARGB);
			AffineTransform at = new AffineTransform();
			at.scale(scale, scale);
			AffineTransformOp scaleOp = new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
			thumbImg = scaleOp.filter(img, thumbImg);
			//create image byte array
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(thumbImg, extension, baos);
			baos.flush();
			thumbnail = baos.toByteArray();
			baos.close();
			return thumbnail;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return thumbnail;
	}
	
	/*
	 * Date format string: dd/mm/yyyy/h:m
	 */
	public static long getTimeFromDate(String date) throws Exception {
		int day, month, year, hours, minutes;
		Scanner sc = new Scanner(date);
		String s;
		sc.useDelimiter("/");
		//scan date
		s = sc.next();
		day = Integer.parseInt(s);
		if(day < 1 || day > 31) {
			sc.close();
			throw new Exception();
		}
		s = sc.next();
		month = Integer.parseInt(s);
		if(month < 1 || month > 12) {
			sc.close();
			throw new Exception();
		}
		s = sc.next();
		year = Integer.parseInt(s);
		if(year < 1970) {
			sc.close();
			throw new Exception();
		}
		//scan time
		sc.skip("/");
		sc.useDelimiter(":");
		s = sc.next();
		hours = Integer.parseInt(s);
		if(hours < 0) {
			sc.close();
			throw new Exception();
		}
		s = sc.next();
		minutes = Integer.parseInt(s);
		if(minutes < 0 || minutes > 59) {
			sc.close();
			throw new Exception();
		}
		sc.close();
		Calendar cal = Calendar.getInstance();
		cal.set(year, month-1, day, hours, minutes, 0);
		return cal.getTimeInMillis();
	}
	
	//TEST MAIN
	public static void main(String[] args) {
		//TEST
		/*try {
			System.out.println(getTimeFromDate("10/5/2017/12:23") + " " + System.currentTimeMillis());
		} catch (Exception e) {
			e.printStackTrace();
		}*/
		//TEST
		RandomAccessFile imageFile;
		try {
			imageFile = new RandomAccessFile("C:\\Users\\Max\\Desktop\\a.jpg", "r");
			byte[] image = new byte[(int) imageFile.length()];
			imageFile.readFully(image);
			imageFile.close();
			MessageViewer messageViewer = new MessageViewer(Constants.MESSAGE_ONLY_IMAGE, 
					"TEST", null, image);
			messageViewer.show();
			MessageViewer messageViewer2 = new MessageViewer(Constants.MESSAGE_ONLY_IMAGE, 
					"TEST", null, Utils.createImageThumbnail(image, "jpg"));
			messageViewer2.show();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
