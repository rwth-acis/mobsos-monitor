package acis.dbis.rwth.mobsos.monitor.log;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;
import java.util.Vector;

import acis.dbis.rwth.mobsos.monitor.Monitor;

/**
 * This class is the central component of the new MobSOS monitoring module for the LAS HTTP Connector.
 * When the LAS HTTP Connector is started (see {@link HttpConnectorStarter}), a singleton instance of this
 * component is started along, including data structures managing the persistent storage of 
 * {@link LogEntryPackage}s to the MobSOS log database.
 * 
 * On an incoming LAS request, a {@link HttpConnectorRequestHandler} first creates a new {@link LogEntryPackage} with a given request id, 
 * which is buffered in a hash table until processing of this request is finished. All logging information collected during the processing
 * of one request is collected in one of the aforementioned partially filled {@link LogEntryPackage} instances in the hash table. On LAS 
 * request completion, the now completely filled package is removed from the hash table and then added to a queue of complete {@link LogEntryPackage} 
 * instances waiting to be persisted to the MobSOS monitoring database.
 * 
 * For the purpose of persisting these complete {@link LogEntryPackage} instances, this class manages a pool of {@link LogEntryPackageWorker} 
 * instances waiting for new packages and persisting them to the database. All such database communication is done over one single 
 * {@link Connection}, shared among all {@link LogEntryPackageWorker} instances.   
 * 
 * In order to guarantee that all LAS requests are monitored and persisted, an important practical requirement of MobSOS is being resilient 
 * against unexpected database connections close-downs. In practice, this happens in two situations. First, LAS exhibits longer idle periods,
 * where no incoming LAS requests need to be processed and logged. In such cases, the underlying {@link Connection} times out, resulting in a
 * closed connection. This situation can easily be handled with a database keep-alive ping. Second, there are situations, where LAS continues 
 * to operate, while the MobSOS database goes down due to maintenance or nightly backup purposes. In such situations, a keep-alive ping is not
 * sufficient anymore, and all {@link LogEntryPackageWorker} instances need to wait until the database comes up again.  
 * 
 * @author Dominik Renzel
 * @version $Revision: 1.1.2.8 $, $Date: 2013/12/15 14:43:43 $
 */
public class LogEntryPackageManager {

	private static LogEntryPackageManager instance;

	private String databaseDriverClassName;
	private String databaseUrl;
	private String databaseLogin;
	private String databasePass;
	private DatabaseWatcher databaseWatcher;

	private int dbWorkers;
	private int dbKeepAlivePingInterval;

	private Connection connection;
	private Hashtable<UUID,LogEntryPackage> partialLogEntryPackages;
	private Queue<LogEntryPackage> queue;
	private Collection<LogEntryPackageWorker> workerPool;

	public LogEntryPackageManager(String jdbcDriverClassName, 
			String jdbcProtocolPrefix, 
			String logDbHostname, 
			int logDbPort,
			String logDbDatabaseName,
			String logDbLogin, String logDbPass,
			int numWorkers, int keepAlive) throws ClassNotFoundException, SQLException{

		this.databaseDriverClassName = jdbcDriverClassName;
		this.databaseUrl = jdbcProtocolPrefix + "//" + logDbHostname + ":" + logDbPort + "/" + logDbDatabaseName +
				":retrieveMessagesFromServerOnGetMessage=true;";
		this.databaseLogin = logDbLogin;
		this.databasePass = logDbPass;
		this.dbWorkers = numWorkers;
		this.dbKeepAlivePingInterval = keepAlive;

		Monitor.log.info("Initializing log entry package manager...");
		// initialize hash table of incomplete log entry packages and log entry package queue
		initDataStructures();

		// initialize connection to log database, a keep-alive pinger and workerPool for persisting log entry packages
		initDatabaseConnection();

		// initialize worker pool
		initWorkers();

		// start a separate thread that keeps the connection alive in longer idle periods,
		// where no requests are incoming.
		databaseWatcher = new DatabaseWatcher(this,this.dbKeepAlivePingInterval);
		databaseWatcher.start();

		Monitor.log.info("Database connection keep-alive ping active with " + this.dbKeepAlivePingInterval + "s interval.");
		Monitor.log.info("Log entry package manager initialization complete.");

	}

	/**
	 * Initializes the database connection shared among {@link LogEntryPackageWorker} instances.
	 * Throws an exception, if the driver class name passed to the constructor is not valid or if
	 * a connection cannot be established for some reason.
	 *  
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 */
	public void initDatabaseConnection() throws ClassNotFoundException, SQLException{

		synchronized(queue){
			
			Class.forName(this.databaseDriverClassName);
			
			connection = DriverManager.getConnection(this.databaseUrl, this.databaseLogin, this.databasePass);
			connection.setAutoCommit(false);
			
			Monitor.log.info("Database connection initialized. (" + this.databaseLogin + "@" + this.databaseUrl + ")");
		}
	}

	public Connection getConnection(){
		return this.connection;
	}

	public Queue<LogEntryPackage> getQueue(){
		return this.queue;
	}

