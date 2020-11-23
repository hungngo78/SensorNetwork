package sensor_network.distributed.naive.utility;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import sensor_network.distributed.naive.Network;
import sensor_network.distributed.naive.data.Axis;
import sensor_network.distributed.naive.data.Edge;
import sensor_network.distributed.naive.data.Node;
import sensor_network.distributed.naive.graph.AggregationGraph;
import sensor_network.distributed.naive.graph.SensorNetworkGraph;
import sensor_network.graph.storing.Graph;
import sensor_network.graph.storing.SensorNode;

public class Util {

	
	public static void randomSleep() throws InterruptedException {
		Thread.sleep((long) (4000 * Math.random() + 1000));
	}
	
	public static void sleep(int s) throws InterruptedException {
		Thread.sleep((long) (1000 * s));
	}
	
	public static boolean checkContain(ArrayList<Edge> edges, int n1, int n2) {
		for (int i= 0; i< edges.size(); i++) {
			if ((edges.get(i).id1 == n1 && edges.get(i).id2 == n2) || 
				(edges.get(i).id1 == n2 && edges.get(i).id2 == n1))
				return true;
		}
		
		return  false;
	}
	
	public static double calculateDistance(int senderID, int receiverID) {
		double cost = 0;
		
		if ((senderID < 0) || (receiverID <0))
			return cost;
		
		if (senderID == receiverID)
			return cost;
		
		// sum up cost
		double distance = 0;
		
		// calculate distance between 2 nodes 
		distance = Network.getWeight(senderID, receiverID);
		
		return distance;
	}
	
	// Message size =  20 B
	public static double calculateCost20B(int senderID, int receiverID) {
		double cost = 0;
		
		if (senderID == receiverID)
			return cost;
		
		// sum up cost
		double distance = 0;
		
		// calculate distance between 2 nodes 
		distance = Network.getWeight(senderID, receiverID);
		
		// calculate transmission energy consumption
		double transmissionCost = (1.6 + 1.6*distance*distance/1000)/100000;
		
		// calculate  receiving energy consumption
		double receivingCost = 0.000016;
		
		// calculate transmission and receiving energy consumption
		cost = transmissionCost + receivingCost;
			
		return cost;
	}
	
	// Message size =  1000 B
	public static double calculateCost1000B(int senderID, int receiverID) {
		double cost = 0;
		
		if (senderID == receiverID)
			return cost;
		
		// sum up cost
		double distance = 0;
		
		// calculate distance between 2 nodes 
		distance = Network.getWeight(senderID, receiverID);
		
		// calculate transmission energy consumption
		double transmissionCost = 0.0008 + 8*distance*distance/10000000;
		
		// calculate  receiving energy consumption
		double receivingCost = 0.0008;
		
		// calculate transmission and receiving energy consumption
		cost = transmissionCost + receivingCost;
			
		return cost;
	}
	
	// aggregation walk
	// Message size = 512 MB
	public static double calculateCost1(int senderID, int receiverID) {
		double cost = 0;
		
		if (senderID == receiverID)
			return cost;
		
		// sum up cost
		double distance = 0;
		
		// calculate distance between 2 nodes 
		distance = Network.getWeight(senderID, receiverID);
		
		// calculate transmission energy consumption
		//double transmissionCost = (1.6 + 1.6*distance*distance/1000)/100000;
		double transmissionCost = 512*(0.8 + 8*distance*distance/10000);
		
		// calculate  receiving energy consumption
		double receivingCost = 512*0.8;
		
		// calculate transmission and receiving energy consumption
		cost = transmissionCost + receivingCost;
			
		return cost;
	}
	
	public static void drawSensorNetworkGraph(int screenHeight, double width, double height, 
								Map<Integer, Axis> nodes, Map<Integer, Set<Integer>> adjList) {
		//Draw sensor network graph
		SensorNetworkGraph graph = new SensorNetworkGraph();
		graph.setGraphWidth(width);
		graph.setGraphHeight(height);
		graph.setNodes(nodes);
		graph.setAdjList(adjList);
		graph.setPreferredSize(new Dimension(screenHeight-30, screenHeight-30));
		Thread graphThread = new Thread(graph);
		graphThread.start(); 
	}
	
