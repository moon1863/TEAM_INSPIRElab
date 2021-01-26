package team.geography;

import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;

public interface GeomConverter {
	public Geometry convert(SimpleFeature feature);
}
