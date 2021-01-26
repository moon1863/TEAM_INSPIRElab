package team.geography.transportation;

import java.util.List;


import repast.simphony.context.space.graph.ContextJungNetwork;
import repast.simphony.space.graph.JungEdgeTransformer;
import repast.simphony.space.graph.JungNetwork;
import repast.simphony.space.graph.Network;
import repast.simphony.space.graph.RepastEdge;
import repast.simphony.space.projection.ProjectionEvent;
import repast.simphony.space.projection.ProjectionListener;
import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath;
import edu.uci.ics.jung.graph.Graph;

public class TeamShortestPath<T> implements ProjectionListener<T> {

	private Network<T> net;
	private JungEdgeTransformer<T> transformer;
	private DijkstraShortestPath<T,RepastEdge<T>> dsp;
  

	public TeamShortestPath(Network<T> net){
		this.net = net;
		transformer = new JungEdgeTransformer<T>();
		calcPaths();
		net.addProjectionListener(this);
	}

	/**
	 * Returns a list of RepastEdges in the shortest path from source to target.
	 * 
	 * @param source
	 * @param target
	 * @return
	 */
	public List<RepastEdge<T>> getPath(T source, T target){
		return dsp.getPath(source, target); 
	}
	
	private void calcPaths(){
		Graph<T, RepastEdge<T>> graph = null;
		
		if (net instanceof JungNetwork)
			graph = ((JungNetwork<T>)net).getGraph();
		else if (net instanceof ContextJungNetwork)
			graph = ((ContextJungNetwork<T>)net).getGraph();
		
		dsp = new DijkstraShortestPath<T,RepastEdge<T>>(graph, transformer, true);
	}
	
	/**
	 * Called when the network is modified so that this will recalculate the
	 * shortest path info.
	 * 
	 * @param evt
	 */
	public void projectionEventOccurred(ProjectionEvent<T> evt) {
		if (evt.getType() != ProjectionEvent.OBJECT_MOVED) calcPaths();
	}
	
	/**
	 * Removes this as a projection listener when this ShortestPath is garbage
	 * collected.
	 */
	public void finalize() {
		if (net != null)
			net.removeProjectionListener(this);
	}
	
	/**
	 * Null the object so that the Garbage Collector recognizes to remove 
	 * the object from the jvm.
	 */
	public static TeamShortestPath<?> finished(TeamShortestPath<?> sp){
		sp.finalize();
		sp=null;
		return sp;
	}
}