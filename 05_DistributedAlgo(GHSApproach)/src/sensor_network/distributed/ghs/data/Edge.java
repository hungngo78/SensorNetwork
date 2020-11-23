package sensor_network.distributed.ghs.data;

import sensor_network.distributed.ghs.Network;

public class Edge implements Comparable<Edge> {
	public int id1, id2;
	public double weight;

	public Edge(int a, int b) {
		id1 = a;
		id2 = b;
		weight = Network.getWeight(this.id1, this.id2);
	}
	
	public Edge(int n1, int n2, double d) {
		id1 = n1;
		id2 = n2;
		
		weight = d;		
	}
	
	@Override     
	public int compareTo(Edge candidate) {          
		double candidateWeight = Network.getWeight(candidate.id1, candidate.id2);
		return (this.weight < candidateWeight ? -1 : 
            (this.weight == candidateWeight ? 0 : 1));
	}  
	
	@Override     
	public String toString() {         
		return " id1: " + this.id1 + ", id2: " + this.id2;     
	} 
}
