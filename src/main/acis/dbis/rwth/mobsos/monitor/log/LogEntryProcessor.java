package acis.dbis.rwth.mobsos.monitor.log;

public abstract class LogEntryProcessor {
	
	/**
	 * processes a given {@link LogEntryPackage}. Processing is done with the main purpose of data completion,
	 * either within or outside the scope of a {@link LogEntryPackage}. 
	 * 
	 * An example for internal log data processing is the retrieval of a user id, given an OIDC token transported in a header.
	 * An example for external log data processing is the retrieval of geo-location data, given a remote address.
	 * 
	 * @param p
	 * @return LogEntryPackage
	 */
	public abstract LogEntryPackage process(LogEntryPackage p);
}
