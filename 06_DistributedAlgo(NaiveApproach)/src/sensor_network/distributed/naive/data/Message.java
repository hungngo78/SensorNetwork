package sensor_network.distributed.naive.data;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Message implements Comparable<Message> {
	public int senderID;
	public int receiverID;
	
	public Map<Integer, ShortestPath> routingTable;
	public Set<Integer> visitedNodes;
	
	public int level;
	public int command;
	public double weight=0;

	public Message(int id1, int id2, int _command) {
		this.senderID = id1;
		this.receiverID = id2;
		this.command = _command;
		
		visitedNodes = new HashSet<>();
	}
	
	public Message(int id1, int id2, int _level, int _command, double weight) {
		this.senderID = id1;
		this.receiverID = id2;
		this.level = _level;
		this.command = _command;
		this.weight = weight;
		
		visitedNodes = new HashSet<>();
	}

//	public Message(int id1, int id2, int _command, double weight) {
//		this.senderID = id1;
//		this.receiverID = id2;
//		this.command = _command;
//		this.weight = weight;
//		
//		visitedNodes = new HashSet<>();
//	}
	
	//public Message(int id1, int id2, int _command, double _weight, Map<Integer, ShortestPath> _routingTable) {
	public Message(int id1, int id2, int _command, Map<Integer, ShortestPath> _routingTable) {
		this.senderID = id1;
		this.receiverID = id2;
		this.command = _command;
		//this.weight = _weight;
		
		this.routingTable = _routingTable;
		visitedNodes = new HashSet<>();
	}
	
	@Override
	public int compareTo(Message o) {
		if (this.weight > o.weight)
			return 1;
		else if (this.weight < o.weight)
			return -1;
		else
			return 0;
	}
}
