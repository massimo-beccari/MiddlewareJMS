package it.polimi.middleware.jms.client;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;

import it.polimi.middleware.jms.Utils;

public class TimelineView extends JFrame {
	private static final long serialVersionUID = 1L;
	private ArrayList<MessageHolder> messageList;
	private DefaultListModel<MessageHolder> listModel;
	private JList<MessageHolder> list;
	
	public TimelineView(Client client, ArrayList<MessageHolder> messageList) {
		this.messageList = messageList;
		listModel = new DefaultListModel<>();
		list = new JList<>(listModel);
		list.setCellRenderer(new MessageRenderer());
		for(MessageHolder msg : messageList)
			listModel.addElement(msg);
		add(new JScrollPane(list));
		this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.setTitle("Message Timeline");
        this.setSize(440, 480);
        this.setLocationRelativeTo(null);
	}
	
	public ArrayList<MessageHolder> getMessageList() {
		return messageList;
	}
	
	public void showTimeline() {
        this.setVisible(true);
	}

	private class MessageRenderer extends JPanel implements ListCellRenderer<MessageHolder> {
		private static final long serialVersionUID = 1L;
		private JLabel id;
		private JLabel username;
		private JTextField text;
		private JLabel image;
		private JLabel imageId;
		private JPanel topPanel;
		private JPanel bottomPanel;
		private JPanel rightPanel;
		private JLabel time;
		
		public MessageRenderer() {
			super();
			BorderLayout layout = new BorderLayout();
			setLayout(layout);
			id = new JLabel();
			username = new JLabel();
	    	text = new JTextField();
	    	image = new JLabel();
	    	imageId = new JLabel();
	        time = new JLabel();
	    	//top panel
	    	topPanel = new JPanel();
	    	BoxLayout topPanelLayout = new BoxLayout(topPanel, BoxLayout.Y_AXIS);
	    	topPanel.setLayout(topPanelLayout);
	    	topPanel.add(new JSeparator());
	    	topPanel.add(id);
	    	topPanel.add(username);
	    	topPanel.setPreferredSize(new Dimension(400, 32));
	    	//right panel
	    	rightPanel = new JPanel();
	    	BoxLayout rightPanelLayout = new BoxLayout(rightPanel, BoxLayout.Y_AXIS);
	    	rightPanel.setLayout(rightPanelLayout);
	    	rightPanel.add(image);
	    	rightPanel.add(imageId);
	    	//bottom panel
	    	bottomPanel = new JPanel();
	    	BoxLayout bottomPanelLayout = new BoxLayout(bottomPanel, BoxLayout.Y_AXIS);
	    	bottomPanel.setLayout(bottomPanelLayout);
	        bottomPanel.add(time);
	        bottomPanel.add(new JSeparator());
	        bottomPanel.setPreferredSize(new Dimension(400, 32));
	        
			add(topPanel, BorderLayout.PAGE_START);
			add(text, BorderLayout.CENTER);
			add(rightPanel, BorderLayout.LINE_END);
			add(bottomPanel, BorderLayout.PAGE_END);
		}
		 
	    @Override
	    public Component getListCellRendererComponent(JList<? extends MessageHolder> list, MessageHolder msg, int index,
	        boolean isSelected, boolean cellHasFocus) {
	        
	    	id.setText("Message id: " + msg.getMessageId());
	    	username.setText("User: " + msg.getUsername());
	    	text.setText("Message text: \"" + msg.getText() + "\"");
	    	if(msg.getImage() != null) {
		        ImageIcon imageIcon = new ImageIcon(msg.getImage());
		        image.setIcon(imageIcon);
		        imageId.setText(msg.getImageMessageId());
	    	} else {
	    		image.setIcon(null);
	    		imageId.setText(null);
	    	}
	        time.setText(Utils.getStringTimeFromLong(Long.parseLong(msg.getTime())));
	         
	        return this;
	    }
	     
	}
	
	//TEST
	public static void main(String[] args) throws IOException {
		MessageHolder m1, m2, m3, m4, m5, m6;
		RandomAccessFile imageFile = new RandomAccessFile("C:\\Users\\Max\\Desktop\\a.jpg", "r");
		byte[] image = new byte[(int) imageFile.length()];
		imageFile.readFully(image);
		imageFile.close();
		m1 = new MessageHolder("id", null, "user", "m1", Utils.createImageThumbnail(image, "jpg"), "time");
		m2 = new MessageHolder("id", null, "user", "m1", null, "time");
		m3 = new MessageHolder("id", null, "user", "m1", Utils.createImageThumbnail(image, "jpg"), "time");
		m4 = new MessageHolder("id", null, "user", "m1", Utils.createImageThumbnail(image, "jpg"), "time");
		m5 = new MessageHolder("id", null, "user", "m1", Utils.createImageThumbnail(image, "jpg"), "time");
		m6 = new MessageHolder("id", null, "user", "m1", Utils.createImageThumbnail(image, "jpg"), "time");
		ArrayList<MessageHolder> messageList = new ArrayList<MessageHolder>();
		messageList.add(m1);
		messageList.add(m2);
		messageList.add(m3);
		messageList.add(m4);
		messageList.add(m5);
		messageList.add(m6);
		TimelineView timeline = new TimelineView(null, messageList);
		timeline.showTimeline();
	}
}
