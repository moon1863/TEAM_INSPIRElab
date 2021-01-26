package team.geography.transportation;

import java.awt.Color;

import gov.nasa.worldwind.render.SurfacePolyline;
import gov.nasa.worldwind.render.SurfaceShape;
import repast.simphony.visualization.gis3D.style.SurfaceShapeStyle;


public class RoadStyle implements SurfaceShapeStyle<Road>{

	@Override
	public SurfaceShape getSurfaceShape(Road object, SurfaceShape shape) {
	  return new SurfacePolyline();
	}

	@Override
	public Color getFillColor(Road obj) {
		return null;
	}

	@Override
	public double getFillOpacity(Road obj) {
		return 0;
	}

	@Override
	public Color getLineColor(Road obj) {
		if (obj.getSelectNum() == 1)
			return Color.BLACK;
		else if(obj.getSelectNum() == 2)
			return Color.YELLOW;
		return Color.GREEN;
		
	}

	@Override
	public double getLineOpacity(Road obj) {
		return 0.75;
	}

	@Override
	public double getLineWidth(Road obj) {
		if (obj.getSelectNum() == 1)
			return 3;
		return 1;
	}
}
