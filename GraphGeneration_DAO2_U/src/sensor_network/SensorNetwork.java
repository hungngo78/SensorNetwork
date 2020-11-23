package sensor_network;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;

import sensor_network.graph.SensorNetworkGraph;
import sensor_network.graph.generating.Axis;
import sensor_network.graph.storing.Graph;
import sensor_network.graph.storing.SensorNode;

public class SensorNetwork {
	private Map<Integer, Axis> nodes = new LinkedHashMap<Integer, Axis>();
	private Map<Integer, Axis> dataNodes = new LinkedHashMap<Integer, Axis>();

	public static void main(String[] args) {
		Scanner scan = new Scanner(System.in);
		
		double beta = 0.6;
		
		// default values
		double width = 2000;
		double height = 2000;
		int transmissionRange = 250;
		
		int numberOfNodes = 100;
		int numberOfDataNodes = 60;
		
		int rCapNumber = 512;
		int mNumber = 512;
		//int rNumber = 0;
		int qNumber = 0;		
		boolean invalid = false;
		
		//String nodesPath = "C:\\02_DH\\99_LuanVanTotNghiep\\12_tmp\\performance_report_6\\graph.dat";
		String nodesPath = 	"/Users/hungngo/data/performance_report_11/71/graph.dat";
		
		System.out.println("width: " + width + " m");
		//width = scan.nextDouble();

		System.out.println("height: " + height + " m");
		//height = scan.nextDouble();
		
		System.out.println("Transmission range: " + transmissionRange + " m");
		//transmissionRange = scan.nextInt();
		
		do {
			System.out.println("number of nodes: " + numberOfNodes);
			//numberOfNodes = scan.nextInt();
			
			System.out.print("Enter the number of data nodes (Ex: 55): ");
			numberOfDataNodes = scan.nextInt();
			
			System.out.println("R number: " + rCapNumber + " MB");
			//rCapNumber = scan.nextInt();
			
			System.out.println("m number: " + mNumber + " MB");
			//mNumber = scan.nextInt();
			
			//System.out.print("Enter r number (Ex: 15): ");
			//rNumber = scan.nextInt();
			
			// calculate number of aggregators need to be walked through
			//qNumber = (numberOfDataNodes * (rCapNumber + mNumber) - (numberOfNodes * mNumber)) / (rCapNumber - rNumber);
			double tmpNo = (numberOfDataNodes*(1+mNumber/rCapNumber) - numberOfNodes*mNumber/rCapNumber) / beta;
			int roundNo = (int) Math.round(tmpNo  );
			if (roundNo < tmpNo)
				qNumber = roundNo + 1;
			else
				qNumber = roundNo;
			System.out.println("\nQ number:" + qNumber);
			
			if (qNumber > numberOfDataNodes) {
				System.out.println("\nPlease input again, your input were wrong, and it made an invalid q number exceeding number of data nodes");
				invalid = true;
			}
			else if (qNumber <= 0 ) {
				System.out.println("\nPlease input again, your input were wrong, and it made nagative q number");
				invalid = true;
			} 
			else 
				invalid = false;
			
		} while (invalid);
		
		scan.close();
		
		Map<Integer, Set<Integer>> adjacencyMatrix = null;
		Map<Integer, Set<Integer>> dataAdjacencyMatrix = null;
		SensorNetwork sensor = new SensorNetwork();
		boolean connected = false;
		while (!connected) {
			/* generate graph */
			sensor.populateNodes(numberOfNodes, width, height, numberOfDataNodes, transmissionRange);
			
			/* populate adjacency matrix */
			//System.out.println("Populating Adjacency Matrix ... ");
			adjacencyMatrix = new LinkedHashMap<Integer, Set<Integer>> ();
			dataAdjacencyMatrix = new LinkedHashMap<Integer, Set<Integer>> ();
			sensor.populateAdjacencyMatrix(numberOfNodes, transmissionRange, adjacencyMatrix, dataAdjacencyMatrix);			
			sensor.eliminateIsolation(numberOfNodes, adjacencyMatrix, dataAdjacencyMatrix, transmissionRange, width, height);
			
			// check if just newly generated graph is connected
			connected = sensor.checkConnected(adjacencyMatrix, numberOfNodes);
			//System.out.println("Connected :"+connected);
		}
		
		// save the generated graph to file
		Graph graph = new Graph(numberOfNodes, numberOfDataNodes, width, height , transmissionRange,
									rCapNumber, mNumber, beta, qNumber);
		graph.setAdjacencyMatrix(adjacencyMatrix);
		graph.setDataAdjacencyMatrix(dataAdjacencyMatrix);
		sensor.saveToFile(graph, nodesPath);
		
		// the screen height
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int screenHeight = (int) Math.round(screenSize.getHeight());
		sensor.drawSensorNetworkGrap(screenHeight, width, height, adjacencyMatrix, dataAdjacencyMatrix);
	}
	
