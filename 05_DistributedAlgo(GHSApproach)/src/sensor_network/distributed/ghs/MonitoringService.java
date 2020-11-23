package sensor_network.distributed.ghs;

import java.awt.Dimension;
import java.util.List;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import sensor_network.distributed.ghs.data.Axis;
import sensor_network.distributed.ghs.data.Cluster;
import sensor_network.distributed.ghs.data.Edge;
import sensor_network.distributed.ghs.data.Node;
import sensor_network.distributed.ghs.data.ShortestPath;
import sensor_network.distributed.ghs.utility.Constant;
import sensor_network.distributed.ghs.utility.Constant.STEP2_END_CONDITION;
import sensor_network.distributed.ghs.utility.SortbyWeight;
import sensor_network.distributed.ghs.utility.Util;

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
		step3(nodes);
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
			Util.sleep(1);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		// initialize minimum spanning tree graph, update it whenever there are any changes in clusters
		MinimumSpanningTree.initGraph();
		
		// check the end condition of step 2
		while (!Network.checkIfStep2Finished()) {
			networkState_debug();
			try {
				Thread.sleep(1000);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		networkState_debug();
		
		// get solution
		List<Edge> edges =  Network.getSpanningEdges();
		
		/* reduce spanning edges */
		if (!reduceSpanningTree()) {
			System.out.println("step 2 finished");
			System.out.println("  Result after step 2");
			System.out.println("    We could find only " + edges.size() + " edges. They are not enough to aggregate");
			return;
		} else {
			// reduce clusters 
			//reduceClusters();
			
			// update minimum spanning tree graph
			MinimumSpanningTree.updateGraph();
			
			System.out.println("step 2 finished");
			System.out.println("  Result after step 2");
			System.out.println("    number of overhead messages: "+ Network.step2overheadMsgCount);
			System.out.println("      . number of Connect messages: "+ Network.step2ConnectMsgCount);
			System.out.println("      . number of Initiate messages: "+ Network.step2InitiateMsgCount);
			System.out.println("      . number of Report messages: "+ Network.step2ReportMsgCound);	
			Network.step2Cost = Math.floor(Network.step2Cost * 1000000) / 1000000;
			System.out.println("    Cost of overhead messages: "+ Network.step2Cost + " J"); 
			System.out.println("");
			
			System.out.println("----------Number of edges: " + edges.size());
		}
		
		Network.setStep(Constant.STEP3); 
	}
	
	private boolean reduceSpanningTree() {
		/*if (Network.step2EndCondition == STEP2_END_CONDITION.GO_TO_END)
			return true;
		
		int qNo = Network.getGraph().getNumberq();
		List<Edge> edges =  Network.getSpanningEdges();
		if (edges.size() < qNo)
		{
			return false;
		}
		
		synchronized (edges) {
			Collections.sort(edges); 
			Iterator<Edge> ite = edges.iterator();
			int i = 0;
			while (ite.hasNext()) {
				ite.next();
				i+= 1;
				
				if (i > qNo) {
					ite.remove();
				}
			}
		}*/
		
		return true;
	}
	
	private void reduceClusters() {
		/*if (Network.step2EndCondition == STEP2_END_CONDITION.GO_TO_END)
			return;
		
		List<Edge> edges =  Network.getSpanningEdges();
		java.util.List<Cluster> clusters = Network.getClusters();
		synchronized (clusters) {
			Iterator<Cluster> iteC = clusters.iterator();
			while (iteC.hasNext()) {
				Cluster c= iteC.next();
				
				ArrayList<Node> clusterNodes = c.getNodes();
				Iterator<Node> iteN = clusterNodes.iterator();
				while (iteN.hasNext()) {
					Node n = iteN.next();
					if (!Util.checkIfInSpanningTree(edges, n.getId())) {
						n.axis.setPainted(false);
						iteN.remove();
					} else {
						n.axis.setPainted(true);
					}
				}
				
				c.setNodes(clusterNodes);
				c.color = null;
			}
			//Network.setClusters(clusters);
		}*/
	}
	
	private void networkState_debug() {
		if (debug) {
			System.out.println();
			System.out.println("Network State:");
			System.out.println("No. of Nodes: " + Network.getNodes().length);
			System.out.println("No. of Clusters: " + Network.getClusters().size());
			System.out.println("No. of Spanning Edges: " + Network.getSpanningEdges().size());
			System.out.println("Spanning Edges:");
			for (Edge e : Network.getSpanningEdges()) {
				System.out.println(e.id1 + " - " + e.id2);
			}
			System.out.println("Clusters' State:");
			for (Cluster c : Network.getClusters()) {
				System.out.println("  Cluster: " + c.getId() + ", level: " + c.getLevel());
				System.out.println("  No. of Nodes: " + c.getNodes().size());
				System.out.print("  Nodes: ");
				for (Node n : c.getNodes()) {
					System.out.print(n.getId() + ", ");
				}
				System.out.println();
				if (c.getLeader() != null)
					System.out.println("  Leader: " + c.getLeader().getId());
				else
					System.out.println("  In leader election phase");
			}
			System.out.println();
			System.out.println();
		}
	}

		
	/*********************** Step 3: find aggregation walk algorithm *************************************/
	private void step3(Node[] nodes) {
		System.out.println("Aggregation Walks");
		
		// get solution
		List<Edge> edges =  Network.getSpanningEdges();
		
		
		
		// sort all the edges in E in non-descending order of their weights
		Collections.sort(edges); 
		
		try {
			Thread.sleep(1000);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		java.util.List<Cluster> clusters = Network.getClusters();
		double aggregationSumCost = 0;
		int numberOfWalk = 0;
		
		synchronized (clusters) {
			Iterator<Cluster> iteC = clusters.iterator();
			int ccIndex = 0;
			while (iteC.hasNext()) {
				int ccWalk = 0;
				double ccSumCost = 0;
				
				Cluster c= iteC.next();
				if (c.getNodes().isEmpty())
					continue;
				
				ArrayList<Node> leafNodes = null;
				Map<Integer, Set<Integer>> aggregatorAdjList = new LinkedHashMap<Integer, Set<Integer>> ();
				double ccMinDistance = Integer.MAX_VALUE;
				double longestDistance = 0;
				int ccMinStartPoint = -1;
				int ccMinEndPoint = 0;
				List<Integer> ccMinDFSWalk = null;
				
				// get leaf list of current connectedComponent
				leafNodes = findAllLeafNodes(c.getNodes(), aggregatorAdjList, edges);
				for (Node leafNode1: leafNodes) {
					// find all shortest paths from n1 to all the other nodes in current connected component
					getShortestPaths(leafNode1.getId(), aggregatorAdjList);
					
					// among found shortest paths, choose the longest one, keep it in longestDistance variable
					for (Node n2: leafNodes) {
						if (n2.getId() != leafNode1.getId()) {
							Axis axis2 = n2.axis;
							if (axis2 == null) 
								continue;
							
							double distance = axis2.getShortestDistance();
							if (distance > longestDistance) {
								longestDistance = distance;
								ccMinStartPoint = leafNode1.getId();
								ccMinEndPoint = n2.getId();
							}
						}
					}
				} 
				
				if (ccMinStartPoint != -1) {
					// from the longestPathStartPoint in longest shortest path, apply DFS to find aggregation walk
					List<Integer> dFSWalk = new ArrayList<Integer>();
					execDFS(ccMinStartPoint, dFSWalk, aggregatorAdjList, c.getNodes().size());
					
					ccIndex += 1;
					System.out.println("  connected component["+ ccIndex +"], Starting Point = " +(ccMinStartPoint) + " ----- ");
					System.out.println("    Aggregation Walk:  ");
					
					
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
					
					
					Iterator<Integer> iteAggWalk = aggregationWalk.iterator();
					//Iterator<Integer> iteAggWalk = dFSWalk.iterator();
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
						double distance = Network.getWeight(n1, n2);
						distance = Math.floor(distance * 100) / 100;  
						if (distance == 0)
							continue;
						
						// total cost
						double cost = Util.calculateAggregationCost(n1, n2);
						//cost  = Math.floor(cost * 100000) / 100000;
						if (cost > 0) {
							ccSumCost += cost;
							System.out.println("       edge[" + i + "], from node " + n1 + " to " + n2 +
									", has distance: " + distance + " m, and energy consumption: " + cost + " J");
							i += 1;
							ccWalk += 1;
						}
					}
					
					//ccSumCost = Math.floor(ccSumCost * 1000000) / 1000000;
					numberOfWalk += ccWalk;
					//ccSumCost  = Math.floor(ccSumCost * 100000) / 100000;
					System.out.println("    Cost of Aggregation Walk in this connected component: " + ccSumCost + " J");
					System.out.println();
					aggregationSumCost += ccSumCost;
				}
			}
		}
		
		aggregationSumCost = Math.floor(aggregationSumCost * 1000000) / 1000000;
		System.out.println("  Number of Messages: "+ numberOfWalk);
		System.out.println("  Total Cost of Aggregation: "+ aggregationSumCost + " J");
		
		System.out.println("----------------- Number of edges = "+ edges.size());
	}
	
	private ArrayList<Node> findAllLeafNodes(ArrayList<Node> connectedComponent, 
											Map<Integer, Set<Integer>> aggregatorAdjList, 
											List<Edge> qEdges) {
		ArrayList<Node> leafNodes = new ArrayList<>(); 
		
		for (Node node: connectedComponent) {
			aggregatorAdjList.put(node.getId(), new HashSet<Integer>());
			
			// check if it is leaf node
			int count = 0;
			Iterator<Edge> ite = qEdges.iterator();
			while (ite.hasNext()) {
				Edge edge = ite.next();
				if (node.getId() == edge.id1) {
					count++;
					aggregatorAdjList.get(node.getId()).add(edge.id2);
				}
				else if (node.getId() == edge.id2) {
					count++;
					aggregatorAdjList.get(node.getId()).add(edge.id1);
				}
			}
			
			if (count == 1) {		// it's leaf node
				// put it into leaf list of current connectedComponent
				leafNodes.add(node);
			}
		}
		
		return leafNodes;
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
