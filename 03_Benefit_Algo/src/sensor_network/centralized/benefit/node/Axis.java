package sensor_network.centralized.benefit.node;


import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Set;

public class Axis implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private int id;
	
	private double xAxis;
	private double yAxis;
	private boolean dataNodeType;
	
	// selected for prize collecting
	//public boolean selected;
	
	// connected component ID
	public int componentId; 
	
	
	// these properties help finding the shortest path between 2 nodes
	private double shortestDistance;
	private Set<Integer> shortestPath;		
	
	// for DFS
	private boolean visited;
	
	private int rCapNumber;
	private int rNumber;
	
	//private Cluster cluster;
	
	//public Cluster getCluster() {
	//	return cluster;
	//}
	//public void setCluster(Cluster _cluster) {
	//	cluster = _cluster;
	//}
	
	public Axis () {
		dataNodeType = false;
		
		shortestDistance = Double.MAX_VALUE;
		shortestPath = new LinkedHashSet<Integer>();
		
		visited = false;
		
		//selected = false;
		
		componentId = -1;
	}
	
	public Axis (int _id) {
		id = _id;
		dataNodeType = false;
		
		shortestDistance = Double.MAX_VALUE;
		shortestPath = new LinkedHashSet<Integer>();
		
		visited = false;
		
		//selected = false;
		
		componentId = -1;
	}
	
	public void setId(int _id) {
		id = _id;
	}
	public int getId() {
		return id;
	}
	
	/*public void setSelected(boolean _selected) {
		this.selected = _selected;
	}
	
	public boolean isSelected() {
		return this.selected;
	}*/
	
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
	
	public int getRCapNumber() {
		return this.rCapNumber;
	}
	public void setRCapNumber(int number) {
		this.rCapNumber = number;
	}
	public int getRNumber() {
		return this.rNumber;
	}
	public void setRNumber(int number) {
		this.rNumber = number;
	}
}