	public static void drawAggregationGraph(int screenHeight, double width, double height, 
								Map<Integer, Axis> sensorNodes, Map<Integer, Set<Integer>> dataAdjList) {	
		//Draw aggregation graph
		AggregationGraph aggregationGraph = new AggregationGraph();
		aggregationGraph.setGraphWidth(width);
		aggregationGraph.setGraphHeight(height);
		aggregationGraph.setNodes(sensorNodes);
		aggregationGraph.setAdjList(dataAdjList);
		aggregationGraph.setPreferredSize(new Dimension(screenHeight-30, screenHeight-30));
		Thread AgraphThread = new Thread(aggregationGraph);
		AgraphThread.start(); 
	}
	
	public static Node[] populateNodes(Graph graph, double width, double height) {
		int N = graph.getNodesNumber();
		if (N <= 0)
			return null;
		
		Map<Integer, SensorNode> savedNodes = graph.getNodes();
		Node[] nodes = new Node[N];
		
		int i= 0;
		for (int key: savedNodes.keySet()) {
			SensorNode sensorNode = savedNodes.get(key);
			
			Axis axis = new Axis();
			axis.setxAxis(sensorNode.getxAxis());
			axis.setyAxis(sensorNode.getyAxis());
			axis.setNodeType(sensorNode.isDataNode());
			
			nodes[i] = new Node(i);
			nodes[i].axis = axis;
			i++;
		}
				
		return nodes;
	}
	
	public static Node[] populateNodes(int N, int dataNodeCount, double width, double height) {
		Node[] nodes = new Node[N];
		
		Random random = new Random();
		int i= 0;
		while (i < N) {
			Axis axis = new Axis();
			int scale = (int) Math.pow(10, 1);
			double xAxis =(0 + random.nextDouble() * (width - 0));
			double yAxis = 0 + random.nextDouble() * (height - 0);
			
			xAxis = (double)Math.floor(xAxis * scale) / scale;
			yAxis = (double)Math.floor(yAxis * scale) / scale;
			
			axis.setxAxis(xAxis);
			axis.setyAxis(yAxis);
			
			// and we dont want it is so close to all the other nodes
			//if (validateAxis(point.x, point.y)) {
			if (validateAxis(nodes, xAxis, yAxis)) {
				//System.out.println("  added node "+i);
				nodes[i] = new Node(i);
				//nodes[i].axis = point;
				nodes[i].axis = axis;
				i++;
			}
		}
		
		while (dataNodeCount > 0) {
			int index = random.nextInt(N);
			Node node = nodes[index];
			if (!node.axis.isDataNode()) {
				node.axis.setNodeType(true);
				dataNodeCount--;
			}   
		}
		
		return nodes;
	}
	
	public static void populateAdjacencyMatrix(Graph graph, Map<Integer, Set<Integer>> _adjacencyMatrix, 
											Map<Integer, Set<Integer>> _dataAdjacencyMatrix) {
		Map<Integer, Set<Integer>> savedMatrix = graph.getAdjacencyMatrix();
		Map<Integer, Set<Integer>> savedDataMatrix = graph.getDataAdjacencyMatrix();
		
		for (int key: savedMatrix.keySet()) {
			Set<Integer> savedList = savedMatrix.get(key);
			Set<Integer> newList = new HashSet<Integer>();
			for (int v: savedList) 
				newList.add(v - 1);
			
			_adjacencyMatrix.put(key-1, newList);
		}
		
		for (int key: savedDataMatrix.keySet()) {
			//if (savedDataMatrix.get(key).size() > 0) {
				Set<Integer> savedList = savedDataMatrix.get(key);
				Set<Integer> newList = new HashSet<Integer>();
				for (int v: savedList) 
					newList.add(v - 1);
				
				_dataAdjacencyMatrix.put(key-1, newList);
			//}
		}
	}
	
