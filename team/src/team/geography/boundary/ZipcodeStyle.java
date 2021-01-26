package team.geography.boundary;

import java.awt.Color;

import com.vividsolutions.jts.geom.Envelope;

import gov.nasa.worldwind.render.SurfacePolygon;
import gov.nasa.worldwind.render.SurfaceShape;
import repast.simphony.visualization.gis3D.style.SurfaceShapeStyle;
import team.transportion.energy.FuelingStation;

import static team.GlobalSetting.*;

public class ZipcodeStyle implements SurfaceShapeStyle<Zipcode>{

	@Override
	public SurfaceShape getSurfaceShape(Zipcode obj, SurfaceShape shape) {
		return new SurfacePolygon();
	}

	@Override
	public Color getFillColor(Zipcode obj) {
		// TODO scale the color properly
//		int yR = (int)Math.round(255*Math.exp((-obj.getPotentialCustomerNum()+1)/ContextManager.agentGeography.getGeometry(obj).getArea()/40000));			

		Color c = Color.GREEN;
		return c;
	}

	@Override
	public double getFillOpacity(Zipcode obj) {
		
		Envelope envelope = engine.getGeography().getGeometry(obj).getEnvelopeInternal();
		engine.getGeography().getObjectsWithin(envelope, FuelingStation.class);
		return 0.1;
	}

	@Override
	public Color getLineColor(Zipcode obj) {
		return Color.RED;
	}

	@Override
	public double getLineOpacity(Zipcode obj) {
		return 1.0;
	}

	@Override
	public double getLineWidth(Zipcode obj) {
		return 1.0;
	}
}
