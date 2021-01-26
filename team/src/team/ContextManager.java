package team;
import repast.simphony.context.Context;
import repast.simphony.context.space.gis.GeographyFactoryFinder;
import repast.simphony.context.space.graph.NetworkBuilder;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.space.gis.Geography;
import repast.simphony.space.gis.GeographyParameters;
import repast.simphony.space.gis.SimpleAdder;
import repast.simphony.space.graph.Network;
import team.geography.FixedGeography;
import team.geography.transportation.Junction;
import team.geography.transportation.NetworkEdge;
import team.geography.transportation.NetworkEdgeCreator;
import team.geography.transportation.Road;
import team.geography.transportation.TeamShortestPath;
import team.tool.Data;
import team.tool.PriorityQueueWithIndex;
import team.transportion.energy.FuelingStation;

import static team.GlobalSetting.*;
import static team.tool.MyFunction.*;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

public class ContextManager implements ContextBuilder<Object> {
	@Override
	public Context<Object> build(Context<Object> mainContext) {
		mainContext.setId(GlobalSetting.mainContextName);
		Geography<Object> geography = GeographyFactoryFinder.createGeographyFactory(null).createGeography(
				GlobalSetting.mainGeographyName, mainContext, new GeographyParameters<Object>(new SimpleAdder<Object>()));
		try { 
			engine = new BackgroundEngine(mainContext, geography);
		} 
		catch (Exception e) {	
			e.printStackTrace(); 
		}
		mainContext.add(engine);	// for schedule global behavior
		
		// net work process
		NetworkBuilder<Object> builder = new NetworkBuilder<Object>(GlobalSetting.roadNetworkName, mainContext, true);
		builder.setEdgeCreator(new NetworkEdgeCreator<Object>());
		engine.setRoadNetwork(builder.buildNetwork());
		buildRoadNetwork(geography, mainContext, engine.getRoadNetwork());
		engine.setShortestPath(new TeamShortestPath<Object>(engine.getRoadNetwork()));
		
		RunEnvironment.getInstance().endAt(sumTick.num);
		return mainContext;
	}
	
	static private void buildRoadNetwork(Geography<Object> geography, Context<Object> mainContext, 
			Network<Object> roadNetwork) {
		Junction junctionS, junctionE;
		Coordinate coordS, coordE, fixedCoord;
		NetworkEdge<Object> edge;
		// Create a cache of all Junctions and coordinates so we know if a junction has already been created at a
		// the cast safety is kept by the function of getLayer
		@SuppressWarnings("unchecked")
		HashSet<Road> roads =  (HashSet<Road>)geography.getLayer(Road.class).getAgentSet();
		Map<Coordinate, Junction> coordJunctionMap = new HashMap<Coordinate, Junction>();
		
		for(Road road : roads) {
			// Create a LineString from the road so we can extract coordinates
			Geometry roadGeom = geography.getGeometry(road);
			if(road.getDirection()) {
				coordS = roadGeom.getCoordinates()[0]; // First coordinate
				coordE = roadGeom.getCoordinates()[roadGeom.getNumPoints() - 1]; // Last coordinate
			} else {
				coordS = roadGeom.getCoordinates()[roadGeom.getNumPoints() - 1]; // First coordinate
				coordE = roadGeom.getCoordinates()[0]; // Last coordinate
			}
			junctionS = getJuncFromCoord.apply(coordS, coordJunctionMap);
			junctionE = getJuncFromCoord.apply(coordE, coordJunctionMap);

			// from the beginning, we think the instant speed should be the max speed
			// TODO it need to be updated frequently
			road.setInstantSpeed(road.getMaxSpeed()); 
			Data averageTime = road.getGeoLength().divide(road.getInstantSpeed());
			edge = new NetworkEdge<Object>(junctionS, junctionE, true, averageTime.num, road);
			engine.getRoadNetwork().addEdge(edge);
			//TODO add transporting speed
		}
		
		// for other fixed geography, we need to add this set
		// firstly, we need to add junction Object to every station
		// for any future fixedGeography object that's need to route, we need to add the project set
		
		HashSet<FixedGeography> fixedGeogs = (new HashSet<FixedGeography>());
		@SuppressWarnings("unchecked")
		var stations = (Set<FixedGeography>) geography.getLayer(FuelingStation.class).getAgentSet();
		fixedGeogs.addAll(stations);
		Junction closestJunc = null;
		Data searchDist = (Data) maxWalkingDist.clone();
		Iterator<HasCoordinate> iterLocal = null;
		for (FixedGeography fixedGeog : fixedGeogs) {
			// find the closest junction
			fixedCoord = fixedGeog.getCoord();
			do {
				iterLocal = getClosestItem.apply(new Coordinate(fixedCoord), Junction.class, null, searchDist).iterator();
				searchDist.num *= 2;
			} while(!iterLocal.hasNext());
			searchDist.num = maxWalkingDist.num;
			closestJunc = (Junction) iterLocal.next();
			
			// if the junction is too far,we need to find the most near road and split, then new a junction 
			if(get2PointsDist.apply(fixedCoord, closestJunc.getCoord()).bigger(maxWalkingDist)) {
				// project to the closest road
				Iterator<? extends HasCoordinate> iterRoads;
				List<HasCoordinate> roadList = new ArrayList<>();
				searchDist = (Data) maxWalkingDist.clone();
				do {
					roadList.clear();
					iterRoads = getBuildingsWitinDist.apply(Road.class, new Coordinate(fixedCoord), searchDist);
			        // Add each element of iterator to the List 
			        iterRoads.forEachRemaining(roadList::add);
			        searchDist.num *= 2;
				} while(roadList.size() < 20);
				searchDist.num = maxWalkingDist.num;
		        List<Coordinate> coordList = roadList.stream().map(v -> fixedGeog.projectRoad((Road)v)).collect(Collectors.toList());
		        List<Double> distList = coordList.stream().map(v -> fixedGeog.getCoord().distance(v)).collect(Collectors.toList());
		        var coordPbq = new PriorityQueueWithIndex<Double>(distList);
		        var firstIndex = coordPbq.removeFirst().getIndex();
		        var secondIndex = coordPbq.removeFirst().getIndex();
				// get all the projected road information
				closestJunc = splitRoad(fixedGeog, (Road) roadList.get(firstIndex), coordList.get(firstIndex), null);
				Data firstSecondDist = get2PointsDist.apply(coordList.get(firstIndex), coordList.get(secondIndex));
				if (firstSecondDist.smaller(maxWalkingDist))
					splitRoad(fixedGeog, (Road) roadList.get(secondIndex), coordList.get(secondIndex), closestJunc);
			}
			
			fixedGeog.setJunction(closestJunc);
		}
	}
	

