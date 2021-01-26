package team.geography;


import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.linearref.LocationIndexedLine;


import team.HasCoordinate;
import team.Identifier;
import team.geography.transportation.Junction;
import team.geography.transportation.Road;

import static team.GlobalSetting.*;

import java.io.Serializable;

abstract public class FixedGeography implements HasCoordinate, Identifier, Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 6863197353918209974L;
	private Coordinate coord;
	private String identifier;
	private Junction junction;
	
	
	public FixedGeography(Coordinate coord) {
		this.coord = coord;
	}
	public FixedGeography() {
	}
	
	public Coordinate projectRoad(Road road) {
		LineString roadLine;
		LocationIndexedLine indexLine;
		roadLine = (LineString) engine.getGeography().getGeometry(road);
		indexLine = new LocationIndexedLine(roadLine);
		return indexLine.extractPoint(indexLine.project(this.getCoord()));
	}
	@Override
	public Coordinate getCoord() {
		return coord;
	}
	@Override // this function could only be invoked once
	public void setCoord(Coordinate coord) {
		this.coord = coord;
	}
	public String getIdentifier() {
		return identifier;
	}

	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}
	
	public Junction getJunction() {
		return junction;
	}
	public void setJunction(Junction junction) {
		this.junction = junction;
	}
	
    @Override
    public String toString() {
        // TODO Auto-generated method stub
        StringBuilder builder = new StringBuilder();
        builder.append("N " + coord.x);
        builder.append(" ");
        builder.append("S " +coord.y);
        return builder.toString();
    }
	
}
