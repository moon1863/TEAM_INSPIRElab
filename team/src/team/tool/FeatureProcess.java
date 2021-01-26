package team.tool;

import org.opengis.feature.simple.SimpleFeature;

public interface FeatureProcess<T> {
	public void apply(T t, SimpleFeature feature) throws Exception;
}