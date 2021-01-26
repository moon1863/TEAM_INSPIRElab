package team;

import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.vividsolutions.jts.geom.Coordinate;

import team.geography.FixedGeography;
import team.geography.transportation.Route;

import static team.GlobalSetting.*;

/**
 * For Task structure, we have some hypothesis, which is if we set the depart time is 03:00:00
 * It means this is a depart time as soon as possible, it usually appear when we have insert a fueling task
 * After the triangle seeking
 * @author houch
 *
 */

public class Task implements Serializable, Cloneable{
	private static final long serialVersionUID = -1804163821279402828L;
	private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
	public String taskID;
	private String purposeID;
	transient private Route route;
	private String departTimeLiteral;	// the most early time to depart
	private String arriveTimeLiteral;   // the most late time to arrive
	
	private Coordinate departCoord;
	private Coordinate arriveCoord;
	private LocalDateTime departTime;	// the most early time to depart
	private LocalDateTime arriveTime;   // the most late time to arrive
	transient private Duration maxDuration;
	
	private boolean isGorForTask;
	private boolean isComebackTask;
	
	public Task(Coordinate departCoord, Coordinate arriveCoord, LocalDateTime departTime, LocalDateTime arriveTime) {
		this.departCoord = departCoord;
		this.arriveCoord = arriveCoord;
		this.departTime = departTime;
		this.arriveTime = arriveTime;
		this.maxDuration = Duration.between(departTime, arriveTime);
	}
	
	public Task(Coordinate departCoord, Coordinate arriveCoord) {
		this.departCoord = departCoord;
		this.arriveCoord = arriveCoord;
	}
	
	public Task(Coordinate coord, FixedGeography geog, LocalDateTime departTime, LocalDateTime arriveTime) {
		this(coord, geog.getCoord(), departTime, arriveTime);
	}

	public Task(FixedGeography geog, Coordinate coord, LocalDateTime departTime, LocalDateTime arriveTime) {
		this(geog.getCoord(), coord, departTime, arriveTime);
	}
	
	
	
	@Override
	public Object clone() throws CloneNotSupportedException {
		// TODO Auto-generated method stub
	    Task copy = null;  
	    try {  
	    	copy = (Task)super.clone();  
	    	copy.departTime = LocalDateTime.from(departTime);
	    	copy.arriveTime = LocalDateTime.from(arriveTime);
	    } catch (CloneNotSupportedException e) {} // Won't happen  
	    return copy; 
	}

	public boolean departureIsDue() {
		return engine.getRealTime().equals(getDepartTime()) ||
				engine.getRealTime().isAfter(getDepartTime());
	}
	
	public void convertTime() {
		departTime = LocalDateTime.parse(departTimeLiteral, formatter);
		arriveTime = LocalDateTime.parse(arriveTimeLiteral, formatter);
	}
	
	public LocalDateTime getDepartTime() {
		return departTime;
	}
	public void setDepartTime(LocalDateTime departTime) {
		this.departTime = departTime;
	}
	public LocalDateTime getArriveTime() {
		return arriveTime;
	}
	public void setArriveTime(LocalDateTime arriveTime) {
		this.arriveTime = arriveTime;
	}
	public Route getRoute() {
		if(route == null) return route = new Route(departCoord, arriveCoord);
		return route;
	}

	public Duration getMaxDuration() {
		if(maxDuration == null) this.maxDuration = Duration.between(departTime, arriveTime);
		return maxDuration;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("TaskID is " + taskID);
		builder.append("\ndepartPosition is " + departCoord);
		builder.append("  arrivePosition is " + arriveCoord);
		builder.append("\ndepartTime is " + departTime);
		builder.append("arriveTime is " + arriveTime + "\n");
//		builder.append(this.getRoute().toString());
		return builder.toString();
	}

	public Coordinate getDepartCoord() {
		return departCoord;
	}

	public void setDepartCoord(Coordinate departCoord) {
		this.departCoord = departCoord;
	}

	public Coordinate getArriveCoord() {
		return arriveCoord;
	}

	public void setArriveCoord(Coordinate arriveCoord) {
		this.arriveCoord = arriveCoord;
	}

	public String getPurposeID() {
		return purposeID;
	}

	public boolean isGorForTask() {
		return isGorForTask;
	}

	public void setGorForTask(boolean isGorForTask) {
		this.isGorForTask = isGorForTask;
	}

	public boolean isComebackTask() {
		return isComebackTask;
	}

	public void setComebackTask(boolean isComebackTask) {
		this.isComebackTask = isComebackTask;
	}


}
