package edance.sceeneditor;

import java.util.Calendar;
import java.util.HashMap;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class MetaData {
	//{"Title","Speaker","Date dd/mm/yyyy", 
	//"Starting time hh:mm", "Finishing Time hh:mm", "Venue", "Session", "Conference"}
	@XmlElement
	public String title="";
	@XmlElement
	public String speaker="";
	@XmlElement
	public Calendar startDateTime= Calendar.getInstance();
	@XmlElement
	public Calendar stopDateTime=Calendar.getInstance();
	@XmlElement
	public String venue="";
	@XmlElement
	public String session="";
	@XmlElement
	public String conference="";
	@XmlElement
	public String choreographer="";
	
	public HashMap<String,String> otherMetadata= new HashMap<String,String>();


	public void setConference(String conference) {
		this.conference = conference;
	}

	public void setSession(String session) {
		this.session = session;
	}

	public void setSpeaker(String speaker) {
		this.speaker = speaker;
	}

	public void setStartDateTime(Calendar startDateTime) {
		this.startDateTime = startDateTime;
	}

	public void setStopDateTime(Calendar stopDateTime) {
		this.stopDateTime = stopDateTime;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void setVenue(String venue) {
		this.venue = venue;
	}

	public void setChoreographer(String choreographer) {
		this.choreographer = choreographer;
	}
	
	public void setOtherMetadata(String field, String value){
		otherMetadata.put(field,value);
	}
	
	public String getOtherMetadata(String field){
		return otherMetadata.get(field);
	}

	public int getOtherMetadataLength() {
		return otherMetadata.size();
	}
	
	public String getOtherMetadata(int field){
		String key = otherMetadata.keySet().toArray(new String[0])[field];
		return otherMetadata.get(key);
		
	}
	
	public String getOtherMetadataKey(int field){
		return otherMetadata.keySet().toArray(new String[0])[field];		
	}
}
