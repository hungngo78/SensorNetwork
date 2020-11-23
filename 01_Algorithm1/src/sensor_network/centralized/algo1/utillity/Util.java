package sensor_network.centralized.algo1.utillity;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import sensor_network.centralized.algo1.graph.AggregationGraph;
import sensor_network.centralized.algo1.graph.AggregationWalkGraph;
import sensor_network.centralized.algo1.graph.QEdgeForest;
import sensor_network.centralized.algo1.graph.SensorNetworkGraph;
import sensor_network.centralized.algo1.node.Axis;
import sensor_network.centralized.algo1.node.Edge;

public class Util {
	
	public static double distance(Map<Integer, Axis> nodes, int node1, int node2) {
		double distance = 0;
		Axis axis1 = nodes.get(node1);
		Axis axis2 = nodes.get(node2);
		
		if (axis1 != null && axis2 != null) {
			double xAxis1 = axis1.getxAxis();
			double yAxis1 = axis1.getyAxis();
				
			double xAxis2 = axis2.getxAxis();
			double yAxis2 = axis2.getyAxis();
			
			distance =  Math.sqrt(((xAxis1-xAxis2)*(xAxis1-xAxis2)) + ((yAxis1-yAxis2)*(yAxis1-yAxis2)));
		}
		
		return distance;
	}
	
	public static boolean validateAxis(Map<Integer, Axis> nodes, double xAxis, double yAxis, int minDistance) {
										//int transmissionRange, int minDistance) {
		//boolean transmissionCheck = false;
		boolean minDistanceCheck = true;
		
		if (nodes.size() == 0) {
			//transmissionCheck = true;
			minDistanceCheck = true;
		} else {
			for(int node1: nodes.keySet()) {
				Axis axis1 = nodes.get(node1);
				double xAxis1 = axis1.getxAxis();
				double yAxis1 = axis1.getyAxis();
				
				double distance =  Math.sqrt(((xAxis1-xAxis)*(xAxis1-xAxis)) + ((yAxis1-yAxis)*(yAxis1-yAxis)));
				//if (distance <= transmissionRange) 
					//transmissionCheck = true;
				
				if (distance <= minDistance)
					minDistanceCheck = false;
			}
		}
		
		//return transmissionCheck&&minDistanceCheck;
		return minDistanceCheck;
	}
	
	public static void eliminateIsolation(int nodeCount, Map<Integer, Set<Integer>> adjacencyList) {
		Random rd = new Random();
		
		for(int node1:adjacencyList.keySet()) {
			Set<Integer> adjList = adjacencyList.get(node1);
			if (adjList.size()==0) {
				int node2 = rd.nextInt(nodeCount) + 1;
				
				adjList.add(node2);
				adjacencyList.put(node1, adjList);
				
				adjList = adjacencyList.get(node2);
				adjList.add(node1);
				adjacencyList.put(node2, adjList);
			}
		}
	}
	
	public static int checkExisting(Edge edge, Map<Integer, Set<Integer>> connectedComponents) {
		int result = 0; 	// both 2 node do not belong to any connectedComponent
		
		int indexN1 = 0;
		int indexN2 = 0;

		for (int key: connectedComponents.keySet()) {
			Set<Integer> connectedComponent = connectedComponents.get(key);

			for (int n: connectedComponent) {
				if (n == edge.getNode1()) 
					indexN1 = key;
				if (n == edge.getNode2()) 
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
				connectedComponents.get(indexN2).add(edge.getNode1());
			
			if (indexN1 != 0)
				connectedComponents.get(indexN1).add(edge.getNode2());
			
			result = 1;	
		}

		return result;
	}
	
	public static void resetShortestPaths(Map<Integer, Axis> nodes) {
		for(int node: nodes.keySet()) {
			Axis axis = nodes.get(node);

			if (axis != null) {
				axis.setShortestDistance(Double.MAX_VALUE);
				axis.getShortestPath().clear();
			}
		}
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
				if (node == edge.getNode1()) {
					count++;
					aggregatorAdjList.get(node).add(edge.getNode2());
				}
				else if (node == edge.getNode2()) {
					count++;
					aggregatorAdjList.get(node).add(edge.getNode1());
				}
			}
			
			if (count == 1) {		// it's leaf node
				// put it into leaf list of current connectedComponent
				leafNodes.add(node);
			}
		}
		
