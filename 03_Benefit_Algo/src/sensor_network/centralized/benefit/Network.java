package sensor_network.centralized.benefit;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

import sensor_network.centralized.benefit.graph.AggregationGraph;
import sensor_network.centralized.benefit.graph.QEdgeForest;
import sensor_network.centralized.benefit.graph.SensorNetworkGraph;
import sensor_network.centralized.benefit.node.Axis;
import sensor_network.centralized.benefit.node.Edge;
import sensor_network.centralized.benefit.utility.ConnectedComponent;
import sensor_network.centralized.benefit.utility.Constant;
import sensor_network.centralized.benefit.utility.OriginalPath;
import sensor_network.centralized.benefit.utility.SortbyWeight;
import sensor_network.centralized.benefit.utility.Util;
import sensor_network.graph.storing.Graph;

public class Network {

	private static Graph graph = null;
	private static int rCapNumberTotal;
	private static int rNumberTotal;
	private static int mNumberTotal;
	
	private static Map<Integer, Axis> nodes = new LinkedHashMap<Integer, Axis>();
	private static Map<Integer, Axis> dataNodes = new LinkedHashMap<Integer, Axis>();
	
	// path between 2 data nodes which does not go through any data node
	private static ArrayList<OriginalPath> originalPaths = new ArrayList<OriginalPath> ();
	
	private static Map<Integer, Set<Integer>> adjacencyMatrix = new LinkedHashMap<Integer, Set<Integer>> ();
	private static Map<Integer, Set<Integer>> dataAdjacencyMatrix = new LinkedHashMap<Integer, Set<Integer>> ();
	
	//private static List<Cluster> clusters;
	
