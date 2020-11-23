package sensor_network.centralized.approximation.utility;
import java.util.LinkedHashSet;
import java.util.Set;

public class OriginalPath {
	private int node1;
	private int node2;
	
	private Set<Integer> originalPath;
	
	
	public OriginalPath() {
		originalPath = null;
	}
	
	public OriginalPath(int n1, int n2) {
		this.node1 = n1;
		this.node2 = n2;
		
		originalPath = new LinkedHashSet<Integer>();
	}

	public void setOriginalPath(Set<Integer> newShortestPath) {
		this.originalPath = newShortestPath;
	}
	
	public Set<Integer> getOriginalPath() {
		return this.originalPath;
	}
	
	public void setNode1(int n1) {
		this.node1 = n1;
	}
	
	public int getNode1() {
		return this.node1;
	}
	
	public void setNode2(int n2) {
		this.node2 = n2;
	}
	
	public int getNode2() {
		return this.node2;
	}
}