	public static void populateAdjacencyMatrix(Node nodes[], Map<Integer, Set<Integer>> _adjacencyMatrix, 
															Map<Integer, Set<Integer>> _dataAdjacencyMatrix) {
		if (nodes.length<=0)
			return;
		
		for(int i=0; i< nodes.length; i++) {
			_adjacencyMatrix.put(i, new HashSet<Integer>());
			if (nodes[i].axis.isDataNode())
				_dataAdjacencyMatrix.put(i, new HashSet<Integer>());
		}
		
		for (int id1 = 0; id1 < nodes.length; id1++) {
			//Point axis1 = nodes[id1].axis;
			Axis axis1 = nodes[id1].axis;
			double xAxis1 = axis1.getxAxis();
			double yAxis1 = axis1.getyAxis();
			
			for (int id2 = 0; id2 < nodes.length; id2++) {
				if (id1 == id2)
					continue;
					
				//Point axis2 = nodes[id2].axis;
				Axis axis2 = nodes[id2].axis;
				double xAxis2 = axis2.getxAxis();
				double yAxis2 = axis2.getyAxis();
				
				double distance =  Math.sqrt(((xAxis1-xAxis2)*(xAxis1-xAxis2)) + ((yAxis1-yAxis2)*(yAxis1-yAxis2)));
				
				if (distance <= Constant.TRANMISSION_RANGE) {
					Set<Integer> tempList = _adjacencyMatrix.get(id1);
					tempList.add(id2);
					_adjacencyMatrix.put(id1, tempList);
						
					tempList = _adjacencyMatrix.get(id2);
					tempList.add(id1);
					_adjacencyMatrix.put(id2, tempList);
					
					// if both are data node, add them into _dataAdjacencyMatrix
					if (nodes[id1].axis.isDataNode() && nodes[id2].axis.isDataNode()) {
						tempList = _dataAdjacencyMatrix.get(id1);
						tempList.add(id2);
						_dataAdjacencyMatrix.put(id1, tempList);
							
						tempList = _dataAdjacencyMatrix.get(id2);
						tempList.add(id1);
						_dataAdjacencyMatrix.put(id2, tempList);
					}
				}
			}
		}
	}
	
	public static void eliminateIsolation(Node nodes[], Map<Integer, Set<Integer>> _adjacencyMatrix, 
											Map<Integer, Set<Integer>> _dataAdjacencyMatrix) {
		Random rd = new Random();
		int nodeCount = nodes.length;
		
		for(int node1: _adjacencyMatrix.keySet()) {
			Set<Integer> adjList = _adjacencyMatrix.get(node1);
			if (adjList.size()==0) {
				int node2 = node1;
				while (node2 == node1)
					node2 = rd.nextInt(nodeCount);
				
				adjList.add(node2);
				_adjacencyMatrix.put(node1, adjList);
				
				adjList = _adjacencyMatrix.get(node2);
				adjList.add(node1);
				_adjacencyMatrix.put(node2, adjList);
				
				// if both are data node, add them into _dataAdjacencyMatrix
				if (nodes[node1].axis.isDataNode() && nodes[node2].axis.isDataNode()) {
					Set<Integer> tempList = _dataAdjacencyMatrix.get(node1);
					tempList.add(node2);
					_dataAdjacencyMatrix.put(node1, tempList);
						
					tempList = _dataAdjacencyMatrix.get(node2);
					tempList.add(node1);
					_dataAdjacencyMatrix.put(node2, tempList);
				}
			}
		}
	}
	
	public static int[] getNeighbours(int id, Map<Integer, Set<Integer>> _adjacencyMatrix) {
		int size = 0;
		Set<Integer> adjList = _adjacencyMatrix.get(id);
		if (adjList == null || adjList.size()==0)
			return null;
		else
			size = adjList.size();
		
		int arr2[] = new int[size];
		int i = 0;
		for(int node: adjList) {
			arr2[i] = node;
			i = i+1;
		}
		return arr2;
	}

