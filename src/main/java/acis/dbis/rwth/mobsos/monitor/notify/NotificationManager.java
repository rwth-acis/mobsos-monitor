package acis.dbis.rwth.mobsos.monitor.notify;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import acis.dbis.rwth.mobsos.monitor.Monitor;

public class NotificationManager {

	private static NotificationManager notificationManager;
	private String mailHost = null;
	private Integer mailPort = null;
	private String mailUsername = null;
	private String mailPassword = null;
	private String mailFrom = null;
	private String mailAuth = null;
	private String mailTLSEnable = null;
	private ArrayList<String> emailAdresses = new ArrayList<String>();

	private NotificationManager () {}

	public static NotificationManager getInstance() {
		return notificationManager;
	}

	public static void initInstance(String mailHost, int mailPort, String mailUsername, String mailPassword, String mailAuth, String mailTLSEnable, String mailFrom, String mailTo) throws Exception{
		Monitor.log.info("Initializing notification manager...");
		notificationManager = new NotificationManager();
		notificationManager.setMailSettings(mailHost, mailPort, mailUsername, mailPassword, mailAuth, mailTLSEnable, mailFrom, mailTo);
		Monitor.log.info("Notification manager initialization complete.");
	}

	public void setMailSettings(String mailHost, Integer mailPort, String mailUsername, String mailPassword, String mailAuth, String mailTLSEnable, String mailFrom, String mailTo) throws Exception {
		this.mailHost = mailHost;
		this.mailPort = mailPort;
		this.mailUsername = mailUsername;
		this.mailPassword = mailPassword;
		this.mailAuth = mailAuth;
		this.mailTLSEnable = mailTLSEnable;
		this.mailFrom = mailFrom;

		// tokenize comma-separated list of email addresses to be notified
		String[] tokens = mailTo.split(",");
		for (int i = 0; i < tokens.length; i++) {
			String address = tokens[i].trim();
			emailAdresses.add(address);
		}

		// a basic check whether the credentials work
		Session emailSession = getSession();

		Transport emailTransport = emailSession.getTransport("smtp");

		if(emailTransport == null) {
			throw new Exception("Unable to get a E-Mail Transport! Check your settings/Credentials!");
		}

		emailTransport.connect(this.mailHost, this.mailPort, this.mailUsername, this.mailPassword);
		emailTransport.close();
	}

	private Session getSession() throws Exception {
		if(mailHost == null || mailHost.isEmpty()) {
			throw new Exception("Must provide a mail host! Check your config files!");
		}
		if(mailPort == null || mailPort<1) {
			throw new Exception("Must provide a mail port! Check your config files!");
		}
		/*
		if(mailUsername == null || mailUsername.isEmpty()) {
			throw new Exception("Must provide a mail username! Check your config files!");
		}
		if(mailPassword == null || mailPassword.isEmpty()) {
			throw new Exception("Must provide a mail password! Check your config files!");
		}
		 */

		Properties props = new Properties();
		props.put("mail.smtp.host", this.mailHost);
		props.put("mail.smtp.port", this.mailPort.toString());
		props.put("mail.smtp.user", this.mailUsername);
		props.put("mail.smtp.password", this.mailPassword);
		props.put("mail.smtp.auth", this.mailAuth);
		props.put("mail.smtp.starttls.enable", this.mailTLSEnable);

		Session emailSession = Session.getInstance(props, null);
		if(emailSession == null) {
			throw new Exception("Unable to create a Mail Session!");
		}

		Transport emailTransport = emailSession.getTransport("smtp");

		if(emailTransport == null) {
			throw new Exception("Unable to get a E-Mail Transport! Check your settings/Credentials!");
		}

		emailTransport.connect(this.mailHost, this.mailPort, this.mailUsername, this.mailPassword);
		emailTransport.close();

		return emailSession;
	}

	public void sendNotification(String subject, String message) throws Exception {
		if(emailAdresses == null || emailAdresses.size() < 1) {
			emailAdresses = new ArrayList<String>();
		}

		Address[] addresses = new Address[emailAdresses.size()];

		for(int i=0; i<addresses.length; i++) {
			try {
				String eMail = emailAdresses.get(i);
				eMail.replaceAll(":", ".");

				addresses[i] = new InternetAddress(eMail);
			}
			catch(Exception e) {
				e.printStackTrace();
				addresses[i] = new InternetAddress("noreply@localhost");
			}
		}

		Session emailSession = getSession();    				
		Message emailMessage = new MimeMessage(emailSession);
		emailMessage.setFrom(new InternetAddress(this.mailFrom));
		emailMessage.setRecipients(MimeMessage.RecipientType.TO, addresses);
		emailMessage.setSubject(subject);
		emailMessage.setContent(message, "text/plain");

		Transport emailTransport = emailSession.getTransport("smtp");

		if(emailTransport == null) {
			throw new Exception("Unable to get an E-Mail Transport! Check your settings/credentials/internet connection!");
		}

		emailTransport.connect(this.mailHost, this.mailPort, this.mailUsername, this.mailPassword);
		emailTransport.sendMessage(emailMessage, emailMessage.getRecipients(Message.RecipientType.TO));
		emailTransport.close();
	}

	public void notifyFailure(String msg, Exception e){
		// first collect complete stack trace into string to later send it with notification mail
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		sw.toString(); // stack trace as a string

		try {
			// send notification about failed writing of log entry package
			String message = new Date() + " - " + msg + " Cause: " + e.getMessage() + "\n\nStacktrace: \n" + sw.toString(); 
			sendNotification("MobSOS 0.5 - Operation Failure", message);
			Monitor.log.error("Mail notification sent due to error: " + message);
		} catch (Exception e1) {
			Monitor.log.error("Unexpected error! ",e1);
		}
	}
}
