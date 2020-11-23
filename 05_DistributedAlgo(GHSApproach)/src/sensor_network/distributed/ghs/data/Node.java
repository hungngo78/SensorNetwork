package sensor_network.distributed.ghs.data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;

import sensor_network.distributed.ghs.Network;
import sensor_network.distributed.ghs.utility.Constant;
import sensor_network.distributed.ghs.utility.Util;

public class Node implements Runnable {
	private int id;
	public Axis axis;
	private BlockingQueue<Message> messageQueue;
	private Cluster cluster;
	
	// step 1
	private int neighbours[];
	private Map<Integer, ShortestPath> routingTable = new ConcurrentHashMap<Integer, ShortestPath> ();
	
	// step 2
	public int furthestBuddy;
	private int dataNeighbours[];
	
	// step 3: aggregation walk
	public double ccMinDistance = Integer.MAX_VALUE;
	public int ccMinStartPoint = -1;
	public List<Integer> ccMinDFSWalk = null;
	

	public Node(int id) {
		this.id = id;
		//this.messageQueue = new LinkedBlockingQueue<Message>();
		this.messageQueue = new PriorityBlockingQueue<Message>();
	}
	
	public void run() {
		furthestBuddy = getFurthestBuddy();
		
		/* Step 1 */
		// initialize routing table including all neighbors
		for (int i = 0; i < neighbours.length; i++) {
			double cost = Network.getWeight(getId(), neighbours[i]);
			
			ShortestPath newPath = new ShortestPath();
			newPath.setCost(cost);
			newPath.addNode(neighbours[i]);
			
			this.routingTable.put(neighbours[i], newPath);
		}
		
		// send 1st msg to all neighbors
		double maxCost = -1;
		int furthestNeighbor = -1;
		double cost = -1;
		Message initMsg = new Message(getId(), -1, Commands.ROUTING, this.routingTable);
		for (int i = 0; i < neighbours.length; i++) {
			//initMsg.visitedNodes.add(neighbours[i]);
			
			cost = Network.getWeight(id, neighbours[i]);
			if (cost > maxCost) {
				furthestNeighbor = neighbours[i];
				maxCost = cost;
			}
		}
		
		for (int i = 0; i < neighbours.length; i++) {
			initMsg.receiverID = neighbours[i];
			
			//double cost = Network.getWeight(id, neighbours[i]);
			//initMsg.weight = cost;
			
			if (neighbours[i] == furthestNeighbor) 
				Network.sendMessage(initMsg, true, true);
			else
				Network.sendMessage(initMsg, false, true);
		}
		
		while (Network.getStep() == Constant.STEP1) {
			try {
				Util.randomSleep();
				
				routingSendAndReceive();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		
		/* step 2, but only data nodes participate this process */
		if (axis.isDataNode()) {
			//furthestBuddy = getFurthestBuddy();
			
			cluster = Network.getCluster(this);
			cluster.setLeader(this);
			
			//while (Network.getClusters().size() > 1 && Network.getStep() == Constant.STEP2) {
			//while (!Network.checkIfReachedQnodes()) {
			while (!Network.checkIfStep2Finished()) {
				if (dataNeighbours == null || dataNeighbours.length == 0) 
					continue;
				
				try {
					//Util.randomSleep();
					Thread.sleep(4000);
					
					// update situation
					cluster = Network.getCluster(this);
					if(cluster == null)
						continue;
					
					// if the current cluster has no leader, vote a new leader
					if (cluster.getLeader() == null
							&& this == cluster.getNodes().get(0)) {	 // only the first node of cluster takes care of this task
						electLeader(getLeastWeightedEdgeOutOfCluster());
					}
					
					if (cluster.getLeader() == this) {
						//System.out.println(getId() + " leader, sending");
						sendAndMerge(getNodeWithLeastWeightedEdgeOutOfCluster());
						//if (sendAndMerge(getNodeWithLeastWeightedEdgeOutOfCluster()))
							//continue;
						
						//System.out.println(getId() + " leader, receiving");
						receiveAndMerge();
						//if (receiveAndMerge())
							//continue;
					} else {
						//System.out.println(getId() + " receiving");
						receiveAndMerge();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		
		// reset routing tables, message queues
		Network.clearMessage(id);
	}
	
	
	
	
	/*********************** Step 1: Bellman Ford algorithm *************************************/
	private boolean isExistingInRoutingTable(int node) {
		for (Integer key: this.routingTable.keySet()) {
			if (node == key)
				return true;
		}
		return false;
	}
	
	private boolean isUpdatingRoutingTable(int sender, int command, double costFromSenderToThis, 
										Map<Integer, ShortestPath> _routingTable) {
		boolean needUpdate = false;

		for (Integer key: _routingTable.keySet()) {
			if (key == id) 
				continue;
			
			// if key is not existing in routingTable, add it
			if (!isExistingInRoutingTable(key)) {
				ShortestPath pathToSender = _routingTable.get(key);
				
				ShortestPath newPath = new ShortestPath();
				costFromSenderToThis = Network.getWeight(sender, id);
				newPath.setCost(pathToSender.getCost() + costFromSenderToThis);
				
				newPath.setPath(pathToSender.getPath());
				newPath.addNode(sender);
				
				this.routingTable.put(key, newPath);
				
				needUpdate = true;
			} else {  // otherwise, check cost, then update if any	
				ShortestPath currentShortestPath = this.routingTable.get(key);
				ShortestPath pathToSender = _routingTable.get(key);
				double currentCost = currentShortestPath.getCost();
				costFromSenderToThis = Network.getWeight(sender, id);
				if (pathToSender.getCost() + costFromSenderToThis < currentCost) {
					currentShortestPath.setCost(pathToSender.getCost() + costFromSenderToThis);
					
					currentShortestPath.setPath(pathToSender.getPath());
					currentShortestPath.addNode(sender);
					
					this.routingTable.put(key, currentShortestPath);
					
					needUpdate = true;
				}
			}
		}
			
		return needUpdate;
	}
	
	private boolean routingSendAndReceive() throws InterruptedException {
		Message recMessage;
		while (Network.hasMessage(getId())) {
			//System.out.println(getId() + ": got message");
			recMessage = Network.receiveMessage(getId());
			if (recMessage != null &&
				(recMessage.command == Commands.ROUTING)) {
				// check if there is any update on routing table
				boolean needUpdate = isUpdatingRoutingTable(recMessage.senderID, recMessage.command, 
																recMessage.weight, recMessage.routingTable);
				if (needUpdate) {
					// send update msg to all neighbor except sender and except nodes who received this message already
					Message newMsg = new Message(getId(), -1, Commands.ROUTING, this.routingTable);
					//newMsg.visitedNodes.addAll(recMessage.visitedNodes);
					
					double maxCost = -1;
					int furthestNeighbor = -1;
					double cost = -1;
					for (int i = 0; i < neighbours.length; i++) {
						if  ((neighbours[i] != recMessage.senderID) && 
								(!recMessage.visitedNodes.contains(neighbours[i])) ) {
							newMsg.visitedNodes.add(neighbours[i]);
							
							cost = Network.getWeight(id, neighbours[i]);
							if (cost > maxCost) {
								furthestNeighbor = neighbours[i];
								maxCost = cost;
							}
						}
					}
					
					for (int i = 0; i < neighbours.length; i++) {
						if  ((neighbours[i] != recMessage.senderID)  && 
								(!recMessage.visitedNodes.contains(neighbours[i])) ) {
							newMsg.receiverID = neighbours[i];
							
							//double cost = Network.getWeight(id, neighbours[i]);
							//newMsg.weight = cost;
							
							if (neighbours[i] == furthestNeighbor) 
								Network.sendMessage(newMsg, true, true);
							else
								Network.sendMessage(newMsg, false, true);
						}
					}
				}
			}
		}
		return false;
	}

	
	/*********************** Step 2: GHS algorithm *************************************/
	public int getFurthestBuddy() {
		double max = 0;
		int furthestBuddy = -1;
		
		for (int i = 0; i < neighbours.length; i++) {
			double distance = Network.getWeight(id, neighbours[i]);
			if (distance > max) {
				max = distance;
				furthestBuddy = neighbours[i];
			}
		}
		
		return furthestBuddy;
	}
	
	private void electLeader(double weight) throws InterruptedException {
		//System.out.println("Node[" + getId()  + "] starts electing Leader for cluster: " + cluster.getId() + "; weight="+weight);
		if (cluster.getNodes().size() == 1) {
			//System.out.println("Elected leader: " + getId() + " for cluster: "
			//		+ cluster.getId());
			cluster.setLeader(this);
			return;
		}

		double maxDistance = -1;
		double distance = -1;
		int furthestNeighbor = -1;
		for (Node n : cluster.getNodes()) {
			if (n.getId() != id) {
				distance = Network.getWeight(id, n.getId());
				if (distance > maxDistance) {
					maxDistance = distance;
					furthestNeighbor = n.getId();
				}
			}
		}
		for (Node n : cluster.getNodes()) {
			if (n.getId() != id) {
				if (furthestNeighbor == n.getId()) {
					Network.sendMessage(new Message(id, n.getId(), Commands.LEADER_ELECTION), true, false);
					Network.step2InitiateMsgCount+=1;
				} else
					Network.sendMessage(new Message(id, n.getId(), Commands.LEADER_ELECTION), false, false);
			}
		}
		
		// System.out.println(getId() + ": sent messages");

		ArrayList<Message> arrm = new ArrayList<Message>();
		while (arrm.size() < cluster.getNodes().size() - 1) {
			// System.out.println(getId() + ": messages size" + arrm.size());
			Message recMessage = Network.receiveMessage(id);
			if (recMessage.command == Commands.LEADER_ELECTION_REPLY) {
				arrm.add(recMessage);
			} else if (recMessage.command == Commands.CONNECT) {
				//double distance = Network.getWeight(id, recMessage.senderID);
				//Message toSend = new Message(id, recMessage.senderID, Commands.REJECT);
				//Network.sendMessage(toSend);
			}
		}
		
		if (weight == 0) 
			weight = Integer.MAX_VALUE; //10000;
		
		double min = weight;
		for (Message m : arrm) {
			if (min > m.weight) {
				min = m.weight;
			}
		}
		ArrayList<Integer> arr = new ArrayList<Integer>();
		for (Message m : arrm) {
			if (min == m.weight) {
				arr.add(m.senderID);
			}
		}
		// System.out.println(getId() + ": Arr size: " + arr.size());
		if (arr.size() == 0) {
			//System.out.println("a. Elected leader: " + getId() + " for cluster: " + cluster.getId());
			cluster.setLeader(this);
			return;
		}

		int node_id = arr.get((int) (Math.random() * arr.size()));
		for (Node n : cluster.getNodes()) {
			if (n.getId() == node_id) {
				//System.out.println("b. Elected leader: " + n.getId() + " for cluster: " + cluster.getId());
				
				// send Connect msg from old leader to new leader
				// for change core
				Network.step2Cost += 2 * Util.calculateCost20B(id, node_id);
				Network.step2overheadMsgCount += 1;
				
				cluster.setLeader(n);
				break;
			}
		}
		//System.out.println("End electing Leader for cluster: " + cluster.getId());
	}
	
	private int leastId = -1;
	private boolean sendAndMerge(int least_id) throws InterruptedException {
		if (least_id == -1)
			return false;
		
		// update situation
		cluster = Network.getCluster(this);
		if(cluster == null)
			return false;
				
		//Message recMessage;
		//double distance = Network.getWeight(id, least_id);
	
		if (leastId == -1) {
			leastId = least_id;
			//System.out.println("---1-----" + getId() +" sends Connect msg to "+ least_id);
			Network.step2ConnectMsgCount+=1;
			Network.sendMessage(new Message(getId(), least_id, cluster.getLevel(), Commands.CONNECT, -1), true, true);
		} else if (leastId != least_id) {
			leastId = least_id;
			//System.out.println("---2-----" + getId() +" sends Connect msg to "+ least_id);
			Network.step2ConnectMsgCount+=1;
			Network.sendMessage(new Message(getId(), least_id, cluster.getLevel(), Commands.CONNECT, -1), true, true);
		} else {
			Network.sendMessage(new Message(getId(), least_id, cluster.getLevel(), Commands.CONNECT, -1), false, true);
		}
		//System.out.println(getId() + " sent CONNECT message to " + least_id);	
		
		/*recMessage = receiveMessageFrom(least_id);
		if (recMessage != null && 
				(recMessage.command == Commands.CONNECT	// no need to compare cluster level in this case
				|| recMessage.command == Commands.ACCEPT)) {
			Network.sendMessage(new Message(getId(), least_id, Commands.ACCEPT));
			cluster = Network.mergeClusters(getId(), least_id);
			submitLeaderElectionRequest(); 
			return true;
		}*/
		
		return true;
	}
	
	private Message receiveMessageFrom(int fromID) throws InterruptedException {
		Message recMessage = null;
		/*while (cluster.getLeader() != null) {
			//System.out.println(getId() + " is waiting for message from " + fromID);
			recMessage = checkForCLusterCommands(Network.receiveMessage(getId()));
			if (recMessage != null) {
				if (recMessage.senderID == fromID) {
					//System.out.println(getId() + " received message from " + fromID);
					break;
				} else if (recMessage.command == Commands.CONNECT) {
					Message toSend = new Message(getId(), recMessage.senderID, Commands.REJECT);
					Network.sendMessage(toSend);
				}
			}
			
			// update situation
			cluster = Network.getCluster(this);
			if(cluster == null)
				break;
		}*/
		return recMessage;
	}
	
	private boolean receiveAndMerge() throws InterruptedException {
		Message recMessage;
		while (Network.hasMessage(getId())) {
			// update situation
			cluster = Network.getCluster(this);
			if(cluster == null)
				break;
			
			//System.out.println(getId() + ": got message");
			recMessage = checkForCLusterCommands(Network.receiveMessage(getId()));
			if (recMessage != null && recMessage.command == Commands.CONNECT) {
				boolean combined = false;
				if (recMessage.level == cluster.getLevel()) {
					if (checkIfSharingSameMWOEWith(recMessage.senderID)) {
						//Network.sendMessage(new Message(getId(), recMessage.senderID, Commands.ACCEPT));
						cluster = Network.mergeClusters(getId(), recMessage.senderID);		
						combined = true;
					}
				} else if (recMessage.level < cluster.getLevel()) {
					cluster = Network.absorbClusters(getId(), recMessage.senderID);	
					combined = true;
				}
				if (combined) {				
					submitLeaderElectionRequest();
					//return true;
				}
			}
			
			try {
				Thread.sleep(500);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}		
		return false;
	}

	private void submitLeaderElectionRequest() {
		if (cluster == null)
			return;
		
		if (this != cluster.getLeader()) {
			double distance = Network.getWeight(id, cluster.getNodes().get(0).getId());
			Network.sendMessage(new Message(id, cluster.getNodes().get(0).getId(), 
									Commands.LEADER_ELECTION_REQUEST), false, true);
		}
	}

	private Message checkForCLusterCommands(Message receivedMessage) throws InterruptedException {
		if (receivedMessage.command == Commands.LEADER_ELECTION) {
			double minWOE = getLeastWeightedEdgeOutOfCluster();
			//System.out.println(getId() + ": got leader election message, rep REPLY msg with " + minWOE + " of MWOE");
			Message m = new Message(getId(), receivedMessage.senderID, -1,
										Commands.LEADER_ELECTION_REPLY,
										minWOE);
			
			if (minWOE == Integer.MAX_VALUE)
				Network.sendMessage(m, false, true);
			else {
				Network.sendMessage(m, true, true);
				Network.step2ReportMsgCound+=1;
			}
			return null;
		} else if (receivedMessage.command == Commands.LEADER_ELECTION_REQUEST) {
			//System.out.println(getId() + ": got leader election request");
			return null;
		}
		return receivedMessage;
	}
	
	private int getNodeWithLeastWeightedEdgeOutOfCluster() {
		if (cluster == null)
			return -1;
		
		double min = getLeastWeightedEdgeOutOfCluster();
		ArrayList<Integer> arr = new ArrayList<Integer>();
		for (int i = 0; i < dataNeighbours.length; i++) {
			if (!cluster.isInCluster(dataNeighbours[i])) {
				double d = Network.getWeight(id, dataNeighbours[i]);
				if (min == d) {
					arr.add(dataNeighbours[i]);
				}
			}
		}
		if (arr.size() > 0) {
			int id = (int) (Math.random() * arr.size());
			return arr.get(id);
		} 
		
		return -1;
	}

	public double getLeastWeightedEdgeOutOfCluster() {
		double min = Integer.MAX_VALUE;
		
		// update situation
		cluster = Network.getCluster(this);
		if(cluster == null)
			return min;
		
		if (dataNeighbours == null)
			return min;
		
		for (int i = 0; i < dataNeighbours.length; i++) {
			if (!cluster.isInCluster(dataNeighbours[i])) {
				double d = Network.getWeight(id, dataNeighbours[i]);
				if (min > d) {
					min = d;
				}
			}
		}
		return min;
	}
	
	private boolean checkIfSharingSameMWOEWith(int fromID) {
		cluster = Network.getCluster(this);
		if(cluster == null)
			return false;
		
		if (this != cluster.getLeader())
			return false;
				
		double min = getLeastWeightedEdgeOutOfCluster();
		ArrayList<Integer> arr = new ArrayList<Integer>();
		for (int i = 0; i < dataNeighbours.length; i++) {
			if (!cluster.isInCluster(dataNeighbours[i])) {
				double d = Network.getWeight(id, dataNeighbours[i]);
				if (min == d) {
					arr.add(dataNeighbours[i]);
				}
			}
		}
		return (arr.contains(fromID));
	}

	
	/*********************** Set/Get methods *************************************/
	public BlockingQueue<Message> getMessageQueue() {
		return messageQueue;
	}

	public int getId() {
		return id;
	}

	public int[] getNeighbours() {
		return neighbours;
	}
	public void setNeighbours(int[] neighbours) {
		this.neighbours = neighbours;
	}
	
	public int[] getDataNeighbours() {
		return dataNeighbours;
	}
	public void setDataNeighbours(int[] neighbours) {
		this.dataNeighbours = neighbours;
	}
	
	public Map<Integer, ShortestPath> getRoutingTable() {
		return this.routingTable;
	}
}
