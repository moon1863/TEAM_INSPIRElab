package team.geography.transportation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;

import repast.simphony.space.graph.RepastEdge;

import team.Task;
import team.geography.transportation.NetworkEdge;
import team.tool.ClosestDistance;

import team.tool.Data;
import team.tool.Data.Unit;
import team.tool.ThrowingBiFunction;

import static team.GlobalSetting.*;
import static team.tool.MyFunction.*;

public class Route {
	// define the junction search distance, it should be adjust by different map scale
	// we define the speed before major road
	// TODO this variable name may need to be adjusted
	private static Data laneSpeed =  new Data(70, Unit.MPH);
	private static TeamShortestPath<Object> shortestPath =
			new TeamShortestPath<Object>(engine.getRoadNetwork());
	private List<RepastEdge<Object>> edges;
	private Data startDistance = new Data(0, Unit.MILE);
	private Data endDistance = new Data(0, Unit.MILE); 
	//TODO it means before arrive the fist junction and after the last junction
	//TODO there are two distance need to be drive
	private Data sumRoadsLength = new Data(0, Unit.MILE);
	public Data remainRoadsLength = new Data(0, Unit.MILE);
	private Data sumTimeLength = new Data(0, Unit.HOUR);
	private Junction startJunction, endJunction;
	Coordinate depart;
	public Coordinate arrive;
	public List<Coordinate> trackCoordinates = new ArrayList<Coordinate>();
	public List<Integer> coordinateIndexes = new ArrayList<Integer>();
	
	public Route(Task task) {
		this(task.getDepartCoord(), task.getArriveCoord());
	}
	
	public Route(Coordinate depart, Coordinate arrive) {
		// for depart and arrive point, she
		this.depart = depart;
		this.arrive = arrive;
		
		Data length = get2PointsDist.apply(depart, arrive);
		startJunction = nearestJunction(depart);
		endJunction = nearestJunction(arrive);
		
		if(startJunction == endJunction) {
			sumRoadsLength = length;
			sumTimeLength = sumRoadsLength.divide(laneSpeed);
			trackCoordinates.add(depart);
			trackCoordinates.add(arrive);
		} else {
			edges =  shortestPath.getPath(startJunction, endJunction);
			startDistance = get2PointsDist.apply(depart, startJunction.getCoord());
			endDistance = get2PointsDist.apply(arrive, endJunction.getCoord());
			
			sumTimeLength.num = edges.stream().mapToDouble(v -> v.getWeight()).sum();
			sumTimeLength.num += startDistance.plus(endDistance).divide(laneSpeed).num;
			sumRoadsLength.num = edges.stream().mapToDouble(v -> ((NetworkEdge<Object>)v).getRoad().getGeoLength().num).sum();
			sumRoadsLength.num += startDistance.plus(endDistance).num;
			
			Road road;
			Coordinate[] coordList = null;
			List<Coordinate> coords = null;
			trackCoordinates.add(depart);
			for (int i = 0; i < edges.size(); i++) {
				road = ((NetworkEdge<Object>) edges.get(i)).getRoad();
				coordList = engine.getGeography().getGeometry(road).getCoordinates();
				if (!road.getDirection()) {
					coordList = Arrays.copyOf(coordList, coordList.length);
					ArrayUtils.reverse(coordList);
				}
				coords = Arrays.asList(coordList);
				trackCoordinates.addAll(coords.subList(0, coords.size() - 1));
			}
			trackCoordinates.add(coords.get(coords.size() - 1));
			trackCoordinates.add(arrive);
			remainRoadsLength = (Data) sumRoadsLength.clone();
		}
	}
	
	
	private Junction nearestJunction(Coordinate coord) {
		Envelope env;
		Iterator<Junction> iterJunction;
		Data junctionSearchDist;
		junctionSearchDist = (Data) maxWalkingDist.clone();
		do {
			env = ThrowingBiFunction.unchecked(getSqureEnvelop).apply(coord, junctionSearchDist);
			iterJunction = engine.getGeography().getObjectsWithin(env, Junction.class).iterator();
			junctionSearchDist.num *= 2;
		} while(!iterJunction.hasNext());
		var iter = (new ClosestDistance(iterJunction, depart)).iterator();
		return (Junction) iter.next();
	}
	public Data getSumRoadsLength() {
		return sumRoadsLength;
	}
	public Data getSumTimeLength() {
		return sumTimeLength;
	}
	public List<RepastEdge<Object>> getEdges() {
		return edges;
	}

	@Override
	public String toString() {
		return "Route \nsumRoadsLength=" + sumRoadsLength + 
				", \nsumTimeLength=" + sumTimeLength + 
				", \nremainRoadsLength=" + remainRoadsLength +"]";
	}
	
}
