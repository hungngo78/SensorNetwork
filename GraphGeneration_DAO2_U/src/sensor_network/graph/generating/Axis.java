package sensor_network.graph.generating;
import java.io.Serializable;

public class Axis implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private double xAxis;
	private double yAxis;
	private boolean dataNodeType;
		
	// for DFS
	private boolean visited;
		
	public Axis () {
		dataNodeType = false;
	}
	
	public void setVisited(boolean _visited) {
		this.visited = _visited;
	}
	
	public boolean isVisited() {
		return this.visited;
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
	
}
