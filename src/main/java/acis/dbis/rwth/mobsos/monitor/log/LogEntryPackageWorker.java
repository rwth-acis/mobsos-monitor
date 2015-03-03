package acis.dbis.rwth.mobsos.monitor.log;

import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import acis.dbis.rwth.mobsos.monitor.Monitor;
import acis.dbis.rwth.mobsos.monitor.notify.NotificationManager;

/**
 * This class defines an individual worker thread, repeatedly consuming single {@link LogEntryPackage} instances from the queue managed 
 * in {@link LogEntryPackageManager} in a synchronized manner and then writing them to the MobSOS monitoring database, using their 
 * {@link LogEntryPackage#write()} method.
 * 
 * Any instance of this class is equipped with a set of {@link PreparedStatement}, one for each type of {@link LogEntry}
 * in a {@link LogEntryPackage}. Since all {@link PreparedStatement}s are bound to the {@link Connection} they were created
 * with, any instance of this class will only consume {@link LogEntryPackage} instances from the queue, if a check on the liveness of
 * the corresponding connection is successful. If this test fails, the particular instance is disposed.
 * 
 * If writing a {@link LogEntryPackage} fails for some reason, this class realizes a notification mechanism, reporting the
 * occurred failure and related information via email to all recipients defined in the configuration file of the LAS HTTP Connector.
 * 
 * @author Dominik Renzel
 * @version $Revision: 1.1.2.8 $, $Date: 2014/02/05 12:59:20 $
 */
public class LogEntryPackageWorker extends Thread {

	private final LogEntryPackageManager manager;
	private static int instance = 0;

	public LogEntryPackageWorker(LogEntryPackageManager m) throws SQLException {
		this.setName("LEPW-"+(instance++));
		this.manager = m;

		Monitor.log.info("Log Entry Package Worker " + getName() + " ready...");
	}
	
	@Override
	public void run() {
		try {
			// only do something when connection is active. As soon as the current connection becomes inactive, the current worker
			// becomes useless, since all of his prepared statements are also closed and need to be renewed with a new connection.
			// In this case, it can be disposed.
			while (!manager.getConnection().isClosed()) {
				LogEntryPackage l = null;


				synchronized ( manager.getQueue() ) {

					while (manager.getConnection().isClosed() || manager.getQueue().isEmpty()){
						manager.getQueue().wait();
					}

					// get the next complete log entry package from the queue 
					l = manager.getQueue().remove();

					// have log entry package written by worker
					if(l != null){
						l.setWorker(this);
						writeLogEntryPackage(l);
					}
				}
			}
			Monitor.log.debug("Worker " + getName() + " going down due to lost database connection.");
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} catch (SQLException e) {
			Thread.currentThread().interrupt();
		}
	}

	public Connection getConnection(){
		return this.manager.getConnection();
	}
	
	public LogEntryPackageManager getManager() {
		return manager;
	}

	private void writeLogEntryPackage(LogEntryPackage l){
		try{
			l.write();
		} 
		catch (Exception e) {

			// if any exception occurs while writing a complete log entry package, roll back
			// complete transaction and send notification via mail.
			boolean rbComplete = false;
			try {
				if(this.manager.getConnection() != null && !this.manager.getConnection().isClosed()){
					Monitor.log.error("Transaction for log entry package " + l.getId() + " rolled back!",e);
					this.manager.getConnection().rollback();
					rbComplete = true;
				}
			} catch (Exception e1) {
				Monitor.log.error("Unknown error!",e1);
			}

			// send notification about failed writing of log entry package
			String message = "Worker " + this.getName() + " failed to write package " + l.getId() + "."; 
			if(rbComplete){
				message += " (transaction rolled back)";
			} else {
				message += " (transaction rollback failed!)";
			}
			System.err.println(message);
			e.printStackTrace();

			// when sending error notifications, let the notified people know from which 
			// MobSOS installation the message came...
			java.net.InetAddress localMachine;
			String hostName = "localhost";
			String hostAddress = "127.0.0.1";
			try {
				localMachine = java.net.InetAddress.getLocalHost();
				hostName = localMachine.getCanonicalHostName();
				hostAddress = localMachine.getHostAddress();
			} catch (UnknownHostException e1) {
				e1.printStackTrace();
			}

			message = "MobSOS Monitor on " + hostName + " (" + hostAddress + ") reports operation failure: " + message;
			// attach deserialization of complete log entry package to message for easier debugging
			message += l.toString();

			if(NotificationManager.getInstance()!=null){
				NotificationManager.getInstance().notifyFailure(message, e);
			} else {
				Monitor.log.error(message,e);
			}

		} finally {
			l = null;
		}
	}
}