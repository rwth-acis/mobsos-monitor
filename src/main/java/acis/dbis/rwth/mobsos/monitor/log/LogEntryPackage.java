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
public class LogEntryPackage {

	private UUID id;
	private LogEntryPackageWorker worker;

	public LogEntryPackage(){
		id = UUID.randomUUID();
	} 

	public UUID getId() {
		return id;
	}

	public LogEntryPackageWorker getWorker() {
		return worker;
	}

	public void setWorker(LogEntryPackageWorker worker) {
		this.worker = worker;
	}

	public void write(){
		
	}
	
	/*{

		// since we consider log entry packages as atomic units to write to the log database, we explicitly use transactions.
		// A commit only takes place if all complete entries in a log entry package could be written successfully within a transaction.
		// If an error occurs for writing one log entry, we roll back to guarantee consistency and completeness among log entries. 


		if(requestLogEntry.isComplete()){

			Connection c = null;

			//access current DB connection via one of the log entries' prepared statements
			c = this.getWorker().getPreparedStatement(LogEntry.SESSION_TYPE).getConnection();
			boolean ses,ups,req,con,inv,exp,hdr;


			ses = sessionLogEntry.write();
			ups = sessionUpdateLogEntry.write();
			req = requestLogEntry.write();

			// writing requestLogEntry log entry includes reception of new requestLogEntry identifier.
			// since all subsequent log entries depend on requestLogEntry identifier, set accordingly.
			invocationLogEntry.setId(requestLogEntry.getId());
			contextLogEntry.setId(requestLogEntry.getId());
			exceptionLogEntry.setId(requestLogEntry.getId());
			headersLogEntry.setId(requestLogEntry.getId());

			// write remaining log entries for service invocations, contextLogEntry, and exceptions
			inv = invocationLogEntry.write();
			con = contextLogEntry.write();
			exp = exceptionLogEntry.write();
			hdr = headersLogEntry.write();

			// commit as soon as all write operations were completed and no error occurred
			// in the meantime.
			c.commit();

			MobSOSLogger.log("Log entry package " + this.getId() + " successfully written and committed (" + writtenEntries(ses,ups,req,con,inv,exp,hdr) + ").", LasLogger.LOGLEVEL_VERBOSE);

		} else {
			MobSOSLogger.logWarning("No attempt to write log entry package " + this.getId() + " due to incomplete request log entry.");
		}
	}*/

}
