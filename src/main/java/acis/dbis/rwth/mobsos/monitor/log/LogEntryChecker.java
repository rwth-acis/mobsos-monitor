package acis.dbis.rwth.mobsos.monitor.log;

public class LogEntryChecker {
	public static String checkTimes(Long start, Long end, String type, boolean tolerateFutureEndTime){
		String warningMessage = "";

		if(start == null || start == 0l){
			warningMessage += type + " start time not set! ";
		}
		
		if(end == null || end == 0l){
			warningMessage += type + " end time not set! ";
		}

		// consider log entry incomplete, if session start/end times are invalid, because in future or negative
		if(start != null){ 
			if(start < 0l){warningMessage += type + " start time invalid (negative)! ";}
			else if(start > System.currentTimeMillis()){warningMessage += type + " start time invalid (in future)! ";}
		}
		
		if(end != null){ 
			if(end < 0l){warningMessage += type + " end time invalid (negative)! ";}
			else if(!tolerateFutureEndTime && end > System.currentTimeMillis()){warningMessage += type + " end time invalid (in future)! ";}
		}

		// consider log entry incomplete, if start time is after invocation end time
		if(start != null && end != null && start > end){
			warningMessage += type + " start time after end time! ";
		}
		
		return warningMessage;
	}
}
