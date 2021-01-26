package team.agent;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.logging.Level;

import com.vividsolutions.jts.geom.Coordinate;

import repast.simphony.engine.schedule.ScheduledMethod;
import team.HailingAnswer;
import team.Task;
import team.transportion.energy.FuelingStation;
import team.vehicle.Vehicle;
import static team.GlobalSetting.*;

/**
 * @author houch
 *
 */
public class Traveler extends BaseAgent implements Serializable, Cloneable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 6546424240945024414L;
	public enum PersonStatus {HALTING, DRIVING, WAITINGANSWER, WALKING, WAITINGPICKUP};
	
	private String ID;
	private String houseHoldID; 
	private PersonStatus personStatus = PersonStatus.HALTING;
	private Vehicle privateVehicle, rentedVehicle;
	private Coordinate coord;
	private String zipcode;
	private String tract;
	private LinkedList<Task> backupTasks;
	private LinkedList<Task> tasks;
	private Task currentTask;
	private HailingAnswer answer;
	private boolean isAdded;
	private boolean finished = true;
	private boolean homePlugOwner;
	private FuelingStation privateHomePlug;

	
	@ScheduledMethod(start = 1, interval = 1, priority = LEVEL7)
	public void personScheduling() {
		if (privateVehicle != null) {
			privateVehicle.vehicleRunning();
		} else
			personRunning();
	}
	
	@Override
	public Object clone() throws CloneNotSupportedException {
		// TODO Auto-generated method stub
		Traveler person = (Traveler) super.clone();
		person.setBackupTasks(new LinkedList<Task>());
		person.setTasks(new LinkedList<Task>());
		for(int i = 0; i < backupTasks.size(); i++) {
			person.getBackupTasks().add((Task) backupTasks.get(i).clone());
			person.getTasks().add((Task) backupTasks.get(i).clone());
		}
		return person;
	}
	
	public void personRunning() {
		if(privateVehicle != null) return; // if a person has a private vehicle
		// vehicle the schedule
		switch (personStatus) {
		case HALTING:
			if(tasks.isEmpty()) {
				engine.getLOGGER().log(Level.INFO, "Task is empty, stay original place");
				break;
			}
			if(currentTask != null)
				currentTask = tasks.getFirst();
			if(currentTask.departureIsDue()) {
				sendTask(currentTask, engine.getCompany());
				personStatus = PersonStatus.WAITINGANSWER;
			}
			break;
			
		case WAITINGANSWER:
			if(answer == null) 
				break;
			rentedVehicle = answer.getRentedVehicle();
			personStatus = PersonStatus.WAITINGPICKUP;
			answer = null;
			break;
	
		case WAITINGPICKUP:
			if(rentedVehicle.isPickupArrived())
				personStatus = PersonStatus.DRIVING;
			break;
			
		case DRIVING:
			if(rentedVehicle.isRideHailingFinished())
				personStatus = PersonStatus.HALTING;
			break;
			
		default:
			break;
		}
	}
	
	private void sendTask(Task task, TransportNetworkCompany company) {
		// TODO Auto-generated method stub
		company.tasksWaitedProcess.add(task);
	}

	public Coordinate getCoord() {
		return coord;
	}

	public void setCoord(Coordinate coord) {
		this.coord = coord;
	}
	
	public void updateCoord(Coordinate coord) {
		engine.getGeography().move(this, geomFac.createPoint(coord));
		setCoord(coord);
	}
	public void updateCoordByVetor(double length, double angle) {
		engine.getGeography().moveByVector(this, length, angle);
		setCoord(engine.getGeography().getGeometry(this).getCoordinate());
	}
	
	public boolean isAdded() {
		return isAdded;
	}


	public void setAdded(boolean isAdded) {
		this.isAdded = isAdded;
	}


	public LinkedList<Task> getTasks() {
		return tasks;
	}

	public void setTasks(LinkedList<Task> tasks) {
		this.tasks = tasks;
	}

	public Vehicle getRentedVehicle() {
		return rentedVehicle;
	}

	public void setRentedVehicle(Vehicle rentedVehicle) {
		this.rentedVehicle = rentedVehicle;
	}

	public String getID() {
		return ID;
	}

	public Vehicle getPrivateVehicle() {
		return privateVehicle;
	}

	public void setPrivateVehicle(Vehicle privateVehicle) {
		this.privateVehicle = privateVehicle;
	}

	public String getHouseHoldID() {
		return houseHoldID;
	}

	public String getZipcode() {
		return zipcode;
	}

	public void setZipcode(String zipcode) {
		this.zipcode = zipcode;
	}



	public LinkedList<Task> getBackupTasks() {
		return backupTasks;
	}



	public void setBackupTasks(LinkedList<Task> backupTasks) {
		this.backupTasks = backupTasks;
	}


	public String getTract() {
		return tract;
	}


	public void setTract(String tract) {
		this.tract = tract;
	}


	public boolean isFinished() {
		return finished;
	}


	public void setFinished(boolean finished) {
		this.finished = finished;
	}

	public boolean isHomePlugOwner() {
		return homePlugOwner;
	}

	public void setHomePlugOwner(boolean homePlugOwner) {
		this.homePlugOwner = homePlugOwner;
	}

	public FuelingStation getPrivateHomePlug() {
		return privateHomePlug;
	}

	public void setPrivateHomePlug(FuelingStation privateHomePlug) {
		this.privateHomePlug = privateHomePlug;
	}

}
