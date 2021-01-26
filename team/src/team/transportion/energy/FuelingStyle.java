package team.transportion.energy;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import team.tool.Data.Unit;
import team.tool.Data;


public enum FuelingStyle {
	//	https://evse.com.au/difference-between-levels-of-chargers/ data source
	L1("l1", new Data(1.4/60, Unit.KWHPM)),	
	L2("l2", new Data(7.6/60, Unit.KWHPM)),
	L3("l3", new Data(50.0/60, Unit.KWHPM)),
	
	GASOLINE("gas", new Data(1.7, Unit.GALPM)),
	DIESEL("diesel", new Data(1.7, Unit.GALPM)),
	HYDROGEN("hydrogen", new Data(1.7, Unit.GALPM));
	
	public String fuelingStylename; 
	public Data chargingSpeed;   
	
	public static Set<FuelingStyle> electricStyles = new HashSet<FuelingStyle>(Arrays.asList(L1, L2, L3));
	public static Set<FuelingStyle> liquidStyles = new HashSet<FuelingStyle>(Arrays.asList(GASOLINE, DIESEL, HYDROGEN));

	private FuelingStyle(String fuelingStylename, Data chargingSpeed) {
		this.fuelingStylename = fuelingStylename;
		this.chargingSpeed = chargingSpeed;
	}
	
	public static FuelingStyle getFuelingStyle(String styleName) {
		return Enum.valueOf(FuelingStyle.class, styleName);
	}

	public String getFuelingStylename() {
		return fuelingStylename;
	}

	public void setFuelingStylename(String fuelingStylename) {
		this.fuelingStylename = fuelingStylename;
	}

	public Data getChargingSpeed() {
		return chargingSpeed;
	}

	public void setChargingSpeed(Data chargingSpeed) {
		this.chargingSpeed = chargingSpeed;
	}
	
	
}