	public DatabaseWatcher getDatabaseWatcher(){
		return this.databaseWatcher;
	}

	/**
	 * Initializes two kinds of helper data structures: a {@link Queue} queue of {@link LogEntryPackage} instances to be written to 
	 * the log database by {@link LogDatabaseWriteWorker} instances and a hash table of yet incomplete {@link LogEntryPackage} instances
	 * to be manipulated by other classes (e.g. LAS HTTP Connector Request Handler).
	 */
	private void initDataStructures(){
		queue = new LinkedList<LogEntryPackage>();
		partialLogEntryPackages = new Hashtable<UUID,LogEntryPackage>();
	}

	/**
	 * Initializes a given number of {@link LogEntryPackageWorker} instances, watching the queue and taking {@link LogEntryPackage} instances 
	 * out for persisting them to the database. Throws an {@link SQLException} in case of problems setting up prepared statements for the worker
	 * instances.
	 * 
	 * @throws SQLException 
	 */
	public void initWorkers() throws SQLException{
		// first initialize worker collection handled by this class
		if(this.workerPool == null){
			this.workerPool = new Vector<LogEntryPackageWorker>();
		} else {
			interruptWorkers();
			this.workerPool.clear();
		}
		// then start requested number of workerPool and add them to the worker collection handled by this class
		for (int i = 0; i < this.dbWorkers; i++) {
			LogEntryPackageWorker w = new LogEntryPackageWorker(this);
			w.start();
			workerPool.add(w);
		}
		Monitor.log.info("Worker pool size: " + workerPool.size());
	}

	/**
	 * Stops and disposes all active {@link LogEntryPackageWorker} instances. This method is needed when the 
	 * database connection was closed for some reason. Closed connections due to idling are already avoided by 
	 * repeated keep-alive pinging by the {@link DatabaseWatcher}. However, one typical case is the downtime
	 * of a database in the case of regular nightly backups.
	 */
	private void interruptWorkers(){
		// first interrupt all active workerPool, then clear the whole worker collection.
		Iterator<LogEntryPackageWorker> it = this.workerPool.iterator();
		while(it.hasNext()){
			LogEntryPackageWorker w = it.next();
			w.interrupt();
			Monitor.log.debug("Stopping worker " + w.getName());
		}
	}


	/**
	 * Retrieves an incomplete and thus yet unqueued log entry package with a given id. 
	 * Returns null if no such unqueued log entry package exists.
	 * 
	 * @param id
	 * @return the {@link LogEntryPackage} with given id
	 */
	public LogEntryPackage getLogEntryPackage(UUID id){
		return partialLogEntryPackages.get(id);
	}

	/**
	 * Creates a new log entry package and adds it to the table of partial log entry packages. This method should be used by external
	 * entities to create new log entry packages, which upon completion of parameter settings can be queued for persistence in the 
	 * log database. 
	 */
	public LogEntryPackage createLogEntryPackage(){
		LogEntryPackage p = new LogEntryPackage();
		partialLogEntryPackages.put(p.getId(),p);
		return partialLogEntryPackages.get(p.getId());
	}


	/**
	 * Queues an existing log entry package with a given id to be picked up by a {@link LogEntryPackageWorker} instance for persisting
	 * its individual log entries to the log database. Remains without effect if a log entry package with the given id does not exist.
	 * 
	 * @param id
	 */
	public void queueLogEntryPackage(UUID id){
		// first check if log entry package with given id is stored in hash table.
		if(!partialLogEntryPackages.keySet().contains(id)){
			// print warning and return, if log entry package does not exist in hash table.
			Monitor.log.warn("Log entry package with id " + id + " does not exist! Nothing queued.");
			return;
		} else {
			// retrieve log entry package from hash table, then remove it from hash table and add it to queue,
			// so it can be picked up by a LogDatabaseWriteWorker and persisted to the log database asynchronously.
			// It should be noted that there is no check for completeness of a log entry package. Completeness checks 
			// only exist for individual log entries.
			LogEntryPackage p = getLogEntryPackage(id);

			partialLogEntryPackages.remove(id);

			synchronized ( queue ) {
				queue.add(p);
				queue.notify();
			}
		}
	}

	public void stop() throws SQLException{
		Monitor.log.info("Log entry package manager waiting for queue finalization...");
		while(!queue.isEmpty()){
			// wait until workerPool have finished persisting all items.
		}
		Monitor.log.info("Log entry package manager closing database connection...");
		this.connection.close();
	}

	public static void initInstance(String jdbcDriverClassName, 
			String jdbcProtocolPrefix, 
			String logDbHostname, 
			int logDbPort,
			String logDbDatabaseName,
			String logDbLogin, String logDbPass,
			int numWorkers, int keepAlive) throws ClassNotFoundException, SQLException{
		instance = new LogEntryPackageManager(jdbcDriverClassName, 
				jdbcProtocolPrefix, 
				logDbHostname, 
				logDbPort,
				logDbDatabaseName,
				logDbLogin, logDbPass,
				numWorkers, keepAlive);
	}

	public static LogEntryPackageManager getInstance(){
		return instance;
	}
}
