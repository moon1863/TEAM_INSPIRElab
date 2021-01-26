package team.tool;


import java.util.HashSet;
import java.util.Iterator;
import java.util.PriorityQueue;

import com.vividsolutions.jts.geom.Coordinate;

import team.HasCoordinate;

/**
 * Class ClosestDistance could return the closest series one in a set of FixedGeography objects
 * for a coordinate, a position, or a fixedGeography
 * construct function initialize the goal fixedGeography set and the source position
 * then client could use the iterable interface return the distance series
 * @author  Chuang Hou
 */

public class ClosestDistance implements Iterable<HasCoordinate>{
	
	PriorityQueue<Distance> pbq = new PriorityQueue<>();
	
	public ClosestDistance(HashSet<? extends HasCoordinate> geogs, Coordinate coord) {
		geogs.stream().forEach(v -> pbq.add(new Distance(v, coord)));
	}
	public ClosestDistance(HashSet<? extends HasCoordinate> geogs, HasCoordinate hasCoord) {
		this(geogs, hasCoord.getCoord());
	}

	public ClosestDistance(Iterator<? extends HasCoordinate> iter, Coordinate coord) {
		while(iter.hasNext()) 	pbq.add(new Distance(iter.next(), coord));
	}
	public ClosestDistance(Iterator<? extends HasCoordinate> iter, HasCoordinate hasCoord) {
		this(iter, hasCoord.getCoord());
	}
	
	public ClosestDistance(Iterator<? extends HasCoordinate> iter, Coordinate coordA, Coordinate coordB) {
		while(iter.hasNext()) pbq.add(new Distance(iter.next(), coordA, coordB));
	}

	public PriorityQueue<Distance> getPbq() {
		return pbq;
	}
	
	public class Distance implements Comparable<Distance> {
		double dist;
		HasCoordinate hasCoord;
		Coordinate coord;
		
		public Distance(HasCoordinate hasCoord, Coordinate coordTarget) {
			this.hasCoord = hasCoord;
			dist = hasCoord.getCoord().distance(coordTarget);
		}
		
		public Distance(HasCoordinate hasCoord, Coordinate coordA, Coordinate coordB) {
			this.hasCoord = hasCoord;
			dist = hasCoord.getCoord().distance(coordA) + hasCoord.getCoord().distance(coordB);
		}

		
		@Override
		public int compareTo(Distance o) {
			return dist > o.dist ? 1:-1;
		}
		public HasCoordinate getHasCoord() {
			// TODO Auto-generated method stub
			return this.hasCoord;
		}
	}

	@Override
	public Iterator<HasCoordinate> iterator() {
		return new Iterator<HasCoordinate>() {
			@Override
			public boolean hasNext() { 
				return !pbq.isEmpty(); 
			}
			@Override
			public HasCoordinate next() { 
				return pbq.remove().getHasCoord(); 
			}
		};
	}

}
