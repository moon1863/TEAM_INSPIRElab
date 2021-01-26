package team.geography.boundary;

import java.awt.Color;
import java.lang.Math;

import gov.nasa.worldwind.render.SurfacePolygon;
import gov.nasa.worldwind.render.SurfaceShape;
import repast.simphony.visualization.gis3D.style.SurfaceShapeStyle;
import team.transportion.energy.FuelingStation;
import team.transportion.energy.FuelingStyle;
import team.tool.Data;
import team.tool.Data.Unit;

import static team.GlobalSetting.*;

public class TractStyle implements SurfaceShapeStyle<Tract>{

	@Override
	public SurfaceShape getSurfaceShape(Tract obj, SurfaceShape shape) {
		return new SurfacePolygon();
	}

	@Override
	public Color getFillColor(Tract obj) {
		// TODO scale the color properly
		Data sale = new Data(0, Unit.KILOWATTHOUR);
		for(FuelingStation station : obj.getFuelingStations()) {	
			if (station.fuelingSales.get(FuelingStyle.L1) != null)
				sale = sale.plus(station.fuelingSales.get(FuelingStyle.L1));
			if (station.fuelingSales.get(FuelingStyle.L2) != null)
				sale = sale.plus(station.fuelingSales.get(FuelingStyle.L2));
			if (station.fuelingSales.get(FuelingStyle.L3) != null)
				sale = sale.plus(station.fuelingSales.get(FuelingStyle.L3));
		}
		double geometryArea = engine.getGeography().getGeometry(obj).getArea();
		int yR = (int) (255 * Math.atan(sale.num / (geometryArea * 10000))/(Math.PI/2.0));	
		Color c = new Color(255, yR, yR);
		return c;
	}

	@Override
	public double getFillOpacity(Tract obj) {
		return 0.8;
	}

	@Override
	public Color getLineColor(Tract obj) {
		return Color.BLACK;
	}

	@Override
	public double getLineOpacity(Tract obj) {
		return 0.5;
	}

	@Override
	public double getLineWidth(Tract obj) {
		return 0.2;
	}
}
