package team.geography.boundary;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import team.agent.Traveler;
import team.geography.FixedGeography;
import team.tool.Data;
import team.tool.Data.Unit;
import team.transportion.energy.FuelingStation;
import team.transportion.energy.FuelingStyle;
public class Tract extends FixedGeography implements Serializable {
	private static final long serialVersionUID = -8077514143377273852L;
	private Set<Traveler> persons = new HashSet<Traveler>();
	private Set<FuelingStation> fuelingStations = new HashSet<FuelingStation>();
	private double meanmedian;
	private double finishedPara;
	private double incomePara;
	private double salePara;
	private double vehicleUtility;
	private double fuelingStationUtility;
	private Map<FuelingStyle, Data> tractSales;
	
	static private Map<FuelingStyle, Double> tractSalesCoeff = new HashMap<>();
	// sale compute parameter
	static {
		FuelingStyle[] styles = FuelingStyle.values();
		for(int i = 0; i < styles.length; i++)	
			tractSalesCoeff.put(styles[i], 0.001);
	}
	
	public void computeParas() {
		computeFinishedPara();
		computeIncomePara();
		computeSalePara();
	}
	
	public void computeFinishedPara() {
		if(persons.size() == 0) {
			finishedPara = 1.0;
			return;
		}
		double sumFinished = 0;
		for(Traveler p : persons) 
			if(p.isFinished()) 
				sumFinished += 1;	
		finishedPara = sumFinished / persons.size();
	}
	
	public void initSales() {
		tractSales = new HashMap<FuelingStyle, Data>();
		FuelingStyle[] styles = FuelingStyle.values();
		FuelingStyle style;
		for(int i = 0; i < styles.length; i++) {
			style = styles[i];
			tractSales.put(style, style.getChargingSpeed().multiply(new Data(0,Unit.MINUTE)));
		}
	}
	
	public void computeIncomePara() {
		incomePara = meanmedian * 0.00002;
	}
	
	public double getSalePara() {
		return salePara;
	}

	public void setSalePara(double salePara) {
		this.salePara = salePara;
	}

	public void computeSalePara() {
		for(FuelingStation fuelingStation : fuelingStations) {
			Data sale1, sale2 ;
			for(FuelingStyle fuelingStyle : fuelingStation.fuelingSales.keySet()) {
				sale1 = tractSales.get(fuelingStyle);
				sale2 = fuelingStation.fuelingSales.get(fuelingStyle);
				tractSales.put(fuelingStyle, sale1.plus(sale2));
			}
		}
		salePara = 0;
		for(FuelingStyle style : FuelingStyle.values()) 
			salePara += tractSales.get(style).num * tractSalesCoeff.get(style);
	}
	
	public double computeVehicleUtility() {
		return vehicleUtility = salePara + finishedPara + incomePara;
	}
	
	public double computeFuelingStationUtility() {
		return fuelingStationUtility = incomePara - finishedPara;
	}

	
	public Map<FuelingStyle, Data> getTractSales() {
		return tractSales;
	}

	public void setTractSales(Map<FuelingStyle, Data> tractSales) {
		this.tractSales = tractSales;
	}

	public Set<Traveler> getPersons() {
		return persons;
	}
	public void setPersons(Set<Traveler> persons) {
		this.persons = persons;
	}

	public Set<FuelingStation> getFuelingStations() {
		return fuelingStations;
	}
	public void setFuelingStations(Set<FuelingStation> fuelingStations) {
		this.fuelingStations = fuelingStations;
	}
	
	public double getVehicleUtility() {
		return vehicleUtility;
	}

	public double getFuelingStationUtility() {
		return fuelingStationUtility;
	}

	public double getMeanmedian() {
		return meanmedian;
	}

	public void setMeanmedian(double meanmedian) {
		this.meanmedian = meanmedian;
	}
	
	
}
