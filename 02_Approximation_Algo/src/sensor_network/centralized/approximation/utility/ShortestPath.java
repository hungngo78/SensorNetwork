package sensor_network.centralized.approximation.utility;

import java.util.LinkedHashSet;
import java.util.Set;

public class ShortestPath {
	
	public int node1;
	public int node2;
	
	public Set<Integer> path;
	public double weight;
	
	public ShortestPath() {
		node1 = -1;
		node2 = -2;
		
		weight = Double.MAX_VALUE;
		path = new LinkedHashSet<Integer>();
	}

}
