package team.vehicle;

import java.io.Serializable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.PriorityQueue;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;

import repast.simphony.engine.schedule.ScheduledMethod;
import team.HasCoordinate;
import team.Task;
import team.agent.Traveler;
import team.geography.transportation.Route;
import team.tool.ClosestDistance;
import team.tool.Data;
import team.tool.Data.Unit;
import team.transportion.energy.FuelingStation;
import team.transportion.energy.FuelingStyle;
import team.vehicle.Model.VehicleType;

import static team.GlobalSetting.*;
import static team.tool.MyFunction.*;

/**
 * In our design philosophy, every vehicle is artificially intelligent It means
 * Every vehicle will pick up automatically, whatever it is, private or
 * ride-hailing In fact, if you have a private car, the car will follow you, or
 * if you choose ride-hailing the driver of ride-hailing will receive a task and
 * pick up you, it almost like an artificial intelligence So it could be
 * unified, that's our basic idea
 * 
 * we only have two kinds of vehicle, pure electrical vehicle or fossil fuel,
 * for phev, we think it only add some mileage before for the first trip, it
 * 
 * @author Chuang Hou
 */

@SuppressWarnings("serial")
public class Vehicle implements HasCoordinate, Serializable{
	static int counter = 0;
	
	private String ID;
	private Type operType;

	private Model model;
	private Task currentTask;
	private Coordinate coord; // record
	private Traveler owner;
	
	private Data speed; // unit mile per hour
	private Route route;
	private List<FuelingStyle> chargingStyles;

	private Task beforeChargingTask; // before this task, this vehicle need a fueling
	private FuelingStation chargingStation;
	private FuelingStyle chargingStyle;

	private Data chargableMileage;	// max mileage when it is charged outside 
	private Data mileage;
	private Data safeLimitMileage;
	private Data remainCapacity;		// corrected fueling capacity for all kinds of vehicle, PHEV BEV FCEV
	private Data chargableCapacity;		// max capacity when it is charged outside 
	private Data milesPerUnitFueling;	// how many miles consuming one unit fueling
	private Data remainMileage;         // how many mileage remaining for current capacity
									
	private Iterator<Coordinate> iterCoord;
	private Coordinate s;
	private Coordinate t;
	private Data stLength;
	private Data stepDist;
	private Data accuLength;
	private boolean pickupArrived;
	private Estimator estimator;
	
	
	private boolean wantCharge = false;
	private boolean atHome = true;
	private boolean canCharge = false;
	private double energyRem;
	private double chargeLoadElectric;
	private double chargeLoadGASOLINE;
	private double chargeLoadDIESEL;
	private double chargeLoadHYDROGEN;
	private boolean conenctedSOC = false;
	
	private Task firstTask;
	private Comparator<Estimator> comparator;

	public LinkedList<Task> tasks;
	public MotionStatus motionStatus;
	
	private Duration[] intervalDuras = new Duration[maxAnticipate];
	private Duration[] routeDurations = new Duration[maxAnticipate];
	private Duration[] taskDurs = new Duration[maxAnticipate];
	private Duration[] onRouteAbunDuras = new Duration[maxAnticipate];
	private Duration[] interAbunDuras = new Duration[maxAnticipate];
	private boolean rideHailingFinished;

	// WANDERING just for ride hailing
	public enum MotionStatus {HOMEPARKING, HOMECHARGING, PARKING, DRIVING, CHARGING, FUELINGRUNNING, CRUISING, STOP };
	public enum Type { TNC, PRIVATE };
	// TODO add schedule procedure styles

	public Vehicle(Traveler person, Model model, Coordinate coord, Comparator<Estimator> comparator, Type operType) {
		this.owner = person;
		this.ID = person.getID();
		this.model = model;
		this.coord = coord;
		this.comparator = comparator;
		this.operType = operType;

		// this speed data should be updated for every road
		this.speed = new Data(30, Unit.MPH);
		this.motionStatus = MotionStatus.HOMEPARKING;
		this.accuLength = new Data(0, Unit.MILE);
		
		// model initiate
		this.modelInit();
	}
	
	@ScheduledMethod(start = 1, interval = 1, priority = LOWEST)
	public double getChargingRateNumber() {
		if(motionStatus == MotionStatus.CHARGING) 
			return chargingStyle.chargingSpeed.multiply(simIntervalInSecond.transfer(Unit.MINUTE)).num;
		else return 0;
	}
	@ScheduledMethod(start = 1, interval = 1, priority = LOWEST)
	public double getRemainMileageNumberConnected() {
		return motionStatus == MotionStatus.CHARGING ? 0 : getRemainMileAgNumber();
	}
	
