package sensor_network.distributed.ghs;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import sensor_network.distributed.ghs.data.Axis;
import sensor_network.distributed.ghs.data.Cluster;
import sensor_network.distributed.ghs.data.Edge;
import sensor_network.distributed.ghs.data.Message;
import sensor_network.distributed.ghs.data.Node;
import sensor_network.distributed.ghs.data.ShortestPath;
import sensor_network.distributed.ghs.utility.Constant;
import sensor_network.distributed.ghs.utility.Constant.STEP2_END_CONDITION;
import sensor_network.distributed.ghs.utility.Util;
import sensor_network.graph.storing.Graph;

public class Network {
	
	private static Node nodes[];
	private static Map<Integer, Set<Integer>> adjacencyMatrix = new LinkedHashMap<Integer, Set<Integer>> ();
	private static Map<Integer, Set<Integer>> dataAdjacencyMatrix = new LinkedHashMap<Integer, Set<Integer>> ();
	
	private static int step;
	private static List<Cluster> clusters;
	private static List<Edge> spanningEdges;
	private static ThreadGroup threadgroup;
	
	private static Graph graph = null;
	
	public static int step1overheadMsgCount = 0;
	public static double step1Cost = 0;
	public static int step2overheadMsgCount = 0;
	public static double step2Cost = 0;
	public static int step2ConnectMsgCount = 0;
	public static int step2InitiateMsgCount = 0;
	public static int step2ReportMsgCound = 0;
	public static STEP2_END_CONDITION step2EndCondition; 
	
	
	//public static void initialise(int N, int dataNodeCount) {
	public static void initialise(String nodesPath ) {
		step = Constant.STEP1;
		step2EndCondition = Constant.STEP2_END_CONDITION.REACH_Q_EDGES;
				
		/* read graph from centralized algorithm */
	    try
	    {
	         FileInputStream fis = new FileInputStream(nodesPath);
	         ObjectInputStream ois = new ObjectInputStream(fis);
	         graph = (Graph) ois.readObject();
	         
	         
	 		 System.out.println("number of nodes: " + graph.getNodesNumber());
	 		 System.out.println("number of data nodes: " + graph.getDataNodesNumber());
	 		 System.out.println("R number: " + graph.getNumberR());
	 		 System.out.println("m number: " + graph.getNumberm());	 		 
	 		 //System.out.println("r number: " + graph.getNumberr());
	 		 System.out.println("q number: " + graph.getNumberq());
	 		 System.out.println();
	 		
	         ois.close();
	         fis.close();
	    } catch(IOException ioe) {
	         ioe.printStackTrace();
	         return;
	    }catch(ClassNotFoundException c) {
	         System.out.println("Class not found");
	         c.printStackTrace();
	         return;
	    }
		
		/* generate graph */
		//nodes = Util.populateNodes(N, dataNodeCount, Constant.graphWidth, Constant.graphHeight);
		//Util.populateAdjacencyMatrix(nodes, adjacencyMatrix, dataAdjacencyMatrix);
		//Util.eliminateIsolation(nodes, adjacencyMatrix, dataAdjacencyMatrix);
	    nodes = Util.populateNodes(graph, graph.getWidth(), graph.getHeight());
	    Util.populateAdjacencyMatrix(graph, adjacencyMatrix, dataAdjacencyMatrix);
		
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int screenHeight = (int) Math.round(screenSize.getHeight());
		
		/* draw original sensor network */
		if (Constant.DRAW_SENSOR_NETWORK_GRAPH) {
			Map<Integer, Axis> sensorNodes = new LinkedHashMap<Integer, Axis>();
			for(int i = 0; i < nodes.length; i++) {
				sensorNodes.put(i, nodes[i].axis);	
			}
			Util.drawSensorNetworkGraph(screenHeight, graph.getWidth(), graph.getHeight(), sensorNodes, adjacencyMatrix);
		}
		
		// set neighbors for N nodes ( this is for step 1 )
		for (int i = 0; i < nodes.length; i++) {
			int[] neighbours = Util.getNeighbours(i, adjacencyMatrix);
			nodes[i].setNeighbours(neighbours);
		}
		
		// initialize N clusters, one cluster consists one node ( this is for step 2)
		clusters = Collections.synchronizedList(new ArrayList<Cluster>(nodes.length));
		spanningEdges = Collections.synchronizedList(new ArrayList<Edge>());
		for (Node node : nodes) {
			// only data nodes are related to step 2 (GHS algorithm)
			if (node.axis.isDataNode())				
				clusters.add(new Cluster(node));
		}
	}
	
