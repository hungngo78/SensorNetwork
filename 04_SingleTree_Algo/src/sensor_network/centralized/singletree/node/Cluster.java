package sensor_network.centralized.singletree.node;

import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import sensor_network.centralized.singletree.utility.ShortestPath;

public class Cluster {
	private static int ids = 0;
	private int id;
	
	private ArrayList<Axis> nodes;
	//private Axis leader = null;
	
	// <ClusterID, distance>
	private Map<Integer, ShortestPath> ratiosInfo;
	
	private boolean prizeCollected;
	private int initiator; 
	
	public Color color;
	
	
//	public Cluster() {
//		id = ids++;
//		
//		nodes = new ArrayList<Axis>();
//		
//		distanceInfo = new LinkedHashMap<Integer, Double>();
//		prizeCollected = false;
//	}

	public Cluster(Axis n) {
		id = ids++;
		
		nodes = new ArrayList<Axis>();
		nodes.add(n);
		n.setCluster(this);
		
		initiator = n.getId();
		
		ratiosInfo = new LinkedHashMap<Integer, ShortestPath>();
		prizeCollected = false;
	}
	
	public ArrayList<Axis> getNodes() {
		return nodes;
	}
	public void setNodes(ArrayList<Axis> _nodes) {
		nodes = _nodes;
	}

	public void addNode(Axis n) {
		nodes.add(n);
	}

	public Map<Integer, ShortestPath> getRatiosInfo() {
		this.ratiosInfo.clear();
		return this.ratiosInfo;
	}
	
	public int getId() {
		return id;
	}

	public ArrayList<Axis> getLeafNodes (Map<Integer, Set<Integer>> adjMatrix) {
		ArrayList<Axis> leafNodes = new ArrayList<>();
		for (Axis n: nodes) {
			Set<Integer> adjList = adjMatrix.get(n.getId());
			
			for (int neighbour: adjList) {
				if (!isInCluster(neighbour)) {
					leafNodes.add(n);
					break;
				}
			}
		}
		
		return leafNodes;
	}
	
//	public Axis getLeader() {
//		return leader;
//	}
//
//	public void setLeader(Axis leader) {
//		this.leader = leader;
//	}
	
	public int getSize () {
		return nodes.size();
	}

	public boolean isInCluster(int _id) {
		for (Axis n: nodes) {
			if (n.getId() == _id)
				return true;
		}
		
		return false;
	}
	
	public void setInitiatorId(int _id) {
		this.initiator = _id;
	}
	public int getInitiatorId() {
		return this.initiator;
	}
	public int getInitiatorPrize() {
		for (Axis n: nodes) {
			if (n.getId() == this.initiator) {
				return (n.getRCapNumber() - n.getRNumber());
			}
		}
		return 0;
	}
	
	public int getPrize() {
		int sum = 0;
		for (Axis n: nodes) {
			sum += (n.getRCapNumber() - n.getRNumber());
		}
		
		return sum;
	}
	public int getPrizeExcludeInitiator() {
		int sum = 0;
		for (Axis n: nodes) {
			if (n.getId() == this.initiator)
				continue;
				
			sum += (n.getRCapNumber() - n.getRNumber());
		}
		
		return sum;
	}
	
	public void collectPrize() {
		this.prizeCollected = true;
	}
	public boolean isCollected() {
		return this.prizeCollected;
	}
}