	@ScheduledMethod(start = 1, interval = 1, priority = LOWEST)
	public double getRemainMileAgNumber() {
		return remainCapacity.multiply(milesPerUnitFueling).num;
	}
	
	@ScheduledMethod(start = 1, interval = 1, priority = LOWEST)
	public boolean getFinished() {
		return tasks.isEmpty();
	}
	
	public void modelInit() {
		// fueling initiate
		Data maxCapacity;
		if (model.getVehicleType() == Model.VehicleType.BEV) {
			chargingStyles = model.elecFuelingStyles;
			maxCapacity = (Data) model.getBatCapacity().clone();
			chargableCapacity = model.getBatCapacity();
			milesPerUnitFueling = model.getBatMPKWH();
			maxCapacity = (Data) chargableCapacity.clone();
		} else {// FCEV or PHEV
			chargingStyles = model.liquidFuelingStyles;
			maxCapacity = (Data) model.getTankCapacity().clone();
			chargableCapacity = model.getTankCapacity();
			milesPerUnitFueling = model.getTankMPG();
			// PHEV correction
			if (model.getVehicleType() == Model.VehicleType.PHEV)  {
				Data batteryMileage = model.getBatCapacity().multiply(model.getBatMPKWH());
				maxCapacity = batteryMileage.divide(milesPerUnitFueling).plus(maxCapacity);
			}	
		}
		mileage = milesPerUnitFueling.multiply(maxCapacity);
		safeLimitMileage = (Data) mileage.clone();
		safeLimitMileage.num = safeLimitMileage.num * 0.2;
		chargableMileage = milesPerUnitFueling.multiply(chargableCapacity);
		if(!owner.isHomePlugOwner()) maxCapacity.num = maxCapacity.num * Math.random();
		remainCapacity = maxCapacity;
		remainMileage = remainCapacity.multiply(milesPerUnitFueling);
	}

	@Override
	public String toString() {
		return "Vehicle [\nTasks=" + tasks + "ID is" + ID + ", \n\n coord=" + coord
			    + ", \n\nchargingStyle=" + chargingStyle 
				+ ", \n\nremainMileAge=" + remainMileage 
				+ ", \nremainCapacity=" + remainCapacity
				+ ", \n\nmotionStatus=" + motionStatus
				 + "]";
	}

//	@ScheduledMethod(start = 1, interval = 1, priority = LEVEL5)
	public void vehicleRunning() {
		if(operType == null) throw new RuntimeException(" the vehicle's type shoudn't be null");
		if(operType == Type.PRIVATE) privateVehicleStepRunning();
		if(operType == Type.TNC) TNCVehicleStepRunning();
	}
	
	private void TNCVehicleStepRunning() {}
	
