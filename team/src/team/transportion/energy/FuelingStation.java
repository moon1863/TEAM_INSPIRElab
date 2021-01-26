
package team.transportion.energy;
import java.io.Serializable;
import java.util.HashMap;

import team.geography.FixedGeography;
import team.tool.Data;
import team.tool.Data.Unit;
import team.transportion.energy.FuelingStyle;

public class FuelingStation extends FixedGeography implements Serializable, Cloneable {
	private static final long serialVersionUID = 6074669920518363901L;
	public HashMap<String, Integer> originalFuelingStyles;
	public HashMap<FuelingStyle, Integer> fuelingStyles;
	public HashMap<FuelingStyle, Data> fuelingSales;
	private double density; 	// should in 0 ~ 1, 0 means empty, 1 means not available
	private double waitingTime; //TODO maybe need to update to common time style, such as MM:SS

	public FuelingStation() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		fuelingStyles.forEach((k, v) -> builder.append(k).append(v));
		return builder.toString();
	}
	@Override
	public Object clone() throws CloneNotSupportedException {
		// TODO Auto-generated method stub
		return super.clone();
	}
	
	public void initSales() {
		fuelingSales = new HashMap<FuelingStyle, Data>();
		fuelingStyles.keySet().forEach(style ->	
			fuelingSales.put(style, style.getChargingSpeed().multiply(new Data(0,Unit.MINUTE))));
	}
	public int getAvailableNumOf(FuelingStyle style) {
		return fuelingStyles.get(style);
	}
	public void addAvailableOneOf(FuelingStyle style) {
		fuelingStyles.put(style, getAvailableNumOf(style) + 1);
	}
	public void decreaseAvailableOneOf(FuelingStyle style) {
		fuelingStyles.put(style, getAvailableNumOf(style) - 1);
	}
	
	public double getDensity() {
		return density;
	}
	public double getWaitingTime() {
		return waitingTime;
	}

	
	
}