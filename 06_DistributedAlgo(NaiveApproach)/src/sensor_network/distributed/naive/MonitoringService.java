package sensor_network.distributed.naive;

import java.awt.Dimension;
import java.util.List;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import sensor_network.distributed.naive.data.Axis;
import sensor_network.distributed.naive.data.Cluster;
import sensor_network.distributed.naive.data.Edge;
import sensor_network.distributed.naive.data.Node;
import sensor_network.distributed.naive.data.ShortestPath;
import sensor_network.distributed.naive.utility.Constant;
import sensor_network.distributed.naive.utility.SortbyWeight;
import sensor_network.distributed.naive.utility.Util;

public class MonitoringService extends Thread {
	public static boolean debug = false;


	public void run() {
		Node[] nodes = Network.getNodes();
		Map<Integer, Set<Integer>> dataAdjacencyMatrix = Network.getDataAdjacencyMatrix();
		
		// step 1 
		step1(nodes, dataAdjacencyMatrix);
				
		// step 2
		step2(nodes, dataAdjacencyMatrix);
		
		// step 3
		//step3(nodes);
	}
	
	/*********************** Step 1: Bellman-Ford algorithm *************************************/	
	private void step1(Node[] nodes, Map<Integer, Set<Integer>> dataAdjacencyMatrix) {
		System.out.println("step 1 (Bellman-Ford algorithm) is running ...");
		while (Network.getStep() == Constant.STEP1) {
			try {
				Util.randomSleep();
				
				// check if step 1 finished 
				boolean step1Finished = true;
				for (int id = 0; id < nodes.length; id++) {
					if (Network.hasMessage(id)) {
						step1Finished = false;
						break;
					}
				}
				
				if (step1Finished) {
					// supplement data adjacency matrix
					for (int node1 = 0; node1 < nodes.length; node1++) {
						if (!nodes[node1].axis.isDataNode()) 
							continue;
						
						Map<Integer, ShortestPath> routingTable = nodes[node1].getRoutingTable();
						for (int node2: routingTable.keySet()) {
							if (!nodes[node2].axis.isDataNode()) 
								continue;
							
							ShortestPath shortestPath = routingTable.get(node2);
							Set<Integer> path = shortestPath.getPath();
							int dataNo = 0;
							int nodeNo = 0;
							for(int v: path) {
								nodeNo++;
								Axis axis = nodes[v].axis;
								if (axis.isDataNode()) {
									dataNo++;
								}
							}
							
							if (nodeNo > 1) {   // not neighbor node of node1
								if (dataNo == 1) {	// there is no other data node --> make connection
									Set<Integer> tempList = dataAdjacencyMatrix.get(node1);
									tempList.add(node2);
									dataAdjacencyMatrix.put(node1, tempList);
										
									tempList = dataAdjacencyMatrix.get(node2);
									tempList.add(node1);
									dataAdjacencyMatrix.put(node2, tempList);																	
								}
							}
						}
					}
					
					/* draw aggregation graph */
					if (Constant.DRAW_AGGREGATION_GRAPH) {
						Map<Integer, Axis> sensorNodes = new LinkedHashMap<Integer, Axis>();
						for(int i = 0; i < nodes.length; i++) {
							sensorNodes.put(i, nodes[i].axis);	
						}
						Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
						int screenHeight = (int) Math.round(screenSize.getHeight());
						Util.drawAggregationGraph(screenHeight, Network.getGraph().getWidth(), Network.getGraph().getHeight(), 
													sensorNodes, dataAdjacencyMatrix);
					}
					
					// set neighbors for data nodes in aggregation graph -> this is for step 2
					for (int i = 0; i < nodes.length; i++) {
						if (!nodes[i].axis.isDataNode()) 
							continue;
						
						int[] dataNeighbours = Util.getNeighbours(i, dataAdjacencyMatrix);
						nodes[i].setDataNeighbours(dataNeighbours);
					}
					
					Network.setStep(Constant.STEP2);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		/*		
		// this is for debug, print out all distances
		ArrayList<Edge> edges = new ArrayList<Edge>();
		for (int node: dataAdjacencyMatrix.keySet()) {			
			if((dataAdjacencyMatrix.get(node) != null) && (!dataAdjacencyMatrix.get(node).isEmpty())) {
				for (int adj: dataAdjacencyMatrix.get(node)) {
					if (!Util.checkExistingInForest(node, adj, edges)) {
						edges.add(new Edge(node, adj));
					}
				}
			}
	    }
		Collections.sort(edges);
		int index = 1;
		for (Edge e: edges) {
			System.out.println("Edge[" + index + "], node: "+ e.id1 + "-" + e.id2 +", distance: "+ Math.floor(e.weight * 100) / 100 + " m");
			index += 1;
		}*/
		
		System.out.println("step 1 finished");
		System.out.println("  Result after step 1");
		System.out.println("    number of overhead messages: "+ Network.step1overheadMsgCount);
		Network.step1Cost = Math.floor(Network.step1Cost * 1000000) / 1000000;  // Round a double to 6 significant figures after decimal point
		System.out.println("    Cost of overhead messages: "+ Network.step1Cost + " J");
		System.out.println();		
	}
	
	
	/*********************** Step 2: GHS algorithm *************************************/
	private void step2(Node[] nodes, Map<Integer, Set<Integer>> dataAdjacencyMatrix) {
		System.out.println("step 2 (GHS algorithm) is running ...");
		
		try {
			Util.sleep(2);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		// specify who is leader
		int leaderId = 0;
		for (int n = 0; n < nodes.length; n++) {
			if (nodes[n].axis.isDataNode()) {
				leaderId = n;
				break;
			}
		}
		
		/* leader does its duty */
		// all nodes update leader their incident edges
		Node receiver = nodes[leaderId];
		Map<Integer, ShortestPath> routingTable = receiver.getRoutingTable();
		for (int senderID = 0; senderID < nodes.length; senderID++) {
			if (senderID == leaderId)
				continue;
			
			ShortestPath shortestPathObj = routingTable.get(senderID);
			if (shortestPathObj != null) {
				Set<Integer> path = shortestPathObj.getPath();
				Iterator<Integer> ite = path.iterator();
				int n1 = senderID; int n2 = senderID;
				while (ite.hasNext()) {
					Integer n = ite.next();
					if (n1 == n2) {
						n2 = n;
					} else {
						n1 = n2;
						n2 = n;
					}
					
					// calculate transmission and receiving energy consumption from n1 to n2
					double cost = Util.calculateCost20B(n1, n2);
					if (cost != 0) {				
						Network.step2Cost += cost;
						Network.step2overheadMsgCount += 1;
					}
				}
				
				// calculate transmission and receiving energy consumption from n2 to receiver
				double cost = Util.calculateCost20B(n2, leaderId);
				if (cost != 0) {
					Network.step2Cost += cost;
					Network.step2overheadMsgCount += 1;
				}
			}
		}
		
		// leader computes itself by apply Algorithm 1
		int qNo = Network.getGraph().getNumberq();	
		algorithm1(nodes, dataAdjacencyMatrix, qNo);
		
		// leader notify the entire network
		double tmp = 0;
		for(int i = 0; i < nodes.length; i++) {
			if (!nodes[i].axis.isDataNode()) 
				continue;
			
			Node nodei = nodes[i];
			tmp = Util.calculateCost1000B(i, nodei.furthestBuddy);
			
			//System.out.println("----- Node " + i + " notify node "+ nodei.furthestBuddy);
			
			Network.step2Cost += tmp;						
			Network.step2overheadMsgCount += 1;
			Network.step2Cost1 += tmp;			
			Network.step2overheadMsgCount1 += 1;
		}
		
		
		// get solution
		List<Edge> edges =  Network.spanningEdges;
		if (edges.size() < qNo)
		{
			System.out.println("step 2 finished");
			System.out.println("  Result after step 2");
			System.out.println("    We could find only " + edges.size() + " edges. They are not enough to aggregate");
			return;	
		} else {
			System.out.println("step 2 finished");
			System.out.println("  Result after step 2");
			//System.out.println("    Leader Id: "+ leaderId);
			//System.out.println("    number of overhead messages (leader notify): "+ Network.step2overheadMsgCount1);
			System.out.println("    number of overhead messages: "+ Network.step2overheadMsgCount);
			
			Network.step2Cost = Math.floor(Network.step2Cost * 1000000) / 1000000;
			System.out.println("    Cost of overhead messages: "+ Network.step2Cost + " J"); 
			Network.step2Cost1 = Math.floor(Network.step2Cost1 * 1000000) / 1000000;
			//System.out.println("    Cost of overhead messages (leader notify): "+ Network.step2Cost1 + " J"); 
			System.out.println("");
		}
		
		try {
			Util.sleep(2);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		//Network.setStep(Constant.STEP3); 
	}	

	// Algorithm 1
	void algorithm1(Node[] nodes, Map<Integer, Set<Integer>> dataAdjList, int q) {
		System.out.println("algorithm1() function start");
		
		Map<Integer, Set<Integer>> connectedComponents = new LinkedHashMap<Integer, Set<Integer>>();
		int maxKey = 0;
		
		// create all edges between every 2 nodes, and store them in edges 
		ArrayList<Edge> edges = new ArrayList<Edge>();
		for  (int n1: dataAdjList.keySet()) {
			Set<Integer> list = dataAdjList.get(n1);
			for (int n2: list) {
				double d = Util.calculateDistance(n1, n2);
				
				if (!Util.checkContain(edges, n1, n2)) {
					Edge e = new Edge(n1, n2, d);
					
					edges.add(e);
				}				
			}
		}
		
		// sort all the edges in E in non-descending order of their weights
		Collections.sort(edges, new Comparator<Edge>() {
			public int compare(Edge e1, Edge e2){
                // Write your logic here.
				if (e1.getDistance() < e2.getDistance()) 
		        	return -1;
		        else if (e1.getDistance() == e2.getDistance())      
		        	return 0;
		        else
		        	return 1;
          }
		});
		
		if (edges.size() < q) {
			System.out.println("Error:  edges.size()="+ edges.size() + ", q="+ q +" ->  Number of edges is not enough to compute q-edge forest");
			return;
		}
		
		System.out.println("edges.size()="+ edges.size());
		
		// go through each item of edges
		int k = 0;
		int prevK = 0;
		while (k < q) {
			prevK = k;
			Iterator<Edge> ite = edges.iterator();
			while (ite.hasNext()) {
				Edge edge = ite.next();
				int n1 = edge.id1;
				int n2 = edge.id2;
				
				// check if this edge has been fetched already
				if (Network.spanningEdges.contains(edge))
					continue;
				
				// check if 2 nodes this edge belong to any existing connected components
				int res= Util.checkExisting(edge, connectedComponents);
				
				// both 2 nodes of this edge already belong to an existing connected components
				if (res == 3) {
					// try with another edge
					continue;
				}					
				
				// both 2 nodes of this edge already belong to fetched connected components
				//   but they belong to different connected components
				else if (res == 2) {
					Network.spanningEdges.add(edge);
					k++;
					break;
				}	
				
				// Only 1 node of this edge belong to fetched connected components
				else if (res == 1) {
					Network.spanningEdges.add(edge);
					k++;
					break;
				}
				
				// Both 2 nodes of this edge do not belong to any fetched connected components
				else {
					// create new connectedComponent 
					Set<Integer> connectedComponent = new LinkedHashSet<Integer>();
					
					// add edge into connectedComponent
					connectedComponent.add(n1);
					connectedComponent.add(n2);
					maxKey++;
					connectedComponents.put(maxKey, connectedComponent);
					
					Network.spanningEdges.add(edge);
					k++;
					break;
				}
			}		
			
			if (prevK == k)
				break;
		}
		
		/* apply LP-Walk for each connected component to know starting node and aggregation walk */
		System.out.println("\nResult after applying algorithm: ");
		
		if (prevK == k) {
			System.out.println("    We could find only " + k + " edges. They are not enough to aggregate");
			return;
		}
			
		
		Map<Integer, Set<Integer>> aggregatorAdjList = new LinkedHashMap<Integer, Set<Integer>> ();
		ArrayList<Integer> leafNodes = null;
		int ccIndex = 0;
		double ccSumCost = 0;
		double aggregationSumCost = 0;
		int numberOfWalk = 0;
		for (int key: connectedComponents.keySet()) {
			Set<Integer> connectedComponent = connectedComponents.get(key);
			
			// get leaf list of current connectedComponent
			leafNodes = Util.findAllLeafNodes(connectedComponent, aggregatorAdjList, Network.spanningEdges);
						
			double longestDistance = 0;
			int longestPathStartPoint = -1;
			int longestPathEndPoint = 0;
			
			// consider all leaf nodes		
			for (int n1: leafNodes) {
				// find all shortest paths from n1 to all the other nodes in current connected component
				getShortestPaths(n1, aggregatorAdjList);
				
				// among found shortest paths, choose the longest one, keep it in longestDistance variable
				for (int n2: leafNodes) {
					if (n2 != n1) {
						Axis axis2 = nodes[n2].axis;
						if (axis2 == null) 
							continue;
						
						double distance = axis2.getShortestDistance();
						if (distance > longestDistance) {
							longestDistance = distance;
							longestPathStartPoint = n1;
							longestPathEndPoint = n2;
						}
					}
				}
				
				// erase all shortest paths from n1 to all the other nodes in connectedComponent
				//Util.resetShortestPaths(this.nodes);
			}
			
			if (longestPathStartPoint != 0) {
				// from the longestPathStartPoint in longest shortest path, apply DFS to find aggregation walk
				List<Integer> dFSWalk = new ArrayList<Integer>();
				execDFS(longestPathStartPoint, dFSWalk, aggregatorAdjList, connectedComponent.size());
				
				ccIndex += 1;
				ccSumCost = 0;
				System.out.println("  connected component["+ ccIndex +"], Starting Point = " +(longestPathStartPoint) + " ----- ");
				
				
				// reflect found path back to original sensor network graph
				List<Integer> aggregationWalk = new ArrayList<Integer>();
				Iterator<Integer> itedFSWalk = dFSWalk.iterator();
				int n1 = -1, n2 = -1;
				while (itedFSWalk.hasNext()) {
					int n = itedFSWalk.next();
					if (n1 == n2) {
						n2 = n;
						aggregationWalk.add(n2);
					} else {
						n1 = n2;
						n2 = n;
						
						// bring n1, n2 back to original sensor network graph
						//  by getting the shortest path from n1 to n2 in original sensor network graph (going through storage nodes)
						Node node2 = nodes[n2];
						Map<Integer, ShortestPath> routingTable = node2.getRoutingTable();
						ShortestPath shortestPathObj = routingTable.get(n1);
						if (shortestPathObj != null) {
							Set<Integer> p = shortestPathObj.getPath();
							
							// get shortest path to n2 in original sensor network graph (going through storage nodes)
							if (p == null) {   // shortest path between n1 and n2 has other data nodes
								aggregationWalk.add(n2);
							}
							else {   // shortest path between n1 and n2 does not have any other data nodes
								for (int i: p) {
									if (i != n1) {
										aggregationWalk.add(i);		
									}
								}			
								aggregationWalk.add(n2);
							}
						}												
					}
				}
				
				int ccWalk = 0;
				System.out.println("    Aggregation Walk:  ");
				Iterator<Integer> iteAggWalk = aggregationWalk.iterator();
				n1 = -1; n2 = -1;
				int i = 1;
				while (iteAggWalk.hasNext()) {
					int n = iteAggWalk.next();
					if (n1 == n2) {
						n2 = n;
					} else {
						n1 = n2;
						n2 = n;
					}
					
					// calculate distance between nodes n1 and n2
					double distance = Util.calculateDistance(n1, n2);
					distance = Math.floor(distance *100) / 100;
					if (distance == 0)
						continue;
					
					// calculate transmission energy consumption
					//double transmissionCost = (1.6 + 1.6*distance*distance/1000)/100000;
					double transmissionCost = 512* (0.8 + 8*distance*distance/10000);
					
					// calculate receiving energy consumption
					//double receivingCost = 0.000016;
					double receivingCost = 512*0.8;
					
					// total cost
					double cost = transmissionCost + receivingCost;
					
					if (cost > 0) {
						ccSumCost += cost;
						System.out.println("       edge[" + i + "], from node " + (n1) + " to " + (n2) +
								", has distance: " + distance + " m, and energy consumption: " + cost + " J");
						i += 1;
						ccWalk += 1;
					}
				}
				
				System.out.println("    Cost of Aggregation Walk in this connected component: " + ccSumCost + " J");
				System.out.println();
				aggregationSumCost += ccSumCost;
				numberOfWalk += ccWalk;								
			}
		}
		aggregationSumCost = Math.floor(aggregationSumCost * 1000000) / 1000000;
		System.out.println("Number of Messages: "+ numberOfWalk);
		System.out.println("Total Cost of Aggregation: "+ aggregationSumCost + " J");
		
		System.out.println("algorithm1() function end");
	}	
		
	
	private int execDFS(int n1, List<Integer> path , Map<Integer, Set<Integer>> dataAdjList , int visitNo ) {
		if (visitNo < 0)
			return visitNo;
		
		Node[] nodes = Network.getNodes();
		Axis axis1 = nodes[n1].axis;
		if (axis1!= null && !axis1.isVisited()) {
			axis1.setVisited(true);
			visitNo--;
		}
		
		Set<Integer> n1AdjList = dataAdjList.get(n1);
		
		List<Integer> numbersList = new ArrayList<Integer>(n1AdjList) ;        //set -> list
		//Sort the list
		Collections.sort(numbersList, new SortbyWeight(n1));
		n1AdjList = new LinkedHashSet<>(numbersList);
		
		if (n1AdjList != null) {
			for (int n2: n1AdjList) {
				Axis axis2 = nodes[n2].axis;
				if (axis2!= null && !axis2.isVisited()) {
					path.add(n1);
					axis2.setVisited(true);
					visitNo--;
					
					visitNo = execDFS(n2, path, dataAdjList, visitNo);
				} 
			}
		}
		
		if (visitNo >= 0) {
			path.add(n1);
			if (visitNo == 0)
				visitNo--;
		}
		
		return visitNo;
	}
	
	// find shortest paths from node1 to all the other nodes 
	void getShortestPaths(int node1, Map<Integer, Set<Integer>> adjList) {
		Node[] nodes = Network.getNodes();
		
		// init shortest paths 
		for(int i=0; i<nodes.length; i++) {
			Axis axis = nodes[i].axis;

			if (axis != null) {
				axis.setShortestDistance(Double.MAX_VALUE);
				axis.getShortestPath().clear();
			}
		}
		
		Axis axis1 = nodes[node1].axis;
		if (axis1 != null) {
			axis1.setShortestDistance(0);
			axis1.getShortestPath().add(node1);
			
			ArrayList<Edge> visited = new ArrayList<Edge> ();
			execDjikstraAlgo(node1, adjList, visited);
		}
	}
	
	private void execDjikstraAlgo(int node1, Map<Integer, Set<Integer>> adjList, ArrayList<Edge> _visited) {
		Axis axis1 = Network.getNodes()[node1].axis;
		if (axis1 == null)
			return;
		
		Set<Integer> shortestPathtoNode1 = axis1.getShortestPath();
					
		Set<Integer> list = adjList.get(node1);
		if (list != null) {
			for(int v: list) {			
				Axis axisV = Network.getNodes()[v].axis;
				
				if (axisV == null)
					continue;
				
				double currentDistance = axisV.getShortestDistance();
				if (currentDistance == 0)
					continue;
					
				double newCalculatedDistance = axis1.getShortestDistance() + Network.getWeight(node1, v);
				if (newCalculatedDistance < currentDistance) {
					axisV.setShortestDistance(newCalculatedDistance);
					
					Set<Integer> newShortestPath = axisV.getShortestPath();
					newShortestPath.clear();
					newShortestPath.addAll(shortestPathtoNode1);
					newShortestPath.add(v);
					axisV.setShortestPath(newShortestPath);
					
					// remove all edges visited list that start with v
					if (currentDistance != Double.MAX_VALUE) {
						Iterator<Edge> it= _visited.iterator();
						while(it.hasNext()){
							Edge edge = (Edge)it.next();
							if (edge.id1 == v) {
								it.remove();
							}
						}
					}
				}
				
				boolean contain = false;
				for (Edge e: _visited) {
					if (e.id1 == node1 && e.id2 == v) {
						contain = true;
						break;
					}
				}
				//if (!_visited.contains(edge)) {
				if (!contain) {
					Edge edge = new Edge(node1, v, 0);
					_visited.add(edge);
					
					execDjikstraAlgo(v, adjList, _visited);
				}
			}
		}
	}
}
