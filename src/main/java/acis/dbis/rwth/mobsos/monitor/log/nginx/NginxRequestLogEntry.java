package acis.dbis.rwth.mobsos.monitor.log.nginx;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.csv.CSVRecord;

import sun.security.action.GetLongAction;
import acis.dbis.rwth.mobsos.monitor.Monitor;
import acis.dbis.rwth.mobsos.monitor.log.LogEntry;
import acis.dbis.rwth.mobsos.monitor.log.LogEntryPackage;

public class NginxRequestLogEntry extends LogEntry {

	private String time, ip, scheme, host, method, uri, status, referer, userAgent, accept, receivedContent, sentContent, requestLength, responseLength, requestTime, clientId, token, uid;

	public NginxRequestLogEntry(LogEntryPackage container) {
		super(container);
	}

	@Override
	public boolean isComplete() {
		return true;
	}

	@Override
	public boolean write() {
		if(isComplete()){

			try{
				// get corresponding prepared statement, then clear and set parameters
				Connection c = this.getContainer().getWorker().getConnection();

				//time, ip, scheme, host, method, uri, status, referer, userAgent, accept, content_type, requestLength, responseLength, requestTime;
				PreparedStatement stmt = c.prepareStatement("insert into mobsos_logs.log(time,ip,scheme,host,method,uri,status,referer,useragent,accept,received_content,sent_content,request_length,response_length,request_time) "
						+ "values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
				
				if(uri.endsWith("/")){
					uri = uri.substring(0,uri.length()-1);
				}
				
				stmt.clearParameters();
				stmt.setTimestamp(1, new Timestamp(DatatypeConverter.parseDateTime(time).getTimeInMillis()));
				stmt.setString(2,ip);
				stmt.setString(3,scheme);
				stmt.setString(4,host);
				stmt.setString(5,method);
				stmt.setString(6,uri);
				stmt.setInt(7,Integer.parseInt(status));
				stmt.setString(8, referer);
				stmt.setString(9, userAgent);
				stmt.setString(10, accept);
				stmt.setString(11, receivedContent);
				stmt.setString(12, sentContent);
				
				stmt.setInt(13,Integer.parseInt(requestLength));
				stmt.setInt(14,Integer.parseInt(responseLength));
				stmt.setFloat(15,Float.parseFloat(requestTime));
				
				stmt.executeUpdate();
				
				ResultSet rs = stmt.getGeneratedKeys();

				if (rs.next()) {
					Long nid = rs.getLong(1);
					this.setId(nid);
					return true;
				} else {
					return false;
				}
			} catch (SQLException e){
				e.printStackTrace();
				return false;
			}

		} else {
			return false;
		}
	}

	/**
	 * @return the time
	 */
	public String getTime() {
		return time;
	}

	/**
	 * @param time the time to set
	 */
	public void setTime(String time) {
		this.time = time;
	}

	/**
	 * @return the ip
	 */
	public String getIp() {
		return ip;
	}

	/**
	 * @param ip the ip to set
	 */
	public void setIp(String ip) {
		this.ip = ip;
	}

	/**
	 * @return the scheme
	 */
	public String getScheme() {
		return scheme;
	}

	/**
	 * @param scheme the scheme to set
	 */
	public void setScheme(String scheme) {
		this.scheme = scheme;
	}

	/**
	 * @return the host
	 */
	public String getHost() {
		return host;
	}

	/**
	 * @param host the host to set
	 */
	public void setHost(String host) {
		this.host = host;
	}

	/**
	 * @return the method
	 */
	public String getMethod() {
		return method;
	}

	/**
	 * @param method the method to set
	 */
	public void setMethod(String method) {
		this.method = method;
	}

	/**
	 * @return the uri
	 */
	public String getUri() {
		return uri;
	}

	/**
	 * @param uri the uri to set
	 */
	public void setUri(String uri) {
		this.uri = uri;
	}

	/**
	 * @return the status
	 */
	public String getStatus() {
		return status;
	}

	/**
	 * @param status the status to set
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * @return the referer
	 */
	public String getReferer() {
		return referer;
	}

	/**
	 * @param referer the referer to set
	 */
	public void setReferer(String referer) {
		this.referer = referer;
	}

	/**
	 * @return the userAgent
	 */
	public String getUserAgent() {
		return userAgent;
	}

	/**
	 * @param userAgent the userAgent to set
	 */
	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}

	/**
	 * @return the accept
	 */
	public String getAccept() {
		return accept;
	}

	/**
	 * @param accept the accept to set
	 */
	public void setAccept(String accept) {
		this.accept = accept;
	}

	/**
	 * @return the receivedContent
	 */
	public String getReceivedContent() {
		return receivedContent;
	}

	/**
	 * @param receivedContent the receivedContent to set
	 */
	public void setReceivedContent(String receivedContent) {
		this.receivedContent = receivedContent;
	}

	/**
	 * @return the sentContent
	 */
	public String getSentContent() {
		return sentContent;
	}

	/**
	 * @param sentContent the sentContent to set
	 */
	public void setSentContent(String sentContent) {
		this.sentContent = sentContent;
	}

	/**
	 * @return the requestLength
	 */
	public String getRequestLength() {
		return requestLength;
	}

	/**
	 * @param requestLength the requestLength to set
	 */
	public void setRequestLength(String requestLength) {
		this.requestLength = requestLength;
	}

	/**
	 * @return the responseLength
	 */
	public String getResponseLength() {
		return responseLength;
	}

	/**
	 * @param responseLength the responseLength to set
	 */
	public void setResponseLength(String responseLength) {
		this.responseLength = responseLength;
	}

	/**
	 * @return the requestTime
	 */
	public String getRequestTime() {
		return requestTime;
	}

	/**
	 * @param requestTime the requestTime to set
	 */
	public void setRequestTime(String requestTime) {
		this.requestTime = requestTime;
	}

	/**
	 * @return the clientId
	 */
	public String getClientId() {
		return clientId;
	}

	/**
	 * @param clientId the clientId to set
	 */
	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	/**
	 * @return the token
	 */
	public String getToken() {
		return token;
	}

	/**
	 * @param token the token to set
	 */
	public void setToken(String token) {
		this.token = token;
	}

	/**
	 * @return the uid
	 */
	public String getUid() {
		return uid;
	}

	/**
	 * @param uid the uid to set
	 */
	public void setUid(String uid) {
		this.uid = uid;
	}

}