	private static int quotaPrize = 0;
	private static List<Edge> selectedEdges = new ArrayList<Edge>();
	//private static Set<Integer> connectedComponents = new LinkedHashSet<Integer>();
	private static Map<Integer, ConnectedComponent> connectedComponents = new LinkedHashMap<Integer, ConnectedComponent>();
	private static Map<Integer, List<Integer>> aggregationWalks = new LinkedHashMap<Integer, List<Integer>>();
	
	
	public static void initialise(String nodesPath, String fileName ) {
		/* read graph from centralized algorithm */
	    try
	    {
	         FileInputStream fis = new FileInputStream(nodesPath);
	         ObjectInputStream ois = new ObjectInputStream(fis);
	         graph = (Graph) ois.readObject();
	         
	         quotaPrize = graph.getNumberR() - graph.getNumberm();
	         quotaPrize = 38414 - 24500;
	         
	 		 System.out.println("number of nodes: " + graph.getNodesNumber());
	 		 System.out.println("number of data nodes: " + graph.getDataNodesNumber());
//	 		 System.out.println("Total R number: " + graph.getNumberR());
//	 		 System.out.println("Total m number: " + graph.getNumberm());	 		 
//	 		 System.out.println("Total r number: " + graph.getNumberr());
	 		 //System.out.println("q number: " + graph.getNumberq());
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
	    Util.populateNodes(graph, graph.getWidth(), graph.getHeight(), nodes, dataNodes);
	    
	    // debug nodes' information
	    /*
	    System.out.println("Nodes' information of given graph");
	    for(int node: nodes.keySet()) {
			Axis axis = nodes.get(node);

			if (axis != null) {
				int rCapNumber = axis.getRCapNumber();
				int rNumber = axis.getRNumber();
				
				if (rCapNumber == 0 && rNumber == 0)
					System.out.print("  node[" + node + "] is storage node"); 
				else
					System.out.print("  node[" + node + "] is data node with R = "+ rCapNumber +"; r = "+ rNumber);
				
				System.out.println();
			}
		}*/
	    
	    
	    Util.populateAdjacencyMatrix(graph, adjacencyMatrix, dataAdjacencyMatrix);	    
	    generateAggregationGraph(adjacencyMatrix, dataAdjacencyMatrix);
		
	    // get network capacity	    
	    loadNodeCapacity(fileName, graph.getNodesNumber(), graph.getDataNodesNumber());
	    
	    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int screenHeight = (int) Math.round(screenSize.getHeight());
		
	    // draw sensor network graph
	    if (Constant.DRAW_SENSOR_NETWORK_GRAPH)
			drawSensorNetworkGrap(screenHeight, graph.getWidth(), graph.getHeight(), nodes, adjacencyMatrix, dataAdjacencyMatrix);
	    
	    // draw Aggregation graph
	    if (Constant.DRAW_AGGREGATION_GRAPH)
	    	drawAggregationGraph(screenHeight, graph.getWidth(), graph.getHeight(), nodes, dataAdjacencyMatrix);
	}
	
	private static boolean loadNodeCapacity(String fileName, int numberOfNodes, int numberOfDataNodes) {
		try
	    {
			ArrayList<Integer> rCapNumberList = new ArrayList<>();
			ArrayList<Integer> rNumberList = new ArrayList<>();
			
			File fi = new File(fileName);
			FileReader fr=new FileReader(fi);   //reads the file  
			BufferedReader br=new BufferedReader(fr);  //creates a buffering character input stream  
			
			int dataNodeCount = 0;
			int count = 0;
			
			String firstLine = null;
			String secondLine = null;
			String line = null;
			while((line=br.readLine())!=null)  
			{  
				//sb.append(line);      //appends line to string buffer  
				//sb.append("\n");
				if (firstLine == null) {
					firstLine = line;
					dataNodeCount = Integer.valueOf(firstLine.trim());
				} else if (secondLine == null) {
					secondLine = line;
					mNumberTotal = Integer.valueOf(secondLine.trim());
				} else {
					List<String> values = getTokensWithCollection(line.trim());
					rCapNumberList.add(Integer.valueOf(values.get(0)));
					rNumberList.add(Integer.valueOf(values.get(1)));
					
					count += 1;
				}
			}
			
			fr.close();
			
			if (count != dataNodeCount || dataNodeCount != numberOfDataNodes) {
				System.out.println("Finish loading data from file, but number of read lines is not the number of nodes");
				return false;
			}
			
			int i = 0;
			int lineIndex = 0;
			while (i < numberOfNodes) {
				Axis axis = nodes.get(i);
				
				if (axis.isDataNode()) {
					// random R 
					int rCapNumber = rCapNumberList.get(lineIndex);
					rCapNumberTotal += rCapNumber;
					axis.setRCapNumber(rCapNumber);
					
					// compute r 
					int rNumber = rNumberList.get(lineIndex);
					rNumberTotal += rNumber;
					axis.setRNumber(rNumber);
					
					lineIndex += 1;
				}
				
				i++;
			}
			
			if (rCapNumberTotal <= rNumberTotal) {
				System.out.println("Finish loading data from file, but data is inconsistent. (rCapNumberTotal <= rNumberTotal)");
		        return false;
			}
			
			if (rNumberTotal > mNumberTotal) {
				System.out.println("Finish loading data from file, but it's impossible to have enough storage capacity after aggregation. (rNumberTotal > mNumberTotal)");
		        return false;
			}
	    } catch(IOException ioe) {
	        ioe.printStackTrace();
	        System.out.println("IOException occured while loading data from file");
	        return false;
	    } catch(NumberFormatException nfe) {
	    	nfe.printStackTrace();
	    	System.out.println("NumberFormatException occured while loading data from file");
	    	return false;
	    }
		
		System.out.println("Total R number: " + rCapNumberTotal + " MB");
		System.out.println("Total r number: " + rNumberTotal + " MB");
		System.out.println("Total m number: " + mNumberTotal + " MB");
		int i = 0;
		while (i < numberOfNodes) {
			Axis axis = nodes.get(i);
			if (axis.isDataNode()) {
				System.out.println("node["+ i +"], R:"+axis.getRCapNumber() + ", r: "+axis.getRNumber());
			}
			i++;
		}
		
		return true;
	}

	public static void algorithm3() {
		// get all edges of given aggregation graph
		ArrayList<Edge> edges = new ArrayList<Edge>();
		for  (int n1: dataAdjacencyMatrix.keySet()) {
			Set<Integer> list = dataAdjacencyMatrix.get(n1);
			for (int n2: list) {
				if (!edges.contains(new Edge(n2, n1, 0))) {
					Edge e = new Edge(n1, n2, Util.distance(nodes, n1, n2));
				
					edges.add(e);
				}
			}
		}
		
		if (edges.size() == 0) 
			return;
		
		System.out.println();
//		System.out.println("Edges' information of given graph");
//		for (int i=0; i<edges.size(); i++) {
//			System.out.println("  Edge["+ i +"] from node " + edges.get(i).getNode1() +" to node "+ edges.get(i).getNode2()+" has distance = "+edges.get(i).getDistance() + ", ratio= "+edges.get(i).getTmpRatio());
//			
//		}
			
		System.out.println();
		System.out.println("algorithm3() function starts");
		System.out.println("  Expected aggregated data size: " + quotaPrize +  " MB");
		
		// get tmp selected node for edges
		for (int i= 0; i<edges.size(); i++) {
			Edge e = edges.get(i);
			
			Axis axis1 = nodes.get(e.getNode1());
			Axis axis2 = nodes.get(e.getNode2());
			Util.findTmpSelectedNode(axis1, axis2, e);
		}
					
		// sort edge by prize/length
		Collections.sort(edges, Collections.reverseOrder(new Comparator<Edge>() {
			public int compare(Edge e1, Edge e2){
                // Write your logic here.
				if (e1.getTmpRatio() < e2.getTmpRatio()) 
		        	return -1;
		        else if (e1.getTmpRatio() == e2.getTmpRatio())      
		        	return 0;
		        else
		        	return 1;
			}
		}));
		
		System.out.println();
		System.out.println("Edges' information of given graph (After sort) ---- ");
		for (int i=0; i<edges.size(); i++) {
			System.out.println("  Edge["+ i +"] from node " + edges.get(i).getNode1() +" to node "+ edges.get(i).getNode2()+" has distance = "+edges.get(i).getDistance() + ", ratio= "+edges.get(i).getTmpRatio());
		}
		
		int maxKey = 0;
		// while not collect enough prize
		int collectedPrize = 0;
		boolean flg = true;
		//while (flg) {
			// get edge with max value
			int edgeCount = 0;
			Iterator<Edge> ite = edges.iterator();
			while (ite.hasNext()) {
				Edge e = ite.next();
				edgeCount += 1;
				
				// skip edges being added already
				if (e.isSelected()) {
					// try with another edge
					continue;
				}
				
				/* skip edges making cycle */ 
				// check if 2 nodes this edge belong to any existing connected components
				int res= Util.checkExisting(e, connectedComponents);
				// both 2 nodes of this edge already belong to an existing connected components
				if (res == 3) {
					// try with another edge
					continue;
				} 
				
				// Both 2 nodes of this edge do not belong to any fetched connected components
				else if (res == 0) {
					// create new connectedComponent 
					ConnectedComponent connectedComponent = new ConnectedComponent();
					//Set<Integer> connectedComponent = new LinkedHashSet<Integer>();
					
					// add edge into connectedComponent
					connectedComponent.nodes.add(e.getNode1());
					connectedComponent.nodes.add(e.getNode2());
					
					// choose initiator
					Axis axis1 = nodes.get(e.getNode1());
					Axis axis2 = nodes.get(e.getNode2());
					
					int prize1 = axis1.getRCapNumber()-axis1.getRNumber();
					int prize2 = axis2.getRCapNumber()-axis2.getRNumber();
					if (prize1 <= prize2) {
						connectedComponent.initiatorId = e.getNode1();
						connectedComponent.initiator = axis1;
						
						//System.out.println("; collected prize: " + prize2 + " MB");
						collectedPrize += prize2;
						//axis2.selected = true;
					} else {
						connectedComponent.initiatorId = e.getNode2();
						connectedComponent.initiator = axis2;
						
						//System.out.println("; collected prize: " + prize1 + " MB");
						collectedPrize += prize1;
						//axis1.selected = true;
					}
					
					maxKey++;
					connectedComponents.put(maxKey, connectedComponent);
					
					axis1.componentId = maxKey;
					axis2.componentId = maxKey;
				}
				
				else if (res == 2) {  // 2 node belong to 2 different connectedComponents
					Axis axis1 = nodes.get(e.getNode1());
					Axis axis2 = nodes.get(e.getNode2());
					
					if (axis1 == null || axis2 == null)
						continue;
					
					Axis initiator1 = null;
					Axis initiator2 = null;
					ConnectedComponent component = connectedComponents.get(axis1.componentId);
					if (component == null)
						System.out.println("Component is Null");
					else
						initiator1 = component.initiator;
					
					component = connectedComponents.get(axis2.componentId);
					if (component == null)
						System.out.println("Component is Null");
					else
						initiator2 = component.initiator;
					
					// compare prizes of 2 initiators
					int prize1 = initiator1.getRCapNumber() - initiator1.getRNumber();
					int prize2 = initiator2.getRCapNumber() - initiator2.getRNumber();
					if (prize1 > prize2) {
						connectedComponents.get(axis1.componentId).initiator = initiator2;
						connectedComponents.get(axis1.componentId).initiatorId = initiator2.getId();
						
						//System.out.println("; collected prize: " + prize1 + " MB");
						collectedPrize += prize1;
						//initiator1.selected = true;
					} else {
						connectedComponents.get(axis2.componentId).initiator = initiator1;
						connectedComponents.get(axis2.componentId).initiatorId = initiator1.getId();
						
						//System.out.println("; collected prize: " + prize2 + " MB");
						collectedPrize += prize2;
						//initiator2.selected = true;
					}
					
					// combine 2 components
					Set<Integer> connectedComponent1 = connectedComponents.get(axis1.componentId).nodes;	
					Set<Integer> connectedComponent2 = connectedComponents.get(axis2.componentId).nodes;	
					if (axis1.componentId < axis2.componentId) {
						connectedComponent1.addAll(connectedComponent2);
						
						int tmpId = axis2.componentId;
						connectedComponent2.forEach( n -> {
							Axis ax = nodes.get(n);
							ax.componentId = axis1.componentId;
						});
						
						connectedComponents.remove(tmpId);
					} else {
						connectedComponent2.addAll(connectedComponent1);
						
						int tmpId = axis1.componentId;
						connectedComponent1.forEach( n -> {
							Axis ax = nodes.get(n);
							ax.componentId = axis2.componentId;
						});
						
						connectedComponents.remove(tmpId);
					}
				} 
				
				else if (res == 1) { // 1 node is belong to 1 connectedComponent, 1 the other node is not
					Axis axis1 = null;  // not belong to any component
					Axis axis2 = null;
					
					if (nodes.get(e.getNode1()).componentId == -1 && nodes.get(e.getNode2()).componentId != -1) { 
						axis1 = nodes.get(e.getNode1());
						axis2 = nodes.get(e.getNode2());
					} else if (nodes.get(e.getNode2()).componentId == -1 && nodes.get(e.getNode1()).componentId != -1) {
						axis1 = nodes.get(e.getNode2());
						axis2 = nodes.get(e.getNode1());
					}
					
					if (axis1 == null || axis2 == null)
						continue;
					
					axis1.componentId = axis2.componentId;
					
					int prize = axis1.getRCapNumber() - axis1.getRNumber();
					ConnectedComponent component = connectedComponents.get(axis2.componentId);
					Axis initiator = null;
					if (component == null)
						System.out.println("Component is Null");
					else
						initiator = component.initiator;
					
					int initiatorPrize = initiator.getRCapNumber() - initiator.getRNumber();
					if (initiatorPrize > prize) {
						//System.out.println("; collected prize: " + initiatorPrize + " MB");
						
						collectedPrize += initiatorPrize;
						//initiator.selected = true;
						
						connectedComponents.get(axis2.componentId).initiator = axis1;
						connectedComponents.get(axis2.componentId).initiatorId = axis1.getId();
					} else {
						//System.out.println("; collected prize: " + prize + " MB");
						
						collectedPrize += prize;
						//axis1.selected = true;
					}
				}
				
				// add to global variable selectedEdges
				if (!selectedEdges.contains(e)) {
					System.out.println("    chose edge from node " + e.getNode1() + " to node " + e.getNode2());
					
					e.setSelected(true);
					selectedEdges.add(e);
				}
				
				// check if gained enough prize
				if (collectedPrize >= quotaPrize)
					//flg = false;
					break;
				else {
					if (edgeCount == edges.size()) {
						// flg = false;
						System.out.println();
						System.out.println("  Stop choosing edges because there are no more edges to choose (The data after aggregating will be larger than the storage capacity)");
						return;
					}
				}
			}
		//}
		
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int screenHeight = (int) Math.round(screenSize.getHeight());
		drawMininumSpanningForest(screenHeight, graph.getWidth(), graph.getHeight(), connectedComponents, selectedEdges);
		
		System.out.println();
		System.out.println("  choosing edges ends with size of aggregated data is " + collectedPrize + " MB");
		System.out.println();
		System.out.println("  finding walk starts");
		
		int numberOfAggregator = 0;
		int numberOfInitiator = connectedComponents.size();
		int ccIndex = 0;
		double ccSumCost = 0;
		double aggregationSumCost = 0;
		//int numberOfWalk = 0;
		for (int key: connectedComponents.keySet()) {
			ConnectedComponent obj = connectedComponents.get(key);
			Set<Integer> connectedComponent = obj.nodes;
			
			// find adjacent map including all added edges
			Map<Integer, Set<Integer>> aggregatorAdjMap = new LinkedHashMap<Integer, Set<Integer>> ();
			//ArrayList<Integer> leafNodes = null;
			
			// get leaf list of current connectedComponent
			Util.findAllLeafNodes(connectedComponent, selectedEdges, aggregatorAdjMap);
			
			/* find initiator then apply DFS to find walks  */
			int initiatorId = obj.initiatorId;
			Axis initiator = obj.initiator;
			
			if (initiatorId == -1)
				continue;
			
			int overflowDatasize = initiator.getRCapNumber(); // - initiator.getRNumber();
			
			List<Integer> dFSWalk = new ArrayList<Integer>();
			execDFS(initiatorId, aggregatorAdjMap, connectedComponent.size(), dFSWalk);
			
			numberOfAggregator +=  (connectedComponent.size() - 1);
			
			ccIndex += 1;
			ccSumCost = 0;
			System.out.println("    connected component["+ ccIndex +"], Starting Point = " +(initiator) + " ----- ");
			
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
					Set<Integer> p = Util.getOriginalPath(n1, n2, originalPaths);
					
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
			System.out.println("      Aggregation Walk:  ");
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
				double distance = Util.distance(nodes, n1, n2);
				distance = Math.floor(distance *100) / 100;
				if (distance == 0)
					continue;
				
				// calculate transmission energy consumption
				//double transmissionCost = (1.6 + 1.6*distance*distance/1000)/100000;
				double transmissionCost = overflowDatasize*(0.8 + 8*distance*distance/10000);
				
				// calculate receiving energy consumption
				//double receivingCost = 0.000016;
				double receivingCost = overflowDatasize*0.8;
				
				// total cost
				double cost = transmissionCost + receivingCost;
				
				if (cost > 0) {
					ccSumCost += cost;
					System.out.println("         edge[" + i + "], from node " + (n1) + " to " + (n2) +
							", has distance: " + distance + " m, and energy consumption: " + cost + " J");
					i += 1;
					ccWalk += 1;
				}
			}
			
			System.out.println("      Cost of Aggregation Walk in this connected component: " + ccSumCost + " J");
			System.out.println();
			aggregationSumCost += ccSumCost;
			//numberOfWalk += ccWalk;
			
			aggregationWalks.put(ccIndex, aggregationWalk);
		}
		
		
		//System.out.println("    Number of Messages: "+ numberOfWalk);
		System.out.println("    Aggregated data size: "+ collectedPrize + " MB");
		aggregationSumCost = Math.floor(aggregationSumCost * 1000000) / 1000000;
		System.out.println("    Total Cost of Aggregation: "+ aggregationSumCost + " J");
		
		System.out.println("  finding walk ends");
		System.out.println("algorithm3() function end");
		
		System.out.println("Number of Initiator = " + numberOfInitiator);
		System.out.println("Number of Aggregator = " + numberOfAggregator);
		
		// draw p prize forest
//	    if (Constant.DRAW_MINIMUM_SPANNING_TREE_GRAPH && selectedEdges.size() > 0 && connectedComponents.size() > 0) {
//	    	Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
//			int screenHeight = (int) Math.round(screenSize.getHeight());
//			
//	    	drawPPrizeForestGraph(screenHeight, graph.getWidth(), graph.getHeight(), nodes,  
//												connectedComponents, selectedEdges);
//	    }
	}
	
