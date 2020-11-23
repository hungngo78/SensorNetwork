package sensor_network.centralized.singletree;

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

import sensor_network.centralized.singletree.graph.AggregationGraph;
import sensor_network.centralized.singletree.graph.QEdgeForest;
import sensor_network.centralized.singletree.graph.SensorNetworkGraph;
import sensor_network.centralized.singletree.node.Axis;
import sensor_network.centralized.singletree.node.Cluster;
import sensor_network.centralized.singletree.node.Edge;
import sensor_network.centralized.singletree.utility.Constant;
import sensor_network.centralized.singletree.utility.OriginalPath;
import sensor_network.centralized.singletree.utility.ShortestPath;
import sensor_network.centralized.singletree.utility.SortbyWeight;
import sensor_network.centralized.singletree.utility.Util;
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
	
	private static List<Cluster> clusters;
	
	private static int quotaPrize = 0;
	
	private static int maxPrize = 0;
	private static Cluster maxPrizeCluster = null;
	
	public static void initialise(String nodesPath, String fileName ) {
		/* read graph from centralized algorithm */
	    try
	    {
	         FileInputStream fis = new FileInputStream(nodesPath);
	         ObjectInputStream ois = new ObjectInputStream(fis);
	         graph = (Graph) ois.readObject();
	         
	         //quotaPrize = graph.getNumberR() - graph.getNumberm();
	         quotaPrize = 38414 - 24500;
	         
	 		 System.out.println("number of nodes: " + graph.getNodesNumber());
	 		 System.out.println("number of data nodes: " + graph.getDataNodesNumber());
	 		 //System.out.println("R number: " + graph.getNumberR());
	 		 //System.out.println("m number: " + graph.getNumberm());	 		 
	 		 //System.out.println("r number: " + graph.getNumberr());
	 		 System.out.println();
	 		
	         ois.close();
	         fis.close();
	    } catch(IOException ioe) {
	         ioe.printStackTrace();
	         return;
	    } catch(ClassNotFoundException c) {
	         System.out.println("Class not found");
	         c.printStackTrace();
	         return;
	    }
	    
	    /* generate graph */
	    Util.populateNodes(graph, graph.getWidth(), graph.getHeight(), nodes, dataNodes);
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
	    
	    // initialize N clusters, one cluster consists one node 
 		clusters = new ArrayList<Cluster>();
 		for (Integer key: nodes.keySet()) {
 			Axis axis = nodes.get(key);
 			
 			// only data nodes 
 			if (axis.isDataNode())				
 				clusters.add(new Cluster(axis));
 		}
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
	
	public static void startMergeWeightedClusters() {	
		boolean flg = true;
		int collectedPrize = 0;
		
		
		// while (collect not enough prize) { 		
		while (flg) {
			collectedPrize = 0;
			
			int cluster1_ID = -1;
			int cluster2_ID = -1;
			double minRatio = Integer.MAX_VALUE;
			Set<Integer> minPath = null;
			int node1 = -1;
			int node2 = -1;
			
			// loop all clusters
			for (Cluster cluster: clusters) {
				Map<Integer, ShortestPath> ratiosInfo = cluster.getRatiosInfo();
				
				ArrayList<Axis> leafNodes = cluster.getLeafNodes(dataAdjacencyMatrix);
				
				// loop all leaf nodes of current cluster 
				for (Axis axis1: leafNodes) {
					// compute shortest distance from current leaf node to all the other nodes
					// store new in current cluster, update if smaller : map<Cluster , distance>
					getShortestPaths(axis1.getId(), dataAdjacencyMatrix);
					for(int key: dataNodes.keySet()) {
						Axis axis2 = dataNodes.get(key);
						
						if (!cluster.isInCluster(axis2.getId())) {
							Cluster c = axis2.getCluster();
							double weight = axis2.getShortestDistance() / Math.min(cluster.getPrize(), c.getPrize());
							
							if (ratiosInfo.containsKey(c.getId())) {
								double oldWeight = ratiosInfo.get(c.getId()).weight;
								
								if (oldWeight > weight) {
									ShortestPath shortestPathObj = new ShortestPath();
									shortestPathObj.weight = weight;
									shortestPathObj.node1 = axis1.getId();
									shortestPathObj.node2 = axis2.getId();
									shortestPathObj.path = axis2.getShortestPath();
									shortestPathObj.path.remove(axis1.getId());
									shortestPathObj.path.remove(axis2.getId());
									ratiosInfo.put(c.getId(), shortestPathObj);
								}
							} else {
								ShortestPath shortestPathObj = new ShortestPath();
								shortestPathObj.weight = weight;
								shortestPathObj.node1 = axis1.getId();
								shortestPathObj.node2 = axis2.getId();
								shortestPathObj.path = axis2.getShortestPath();
								shortestPathObj.path.remove(axis1.getId());
								shortestPathObj.path.remove(axis2.getId());
								ratiosInfo.put(c.getId(), shortestPathObj);
							}
						}
					}
				}
				        
			    // check ratio of distance and prize between current cluster to all other clusters
				// get min & compare min
				for (Integer key: ratiosInfo.keySet()) {
					if (ratiosInfo.get(key).weight < minRatio) {
						cluster1_ID = cluster.getId();
						cluster2_ID = key;
						minRatio = ratiosInfo.get(key).weight;
						minPath = new LinkedHashSet<Integer>();
						minPath.addAll(ratiosInfo.get(key).path);
						node1 = ratiosInfo.get(key).node1;
						node2 = ratiosInfo.get(key).node2;
					}
				}
			}
			
			Cluster cluster1 = null;
			Cluster cluster2 = null;
			for (Cluster c: clusters) {
				if (c.getId() == cluster1_ID)
					cluster1 = c;
				else if (c.getId() == cluster2_ID)
					cluster2 = c;
						
				if (cluster1 != null && cluster2 != null)
					break;
			}
			
			if (cluster1 != null && cluster2 != null) {
				// merge min pair
				mergeClusters(cluster1, cluster2, minPath, node1, node2);
				
				// get cluster providing max prize				
				for (Cluster c: clusters) {
					if (c.getPrizeExcludeInitiator() == 0)
						continue;
					
					if (c.getPrizeExcludeInitiator() > maxPrize) {
						maxPrize = c.getPrizeExcludeInitiator();
						maxPrizeCluster = c;
					}
					
					//collectedPrize += c.getPrizeExcludeInitiator();
				}
				
				if (maxPrize == 0)
					return;
					
				collectedPrize = maxPrizeCluster.getPrizeExcludeInitiator();
				
				
				
				if (collectedPrize >= quotaPrize)
					flg = false;
				
				/*
				if (!cluster1.isCollected()) {
					collectedPrize += cluster1.getPrize();
					cluster1.collectPrize();
				}
				
				if (!cluster2.isCollected()) {
					collectedPrize += cluster2.getPrize();
					cluster2.collectPrize();
				}*/
				
			    // merge min pair
				//mergeClusters(cluster1, cluster2);
				
				// check if gained enough prize
				//if (collectedPrize >= prize)
				//	flg = false;
			}
			else {
				System.out.println("-------- Error, somethings went wrong ! ------------------");
			}
	    }
		
		//drawClusters();
	}
	
	public static void computeMinimumSpanningForest() {
		/* use 2 global variables dataAdjacencyMatrix, clusters */
		
		int collectedPrize = 0;
		
		/*
		// combine all clusters that were collected prize
		Cluster forest = null;
		Iterator<Cluster> ite = clusters.iterator();
		while (ite.hasNext()) {
			Cluster c = ite.next();
			
			int initiator = c.getInitiatorId();
			for (Axis a: c.getNodes()) {
				if (a.getId() == initiator) {
					a.isInitiator = true;
					break;
				}
			}
			
			//if (c.isCollected()) {
			int tmp = c.getPrizeExcludeInitiator();
			if (tmp != 0) {
				collectedPrize += tmp; 
						
				if (forest == null) {
					forest = c;
				} else {
					forest.getNodes().addAll(c.getNodes());
					for (Axis a: c.getNodes()) {
						a.setCluster(forest);
					}
					ite.remove();
				}
			}
		}
		
		if (forest == null || forest.getSize()== 0)
			return;
		
		// report prizes of nodes in forest
		forest.getNodes().forEach( axis -> 
			{
				if (!axis.isInitiator)
					System.out.println("Get prize: " + (axis.getRCapNumber() - axis.getRNumber()) + " from node " + axis.getId());
				else
					System.out.println("Node "+ axis.getId() + " is initiator");
			}
		);*/
		
		// get all edges belonging to largest cluster
		ArrayList<Edge> edges = new ArrayList<Edge>();
		
		int initiator = maxPrizeCluster.getInitiatorId();
		for (Axis a: maxPrizeCluster.getNodes()) {
			if (a.getId() == initiator) {
				a.isInitiator = true;
				break;
			}
		}
		
		collectedPrize = maxPrizeCluster.getPrizeExcludeInitiator();
		
		for  (int n1: dataAdjacencyMatrix.keySet()) {
			if (!maxPrizeCluster.isInCluster(n1)) 
				continue;
			
			Set<Integer> list = dataAdjacencyMatrix.get(n1);
			for (int n2: list) {
				if (!maxPrizeCluster.isInCluster(n2)) 
					continue;
				
				if (!edges.contains(new Edge(n2, n1, 0))) {
					Edge e = new Edge(n1, n2, Util.distance(nodes, n1, n2));
				
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
		
		// compute all connected components ( there might be more than 1 )
		ArrayList<Edge> qEdges = new ArrayList<Edge>();
		Map<Integer, Set<Integer>> connectedComponents = new LinkedHashMap<Integer, Set<Integer>>();
		int k = 0;
		int prevK = 0;
		int maxKey = 0;
		while (k < edges.size()) {
			prevK = k;
			Iterator<Edge> ite1 = edges.iterator();
			while (ite1.hasNext()) {
				Edge edge = ite1.next();
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
		
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int screenHeight = (int) Math.round(screenSize.getHeight());
		drawMininumSpanningForest(screenHeight, graph.getWidth(), graph.getHeight(), connectedComponents, qEdges);
		
		System.out.println("  choosing edges ends with collected prize is " + collectedPrize + " MB");
		System.out.println();
		System.out.println("  finding walk starts");
		
		int numberOfAggregator = 0;
		int numberOfInitiator = connectedComponents.size();
		int ccIndex = 0;
		double ccSumCost = 0;
		double aggregationSumCost = 0;
		int numberOfWalk = 0;
		for (int key: connectedComponents.keySet()) {
			Set<Integer> connectedComponent = connectedComponents.get(key);
			
			// find adjacent map including all added edges
			Map<Integer, Set<Integer>> aggregatorAdjMap = new LinkedHashMap<Integer, Set<Integer>> ();
			//ArrayList<Integer> leafNodes = null;
			
			// get leaf list of current connectedComponent
			Util.findAllLeafNodes(connectedComponent, qEdges, aggregatorAdjMap);
			
			/* find initiator then apply DFS to find walks  */
			int overflowDatasize = 0;
			initiator = -1; 
			Iterator<Integer> it = connectedComponent.iterator();
			while (it.hasNext()) {
				int n = it.next();
				Axis axis = nodes.get(n);
				if (axis != null && axis.isInitiator) {
					initiator = n;
					overflowDatasize = axis.getRCapNumber(); // - axis.getRNumber();
				}
			}
			
			if (initiator == -1)
				continue;
			
			List<Integer> dFSWalk = new ArrayList<Integer>();
			execDFS(initiator, aggregatorAdjMap, connectedComponent.size(), dFSWalk);
			
			numberOfAggregator +=  (connectedComponent.size() - 1);
			
			ccIndex += 1;
			ccSumCost = 0;
			System.out.println("    connected component["+ ccIndex +"]");
			System.out.println("      Starting Point = " +(initiator) + " -----, overflow data size = " + overflowDatasize);
			
			
			
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
			numberOfWalk += ccWalk;
			
			//aggregationWalks.put(ccIndex, aggregationWalk);
		}
		
		//System.out.println("    Number of Messages: "+ numberOfWalk);
		System.out.println("    Aggregated data size: "+ collectedPrize + " MB");
		aggregationSumCost = Math.floor(aggregationSumCost * 1000000) / 1000000;
		System.out.println("    Total Cost of Aggregation: "+ aggregationSumCost + " J");
		
		System.out.println("  finding walk ends");
		System.out.println("algorithm4() function end");
		
		System.out.println("Number of Initiator = " + numberOfInitiator);
		System.out.println("Number of Aggregator = " + numberOfAggregator);
	}
	
	private static void mergeClusters (Cluster cluster1, Cluster cluster2, Set<Integer> shortestPath, int node1, int node2) {
		if (cluster1 != null && cluster2 != null) {
			if (cluster1.getId() <= cluster2.getId()) {
				cluster1.getNodes().addAll(cluster2.getNodes());
				for (Axis a: cluster2.getNodes()) {
					a.setCluster(cluster1);
				}
				if (cluster1.getInitiatorPrize() > cluster2.getInitiatorPrize()) 
					cluster1.setInitiatorId(cluster2.getInitiatorId());
				
				// add immediate nodes
				Iterator<Integer> ite = shortestPath.iterator();
				while (ite.hasNext()) {
					int n = ite.next();
					
					Axis axis = nodes.get(n);
					
					Cluster oldCluster = axis.getCluster();
					if (oldCluster.getNodes().size() == 1)
						clusters.remove(oldCluster);
					else
						oldCluster.getNodes().remove(axis);
					
					axis.setCluster(cluster1);
					cluster1.getNodes().add(axis);
				}
				
				clusters.remove(cluster2);
			} else {
				cluster2.getNodes().addAll(cluster1.getNodes());
				for (Axis a: cluster1.getNodes()) {
					a.setCluster(cluster2);
				}
				
				if (cluster2.getInitiatorPrize() > cluster1.getInitiatorPrize()) 
					cluster2.setInitiatorId(cluster1.getInitiatorId());
				
				// add immediate nodes
				Iterator<Integer> ite = shortestPath.iterator();
				while (ite.hasNext()) {
					int n = ite.next();
				
					Axis axis = nodes.get(n);
					
					Cluster oldCluster = axis.getCluster();
					if (oldCluster.getNodes().size() == 1)
						clusters.remove(oldCluster);
					else
						oldCluster.getNodes().remove(axis);
					
					axis.setCluster(cluster2);
					cluster2.getNodes().add(axis);
				}
				
				clusters.remove(cluster1);
			}
			
			/*if (cluster1.getSize() > cluster2.getSize()) {
				cluster1.getNodes().addAll(cluster2.getNodes());
				for (Axis a: cluster2.getNodes()) {
					a.setCluster(cluster1);
				}
				clusters.remove(cluster2);
			} else {
				cluster2.getNodes().addAll(cluster1.getNodes());
				for (Axis a: cluster1.getNodes()) {
					a.setCluster(cluster2);
				}
				clusters.remove(cluster1);
			}*/
		}
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
	
	
	private static void drawSensorNetworkGrap(int screenHeight, double width, double height, Map<Integer, Axis> nodes, Map<Integer, Set<Integer>> adjList, Map<Integer, Set<Integer>> dataAdjList) {
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

	private static void drawClusters() {
		
	}
	
	private static void drawMininumSpanningForest(int screenHeight, double width, double height, 
													Map<Integer, Set<Integer>> connectedComponents,
													ArrayList<Edge> qEdges) {
		//Draw Minimum Spanning Tree graph
		QEdgeForest qEdgesGraph = new QEdgeForest();
		qEdgesGraph.setGraphWidth(width);
		qEdgesGraph.setGraphHeight(height);
		qEdgesGraph.setNodes(nodes);
		qEdgesGraph.setAdjList(dataAdjacencyMatrix);
		qEdgesGraph.setConnectedComponents(connectedComponents);
		qEdgesGraph.setQEdges(qEdges);
		qEdgesGraph.setPreferredSize(new Dimension(960, 800));
		qEdgesGraph.setPreferredSize(new Dimension(screenHeight-30, screenHeight-30));
		Thread BgraphThread = new Thread(qEdgesGraph);
		BgraphThread.start();
	}
	
	private static List<String> getTokensWithCollection(String str) {
	    return Collections.list(new StringTokenizer(str, "	")).stream()
	      .map(token -> (String) token)
	      .collect(Collectors.toList());
	}
}
