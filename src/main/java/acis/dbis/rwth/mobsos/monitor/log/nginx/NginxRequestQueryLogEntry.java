package acis.dbis.rwth.mobsos.monitor.log.nginx;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Hashtable;

import acis.dbis.rwth.mobsos.monitor.log.LogEntry;
import acis.dbis.rwth.mobsos.monitor.log.LogEntryPackage;

public class NginxRequestQueryLogEntry extends LogEntry {

	public NginxRequestQueryLogEntry(LogEntryPackage container) {
		super(container);
	}

	private Hashtable<String,String> queryParams;

	@Override
	public boolean isComplete() {
		// consider log entry as complete, if id is set from parent request log entry and if query params table is not empty.
		if(getId()!=null && getQueryParams() !=null && getQueryParams().size()>0){
			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean write(Connection c) throws SQLException {

		PreparedStatement stmt = null;

		try{

			if(isComplete()){

				stmt = c.prepareStatement("insert into mobsos_logs.log_query(id,name,value) values (?,?,?)");

				for(String key: getQueryParams().keySet()){
					stmt.setLong(1,getId());
					stmt.setString(2,key);
					stmt.setString(3, getQueryParams().get(key));
					stmt.addBatch();
				}

				stmt.executeBatch();

				return true;
			} else {
				return false;
			}
		}
		catch(SQLException e){
			throw e;
		}
		finally {
			if(stmt != null) stmt.close();
		}

	}

	/**
	 * @return the queryParams
	 */
	public Hashtable<String,String> getQueryParams() {
		return queryParams;
	}

	/**
	 * @param queryParams the queryParams to set
	 */
	public void setQueryParams(Hashtable<String,String> queryParams) {
		this.queryParams = queryParams;
	}

}