	private static int execDFS(int n1, Map<Integer, Set<Integer>> dataAdjList , int visitNo, List<Integer> path) {
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
					path.add(n1); //  quay lui sau khi visit 1 node con, trc khi visit 1 node con khac
					axis2.setVisited(true);
					visitNo--;
					
					visitNo = execDFS(n2, dataAdjList, visitNo, path);
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
	
	private static void generateAggregationGraph(Map<Integer, Set<Integer>> adjMatrix, 
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
					}
				}
			}
		}
	}
	
	// find shortest paths from node1 to all the other nodes 
	private static void getShortestPaths(int node1, Map<Integer, Set<Integer>> adjList) {
		// erase all existing shortest paths 
		Util.resetShortestPaths(nodes);
		
		Axis axis1 = nodes.get(node1);
		if (axis1 != null) {
			axis1.setShortestDistance(0);
			axis1.getShortestPath().add(node1);
			
			ArrayList<Edge> visited = new ArrayList<Edge> ();
			execDjikstraAlgo(node1, adjList, visited);
		}
	}
	
	private static void execDjikstraAlgo(int node1, Map<Integer, Set<Integer>> adjList, ArrayList<Edge> _visited) {
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
					
				double newCalculatedDistance = axis1.getShortestDistance() + Util.distance(nodes, node1, v);
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
	
	private static void drawSensorNetworkGrap(int screenHeight, double width, double height, 
												Map<Integer, Axis> nodes, 
												Map<Integer, Set<Integer>> adjList, 
												Map<Integer, Set<Integer>> dataAdjList) {
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
	
	private static void drawAggregationGraph(int screenHeight, double width, double height, 
												Map<Integer, Axis> nodes, Map<Integer, Set<Integer>> dataAdjList) {
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
	
	private static void drawMininumSpanningForest(int screenHeight, double width, double height, 
			Map<Integer, ConnectedComponent> connectedComponents,
			List<Edge> qEdges) {
		//Draw Minimum Spanning Tree graph
		QEdgeForest qEdgesGraph = new QEdgeForest();
		qEdgesGraph.setGraphWidth(width);
		qEdgesGraph.setGraphHeight(height);
		qEdgesGraph.setNodes(nodes);
		//qEdgesGraph.setAdjList(dataAdjacencyMatrix);
		qEdgesGraph.setConnectedComponents(connectedComponents);
		qEdgesGraph.setQEdges(qEdges);
		qEdgesGraph.setPreferredSize(new Dimension(960, 800));
		qEdgesGraph.setPreferredSize(new Dimension(screenHeight-30, screenHeight-30));
		Thread BgraphThread = new Thread(qEdgesGraph);
		BgraphThread.start();
	}
	
//	public static  void drawPPrizeForestGraph(int screenHeight, double width, double height, 
//												Map<Integer, Axis> nodes, 
//												Map<Integer, Set<Integer>> connectedComponents, List<Edge> qEdges) {
//		//Draw Minimum Spanning Tree graph
//		QEdgeForest qEdgesGraph = new QEdgeForest();
//		qEdgesGraph.setGraphWidth(width);
//		qEdgesGraph.setGraphHeight(height);
//		qEdgesGraph.setNodes(nodes);
//		qEdgesGraph.setConnectedComponents(connectedComponents);
//		qEdgesGraph.setQEdges(qEdges);
//		qEdgesGraph.setPreferredSize(new Dimension(960, 800));
//		qEdgesGraph.setPreferredSize(new Dimension(screenHeight-30, screenHeight-30));
//		Thread BgraphThread = new Thread(qEdgesGraph);
//		BgraphThread.start();
//	}
	
	private static List<String> getTokensWithCollection(String str) {
	    return Collections.list(new StringTokenizer(str, "	")).stream()
	      .map(token -> (String) token)
	      .collect(Collectors.toList());
	}
}
