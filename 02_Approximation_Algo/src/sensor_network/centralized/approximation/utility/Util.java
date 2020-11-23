package sensor_network.centralized.approximation.utility;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import sensor_network.centralized.approximation.node.Axis;
import sensor_network.centralized.approximation.node.Edge;
import sensor_network.graph.storing.Graph;
import sensor_network.graph.storing.SensorNode;


public class Util {

	public static void populateNodes(Graph graph, double width, double height, 
									  Map<Integer, Axis> nodes, Map<Integer, Axis> dataNodes) {
		int N = graph.getNodesNumber();
		if (N <= 0)
			return;
		
		Map<Integer, SensorNode> savedNodes = graph.getNodes();
		
		for (int key: savedNodes.keySet()) {
			SensorNode sensorNode = savedNodes.get(key);
			
			Axis axis = new Axis(key - 1);
			axis.setxAxis(sensorNode.getxAxis());
			axis.setyAxis(sensorNode.getyAxis());
			axis.setNodeType(sensorNode.isDataNode());
			axis.setRCapNumber(sensorNode.getRCapNumber());
			axis.setRNumber(sensorNode.getRNumber());
			
			nodes.put(key - 1, axis);
			
			if (axis.isDataNode())
				dataNodes.put(key - 1, axis);
		}
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
			Set<Integer> savedList = savedDataMatrix.get(key);
			Set<Integer> newList = new HashSet<Integer>();
			for (int v: savedList) 
				newList.add(v - 1);
			
			_dataAdjacencyMatrix.put(key-1, newList);
		}
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
	
	public static ArrayList<Integer> findAllLeafNodes(Set<Integer> connectedComponents, List<Edge> qEdges,
													Map<Integer, Set<Integer>> aggregatorAdjList) {
		ArrayList<Integer> leafNodes = new ArrayList<>(); 
		
		for (int node: connectedComponents) {
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
}
