package team.vehicle;

import java.io.Serializable;
import java.util.HashMap;

import java.util.List;
import java.util.TreeMap;

import team.tool.Data;
import team.transportion.energy.FuelingStyle;


// all the unit is the international system of unit
// So need some procedure to get the data
// length meter, column litter, power kwh
@SuppressWarnings("serial")
public class Model implements Serializable{
	//TODO need to specifier all the details
	public static enum VehicleType { BEV, FCEV, PHEV };
	// model table
	private static TreeMap<Double, Model> sharesTable;  // it's an order tree map
	
	// private variable
	private VehicleType vehicleType;    	//BEV,CEV,FCEV
	private Data batteryCapacity;			// commonly the unit is kwh
	private Data MPKWH; 					// miles per kilowatt
	private Data tankCapacity;				// the max range number when the gas tank is full
	private Data MPG;						// miles per gallon
	private double marketShare;
	public List<FuelingStyle> elecFuelingStyles;
	public List<FuelingStyle> liquidFuelingStyles;
	
	public double getMarketShare() {
		return marketShare;
	}
	
	static public void calcuteRandomRange(HashMap<String, Model> models) {
		double startPosition = 0;
		double range = 0;
		Model model = null;
		double sumMarketShare = models.values().stream().mapToDouble(Model::getMarketShare).sum();
		sharesTable = new TreeMap<Double, Model>();
		for(String ID:models.keySet()) {
			model = models.get(ID);
			range = model.getMarketShare()/sumMarketShare;
			sharesTable.put(startPosition = startPosition + range, model);
		}
	}
	// randomly return a Model for a vehicle, and it�s based the distribution in the present market
	static public Model randomModel() {
		double key = Math.random();
		return sharesTable.higherEntry(key).getValue();
	}
	
	public Data getBatCapacity() {
		return batteryCapacity;
	}

	public void setBatteryCapacity(Data batteryCapacity) {
		this.batteryCapacity = batteryCapacity;
	}

	public Data getBatMPKWH() {
		return MPKWH;
	}

	public void setBatterytMPKWH(Data MPKWH) {
		this.MPKWH = MPKWH;
	}

	public Data getTankCapacity() {
		return tankCapacity;
	}

	public void setTankCapacity(Data tankCapacity) {
		this.tankCapacity = tankCapacity;
	}

	public Data getTankMPG() {
		return MPG;
	}

	public void setTankMPG(Data MPG) {
		this.MPG = MPG;
	}

	public VehicleType getVehicleType() {
		return vehicleType;
	}

	public void setVehicleType(VehicleType vehicleType) {
		this.vehicleType = vehicleType;
	}

	@Override
	public String toString() {
		return "Model [vehicleType=" + vehicleType + ", batteryCapacity=" + batteryCapacity + ", MPKWH=" + MPKWH
				+ ", tankCapacity=" + tankCapacity + ", MPG=" + MPG + ", marketShare=" + marketShare
				+ ", elecFuelingStyles=" + elecFuelingStyles + ", liquidFuelingStyles=" + liquidFuelingStyles + "]";
	}
	
	
}
