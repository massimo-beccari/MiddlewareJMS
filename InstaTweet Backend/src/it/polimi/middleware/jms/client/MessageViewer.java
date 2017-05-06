package it.polimi.middleware.jms.client;

import it.polimi.middleware.jms.Constants;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class MessageViewer {
	private final int messageType;
	private final String messageId;
	private final JFrame window;
	private final JPanel mainPanel;
	private final BoxLayout layout;
	private final JTextField idArea;
	private final JTextField textArea;
	private final ImagePanel imagePanel;
	
	public MessageViewer(int messageType, String messageId, String messageText, byte[] messageImage) {
		this.messageType = messageType;
		this.messageId = messageId;
		//principal components setup
		window = new JFrame();
		mainPanel = new JPanel();
		layout = new BoxLayout(mainPanel, BoxLayout.Y_AXIS);
		mainPanel.setLayout(layout);
		idArea = new JTextField();
		idArea.setText("MSG_ID: " + messageId);
		mainPanel.add(idArea);
		//image loading
		BufferedImage img = null;
		try {
			img = ImageIO.read(new ByteArrayInputStream(messageImage));
		} catch (IOException e) {
			e.printStackTrace();
		}
		//other components setup
		Dimension size;
		switch(messageType) {
		case Constants.MESSAGE_ONLY_TEXT:
			//text
			textArea = new JTextField();
			mainPanel.add(textArea);
			textArea.setText("MSG_TXT: " + messageText);
			//image
			imagePanel = null;
			break;
			
		case Constants.MESSAGE_ONLY_IMAGE:
			//text
			textArea = null;
			//image
			imagePanel = new ImagePanel(img);
			size = new Dimension(img.getWidth(), img.getHeight());
			imagePanel.setPreferredSize(size);
			imagePanel.setMaximumSize(size);
			imagePanel.setMinimumSize(size);
			mainPanel.add(imagePanel);
			break;
			
		case Constants.MESSAGE_TEXT_AND_IMAGE:
			//text
			textArea = new JTextField();
			mainPanel.add(textArea);
			textArea.setText("MSG_TXT: " + messageText);
			//image
			imagePanel = new ImagePanel(img);
			size = new Dimension(img.getWidth(), img.getHeight());
			imagePanel.setPreferredSize(size);
			imagePanel.setMaximumSize(size);
			imagePanel.setMinimumSize(size);
			mainPanel.add(imagePanel);
			break;
			
		default:
			textArea = null;
			imagePanel = null;
		}
		
		window.setTitle("MSG_ID: " + messageId);
		window.setContentPane(mainPanel);
		window.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		window.pack();
	}
	
	public void show() {
		window.setVisible(true);
	}
	
	public int getMessageType() {
		return messageType;
	}

	public String getMessageId() {
		return messageId;
	}

	private class ImagePanel extends JPanel {
		private static final long serialVersionUID = 1L;
		private BufferedImage img;
		
		public ImagePanel(BufferedImage img) {
			super();
			this.img = img;
		}
		
		@Override
		 protected void paintComponent(Graphics g) {
			 super.paintComponent(g);
		     g.drawImage(img, 0, 0, img.getWidth(), img.getHeight(), this);           
		 }
	}

}
