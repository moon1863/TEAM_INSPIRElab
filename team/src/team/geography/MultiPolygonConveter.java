package team.geography;

import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

public class MultiPolygonConveter implements GeomConverter {

	@Override
	public Geometry convert(SimpleFeature feature) {
		// TODO Auto-generated method stub
		Geometry geom = (Geometry)feature.getDefaultGeometry();
	    if (geom instanceof MultiPolygon){
			MultiPolygon mp = (MultiPolygon)feature.getDefaultGeometry();
			geom = (Polygon)mp.getGeometryN(0);
			
	    }
	    return geom;
	}
}