	public static Graph getGraph() {
		return graph;
	}
	
	public static void startGHS() {
		threadgroup = new ThreadGroup("GHS Nodes");
		for (int i = 0; i < nodes.length; i++) {
			new Thread(threadgroup, nodes[i]).start();
		}
	}

	@SuppressWarnings("deprecation")
	public static void stopGHS() {
		threadgroup.stop();
	}
	
	public static void sendMessage(Message message, boolean countCost, boolean direct) {
		nodes[message.receiverID].getMessageQueue().offer(message);
		
		double cost = 0;
		/*
		if (step == Constant.STEP1) {
			// calculate transmission and receiving energy consumption
			cost += Util.calculateCost(message.senderID, message.receiverID);
			if (cost != 0) {
				step1Cost += cost;
				step1overheadMsgCount += 1;
			}
		}*/
		
		if (!countCost)
			return;
		
		//double cost = 0;
		if (step == Constant.STEP1) {
			// calculate transmission and receiving energy consumption
			cost += Util.calculateCost1000B(message.senderID, message.receiverID);
			if (cost != 0) {
				step1Cost += cost;
				step1overheadMsgCount += 1;
			}
		} else if (step == Constant.STEP2) {
			double cost1 = 0, cost2 = 0;
			if (direct) {
				cost1 = Util.calculateCost20B(message.receiverID, message.senderID);
				step2overheadMsgCount += 1;
				step2Cost += cost1;
			}
			else {
				Node receiver = nodes[message.receiverID];
				Map<Integer, ShortestPath> routingTable = receiver.getRoutingTable();
				ShortestPath shortestPathObj = routingTable.get(message.senderID);
				if (shortestPathObj != null) {
					Set<Integer> path = shortestPathObj.getPath();
					Iterator<Integer> ite = path.iterator();
					int n1 = message.senderID; int n2 = message.senderID;
					while (ite.hasNext()) {
						Integer n = ite.next();
						if (n1 == n2) {
							n2 = n;
						} else {
							n1 = n2;
							n2 = n;
						}
						
						// calculate transmission and receiving energy consumption from n1 to n2
						cost = Util.calculateCost20B(n1, n2);
						if (cost != 0) {
							//step2Cost += cost;
							cost2 += cost;
							step2overheadMsgCount += 1;
						}
					}
					
					// calculate transmission and receiving energy consumption from n2 to receiver
					cost = Util.calculateCost20B(n2, message.receiverID);
					if (cost != 0) {
						//step2Cost += cost;
						cost2 += cost;
						step2overheadMsgCount += 1;
					}
				}
				
				step2Cost += cost2;
			}
		}
	}

	public static Message receiveMessage(int id) throws InterruptedException {
		return nodes[id].getMessageQueue().take();
	}
	
	public static void clearMessage(int id) {
		nodes[id].getMessageQueue().clear();
	}

	synchronized public static Cluster absorbClusters(int a, int b) {
		if (step != Constant.STEP2)
			return null;
		
		if (checkIfStep2Finished()) 
			return null;
		
		Cluster clus, ca = getCluster(a), cb = getCluster(b);
		if (ca == null || cb == null)
			return null;
		if (ca == cb)
			return ca;
		int oldLevel = ca.getLevel();
		clus = new Cluster();
		clus.setLevel(oldLevel);
		clus.getNodes().addAll(ca.getNodes());
		clus.getNodes().addAll(cb.getNodes());
		clusters.remove(ca);
		clusters.remove(cb);
		clusters.add(clus);
		spanningEdges.add(new Edge(a, b));		
		
		// notify all other fragments
		for(int i = 0; i < nodes.length; i++) {
			if (!nodes[i].axis.isDataNode()) 
				continue;
			
			Node nodei = nodes[i];
			step2Cost += Util.calculateCost20B(i, nodei.furthestBuddy);
			step2overheadMsgCount += 1;
		}		
		
		// for change core
		step2Cost += 2 * Util.calculateCost20B(a, b);
		step2overheadMsgCount += 2;
		
		// update minimum spanning tree graph
		MinimumSpanningTree.updateGraph();
		
		//System.out.println("Cluster of node " + a + " absorb cluster of node "+ b);
		
		return clus;
	}
	
