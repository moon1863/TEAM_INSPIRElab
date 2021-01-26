package team.vehicle;

import java.time.Duration;

import team.Task;
import team.tool.Data;
import team.transportion.energy.FuelingStation;
import team.transportion.energy.FuelingStyle;

// internal class for fueling station choosing
/*
 * we will filter, then get a good choose
 */
public class Estimator{
	
	private int taskIndex;
	private boolean isNeedFueling;
	private boolean haveFoundOne;
	private Data dist;
	private FuelingStation station;
	private FuelingStyle style;
	private Vehicle vehicle;
	private Duration duration;
	private Data remainMileage;
	private Task goForTask;
	private Task comeBackTask;
	
	private Estimator(Builder builder) {
		// required parameter
		this.isNeedFueling = builder.isNeedFueling; 		// when we have all the information, it means at sometime, we fueling
		this.haveFoundOne = builder.haveFoundOne;
		
		this.taskIndex = builder.taskIndex;
		this.dist = builder.distance;
		this.station = builder.station;
		this.style = builder.style;
		this.vehicle = builder.vehicle;
		this.duration = builder.duration;
		this.remainMileage = builder.remainMileage;
		this.goForTask = builder.goForTask;
		this.comeBackTask = builder.comeBackTask;
	}
	
	public static class Builder {
		// must have member variable
		private final boolean isNeedFueling;
		
		private boolean haveFoundOne;
		private int taskIndex;
		private Data distance;
		private FuelingStation station;
		private FuelingStyle style;
		private Vehicle vehicle;
		private Duration duration;
		private Data remainMileage;
		private Task goForTask;
		private Task comeBackTask;
		
		public Builder(boolean isNeedFueling) {	this.isNeedFueling = isNeedFueling;	}
		
		public Builder haveFoundOne(boolean haveFoundOne) {	this.haveFoundOne = haveFoundOne; return this; }
		public Builder taskIndex(int taskIndex) { this.taskIndex = taskIndex; return this; }
		public Builder distance(Data distance) { this.distance = distance; return this;	}
		public Builder station(FuelingStation station) { this.station = station; return this; }
		public Builder style(FuelingStyle style) { this.style = style; return this; }
		public Builder vehicle(Vehicle vehicle) { this.vehicle = vehicle; return this; }
		public Builder duration(Duration duration) { this.duration = duration; return this; }
		public Builder remainMileage(Data remainMileage) { this.remainMileage = remainMileage; return this; }
		public Builder goForTask(Task goForTask) { this.goForTask = goForTask; return this; }
		public Builder comeBackTask(Task comeBackTask) { this.comeBackTask = comeBackTask; return this; }
		
		public Estimator build() {
			return new Estimator(this);
		}
	}
	
	@Override
	public String toString() {
		return "Estimator [taskIndex=" + taskIndex + ", isNeedFueling=" + isNeedFueling + ", haveFoundOne="
				+ haveFoundOne + ", dist=" + dist + ", station=" + station + ", style=" + style + ", vehicle=" + vehicle
				+ ", duration=" + duration + ", remainMileage=" + remainMileage + ", goForTask=" + goForTask
				+ ", comeBackTask=" + comeBackTask + "]";
	}

	public Data getDist() {
		return dist;
	}

	public FuelingStation getStation() {
		return station;
	}

	public FuelingStyle getStyle() {
		return style;
	}

	public Vehicle getVehicle() {
		return vehicle;
	}

	public Duration getDuration() {
		return duration;
	}

	public boolean chargingInit() {
		return false;
	}

	public Data getRemainMileage() {
		return remainMileage;
	}

	public boolean isNeedFueling() {
		return isNeedFueling;
	}

	public void setNeedFueling(boolean isNeedFueling) {
		this.isNeedFueling = isNeedFueling;
	}
	
	public boolean isHaveFound() {
		return haveFoundOne;
	}
	
	public void setHaveFoundOne(boolean haveFoundOne) {
		this.haveFoundOne = haveFoundOne;
	}

	public int getTaskIndex() {
		return taskIndex;
	}

	public Task getGoForTask() {
		return goForTask;
	}

	public Task getComeBackTask() {
		return comeBackTask;
	}

}