	private void privateVehicleStepRunning() {
		boolean hasArrived; // step driving
		boolean isFull;
		
		switch (motionStatus) {
		case HOMEPARKING:
			// get an appropriate task
			if(currentTask == null && !tasks.isEmpty()) 
				currentTask = tasks.getFirst(); // setting the present task once
			
			// current is due
			if (currentTask != null && currentTask.departureIsDue()) {
				estimator = calculateFuelingEstimator();
				
				if (estimator.isNeedFueling()) setWantCharge(true);
				if (estimator.isNeedFueling() && !estimator.isHaveFound()) setCanCharge(true);
				
				if (estimator.isNeedFueling() && estimator.isHaveFound()) {
					chargingInit(estimator);
					currentTask = tasks.getFirst(); // on-route charging need to update current task
				}
				setAtHome(false);
				//statusObersate(motionStatus, MotionStatus.DRIVING);
				motionStatus = MotionStatus.DRIVING;
				//
				break;
			}
			// has a home plug and remainCapacity is smaller than maxChargableCapacity 
			if(model.getVehicleType() == VehicleType.BEV && owner.isHomePlugOwner() 
					&& remainCapacity.smaller(chargableCapacity)) {
				chargingStation = owner.getPrivateHomePlug();
				chargingStyle = FuelingStyle.L2;
				//statusObersate(motionStatus, MotionStatus.HOMECHARGING);
				motionStatus = MotionStatus.HOMECHARGING;
				break;
			}
			
			break;
			
		case HOMECHARGING:
			// get the full status
			isFull = stepCharging();
			// if it is home charging in the morning, it has a current task, it should run when time is due
			if (currentTask != null && currentTask.departureIsDue()) {
				estimator = calculateFuelingEstimator();
				if (estimator.isNeedFueling()) setWantCharge(true);
				if (estimator.isNeedFueling() && !estimator.isHaveFound()) setCanCharge(true);
				if (estimator.isNeedFueling() && estimator.isHaveFound()) {
					chargingInit(estimator);
					currentTask = tasks.getFirst(); // on-route charging need to update current task
				}
				setAtHome(false);
				//statusObersate(motionStatus, MotionStatus.DRIVING);
				motionStatus = MotionStatus.DRIVING;
				break;
			}
			// charging finished transfer to home parking
			if(isFull) {
				//statusObersate(motionStatus, MotionStatus.HOMEPARKING);
				motionStatus = MotionStatus.HOMEPARKING;
				break;
			}
			
			break;
		
		case PARKING:
			if (currentTask != null && currentTask.departureIsDue()) {
				estimator = calculateFuelingEstimator();
				if (estimator.isNeedFueling()) setWantCharge(true);
				if (estimator.isNeedFueling() && !estimator.isHaveFound()) setCanCharge(true);
				if(estimator.isNeedFueling() && estimator.isHaveFound()) {
					chargingInit(estimator);
					currentTask = tasks.getFirst(); // on-route charging need to update current task
				}
				//statusObersate(motionStatus, MotionStatus.DRIVING);
				motionStatus = MotionStatus.DRIVING;
				break;
			}
			
			// current task is not due
			if(beforeChargingTask != null && beforeChargingTask == currentTask) {
				estimator = null;
				beforeChargingTask = null;
				//statusObersate(motionStatus, MotionStatus.CHARGING);
				motionStatus = MotionStatus.CHARGING;
				break;
			}
			
			break;
			
		case DRIVING:
			if(!stepConsuming()) owner.setFinished(false);
			hasArrived = stepDriving();
			
			if(!hasArrived) 					//  hasn't finish current task, directly break, 
				break;							//from now, blow this line, has arrived is true
	
			tasks.removeFirst(); 	// only here remove task from tasks
			currentTask = null; 	// only here set current task finish
			if (!tasks.isEmpty())
				currentTask = tasks.getFirst(); // setting the present task once
			
			if(currentTask == null) {			// 
				setAtHome(true);
				setWantCharge(false);
				setCanCharge(false);
				//statusObersate(motionStatus, MotionStatus.HOMEPARKING);
				motionStatus = MotionStatus.HOMEPARKING;
				break;
			} 
			// from this line, has arrived and current task is not null
			if(currentTask.departureIsDue()) {
				estimator = calculateFuelingEstimator();
				if (estimator.isNeedFueling()) setWantCharge(true);
				if (estimator.isNeedFueling() && !estimator.isHaveFound()) setCanCharge(true);
				if(estimator.isNeedFueling() && estimator.isHaveFound()) {
					chargingInit(estimator);
					currentTask = tasks.getFirst(); // on-route charging need to update current task
				}
				//statusObersate(motionStatus, MotionStatus.DRIVING);
				motionStatus = MotionStatus.DRIVING;
				break;
			}
			// current task is not due
			if(beforeChargingTask != null && beforeChargingTask == currentTask) {
				estimator = null;
				beforeChargingTask = null;
				//statusObersate(motionStatus, MotionStatus.CHARGING);
				motionStatus = MotionStatus.CHARGING;
				break;
			}
			//statusObersate(motionStatus, MotionStatus.PARKING);
			motionStatus = MotionStatus.PARKING;
			break;
			
 		case CHARGING:
 			isFull = stepCharging();
 			
 			if(currentTask.departureIsDue() || isFull) {
 				chargingStation.addAvailableOneOf(chargingStyle);
 			}
 			
 			if(currentTask.departureIsDue() || (isFull && currentTask.isComebackTask())) {
				estimator = calculateFuelingEstimator();
				if (estimator.isNeedFueling()) setWantCharge(true);
				if (estimator.isNeedFueling() && !estimator.isHaveFound()) setCanCharge(true);
				if(estimator.isNeedFueling() && estimator.isHaveFound()) {
					chargingInit(estimator);
					currentTask = tasks.getFirst();
				}
				//statusObersate(motionStatus, MotionStatus.DRIVING);
 				motionStatus = MotionStatus.DRIVING;
 				break;
 			}
 			
 			if(isFull) {
 				//statusObersate(motionStatus, MotionStatus.PARKING);
 				motionStatus = MotionStatus.PARKING;
 				break;
 			}
 			
		default:
			break;
		}
		
		/*
		 * this part is used to organize the data for output
		 */
		
		setEnergyRem(remainCapacity.num);
		
		
		if(this.motionStatus == MotionStatus.CHARGING || this.motionStatus == MotionStatus.HOMECHARGING) {
			switch(chargingStyle) {
			case GASOLINE: this.setChargeLoadGASOLINE(chargingStyle.chargingSpeed.num); break;
			case DIESEL: this.setChargeLoadDIESEL(chargingStyle.chargingSpeed.num); break;
			case HYDROGEN: this.setChargeLoadHYDROGEN(chargingStyle.chargingSpeed.num); break;
			default: setChargeLoadElectric(chargingStyle.chargingSpeed.num); break;
			}
			setConenctedSOC(true);
		}
		else {
			setChargeLoadGASOLINE(0);
			setChargeLoadDIESEL(0);
			setChargeLoadHYDROGEN(0);
			setChargeLoadElectric(0);
			setConenctedSOC(false);
		}
		
	}
	
