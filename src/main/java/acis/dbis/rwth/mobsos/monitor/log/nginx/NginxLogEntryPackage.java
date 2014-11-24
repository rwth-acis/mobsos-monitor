package acis.dbis.rwth.mobsos.monitor.log.nginx;

import com.mysql.jdbc.Connection;

import acis.dbis.rwth.mobsos.monitor.Monitor;
import acis.dbis.rwth.mobsos.monitor.log.LogEntryPackage;

public class NginxLogEntryPackage extends LogEntryPackage{

	public void write(){

		// since we consider log entry packages as atomic units to write to the log database, we explicitly use transactions.
		// A commit only takes place if all complete entries in a log entry package could be written successfully within a transaction.
		// If an error occurs for writing one log entry, we roll back to guarantee consistency and completeness among log entries. 

/*
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

			Monitor.log.info("Log entry package " + this.getId() + " successfully written and committed (" + writtenEntries(ses,ups,req,con,inv,exp,hdr) + ").", LasLogger.LOGLEVEL_VERBOSE);

		} else {
			Monitor.log.warn("No attempt to write log entry package " + this.getId() + " due to incomplete request log entry.");
		}
		*/
	}
}
