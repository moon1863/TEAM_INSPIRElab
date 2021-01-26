package team;

import com.vividsolutions.jts.geom.Coordinate;

public interface HasCoordinate {
	public Coordinate getCoord();
	public void setCoord(Coordinate coord);
}
