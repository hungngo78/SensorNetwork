package sensor_network.centralized.benefit.node;

import sensor_network.centralized.benefit.node.Edge;

public class Edge {
	private int node1;
	private int node2;
	
	// to sort edges by ratio prize/length
	//private int tmpSelectedNode;
	private double tmpRatio;
	
	// selected for prize collecting
	private boolean selected;
	
	private double distance;
	
	public Edge() {
		// TODO Auto-generated constructor stub
	}
	
	public Edge(int n1, int n2, double d) {
		this.node1 = n1;
		this.node2 = n2;
		
		//this.tmpSelectedNode = -1;
		
		distance = d;		
		
		tmpRatio = -1;
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
	
	// for select edge, node in algorithm 3
	//public void setTmpSelectedNode(int n) {
	//	this.tmpSelectedNode = n;
	//}
	
//	public int getTmpSelectedNode() {
//		return this.tmpSelectedNode;
//	}
	public void setTmpRatio(double r) {
		this.tmpRatio = r;
	}
	
	public double getTmpRatio() {
		return this.tmpRatio;
	}
	
	// selected for prize collecting
	public void setSelected(boolean _selected) {
		this.selected = _selected;
	}
	
	public boolean isSelected() {
		return this.selected;
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