	public boolean checkConnected(Map<Integer, Set<Integer>> adjacencyMatrix, int _numberOfNodes) {
		//Random random = new Random();
		boolean res = true;
		List<Integer> dFSWalk = null;
		for(int n:adjacencyMatrix.keySet()) {
			dFSWalk = new ArrayList<Integer>();
			execDFS(n, dFSWalk, adjacencyMatrix, _numberOfNodes);
			for (int i=1; i<=(_numberOfNodes); i++) {
				//int index = random.nextInt(_numberOfNodes)+1;
				if (!dFSWalk.contains(i)) {
					res = false;
					break;
				}
			}
			if (!res)
				break;
			
			// reset for next round
			for(int n1:adjacencyMatrix.keySet()) {
				Axis axis1 = nodes.get(n1);
				if (axis1!= null && axis1.isVisited()) {
					axis1.setVisited(false);
				}
			}
		}
		return res;
	}
	
	private void populateNodes(int nodeCount, double width, double height, int dataNodeCount, int _transmissionRange) {
		//System.out.println("populateNodes() function start");
		Random random = new Random();
		int i= 1;
		while (i <= nodeCount) {
			Axis axis = new Axis();
			
			axis.setNodeType(false);
			
			int scale = (int) Math.pow(10, 1);
			double xAxis =(0 + random.nextDouble() * (width - 0));
			double yAxis = 0 + random.nextDouble() * (height - 0);
			
			xAxis = (double)Math.floor(xAxis * scale) / scale;
			yAxis = (double)Math.floor(yAxis * scale) / scale;
			
			axis.setxAxis(xAxis);
			axis.setyAxis(yAxis);
			
			// and we dont want it is so close to all the other nodes
			//if (Util.validateAxis(nodes, xAxis, yAxis, Constant.MIN_DISTANCE_BETWEEN_NODES)) {
				//System.out.println("  added node "+i);
				nodes.put(i, axis);	
				i++;
			//}
		}
		
		while (dataNodeCount > 0) {
			int index = random.nextInt(nodeCount);
			Axis node = nodes.get(index + 1);
			if (!node.isDataNode()) {
				node.setNodeType(true);
				dataNodeCount--;
				
				dataNodes.put(index + 1, node);
			}   
		}
		//System.out.println("populateNodes() function end");
	}
	
	private void populateNode(int n, double width, double height) {
		Random random = new Random();
		Axis oldAxis = nodes.get(n);
		
		Axis axis = new Axis();
		int scale = (int) Math.pow(10, 1);
		double xAxis =(0 + random.nextDouble() * (width - 0));
		double yAxis = 0 + random.nextDouble() * (height - 0);
		
		xAxis = (double)Math.floor(xAxis * scale) / scale;
		yAxis = (double)Math.floor(yAxis * scale) / scale;
		
		axis.setxAxis(xAxis);
		axis.setyAxis(yAxis);
		
		nodes.put(n, axis);	

		// dataNodes
		if (oldAxis.isDataNode()) {
			axis.setNodeType(true);
			dataNodes.put(n, axis);
		}   
	}

	private void populateAdjacencyMatrix(int nodeCount, int _transmissionRange, 
							Map<Integer, Set<Integer>> adjMatrix, Map<Integer, Set<Integer>> dataAdjMatrix) {
		//System.out.println("populateAdjacencyList() function start");
		for(int i=1; i<= nodeCount; i++) {
			adjMatrix.put(i, new HashSet<Integer>());
			if (nodes.get(i).isDataNode())
				dataAdjMatrix.put(i, new HashSet<Integer>());
		}
		
		for(int node1: nodes.keySet()) {
			Axis axis1 = nodes.get(node1);
			for(int node2: nodes.keySet()) {
				Axis axis2 = nodes.get(node2);
				
				if(node1 == node2) {
					continue;
				}
				double xAxis1 = axis1.getxAxis();
				double yAxis1 = axis1.getyAxis();
					
				double xAxis2 = axis2.getxAxis();
				double yAxis2 = axis2.getyAxis();
				
				double distance =  Math.sqrt(((xAxis1-xAxis2)*(xAxis1-xAxis2)) + ((yAxis1-yAxis2)*(yAxis1-yAxis2)));
				
				if (distance <= _transmissionRange) {
					Set<Integer> tempList = adjMatrix.get(node1);
					tempList.add(node2);
					adjMatrix.put(node1, tempList);
						
					tempList = adjMatrix.get(node2);
					tempList.add(node1);
					adjMatrix.put(node2, tempList);
					
					if (nodes.get(node1).isDataNode() && nodes.get(node2).isDataNode()) {
						tempList = dataAdjMatrix.get(node1);
						tempList.add(node2);
						dataAdjMatrix.put(node1, tempList);
							
						tempList = dataAdjMatrix.get(node2);
						tempList.add(node1);
						dataAdjMatrix.put(node2, tempList);
					}
				}
			}
		}
		//System.out.println("populateAdjacencyList() function end");
	}
	
