package team.geography.transportation;

import java.util.Arrays;

import repast.simphony.space.graph.EdgeCreator;

@SuppressWarnings("unused")
public class NetworkEdgeCreator<T> implements EdgeCreator<NetworkEdge<T>, T> {

	/**
	 * Creates an Edge with the specified source, target, direction and weight.
	 * 
	 * @param source
	 *            the edge source
	 * @param target
	 *            the edge target
	 * @param isDirected
	 *            whether or not the edge is directed
	 * @param weight
	 *            the weight of the edge
	 * @return the created edge.
	 */
	@Override
	public NetworkEdge<T> createEdge(T source, T target, boolean isDirected, double weight) {
		return new NetworkEdge<T>(source, target, isDirected, weight, (Road)null);
	}

	/**
	 * Gets the edge type produced by this EdgeCreator.
	 * 
	 * @return the edge type produced by this EdgeCreator.
	 */
	@Override
	@SuppressWarnings("rawtypes")
	public Class<NetworkEdge> getEdgeType() {
		return NetworkEdge.class;
	}

}

