package sensor_network.distributed.ghs.data;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;

public class Cluster {
	private static int ids = 0;
	private int id;
	private int level;
	private ArrayList<Node> nodes;
	private Node leader = null;
	public Color color;
	
	public Cluster() {
		id = ids++;
		nodes = new ArrayList<Node>();
	}

	public Cluster(Node n) {
		id = ids++;
		nodes = new ArrayList<Node>();
		nodes.add(n);
		setLeader(n);
	}

	public void setLevel(int _level) {
		this.level = _level;
	}
	public int getLevel() {
		return this.level;
	}
	
	public ArrayList<Node> getNodes() {
		return nodes;
	}
	public void setNodes(ArrayList<Node> _nodes) {
		nodes = _nodes;
	}

	public void addNode(Node n) {
		nodes.add(n);
	}

	public int getId() {
		return id;
	}

	public Node getLeader() {
		return leader;
	}

	public void setLeader(Node leader) {
		this.leader = leader;
	}

	public boolean isInCluster(int id) {
		for (Node n : nodes) {
			if (n.getId() == id)
				return true;
		}
		return false;
	}
	
	public boolean isIsolated() {
		boolean res = true;
		
		Iterator<Node> ite = nodes.iterator();
		while (ite.hasNext()) {
			Node n = ite.next();
			double MWOE = n.getLeastWeightedEdgeOutOfCluster();
			if (MWOE != Integer.MAX_VALUE) {
				res = false;
				break;
			} 
		}
		
		return res;
	}
}
