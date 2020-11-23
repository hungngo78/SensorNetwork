package sensor_network.centralized.algo1;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import sensor_network.centralized.algo1.node.Axis;
import sensor_network.centralized.algo1.node.Edge;
import sensor_network.centralized.algo1.utillity.Constant;
import sensor_network.centralized.algo1.utillity.OriginalPath;
import sensor_network.centralized.algo1.utillity.SortbyWeight;
import sensor_network.centralized.algo1.utillity.Util;
import sensor_network.graph.storing.Graph;
import sensor_network.graph.storing.SensorNode;


public class SensorNetwork {
	private static Graph graph = null;
	 
	private Map<Integer, Axis> nodes = new LinkedHashMap<Integer, Axis>();
	private Map<Integer, Axis> dataNodes = new LinkedHashMap<Integer, Axis>();
	
	// path between 2 data nodes which does not go through any data node
	ArrayList<OriginalPath> originalPaths = new ArrayList<OriginalPath> ();
	
	// Algorithm 1
	Map<Integer, Set<Integer>> connectedComponents = new LinkedHashMap<Integer, Set<Integer>>();
	ArrayList<Edge> qEdges = new ArrayList<Edge>();
	Map<Integer, List<Integer>> aggregationWalks = new LinkedHashMap<Integer, List<Integer>>();
	
