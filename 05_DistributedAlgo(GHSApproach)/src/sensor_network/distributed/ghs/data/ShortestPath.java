package sensor_network.distributed.ghs.data;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class ShortestPath {
	private double cost;
	private Set<Integer> path;
	
	public ShortestPath() {
		path = Collections.synchronizedSet(new LinkedHashSet<Integer>());
	}
	
	public ShortestPath(int _cost) {
		cost = _cost;
		path = Collections.synchronizedSet(new LinkedHashSet<Integer>());
	}
	
	public double getCost() {
		return this.cost;
	}
	public void setCost(double _cost) {
		this.cost = _cost;
	}
	
	public void addNode(int n) {
		path.add(n);
	}

	public void setPath(Set<Integer> _path) {
		synchronized (path) { 
			path.clear();
			path.addAll(_path);
		}
	}
	public Set<Integer> getPath() {
		return this.path;
	}
}
