package acis.dbis.rwth.mobsos.monitor.log;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import acis.dbis.rwth.mobsos.monitor.Monitor;

/**
 * This class is actively watching over the one database connection shared among all MobSOS components, i.e. the Log Entry Package Manager,
 * the Log Entry Package Workers. It serves as a helper to achieve resilience against lost database connections by actively attempting to
 * create new connections and prepared statements, wherever and whenever necessary. First, it realizes a keep-alive ping with an interval
 * configurable in the HTTP Connector's configuration XML file. This mechanism makes MobSOS resilient against database connection timeouts
 * due to long idle periods. Second, it realizes a mechanism to renew the database connection, when it was lost, including the exchange 
 * of obsolete Log Entry Workers (because their prepared statements became useless) with new workers, working on the new connection. This 
 * case occurs in particular in situations, where the database goes down for maintenance or backup. In this case, the watcher first attempts
 * to reconnect three times within the configured ping interval. If all three re-connection attempts were unsuccessful, the interval is 
 * automatically raised to 10 minutes, assuming that the database is down for longer maintenance. Whenever a connection could be successfully
 * established after the longer waiting periods, the watcher switches back to the configured interval with keep-alive pings.
 * 
 * @author renzel
 * @version $Revision: 1.1.2.1 $, $Date: 2013/12/12 17:45:53 $
 */
public class DatabaseWatcher extends Thread {

	private final LogEntryPackageManager manager;
	private PreparedStatement keepAliveStatement;
	private int pingInterval;
	// when database is down for longer time, set interval for reconnection attempts to 10 min 
	private static final int dbDownPingInterval = 600000; 
	public boolean dbDownForLonger = false;
	private int dbUnsuccessfulReconnects = 0;

	public DatabaseWatcher (LogEntryPackageManager m, int pingSeconds) throws IllegalArgumentException, SQLException{
		this.manager = m;
		if(this.manager.getConnection().isClosed()){
			throw new IllegalArgumentException("Passed database connection must be alive!");
		}
		// TODO: change keep alive query to a more generic, database independent query
		renewKeepAliveStatement();
		this.pingInterval = pingSeconds * 1000;
		if(pingInterval >= dbDownPingInterval){
			Monitor.log.warn("Configured keep-alive interval (" + pingSeconds + "s) is longer than pre-set waiting interval for longer database downtimes (" + dbDownPingInterval/1000 + "s)!");
		}
	}

	protected void renewKeepAliveStatement() throws SQLException{
		this.keepAliveStatement = this.manager.getConnection().prepareStatement(Monitor.conf.getProperty("jdbcKeepAliveStatement"));
	}

	public void renewConnection() throws SQLException{

		try {
			Monitor.log.info("Renewing database connection");
			this.manager.initDatabaseConnection();
		} catch (ClassNotFoundException e) {
			Monitor.log.error("Cannot re-initialize database...",e);
		}
	}

	public void renewWorkers() throws SQLException{
		this.manager.initWorkers();
	}

	@Override
	public void run() {

		while(true){
			try
			{
				int actualPingInterval = this.pingInterval;

				if(dbUnsuccessfulReconnects >= 3){
					Monitor.log.info("Database down for longer...");
					actualPingInterval = dbDownPingInterval;
				}
				
				Thread.sleep(actualPingInterval);

				if(this.manager.getConnection().isClosed()){
					Monitor.log.debug("Database connection closed!");

					try {
						if(dbDownForLonger){
							dbUnsuccessfulReconnects++;
							throw new SQLException("Database still down...");
						} else {
							renewConnection();
							dbUnsuccessfulReconnects = 0;
						}
					} catch (SQLException e) {
						dbUnsuccessfulReconnects++;
						throw new SQLException("Database still down...");
					}

					renewWorkers();
					renewKeepAliveStatement();

				} else {
					this.keepAliveStatement.execute();
					Monitor.log.debug("Database keep-alive ping performed (interval: " + (actualPingInterval/1000) + "s).");
				}

			}
			catch(InterruptedException e)
			{
				Monitor.log.error("MobSOS DatabaseWatcher encountered a problem!",e);
			}  catch (SQLException e) {
				Monitor.log.error("MobSOS DatabaseWatcher encountered a problem!",e);
			}
		}
	}

}
