package team.tool;

import static team.GlobalSetting.*;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.geotools.referencing.GeodeticCalculator;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.shape.random.RandomPointsBuilder;

import team.HasCoordinate;
import team.tool.Data.Unit;
import team.vehicle.Estimator;
import team.transportion.energy.FuelingStyle;

/**
 * This file offer some common function for team project
 * @author Chuang Hou
 *
 */

public class MyFunction {
	// return a square envelope using a parameter of a distance
	public static ThrowingBiFunction<Coordinate, Data, Envelope, Exception> getSqureEnvelop = (coord, dist) -> {
		var geodeticCalculator = new GeodeticCalculator();
		geodeticCalculator.setStartingGeographicPoint(coord.x, coord.y);
		geodeticCalculator.setDirection(45, dist.transfer(Unit.METER).num);
		double[] coordLD = geodeticCalculator.getDestinationPosition().getCoordinate();
		geodeticCalculator.setDirection(-135, dist.transfer(Unit.METER).num);
		double[] coordRU =  geodeticCalculator.getDestinationPosition().getCoordinate();
		return  new Envelope(new Coordinate(coordLD[0], coordLD[1]), new Coordinate(coordRU[0], coordRU[1]));
	};
	
	// get the most closest building to a position within a envelope
	public static QuarFunction <Coordinate, Class<? extends HasCoordinate>, Coordinate, Data, ClosestDistance> 
		getClosestItem = (originalPos, clazz, searchPos, searchDistance) -> {
		if(searchPos == null) 	searchPos = originalPos;
		Envelope env = ThrowingBiFunction.unchecked(getSqureEnvelop).apply(searchPos, searchDistance);
		var iterLocal = engine.getGeography().getObjectsWithin(env, clazz).iterator();
		return new ClosestDistance(iterLocal, originalPos);
	};
	
	// get a set of buildings within a envelope
	public static TriFunction <Class<? extends HasCoordinate>, Coordinate, Data, Iterator<? extends HasCoordinate>> 
	getBuildingsWitinDist = (clazz, searchPos, searchDistance) -> {
		Envelope env = ThrowingBiFunction.unchecked(getSqureEnvelop).apply(searchPos, searchDistance);
		return engine.getGeography().getObjectsWithin(env, clazz).iterator();
	};
	
	// calculate the distance
	public static BiFunction<Coordinate, Coordinate, Data> get2PointsDist = (s, t) -> {
		GeodeticCalculator calculator = new GeodeticCalculator(engine.getGeography().getCRS());
		calculator.setStartingGeographicPoint(s.x, s.y);
		calculator.setDestinationGeographicPoint(t.x, t.y);
		Data data = new Data(calculator.getOrthodromicDistance(), Unit.METER);
		data = data.transfer(Unit.MILE);
		return data;
	};
	
	// calculate the angle
	public static BiFunction<Coordinate, Coordinate, Double> getTwoPointsGeoAngle = (s, t) -> {
		GeodeticCalculator calculator = new GeodeticCalculator(engine.getGeography().getCRS());
		calculator.setStartingGeographicPoint(s.x, s.y);
		calculator.setDestinationGeographicPoint(t.x, t.y);
		return (2.5 * Math.PI - Math.toRadians(calculator.getAzimuth())) % (2 * Math.PI);
	};
	
	// for a vehicle and station, find a style and the number of style is bigger than zero
	public static BiFunction<HashMap<FuelingStyle, Integer>, List<FuelingStyle>, FuelingStyle> findAvailableFuelingPos = (map, list) -> {
		Set<FuelingStyle> styles = new HashSet<FuelingStyle>();
		for(FuelingStyle tempStyle : map.keySet())
			if(map.get(tempStyle) > 0) styles.add(tempStyle);
		for(FuelingStyle style : list) 
			if (styles.contains(style))	return style;
		return null;
	};
	/*
	 * need a algorithm to get a appropriate estimator
	 * 
	 * 
	 * 
	 */
	public static  Comparator<Estimator> comparator = new Comparator<Estimator>() {
		//TODO that's the key point, I need to have a conversation with Dr. Guo to have very good Estimator
		//TODO for now, we only use duration
		@Override
		public int compare(Estimator o1, Estimator o2) {
			int o1Num = o1.getStation().getAvailableNumOf(o1.getStyle());
			int o2Num = o2.getStation().getAvailableNumOf(o2.getStyle());
			if(o1Num > 0 && o2Num > 0) return o1.getDuration().compareTo(o2.getDuration());
			else if(o1Num > 0 && o2Num <= 0) return 1;
			else if(o1Num <= 0 && o2Num > 0) return -1;
			else throw new RuntimeException();
		}
		
	};
	
	public static Function< ? extends HasCoordinate, Geometry> getGeometry = hasCoordinate -> engine.getGeography().getGeometry(hasCoordinate);
	
	public static RandomPointsBuilder shapeBuilder = new RandomPointsBuilder();
	
	public static MultiPoint createRandomCoord(Geometry geom, int numPoints) {
		shapeBuilder.setExtent(geom);
		shapeBuilder.setNumPoints(numPoints);
		MultiPoint mtPoint = (MultiPoint)shapeBuilder.getGeometry();
		return mtPoint; 
	  }

 }