	static private Junction splitRoad(FixedGeography fixedGeog, Road road, Coordinate coord, Junction newJunction) {
		Coordinate[] lineCoords, previousGroupCoords, nextGroupCoords;
		LineString projectLine;
		int index, pointsNumber;
		NetworkEdge<Object> edge;
		Road newRoad ,firstRoad, secondRoad;
		Point projectPoint = null;
		Junction start, end;

		projectLine = (LineString) engine.getGeography().getGeometry(road);
		// If the second split point is close to the first one, we use this junction as the new junction
		if(newJunction == null) {
			newJunction  = new Junction(coord);
			projectPoint = GlobalSetting.geomFac.createPoint(coord);
			engine.getContext().add(newJunction);
			engine.getGeography().move(newJunction, projectPoint);
			fixedGeog.setJunction(newJunction);
		} else {
			projectPoint = (Point) engine.getGeography().getGeometry(newJunction);
		}
		
		pointsNumber = projectLine.getNumPoints();
		lineCoords = projectLine.getCoordinates();
		// find the line that the point was in
		index = splitLine.apply(projectLine, projectPoint);

		// get two new coordinates
		previousGroupCoords = new Coordinate[index + 2];
		System.arraycopy(lineCoords, 0, previousGroupCoords, 0, index + 1);
		previousGroupCoords[index + 1] = projectPoint.getCoordinate();
		engine.getGeography().move(road, GlobalSetting.geomFac.createLineString(previousGroupCoords));
		
		engine.getContext().add(newRoad = (Road) road.clone());
		nextGroupCoords = new Coordinate[pointsNumber - (index + 1) + 1];
		nextGroupCoords[0] = projectPoint.getCoordinate();
		System.arraycopy(lineCoords, index + 1, nextGroupCoords, 1, pointsNumber - (index + 1));
		engine.getGeography().move(newRoad, GlobalSetting.geomFac.createLineString(nextGroupCoords));

		if(road.getDirection()) {
			firstRoad = road;
			secondRoad = newRoad;
		} else {
			firstRoad = newRoad;
			secondRoad = road;
		}
		
		NetworkEdge<Object> originEdge = road.getEdge();
		start = (Junction) originEdge.getSource();
		end = (Junction) originEdge.getTarget();
		
		edge = new NetworkEdge<Object>(start, newJunction, true, originEdge.getWeight(), firstRoad);
		engine.getRoadNetwork().addEdge(edge);
		edge = new NetworkEdge<Object>(newJunction, end, true, originEdge.getWeight(), secondRoad);
		engine.getRoadNetwork().addEdge(edge);
		engine.getRoadNetwork().removeEdge(originEdge);

		return newJunction;
	}
	
	// add the junction by coordinate 
	static public BiFunction<Coordinate, Map<Coordinate, Junction>, Junction> getJuncFromCoord = (coord, coordJuncMap) -> {
		if (!coordJuncMap.containsKey(coord)) {
			Junction junction = new Junction(coord);
			engine.getContext().add(junction);
			engine.getGeography().move(junction, GlobalSetting.geomFac.createPoint(coord));
			coordJuncMap.put(coord, junction); // com.vividsolutions.jts.geom.Coordinate has implemented the hashCode function
			return junction;
		}
		return coordJuncMap.get(coord);
	};
	
	// get the project point in a line string
	static public BiFunction<LineString, Point, Integer> splitLine = (lineString, point) -> {
		Double[] dists = new Double[lineString.getNumPoints() - 1];
		for(int i = 0; i < lineString.getNumPoints() - 1; i++) {
			double dist1 = lineString.getPointN(i).distance(point) + lineString.getPointN(i + 1).distance(point);
			double dist2 = lineString.getPointN(i).distance(lineString.getPointN(i + 1));
			if(dist1 == dist2) return i;
			else dists[i] = dist1;
		}
		var pqWithIndex = new PriorityQueueWithIndex<Double>(dists);
		if(pqWithIndex.hasFirst()) return pqWithIndex.removeFirst().getIndex();
		else throw new RuntimeException("the point has not been found this linestring!");
	};
	
}