	private boolean stepDriving() {
		Objects.requireNonNull(currentTask, "The present task is null");
		stepDist = speed.multiply(simIntervalInHour);
		accuLength.num = 0;
		
		if (iterCoord == null) {
			route = currentTask.getRoute();
			iterCoord = route.trackCoordinates.iterator();
			s = iterCoord.next();
			t = iterCoord.next();
		} else {
			s = this.getCoord();
		}
		
		while(true) {
			stLength = get2PointsDist.apply(s, t);
			accuLength = accuLength.plus(stLength);
			if(accuLength.bigger(stepDist)) {
				updateCoord(s);
				double length = stepDist.minus(accuLength.minus(stLength)).transfer(Unit.METER).num;
				double angle = getTwoPointsGeoAngle.apply(s, t);
				updateCoordByVetor(length, angle);
				break;
			} else if(!iterCoord.hasNext() ) {
				updateCoord(t);
				iterCoord = null;
				return true;
			} else {
				s = t;
				t = iterCoord.next();
			}
		}
		accuLength.num = 0;
		route.remainRoadsLength = route.remainRoadsLength.minus(stepDist);
		return false; // present has not been finished
	}

	private boolean stepConsuming() {
		remainCapacity = remainCapacity.minus(speed.multiply(simIntervalInHour).divide(milesPerUnitFueling));
		remainMileage = remainCapacity.multiply(milesPerUnitFueling);
		if(remainCapacity.num < 0) return true;
		else return false;
	}
	public boolean stepCharging() { // return
		
		Data stepCapacity = chargingStyle.chargingSpeed.multiply(simIntervalInSecond.transfer(Unit.MINUTE));
		Data sale = chargingStation.fuelingSales.get(chargingStyle);
		chargingStation.fuelingSales.put(chargingStyle, sale.plus(stepCapacity));
		remainCapacity = remainCapacity.plus(stepCapacity);
		remainCapacity = remainCapacity.bigger(chargableCapacity) ? (Data) chargableCapacity.clone() : remainCapacity;
		remainMileage = remainCapacity.multiply(milesPerUnitFueling);
		if (remainCapacity.equals(chargableCapacity)) return true;
		else return false;
	}
	
	private void chargingInit(Estimator estimator) {
		// TODO Auto-generated method stub
		if(estimator.getGoForTask() != null) {
			tasks.set(estimator.getTaskIndex(), estimator.getGoForTask());
			tasks.add(estimator.getTaskIndex() + 1, estimator.getComeBackTask());
			beforeChargingTask = tasks.get(estimator.getTaskIndex() + 1);
		} else {
			beforeChargingTask = tasks.get(estimator.getTaskIndex());
			// for a charging task, we can adjust the depart time to maximize the interval 
			var duration = beforeChargingTask.getRoute().getSumTimeLength().transferToDuration();
			if (beforeChargingTask.getArriveTime().minus(duration).isAfter(beforeChargingTask.getDepartTime()))
				beforeChargingTask.setDepartTime(beforeChargingTask.getArriveTime().minus(duration));
		}
		chargingStyle = estimator.getStyle();
		chargingStation = estimator.getStation();
	}

