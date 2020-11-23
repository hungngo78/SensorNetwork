package sensor_network.distributed.naive.data;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Set;

public class Axis implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private double xAxis;
	private double yAxis;
	private boolean dataNodeType;
	
	// these properties help finding the shortest path between 2 nodes
	private double shortestDistance;
	private Set<Integer> shortestPath;
	
	// for DFS
	private boolean visited;
	
	private boolean painted;
	
	public Axis () {
		dataNodeType = false;
		visited = false;
		
		shortestDistance = Double.MAX_VALUE;
		shortestPath = new LinkedHashSet<Integer>();
	}
	
	public void setVisited(boolean _visited) {
		this.visited = _visited;
	}
	
	public boolean isVisited() {
		return this.visited;
	}
	
	public void setShortestPath(Set<Integer> newShortestPath) {
		this.shortestPath = newShortestPath;
	}
	
	public Set<Integer> getShortestPath() {
		return this.shortestPath;
	}
	
	public void setShortestDistance(double dis) {
		this.shortestDistance = dis;
	}
	
	public double getShortestDistance() {
		return this.shortestDistance;
	}
	
	public double getxAxis() {
		return xAxis;
	}
	
	public void setxAxis(double xAxis) {
		this.xAxis = xAxis;
	}
	
	public double getyAxis() {
		return yAxis;
	}
	
	public void setyAxis(double yAxis) {
		this.yAxis = yAxis;
	}
	
	public boolean isDataNode() {
		return dataNodeType;
	}
	
	public void setNodeType(boolean _nodeType) {
		this.dataNodeType = _nodeType;
	}
	
	public boolean isPainted() {
		return painted;
	}
	
	public void setPainted(boolean _type) {
		this.painted = _type;
	}
}
