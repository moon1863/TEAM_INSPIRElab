package team;

import java.util.HashMap;
import java.util.List;
import java.util.function.Function;

import com.thoughtworks.xstream.XStream;
import com.vividsolutions.jts.geom.Coordinate;

import team.agent.Traveler;
import team.tool.Data;
import team.vehicle.Model;
import team.transportion.energy.FuelingStation;
import team.transportion.energy.FuelingStyle;

public class XMLProcess {
	public static Function<XStream, XStream> personsXStreamSetting = xStream -> {
		// change the alias of internal Class for xml
		xStream.alias("persons", HashMap.class);
		xStream.alias("ID", String.class);
		xStream.alias("person", Traveler.class);
		xStream.alias("task", Task.class);
		xStream.alias("departCoord", Coordinate.class);
		xStream.alias("arriveCoord", Coordinate.class);
		// inside Person Class, need to omit the tasks collection
		xStream.addImplicitCollection(Traveler.class, "tasks");
		// Person class some attribute
		xStream.useAttributeFor(Traveler.class, "houseHoldID");
		xStream.useAttributeFor(Traveler.class, "zipcode");
		xStream.useAttributeFor(Traveler.class, "tract");
		// task attribute
		xStream.useAttributeFor(Task.class, "taskID");
		xStream.useAttributeFor(Task.class, "purposeID");
		xStream.useAttributeFor(Task.class, "departTimeLiteral");
		xStream.useAttributeFor(Task.class, "arriveTimeLiteral");
		// original data transfer to internal data
		xStream.aliasField("departTime", Task.class, "departTimeLiteral");
		xStream.aliasField("arriveTime", Task.class, "arriveTimeLiteral");
		xStream.useAttributeFor(Coordinate.class, "x");
		xStream.useAttributeFor(Coordinate.class, "y");
		xStream.aliasField("longitude", Coordinate.class, "x");
		xStream.aliasField("latitude", Coordinate.class, "y");
		return xStream;
	};

	public static Function<XStream, XStream> modelsXStreamSetting = xStream -> {
		xStream.alias("models", HashMap.class);
		xStream.alias("ID", String.class);
		xStream.alias("model", Model.class);
		xStream.alias("vehicleType", Model.VehicleType.class);
		xStream.alias("batteryCapacity", Data.class);
		xStream.alias("MPKWH", Data.class);
		xStream.alias("tankCapacity", Data.class);
		xStream.alias("MPG", Data.class);
		xStream.alias("marketShare", double.class);
		xStream.alias("elecFuelingStyles", List.class);
		xStream.alias("liquidFuelingStyles", List.class);
		xStream.alias("FuelingStyle", FuelingStyle.class);
		return xStream;
	};
	public static Function<XStream, XStream> fuelingStationsXStreamSetting = xStream -> {
		xStream.alias("fuelingStations", HashMap.class);
		xStream.alias("ID", String.class);
		xStream.alias("fuelingStation", FuelingStation.class);
		
		xStream.alias("coord", Coordinate.class);
		xStream.useAttributeFor(Coordinate.class, "x");
		xStream.useAttributeFor(Coordinate.class, "y");
		xStream.aliasField("longitude", Coordinate.class, "x");
		xStream.aliasField("latitude", Coordinate.class, "y");
		
		
		xStream.alias("originalFuelingStyles", HashMap.class);
		xStream.alias("interfaceNumber", Integer.class);
		return xStream;
	};
	
	public static Function<XStream, XStream> vehiclesXStreamSetting = xStream -> {
		xStream.alias("vehiclesWithinZipcode", HashMap.class);
		xStream.alias("zipcode", String.class);
		xStream.alias("number", Integer.class);
		return xStream;
	};

	public static Function<XStream, XStream> projectionsXStreamSetting = xStream -> {
		xStream.alias("projections", HashMap.class);
		xStream.alias("year", Integer.class);
		xStream.alias("growth", Float.class);
		return xStream;
	};
	
	public static Function<XStream, XStream> HPMapXStreamSetting = xStream -> {
		/*
		 * add some setting for HPmap
		 * TODO
		 */
		return xStream;
	};
}