	public static void main(String[] args) {
		try
	    {
			 //String nodesPath = "C:\\02_DH\\99_LuanVanTotNghiep\\12_tmp\\01_original\\performance_report_6\\55_data_nodes\\graph.dat";
			 //String nodesPath = "C:\\02_DH\\99_LuanVanTotNghiep\\12_tmp\\performance_report_6\\55_data_nodes\\graph.dat";
			 //String nodesPath = "C:\\02_DH\\99_LuanVanTotNghiep\\12_tmp\\performance_report_2\\60_data_nodes\\graph.dat";
			 //String nodesPath = "C:\\02_DH\\99_LuanVanTotNghiep\\12_tmp\\performance_report_2\\65_data_nodes\\graph.dat";
			 //String nodesPath = "C:\\02_DH\\99_LuanVanTotNghiep\\12_tmp\\performance_report_6\\70_data_nodes\\graph.dat";
			 //String nodesPath = "C:\\02_DH\\99_LuanVanTotNghiep\\12_tmp\\performance_report_2\\71_data_nodes\\graph.dat";
			
			 //String nodesPath = 	"/Users/hungngo/data/performance_report_13/55/graph.dat";
			 String nodesPath = 	"/Users/hungngo/data/performance_report_13/60/graph.dat";
			 //String nodesPath = 	"/Users/hungngo/data/performance_report_15/65/graph.dat";
			 //String nodesPath = 	"/Users/hungngo/data/performance_report_15/70/graph.dat";
			 //String nodesPath = 	"/Users/hungngo/data/performance_report_12/71/graph.dat";
			 
			 //String nodesPath = 	"/Users/hungngo/data/performance_report_11/55/graph.dat";
			 
	         FileInputStream fis = new FileInputStream(nodesPath);
	         ObjectInputStream ois = new ObjectInputStream(fis);
	         graph = (Graph) ois.readObject();
	         
	         System.out.println("width: " + graph.getWidth() + " m");
	 		 System.out.println("height: " + graph.getWidth() + " m");
	 		 System.out.println("Transmission range: " + graph.getTransmissionRange() + " m");
	 		 System.out.println("number of nodes: " + graph.getNodesNumber());
	 		 System.out.println("number of data nodes: " + graph.getDataNodesNumber());
	 		 System.out.println("R number: " + graph.getNumberR() + " MB");
	 		 System.out.println("m number: " + graph.getNumberm() + " MB");	 		 
	 		 //System.out.println("r number: " + graph.getNumberr());
	 		 System.out.println("beta: " + graph.getBeta());
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
		
		Map<Integer, Set<Integer>> adjacencyMatrix = new LinkedHashMap<Integer, Set<Integer>> ();
		Map<Integer, Set<Integer>> dataAdjacencyMatrix = new LinkedHashMap<Integer, Set<Integer>> ();
		
		SensorNetwork sensor = new SensorNetwork();		
		int numberq = graph.getNumberq();
		//numberq = 11;
		
		sensor.populateNodes(graph, graph.getWidth(), graph.getHeight());
		sensor.populateAdjacencyMatrix(graph, adjacencyMatrix, dataAdjacencyMatrix);
		sensor.generateAggregationGraph(adjacencyMatrix, dataAdjacencyMatrix);
		
		sensor.algorithm1(dataAdjacencyMatrix, numberq);
		
		sensor.drawResults(graph.getWidth(), graph.getHeight(), adjacencyMatrix, dataAdjacencyMatrix);
	}
	
	
	private void populateNodes(Graph graph, double width, double height) {
		int N = graph.getNodesNumber();
		if (N <= 0)
			return;
		
		Map<Integer, SensorNode> savedNodes = graph.getNodes();
		for (int key: savedNodes.keySet()) {
			SensorNode sensorNode = savedNodes.get(key);
			
			Axis axis = new Axis();
			axis.setxAxis(sensorNode.getxAxis());
			axis.setyAxis(sensorNode.getyAxis());
			axis.setNodeType(sensorNode.isDataNode());
			
			nodes.put(key, axis);	
			
			if (axis.isDataNode())
				dataNodes.put(key, axis);
		}
	}
	
	private void populateAdjacencyMatrix(Graph graph, Map<Integer, Set<Integer>> _adjacencyMatrix, 
													Map<Integer, Set<Integer>> _dataAdjacencyMatrix) {
		Map<Integer, Set<Integer>> savedMatrix = graph.getAdjacencyMatrix();
		Map<Integer, Set<Integer>> savedDataMatrix = graph.getDataAdjacencyMatrix();
		
		for (int key: savedMatrix.keySet()) {
			Set<Integer> savedList = savedMatrix.get(key);
			Set<Integer> newList = new HashSet<Integer>();
			for (int v: savedList) 
				newList.add(v);
			
			_adjacencyMatrix.put(key, newList);
		}
		
		for (int key: savedDataMatrix.keySet()) {
			//if (savedDataMatrix.get(key).size() > 0) {
				Set<Integer> savedList = savedDataMatrix.get(key);
				Set<Integer> newList = new HashSet<Integer>();
				for (int v: savedList) 
					newList.add(v);
				
				_dataAdjacencyMatrix.put(key, newList);
			//}
		}
	}

	private void generateAggregationGraph(Map<Integer, Set<Integer>> adjMatrix, 
											Map<Integer, Set<Integer>> dataAdjMatrix) {
		for(int node1: dataNodes.keySet()) {
			Set<Integer> notAdjList = new LinkedHashSet<Integer>();
			Set<Integer> tempList1 = dataAdjMatrix.get(node1);
			
			for(int node2: dataNodes.keySet()) {
				if(node1 == node2) {
					continue;
				}
				
				if (!tempList1.contains(node2))
					notAdjList.add(node2);
			}
			
			/* convert sensor network graph to aggregation graph.  
			We supplement edges into data adjacency list to make a new graph including all data nodes */ 	
			// get shortest paths from node1 to all the other node 
			getShortestPaths(node1, adjMatrix);
			
			for(int node2: notAdjList) {
				// check if the shortest paths between note1 and note2 in G do not contain any other data nodes
				Axis axis2 = nodes.get(node2);
				Set<Integer> shortestPath = axis2.getShortestPath();
				
				int dataNo = 0 ;
				int nodeNo = 0;
				for(int v: shortestPath) {
					nodeNo++;
					Axis axis = nodes.get(v);
					if (axis.isDataNode()) {
						dataNo++;
					}
				}
				if (nodeNo > 2) {
					if (dataNo == 2) {
						//tempList = dataAdjMatrix.get(node1);
						//if (tempList1 != null) {
							tempList1.add(node2);
							//dataAdjMatrix.put(node1, tempList);
								
							Set<Integer> tempList2 = dataAdjMatrix.get(node2);
							//tempList = new HashSet<Integer>();
							tempList2.add(node1);
							//dataAdjMatrix.put(node2, tempList2);
							
							// store the shortest path from node1 to node2, the path does not go through any data node
							OriginalPath path = new OriginalPath(node1, node2);
							path.getOriginalPath().addAll(shortestPath);
							originalPaths.add(path);
						//}
					}
				}
			}
		}
	}
	
	void algorithm1(Map<Integer, Set<Integer>> dataAdjList, int q) {
		System.out.println("algorithm1() function start");
		
		int maxKey = 0;
		
		// create all edges between every 2 nodes, and store them in edges 
		ArrayList<Edge> edges = new ArrayList<Edge>();
		for  (int n1: dataAdjList.keySet()) {
			Set<Integer> list = dataAdjList.get(n1);
			for (int n2: list) {
				if (!edges.contains(new Edge(n2, n1, 0))) {
					Edge e = new Edge(n1, n2, Util.distance(this.nodes, n1, n2));
				
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
				int n1 = edge.getNode1();
				int n2 = edge.getNode2();
				
				// check if this edge has been fetched already
				if (qEdges.contains(edge))
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
					qEdges.add(edge);
					k++;
					break;
				}	
				
				// Only 1 node of this edge belong to fetched connected components
				else if (res == 1) {
					qEdges.add(edge);
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
					
					qEdges.add(edge);
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
			leafNodes = Util.findAllLeafNodes(connectedComponent, aggregatorAdjList, this.qEdges);
						
			double longestDistance = 0;
			int longestPathStartPoint = 0;
			int longestPathEndPoint = 0;
			
			// consider all leaf nodes		
			for (int n1: leafNodes) {
				// find all shortest paths from n1 to all the other nodes in current connected component
				getShortestPaths(n1, aggregatorAdjList);
				
				// among found shortest paths, choose the longest one, keep it in longestDistance variable
				for (int n2: leafNodes) {
					if (n2 != n1) {
						Axis axis2 = nodes.get(n2);
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
				System.out.println("  connected component["+ ccIndex +"], Starting Point = " +(longestPathStartPoint-1) + " ----- ");
				
				
				// reflect found path back to original sensor network graph
				List<Integer> aggregationWalk = new ArrayList<Integer>();
				Iterator<Integer> itedFSWalk = dFSWalk.iterator();
				int n1 = 0, n2 = 0;
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
						Set<Integer> p = Util.getOriginalPath(n1, n2, this.originalPaths);
						
						//Axis axis1 = dataNodes.get(n1);
						// get shortest path to n2 in original sensor network graph (going through storage nodes)
						//Set<Integer> p1 = axis1.getRetainPath(n2);
						if (p == null) {   // shortest path between n1 and n2 has other data nodes
							aggregationWalk.add(n2);
						}
						else {   // shortest path between n1 and n2 does not have any other data nodes
							for (int i: p) {
								if (i != n1) {
									aggregationWalk.add(i);		
								}
							}			
						}
					}
				}
				
				int ccWalk = 0;
				System.out.println("    Aggregation Walk:  ");
				Iterator<Integer> iteAggWalk = aggregationWalk.iterator();
				n1 = 0; n2 = 0;
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
					double distance = Util.distance(this.nodes, n1, n2);
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
						System.out.println("       edge[" + i + "], from node " + (n1-1) + " to " + (n2-1) +
								", has distance: " + distance + " m, and energy consumption: " + cost + " J");
						i += 1;
						ccWalk += 1;
					}
				}
				
				System.out.println("    Cost of Aggregation Walk in this connected component: " + ccSumCost + " J");
				System.out.println();
				aggregationSumCost += ccSumCost;
				numberOfWalk += ccWalk;
				
				this.aggregationWalks.put(key, aggregationWalk);
			}
		}
		aggregationSumCost = Math.floor(aggregationSumCost * 1000000) / 1000000;
		System.out.println("Number of Messages: "+ numberOfWalk);
		System.out.println("Total Cost of Aggregation: "+ aggregationSumCost + " J");
		
		System.out.println("algorithm1() function end");
	}
	
	// find shortest paths from node1 to all the other nodes 
	void getShortestPaths(int node1, Map<Integer, Set<Integer>> adjList) {
		// erase all existing shortest paths 
		Util.resetShortestPaths(this.nodes);
		
		Axis axis1 = nodes.get(node1);
		if (axis1 != null) {
			axis1.setShortestDistance(0);
			axis1.getShortestPath().add(node1);
			
			ArrayList<Edge> visited = new ArrayList<Edge> ();
			execDjikstraAlgo(node1, adjList, visited);
		}
	}
	
	private void execDjikstraAlgo(int node1, Map<Integer, Set<Integer>> adjList, ArrayList<Edge> _visited) {
		Axis axis1 = nodes.get(node1);
		if (axis1 == null)
			return;
		
		Set<Integer> shortestPathtoNode1 = axis1.getShortestPath();
					
		Set<Integer> list = adjList.get(node1);
		if (list != null) {
			for(int v: list) {			
				Axis axisV = nodes.get(v);
				
				if (axisV == null)
					continue;
				
				double currentDistance = axisV.getShortestDistance();
				if (currentDistance == 0)
					continue;
					
				double newCalculatedDistance = axis1.getShortestDistance() + Util.distance(this.nodes, node1, v);
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
							if (edge.getNode1() == v) {
								it.remove();
							}
						}
					}
				}
				
				boolean contain = false;
				for (Edge e: _visited) {
					if (e.getNode1() == node1 && e.getNode2() == v) {
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
	
	private int execDFS(int n1, List<Integer> path , Map<Integer, Set<Integer>> dataAdjList , int visitNo ) {
		if (visitNo < 0)
			return visitNo;
		
		Axis axis1 = nodes.get(n1);
		if (axis1!= null && !axis1.isVisited()) {
			axis1.setVisited(true);
			visitNo--;
		}
		
		Set<Integer> n1AdjList = dataAdjList.get(n1);
		
		List<Integer> numbersList = new ArrayList<Integer>(n1AdjList) ;        //set -> list
		//Sort the list
		Collections.sort(numbersList, new SortbyWeight(dataNodes, n1));
		n1AdjList = new LinkedHashSet<>(numbersList);
		
		if (n1AdjList != null) {
			for (int n2: n1AdjList) {
				Axis axis2 = nodes.get(n2);
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
	
	private void drawResults (double width, double height, Map<Integer, Set<Integer>> adjacencyList, Map<Integer, Set<Integer>> dataAdjacencyList) {
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		// the screen height
		int screenHeight = (int) Math.round(screenSize.getHeight());
		
		if (Constant.DRAW_SENSOR_NETWORK_GRAPH)
			Util.drawSensorNetworkGrap(screenHeight, width, height, this.nodes, adjacencyList, dataAdjacencyList);
		
		if (Constant.DRAW_AGGREGATION_GRAPH)
			Util.drawAggregationGraph(screenHeight, width, height, this.nodes, dataAdjacencyList);
		
		if (Constant.DRAW_MINIMUM_SPANNING_TREE_GRAPH && this.qEdges.size() > 0 && this.connectedComponents.size() > 0)
			Util.drawMinimumSpanningTreeGraph(screenHeight, width, height, this.nodes, dataAdjacencyList, this.connectedComponents, this.qEdges);
		
		if (Constant.DRAW_AGGREGATION_WALK && this.aggregationWalks.size() > 0 )
			Util.drawAggregationWalk(screenHeight, width, height, this.nodes, this.aggregationWalks);
	}
}