	private void populateAdjacencyList(int node1, int _transmissionRange, 
									Map<Integer, Set<Integer>> adjMatrix, Map<Integer, Set<Integer>> dataAdjMatrix) {
		Axis axis1 = nodes.get(node1);
		
		adjMatrix.get(node1).clear();
		if (axis1.isDataNode())
			dataAdjMatrix.get(node1).clear();
				
		for(int node2: nodes.keySet()) {
			Axis axis2 = nodes.get(node2);
			
			if(node1 == node2) {
				continue;
			}
			double xAxis1 = axis1.getxAxis();
			double yAxis1 = axis1.getyAxis();
				
			double xAxis2 = axis2.getxAxis();
			double yAxis2 = axis2.getyAxis();
			
			double distance =  Math.sqrt(((xAxis1-xAxis2)*(xAxis1-xAxis2)) + ((yAxis1-yAxis2)*(yAxis1-yAxis2)));
			
			if (distance <= _transmissionRange) {
				Set<Integer> tempList = adjMatrix.get(node1);
				tempList.add(node2);
				adjMatrix.put(node1, tempList);
					
				tempList = adjMatrix.get(node2);
				tempList.add(node1);
				adjMatrix.put(node2, tempList);
				
				if (axis1.isDataNode() && axis2.isDataNode()) {
					tempList = dataAdjMatrix.get(node1);
					tempList.add(node2);
					dataAdjMatrix.put(node1, tempList);
						
					tempList = dataAdjMatrix.get(node2);
					tempList.add(node1);
					dataAdjMatrix.put(node2, tempList);
				}
			}
		}
	}
	
	public void eliminateIsolation(int nodeCount, Map<Integer, Set<Integer>> adjacencyMatrix, 
										Map<Integer, Set<Integer>> dataAdjacencyMatrix, 
										int _transmissionRange, double width, double height) {
		for(int n:adjacencyMatrix.keySet()) {
			while (true) {
				Set<Integer> adjList = adjacencyMatrix.get(n);
				if (adjList.size()==0) {
					populateNode(n, width, height);
					populateAdjacencyList(n, _transmissionRange, adjacencyMatrix, dataAdjacencyMatrix);
				} else
					break;
			}
		}
	}
	
	public void saveToFile(Graph graph, String nodesPath) {
		
		FileOutputStream fos;
		try {
			fos = new FileOutputStream(nodesPath);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			Map<Integer, SensorNode> savedNodes = new LinkedHashMap<Integer, SensorNode>();
			for (int key: nodes.keySet()) {
				Axis n = nodes.get(key);
				SensorNode sensorNode = new SensorNode(n.getxAxis(), n.getyAxis(), n.isDataNode());
				savedNodes.put(key, sensorNode);
			}
			graph.setNodes(savedNodes);
			
	        oos.writeObject(graph);
	        oos.close();
	        fos.close();
		} catch (FileNotFoundException fe) {
			fe.printStackTrace();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void drawSensorNetworkGrap(int screenHeight, double width, double height, 
										Map<Integer, Set<Integer>> adjList, Map<Integer, Set<Integer>> dataAdjList) {
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

	private int execDFS(int n1, List<Integer> path , Map<Integer, Set<Integer>> dataAdjList , int visitNo ) {
//		if (visitNo < 0)
//			return visitNo;
		
		Axis axis1 = nodes.get(n1);
		if (axis1!= null && !axis1.isVisited()) {
			axis1.setVisited(true);
			visitNo--;
		}
		
		Set<Integer> n1AdjList = dataAdjList.get(n1);
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
}