	synchronized public static Cluster mergeClusters(int a, int b) {
		if (step != Constant.STEP2)
			return null;
		
		if (checkIfStep2Finished()) 
			return null;
		
		Cluster clus, ca = getCluster(a), cb = getCluster(b);
		if (ca == null || cb == null)
			return null;
		if (ca == cb)
			return ca;
		int oldLevel = ca.getLevel();
		clus = new Cluster();
		clus.setLevel(++oldLevel);
		clus.getNodes().addAll(ca.getNodes());
		clus.getNodes().addAll(cb.getNodes());
		clusters.remove(ca);
		clusters.remove(cb);
		clusters.add(clus);
		spanningEdges.add(new Edge(a, b));
		
		
		// notify all other fragments
		double tmp = 0;
		for(int i = 0; i < nodes.length; i++) {
			if (!nodes[i].axis.isDataNode()) 
				continue;
			
			Node nodei = nodes[i];
			tmp = Util.calculateCost20B(i, nodei.furthestBuddy);
			step2Cost += tmp;
			
			step2overheadMsgCount += 1;
		}
		
		// for change core
		step2Cost += 2 * Util.calculateCost20B(a, b);
		step2overheadMsgCount += 2;
		
		// update minimum spanning tree graph
		MinimumSpanningTree.updateGraph();
		
		//System.out.println("Merge cluster of node " + a + " with cluster of node "+ b);
		
		return clus;
	}

	public static double getWeight(int id1, int id2) {
		if (id1 < 0 || id2 < 0 || id1 >= nodes.length || id2 >= nodes.length)
			return 0;
		
		double xAxis1 = nodes[id1].axis.getxAxis();
		double yAxis1 = nodes[id1].axis.getyAxis();
			
		double xAxis2 = nodes[id2].axis.getxAxis();
		double yAxis2 = nodes[id2].axis.getyAxis();
		
		double distance =  Math.sqrt(((xAxis1-xAxis2)*(xAxis1-xAxis2)) + ((yAxis1-yAxis2)*(yAxis1-yAxis2)));
		return distance;
	}
	
	public static Node[] getNodes() {
		return nodes;
	}

	public static Map<Integer, Set<Integer>> getDataAdjacencyMatrix() {
		return dataAdjacencyMatrix;
	}
	
	public static boolean hasMessage(int id) {
		return nodes[id].getMessageQueue().size() > 0;
	}

	public static Cluster getCluster(int id) {
		//for (Cluster c : clusters) {
		Iterator<Cluster> ite = clusters.iterator();
		while (ite.hasNext()) {
			Cluster c = ite.next();
			
			for (Node n : c.getNodes()) {
				if (n.getId() == id) {
					return c;
				}
			}
		}
		return null;
	}

	public static Cluster getCluster(Node n) {
		//for (Cluster c : clusters) {
		Iterator<Cluster> ite = clusters.iterator();
		while (ite.hasNext()) {
			Cluster c = ite.next();
			
			if (c.getNodes().contains(n)) {
				return c;
			}
		}
		return null;
	}

	public static int getStep() {
		return step;
	}
	
	public static void setStep(int _step) {
		step = _step;
	}
	
	public static List<Edge> getSpanningEdges() {
		return spanningEdges;
	}
	
	public static void setSpanningEdges(List<Edge> _spanningEdges) {
		spanningEdges = _spanningEdges;
	}

	public static List<Cluster> getClusters() {
		return clusters;
	}
	
	public static void setClusters(List<Cluster> _clusters) {
		clusters = _clusters;
	}
	
	public static boolean checkIfStep2Finished() {
		boolean res = false;
		try {
			if (step2EndCondition == Constant.STEP2_END_CONDITION.GO_TO_END) {
				res = checkIfReachedEnd();
			} else if (step2EndCondition == Constant.STEP2_END_CONDITION.REACH_Q_EDGES) {
				res = checkIfReachedQnodes();
			}
		} catch (Exception e) {
			//e.printStackTrace();
		}
		
		return res;
	}
	
	public static boolean checkIfReachedEnd() {
		if (Network.getClusters().size() == 1)
			return true;
		
		boolean step2Finished = true; 
		Iterator<Cluster> ite = clusters.iterator();
		while (ite.hasNext()) {
			Cluster c = ite.next();
			if (!c.isIsolated()) {
				step2Finished = false;
				break;
			}
		}
		
		return step2Finished;
	}
	
	public static boolean checkIfReachedQnodes() {
		if (spanningEdges != null) 
			if (spanningEdges.size() >= graph.getNumberq()) 
					return true;
		
		return false;
	}
	
	public static ThreadGroup getThreadgroup() {
		return threadgroup;
	}
}
