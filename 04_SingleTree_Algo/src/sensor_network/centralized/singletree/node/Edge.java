package sensor_network.centralized.singletree.node;

public class Edge {
	private int node1;
	private int node2;
	private double distance;
	
	public Edge() {
		// TODO Auto-generated constructor stub
	}
	
	public Edge(int n1, int n2, double d) {
		this.node1 = n1;
		this.node2 = n2;
		
		distance = d;		
	}
	
	public void setDistance(double d) {
		this.distance = d;
	}
	
	public double getDistance() {
		return this.distance;
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
	
	@Override
	public boolean equals(Object obj) {
		boolean flg = false;
		if(obj instanceof Edge) {
			if( ((Edge)obj).node1 == this.node1 && ((Edge)obj).node2 == this.node2 ) {
				flg = true;
			}
		}
		return flg;
	}
}