	public static boolean checkIfInSpanningTree(List<Edge> edges, int node) {
		boolean res = false;
		
		synchronized (edges) {
			Iterator<Edge> ite = edges.iterator();
			while (ite.hasNext()) {
				Edge edge = ite.next();
				if (edge.id1 == node || edge.id2 == node) {
					res = true;
					break;
				}
					
			}
		}
		
		return res;
	}
	
	private static boolean validateAxis(Node[] nodes, double xAxis, double yAxis) {
		boolean minDistanceCheck = true;
		
		for(int i=0; i< nodes.length; i++) {
			Node n = nodes[i];
			if (n != null) {
				double xAxis1 = n.axis.getxAxis();
				double yAxis1 = n.axis.getyAxis();
				
				int distance =  (int) Math.sqrt(((xAxis1-xAxis)*(xAxis1-xAxis)) + ((yAxis1-yAxis)*(yAxis1-yAxis)));
				if (distance <= Constant.MIN_DISTANCE_BETWEEN_NODES) {
					minDistanceCheck = false;
					break;
				}
			}
		}
		
		return minDistanceCheck;
	}

	public static int checkExisting(Edge edge, Map<Integer, Set<Integer>> connectedComponents) {
		int result = 0; 	// both 2 node do not belong to any connectedComponent
		
		int indexN1 = 0;
		int indexN2 = 0;

		for (int key: connectedComponents.keySet()) {
			Set<Integer> connectedComponent = connectedComponents.get(key);

			for (int n: connectedComponent) {
				if (n == edge.id1) 
					indexN1 = key;
				if (n == edge.id2) 
					indexN2 = key;
			}
			
			// both 2 node belong to 1 connectedComponent
			if (indexN1 != 0 && indexN2 != 0 && indexN1 == indexN2) {
				result = 3;
				break;
			}
			
			// 2 node belong to 2 different connectedComponents
			else if (indexN1 != 0 && indexN2 != 0 && indexN1 != indexN2) {
				// combine 2 connectedComponents
				Set<Integer> connectedComponent1 = connectedComponents.get(indexN1);	
				Set<Integer> connectedComponent2 = connectedComponents.get(indexN2);	
				connectedComponent1.addAll(connectedComponent2);
				connectedComponents.remove(indexN2);
				
				result = 2;
				break;
			}
		}
		
		// 1 node is belong to 1 connectedComponent, 1 the other node is not
		if ((indexN1 == 0 && indexN2 != 0) || (indexN1 != 0 && indexN2 == 0)) {
			// add node into connectedComponent
			if (indexN2 != 0)
				connectedComponents.get(indexN2).add(edge.id1);
			
			if (indexN1 != 0)
				connectedComponents.get(indexN1).add(edge.id2);
			
			result = 1;	
		}

		return result;
	}
	
	public static ArrayList<Integer> findAllLeafNodes(Set<Integer> connectedComponent, Map<Integer, Set<Integer>> aggregatorAdjList, ArrayList<Edge> qEdges) {
		ArrayList<Integer> leafNodes = new ArrayList<>(); 
		
		for (int node: connectedComponent) {
			aggregatorAdjList.put(node, new HashSet<Integer>());
			
			// check if it is leaf node
			int count = 0;
			Iterator<Edge> ite = qEdges.iterator();
			while (ite.hasNext()) {
				Edge edge = ite.next();
				if (node == edge.id1) {
					count++;
					aggregatorAdjList.get(node).add(edge.id2);
				}
				else if (node == edge.id2) {
					count++;
					aggregatorAdjList.get(node).add(edge.id1);
				}
			}
			
			if (count == 1) {		// it's leaf node
				// put it into leaf list of current connectedComponent
				leafNodes.add(node);
			}
		}
		
		return leafNodes;
	}
	
	/*
	 * for debug
	public static boolean checkExistingInForest(int n1, int n2, List<Edge> queue) {
		boolean existing = false;
		Iterator<Edge> ite = queue.iterator();
		while (ite.hasNext()) {
			Edge e = ite.next();
			if ((e.id1 == n1 && e.id2 == n2) || (e.id1 == n2 && e.id2 == n1)) {
				existing = true;
				break;
			}
		}
		return existing;
	}*/

	
}
