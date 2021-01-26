package team.geography.transportation;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import com.vividsolutions.jts.geom.Coordinate;

import team.geography.FixedGeography;

public class Junction extends FixedGeography implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = -1640943156780196485L;
	private int selected;
	public static Map<Coordinate, Junction> coordMap = new HashMap<>();
	public HashSet<FixedGeography> geogs = new HashSet<>(); 
	
	public Junction(Coordinate coord) {
		super(coord);
		// TODO Auto-generated constructor stub
	}

	public int getSelected() {
		return selected;
	}

	public void setSelected(int selected) {
		this.selected = selected;
	}

	@Override
	public String toString() {
		return "Junction [selected=" + selected + ", geogs=" + geogs + ", getCoord()=" + getCoord() + "]";
	}
	
}