	private Estimator calculateFuelingEstimator() {
		int taskIndex = 0;
		Data tempRemainMileage = (Data) remainMileage.clone();
		Task task;
		Coordinate departCoord, arriveCoord;
		Iterator<HasCoordinate> iterClosestDistance;
		List<Data> remainMileages = new ArrayList<Data>();

		while (taskIndex < tasks.size()) {
			remainMileages.add(tempRemainMileage);
			//System.out.println("taskIndex is  " + taskIndex + "  tempRemainMileage is " + tempRemainMileage);
			tempRemainMileage = tempRemainMileage.minus(tasks.get(taskIndex++).getRoute().getSumRoadsLength());
			if (tempRemainMileage.smaller(safeLimitMileage))	break;
			if (taskIndex == maxAnticipate || taskIndex == tasks.size()) 
				return new Estimator.Builder(false).build();// 	it needn't fueling
		}
		
		// route time stands for the time need to finish every task calculated by route
		for (int i = 0; i < taskIndex; i++) {
			if(i == 0) intervalDuras[i] = Duration.between(engine.getRealTime(), tasks.getFirst().getDepartTime());
			else intervalDuras[i] = Duration.between(tasks.get(i - 1).getArriveTime(), tasks.get(i).getDepartTime());
			routeDurations[i] = tasks.get(i).getRoute().getSumTimeLength().transferToDuration();
			taskDurs[i] = tasks.get(i).getMaxDuration();
			onRouteAbunDuras[i] = taskDurs[i].minus(routeDurations[i]);
			if (i == 0) interAbunDuras[i] = intervalDuras[i].plus(onRouteAbunDuras[i]);
			else interAbunDuras[i] = intervalDuras[i].plus(onRouteAbunDuras[i - 1]).plus(onRouteAbunDuras[i]);
		}

		
		// the biggest heap comparator need to be a  return a > b ? -1 : 1; reverse
		var fuelingPq = new PriorityQueue<Estimator>(comparator.reversed());  

		for (int i = 0; i < taskIndex; i++) {
			task = tasks.get(i);
			departCoord = task.getDepartCoord();
			arriveCoord = task.getArriveCoord();
			// for interval time, we need to analyze the every one to make sure we could choose a best one
			iterClosestDistance = getClosestItem.apply(departCoord, FuelingStation.class, departCoord, maxWalkingDist).iterator();
			while (iterClosestDistance.hasNext()) {
				FuelingStation station = (FuelingStation) iterClosestDistance.next();
				FuelingStyle style = findAvailableFuelingPos.apply(station.fuelingStyles, chargingStyles);
				if (null != style && remainMileages.get(i).smaller(chargableMileage)) {
					var estimator = new Estimator.Builder(true).haveFoundOne(true).taskIndex(i).
							distance(new Data(0, Unit.MILE)).station(station).style(style).vehicle(this).duration(interAbunDuras[i]).
							remainMileage(remainMileages.get(i)).comeBackTask(null).goForTask(null).build();
					fuelingPq.add(estimator);
					break;
				}
			}
			var<FuelingStation> iterLocal = engine.getGeography().
					getObjectsWithin(new Envelope(departCoord, arriveCoord), FuelingStation.class).iterator();
			var searachStationsIter = (new ClosestDistance(iterLocal, departCoord, arriveCoord)).iterator();
			while (searachStationsIter.hasNext()) {
				FuelingStation station = (FuelingStation) searachStationsIter.next();
				FuelingStyle style = findAvailableFuelingPos.apply(station.fuelingStyles, chargingStyles);
				if (null != style) {
					// remainMileage limit
					var taskGoFor = new Task(departCoord, station.getCoord()); 
					var taskComeBack = new Task(station.getCoord(), arriveCoord);
					if (i == taskIndex - 1) {
						var lastRemainMileage = taskGoFor.getRoute().getSumRoadsLength();
						lastRemainMileage.num *= 1.1; // at least the remain Mileage is 1.1 times of the
						if (lastRemainMileage.bigger(remainMileages.get(i))) continue;
					}
					// depart and arrive setting
					var goDur = taskGoFor.getRoute().getSumTimeLength().transferToDuration();
					var backDur = taskComeBack.getRoute().getSumTimeLength().transferToDuration();
					var maxDur = taskDurs[i].minus(goDur).minus(backDur);
					if(maxDur.isNegative()) continue;
					
					taskGoFor.setDepartTime(task.getDepartTime());
					taskGoFor.setArriveTime(task.getDepartTime().plus(goDur));
					taskGoFor.setGorForTask(true);
					taskComeBack.setArriveTime(task.getArriveTime());
					taskComeBack.setDepartTime(task.getArriveTime().minus(backDur));
					taskComeBack.setComebackTask(true);
					
					var estimator = new Estimator.Builder(true).haveFoundOne(true).taskIndex(i).
							distance(taskGoFor.getRoute().getSumRoadsLength()).
							station(station).style(style).vehicle(this).duration(maxDur).
							remainMileage(remainMileages.get(i).minus(taskGoFor.getRoute().getSumRoadsLength())).
							comeBackTask(taskComeBack).goForTask(taskGoFor).build();
					fuelingPq.add(estimator);
					break;
				}
			}
		}
		if (fuelingPq.size() == 0) 
			return new Estimator.Builder(true).haveFoundOne(false).build();
		return fuelingPq.remove();
	}
	
