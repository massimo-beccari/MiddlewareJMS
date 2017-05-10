package it.polimi.middleware.jms.client;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.ArrayList;

import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;

import it.polimi.middleware.jms.server.model.message.GeneralMessage;

public class TimelineView extends JFrame {
	private static final long serialVersionUID = 1L;
	private ArrayList<GeneralMessage> messageList;
	private DefaultListModel<GeneralMessage> listModel;
	private JList<GeneralMessage> list;
	
	public TimelineView(ArrayList<GeneralMessage> messageList) {
		this.messageList = messageList;
		listModel = new DefaultListModel<>();
		list.setCellRenderer(new MessageRenderer());
		for(GeneralMessage msg : messageList)
			listModel.addElement(msg);
		add(new JScrollPane(list));
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setTitle("Message Timeline");
        this.setSize(640, 480);
        this.setLocationRelativeTo(null);
        this.setVisible(true);
	}
	
	public ArrayList<GeneralMessage> getMessageList() {
		return messageList;
	}

	private class MessageRenderer extends JPanel implements ListCellRenderer<GeneralMessage> {
		private static final long serialVersionUID = 1L;
		private JLabel username;
		private JTextField text;
		private JLabel image;
		private JLabel time;
		
		public MessageRenderer() {
			super();
			BorderLayout layout = new BorderLayout();
			setLayout(layout);
			add(username, BorderLayout.PAGE_START);
			add(text, BorderLayout.CENTER);
			add(image, BorderLayout.LINE_END);
			add(time, BorderLayout.PAGE_END);
		}
		 
	    @Override
	    public Component getListCellRendererComponent(JList<? extends GeneralMessage> list, GeneralMessage msg, int index,
	        boolean isSelected, boolean cellHasFocus) {
	        
	    	username = new JLabel("");
	    	text = new JTextField(msg.getText());
	        ImageIcon imageIcon = new ImageIcon(msg.getImage());
	        image = new JLabel(imageIcon);
	        time = new JLabel("");
	         
	        return this;
	    }
	     
	}
}
