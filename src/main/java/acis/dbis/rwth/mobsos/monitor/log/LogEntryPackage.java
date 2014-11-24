 package acis.dbis.rwth.mobsos.monitor.log;

import java.util.UUID;

/**
 * This class is the central unit of processing in the MobSOS Monitoring framework. Any instance contains all information, collected
 * during one single LAS request. Thereby, both request and corresponding {@link LogEntryPackage} instance share the same internal id.
 * This id is also used to locate a partial log entry package in the hash table of {@link LogEntryPackageManager}.
 * 
 * A {@link LogEntryPackage} contains information on: 
 *
 * <ul>
 * 	<li>the session in whose context a request was submitted (cf. {@link SessionLogEntry} for static and {@link SessionUpdateLogEntry} 
 * for dynamic session information such as session end time),</li>
 * 	<li>the request itself (cf. {@link RequestLogEntry}),
 * 	<li>the dynamic context of the request (cf. {@link ContextLogEntry}),
 * 	<li>the invocation of a LAS service method (cf. {@link InvocationLogEntry}), and
 * 	<li>any exceptions/errors occurred during request processing (cf. {@link ExceptionLogEntry}).</li>
 * </ul>  
 * 
 * Method {@link LogEntryPackage#write()} is the central task method executed by {@link LogEntryPackageWorker} instances. It defines 
 * how and in which order individual {@link LogEntry} instances should be written to the database. Writing order is imposed by data 
 * integrity constraints defined in the MobSOS monitoring database schema. The {@link SessionLogEntry} must be written before any 
 * log entry describing a request in the context of that session can be written. The {@link SessionUpdateLogEntry} can only be executed, 
 * if session information is already persisted in the MobSOS monitoring database. The {@link RequestLogEntry} must must be written before 
 * {@link ContextLogEntry},{@link InvocationLogEntry} and {@link ExceptionLogEntry} can be written. Furthermore, each {@link LogEntry} 
 * defines its own method {@link LogEntry#write()}, which will one have an effect, if the contained information is complete.
 * 
 * @author Dominik Renzel
 * @version $Revision: 1.1.2.8 $, $Date: 2014/02/05 12:59:20 $
 */
public abstract class LogEntryPackage {

	private UUID id;
	private LogEntryPackageWorker worker;

	public LogEntryPackage(){
		this.id = UUID.randomUUID();
	} 

	public UUID getId() {
		return this.id;
	}

	public LogEntryPackageWorker getWorker() {
		return this.worker;
	}

	public void setWorker(LogEntryPackageWorker worker) {
		this.worker = worker;
	}

	public abstract void write();

}