		return leafNodes;
	}
	
	public static Set<Integer> getOriginalPath(int n1, int n2, ArrayList<OriginalPath> _retainedPaths) {
		Set<Integer> res = null;
		for (OriginalPath path: _retainedPaths) {
			if (path == null)
				continue;
			
			if ((path.getNode1() == n1) && (path.getNode2() == n2)) {
				res = path.getOriginalPath();
				break;
			}
			
			if ((path.getNode1() == n2) && (path.getNode2() == n1)) {
				res = path.getOriginalPath();
				// convert to ArrayList
		        ArrayList<Integer> tmpArrayList = new ArrayList<>(res);
				Collections.reverse(tmpArrayList);
				res = new LinkedHashSet<Integer>(tmpArrayList);
				break;
			}
		}
		return res;
	}
	
	public static void drawSensorNetworkGrap(int screenHeight, double width, double height, Map<Integer, Axis> nodes, Map<Integer, Set<Integer>> adjList, Map<Integer, Set<Integer>> dataAdjList) {
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
		
	public static  void drawAggregationGraph(int screenHeight, double width, double height, Map<Integer, Axis> nodes, Map<Integer, Set<Integer>> dataAdjList) {	
		//Draw aggregation graph
		AggregationGraph aggregationGraph = new AggregationGraph();
		aggregationGraph.setGraphWidth(width);
		aggregationGraph.setGraphHeight(height);
		aggregationGraph.setNodes(nodes);
		aggregationGraph.setAdjList(dataAdjList);
		aggregationGraph.setPreferredSize(new Dimension(screenHeight-30, screenHeight-30));
		Thread AgraphThread = new Thread(aggregationGraph);
		AgraphThread.start(); 
	}
	
	public static  void drawMinimumSpanningTreeGraph(int screenHeight, double width, double height, Map<Integer, Axis> nodes, Map<Integer, Set<Integer>> dataAdjList,
											  Map<Integer, Set<Integer>> connectedComponents, ArrayList<Edge> qEdges) {
		//Draw Minimum Spanning Tree graph
		QEdgeForest qEdgesGraph = new QEdgeForest();
		qEdgesGraph.setGraphWidth(width);
		qEdgesGraph.setGraphHeight(height);
		qEdgesGraph.setNodes(nodes);
		qEdgesGraph.setAdjList(dataAdjList);
		qEdgesGraph.setConnectedComponents(connectedComponents);
		qEdgesGraph.setQEdges(qEdges);
		qEdgesGraph.setPreferredSize(new Dimension(960, 800));
		qEdgesGraph.setPreferredSize(new Dimension(screenHeight-30, screenHeight-30));
		Thread BgraphThread = new Thread(qEdgesGraph);
		BgraphThread.start();
	}
	
	public static  void drawAggregationWalk(int screenHeight, double width, double height, Map<Integer, Axis> nodes, Map<Integer, List<Integer>> aggregationWalks) {
		//Draw aggregation walk
		AggregationWalkGraph aggregationWalkGraph = new AggregationWalkGraph();
		aggregationWalkGraph.setGraphWidth(width);
		aggregationWalkGraph.setGraphHeight(height);
		aggregationWalkGraph.setNodes(nodes);
		aggregationWalkGraph.setConnectedComponents(aggregationWalks);
		aggregationWalkGraph.setPreferredSize(new Dimension(960, 800));
		aggregationWalkGraph.setPreferredSize(new Dimension(screenHeight-30, screenHeight-30));
		Thread aggregationWalkThread = new Thread(aggregationWalkGraph);
		aggregationWalkThread.start();
	}
}
