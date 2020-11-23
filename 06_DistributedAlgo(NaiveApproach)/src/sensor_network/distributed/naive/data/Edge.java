package sensor_network.distributed.naive.data;

import sensor_network.distributed.naive.Network;

public class Edge { //implements Comparable<Edge> {
	public int id1, id2;
	private double distance;
	//public double weight;

	public Edge(int a, int b) {
		id1 = a;
		id2 = b;
		//weight = Network.getWeight(this.id1, this.id2);
		distance = Network.getWeight(this.id1, this.id2);
	}
	
	public Edge(int n1, int n2, double d) {
		id1 = n1;
		id2 = n2;
		
		distance = d;
		//weight = d;		
	}
	
	public void setDistance(double d) {
		this.distance = d;
	}
	
	public double getDistance() {
		return this.distance;
	}
		
//	@Override     
//	public int compareTo(Edge candidate) {     
//		double candidateWeight = Network.getWeight(candidate.id1, candidate.id2);
//		return (this.distance < candidateWeight ? -1 : 
//            (this.distance == candidateWeight ? 0 : 1));
//	}  
	
	@Override     
	public String toString() {         
		return " id1: " + this.id1 + ", id2: " + this.id2;     
	} 
}
