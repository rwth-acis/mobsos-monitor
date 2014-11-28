package acis.dbis.rwth.mobsos.monitor.log;

import java.sql.SQLException;

/**
 * This abstract class defines the principal functionality of different log entry types. For any logging information managed by an instance,
 * it must be clear if data is complete (cf. {@link LogEntry#isComplete()} and how it should be written to the MobSOS monitoring database, 
 * according to its type (cf {@link LogEntry#write()}. In particular, MobSOS defines six types of log entries. Any {@link LogEntryPackage} 
 * instance will contain one instance per type, where all instance in a package altogether describe a LAS request, as processed by a 
 * {@link HttpConnectorRequestHandler}.
 *  
 * @author Dominik Renzel
 * @version $Revision: 1.1.2.3 $, $Date: 2014/01/14 16:32:39 $
 */
public abstract class LogEntry {
	
	private Long id;
	private static int type;

	private LogEntryPackage container;
	
	public LogEntry(LogEntryPackage container){
		this.container = container;
	}
	
	/**
	 * @return the id
	 */
	public Long getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(Long id) {
		this.id = id;
	}
	
	/**
	 * @return the type
	 */
	public int getType() {
		return type;
	}

	public LogEntryPackage getContainer() {
		return container;
	}

	public void setContainer(LogEntryPackage container) {
		this.container = container;
	}

	public abstract boolean isComplete();
	
	public abstract boolean write() throws SQLException;
	
}