	@Override
	public Coordinate getCoord() {
		return coord = engine.getGeography().getGeometry(this).getCoordinate();
	}
	@Override
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
	

	public Data getRemainMileAge() {
		return remainMileage = remainCapacity.multiply(milesPerUnitFueling);
	}

	public Data getMileAge() {
		return chargableMileage;
	}
	public boolean isPickupArrived() {
		return pickupArrived;
	}
	public boolean isRideHailingFinished() {
		return rideHailingFinished;
	}

//	public void //statusObersate(MotionStatus t1, MotionStatus t2) {
//		System.out.println("\n--------------------------");
//		System.out.println("currenttask is " + currentTask);
//		System.out.print("---> " + t1);
//		System.out.print("---> " + t2);
//		System.out.println("realtime is " + engine.getRealTime());
//		System.out.println("home status is " + isAtHome());
//		System.out.println("--------------------------\n");
//	}
	
	
	
	
	public Traveler getOwner() {
		return owner;
	}


	public void setOwner(Traveler owner) {
		this.owner = owner;
	}

	public boolean isWantCharge() {
		return wantCharge;
	}

	public void setWantCharge(boolean wantCharge) {
		this.wantCharge = wantCharge;
	}

	public boolean isAtHome() {
		return atHome;
	}

	public void setAtHome(boolean atHome) {
		this.atHome = atHome;
	}

	public boolean isCanCharge() {
		return canCharge;
	}

	public void setCanCharge(boolean canCharge) {
		this.canCharge = canCharge;
	}

	public double getEnergyRem() {
		return energyRem;
	}

	public void setEnergyRem(double energyRem) {
		this.energyRem = energyRem;
	}

	public double getChargeLoadElectric() {
		return chargeLoadElectric;
	}

	public void setChargeLoadElectric(double chargeLoadElectric) {
		this.chargeLoadElectric = chargeLoadElectric;
	}

	public double getChargeLoadGASOLINE() {
		return chargeLoadGASOLINE;
	}

	public void setChargeLoadGASOLINE(double chargeLoadGASOLINE) {
		this.chargeLoadGASOLINE = chargeLoadGASOLINE;
	}

	public double getChargeLoadDIESEL() {
		return chargeLoadDIESEL;
	}

	public void setChargeLoadDIESEL(double chargeLoadDIESEL) {
		this.chargeLoadDIESEL = chargeLoadDIESEL;
	}

	public double getChargeLoadHYDROGEN() {
		return chargeLoadHYDROGEN;
	}

	public void setChargeLoadHYDROGEN(double chargeLoadHYDROGEN) {
		this.chargeLoadHYDROGEN = chargeLoadHYDROGEN;
	}

	public boolean isConenctedSOC() {
		return conenctedSOC;
	}

	public void setConenctedSOC(boolean conenctedSOC) {
		this.conenctedSOC = conenctedSOC;
	}
	
	public Task getFirstTask() {
		return firstTask;
	}

	public void setFirstTask(Task firstTask) {
		this.firstTask = firstTask;
	}
	
	public Task getCurrentTask() {
		return currentTask;
	}

	public void setCurrentTask(Task currentTask) {
		this.currentTask = currentTask;
	}
	
	
}
