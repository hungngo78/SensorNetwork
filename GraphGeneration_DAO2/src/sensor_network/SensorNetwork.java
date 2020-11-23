package sensor_network;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

import sensor_network.graph.SensorNetworkGraph;
import sensor_network.graph.generating.Axis;
import sensor_network.graph.storing.Graph;
import sensor_network.graph.storing.SensorNode;

public class SensorNetwork {
	private Map<Integer, Axis> nodes = new LinkedHashMap<Integer, Axis>();
	private Map<Integer, Axis> dataNodes = new LinkedHashMap<Integer, Axis>();

	private int rCapNumberTotal = 0;
	private int rNumberTotal = 0;
	private int mNumberTotal = 0;
	
	public static void main(String[] args) {
		Scanner scan = new Scanner(System.in);
		
		double beta = 0.5;
		
		// default values
		double width = 2000;
		double height = 2000;
		int transmissionRange = 250;
		
		int numberOfNodes = 100;
		
		int numberOfDataNodes = 50;
//		String nodesPath = "/Users/hungngo/data/PForest/70/graph.dat";
//		String fileName  = "/Users/hungngo/Data/PForest/70/capacity.txt";
		String nodesPath = "/Users/hungngo/data/Algos/50/graph.dat";
		String fileName  = "/Users/hungngo/Data/Algos/50/capacity.txt";
		
		//String nodesPath = "C:\\02_DH\\99_LuanVanTotNghiep\\12_tmp\\02_extension\\console_output1\\65_data_nodes\\graph.dat";
		//String fileName  = "C:\\02_DH\\99_LuanVanTotNghiep\\12_tmp\\02_extension\\console_output1\\65_data_nodes\\capacity65.txt";
		
		System.out.println("width: " + width + " m");
		System.out.println("height: " + height + " m");
		System.out.println("Transmission range: " + transmissionRange + " m");
		
		System.out.println("number of nodes: " + numberOfNodes);
		//numberOfNodes = scan.nextInt();
		
		System.out.println("number of data nodes: " + numberOfDataNodes);
		//numberOfDataNodes = scan.nextInt();
		
		
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
		
		// calculate total of prize need to gain
		int prizeTotal = 8000; //5000;  //rCapNumberTotal - mNumberTotal;
		//System.out.println("Total prize need to gain: " + prizeTotal + " MB");
				
		System.out.print("Do you want to load node capacity from file ? (Y/N) ");
		String answer = scan.next();
		if ("Y".equals(answer.toUpperCase())) {
			sensor.loadNodeCapacity(fileName, numberOfNodes, numberOfDataNodes);
		} else {
			sensor.generateNodeCapacity(scan, numberOfNodes, prizeTotal);
			scan.close();
		}
				
		// save the generated graph to file
		Graph graph = new Graph(numberOfNodes, numberOfDataNodes, width, height , transmissionRange); 
									//rCapNumberTotal, rNumberTotal, mNumberTotal);
		graph.setAdjacencyMatrix(adjacencyMatrix);
		graph.setDataAdjacencyMatrix(dataAdjacencyMatrix);
		sensor.saveToFile(graph, nodesPath);
		
		// the screen height
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int screenHeight = (int) Math.round(screenSize.getHeight());
		sensor.drawSensorNetworkGrap(screenHeight, width, height, adjacencyMatrix, dataAdjacencyMatrix);
	}
	
	private List<String> getTokensWithCollection(String str) {
	    return Collections.list(new StringTokenizer(str, "	")).stream()
	      .map(token -> (String) token)
	      .collect(Collectors.toList());
	}
	
	private boolean loadNodeCapacity(String fileName, int numberOfNodes, int numberOfDataNodes) {
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
			
			int i = 1;
			int lineIndex = 0;
			while (i <= numberOfNodes) {
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
		int i = 1;
		while (i <= numberOfNodes) {
			Axis axis = nodes.get(i);
			if (axis.isDataNode()) {
				System.out.println("node["+ i +"], R:"+axis.getRCapNumber() + ", r: "+axis.getRNumber());
			}
			i++;
		}
		
		return true;
	}
	
	private void generateNodeCapacity(Scanner scan, int nodeCount, int prizeTotal) {
		boolean invalid = false;
		do {
			rCapNumberTotal = 0;
			rNumberTotal = 0;
			
			System.out.print("Enter percent of data aggregate to (Ex: 0.8): ");
			double rNumberPercent = scan.nextDouble();
			
			Random random = new Random();
			int i= 1;
			while (i <= nodeCount) {
				Axis axis = nodes.get(i);
				
				if (axis.isDataNode()) {
					// random R 
					int rCapNumber = 512 + random.nextInt(512);
					rCapNumberTotal += rCapNumber;
					axis.setRCapNumber(rCapNumber);
					
					// compute r 
					int rNumber = (int) Math.round(rNumberPercent * rCapNumber);
					rNumberTotal += rNumber;
					axis.setRNumber(rNumber);
				}
				
				i++;
			}
			
			if (rCapNumberTotal > prizeTotal) {
				mNumberTotal = rCapNumberTotal - prizeTotal;
				
				// check if data could be aggregated will be enough or not
				if (rNumberTotal > mNumberTotal) {
					System.out.println("\nPlease input again, the data could be aggregated will not be enough");
					invalid = true;
				}
				else
					invalid = false;
			} else {
				System.out.println("\nPlease input again, total value of generated R is so small");
				invalid = true;
			}
		} while (invalid);
		
		System.out.println("Total R number: " + rCapNumberTotal + " MB");
		System.out.println("Total r number: " + rNumberTotal + " MB");
		System.out.println("Total m number: " + mNumberTotal + " MB");
		
		int i = 1;
		while (i <= nodeCount) {
			Axis axis = nodes.get(i);
			if (axis.isDataNode()) {
				System.out.println(axis.getRCapNumber() + "	"+axis.getRNumber());
			}
			i++;
		}
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
		
		graph.setNumberR(this.rCapNumberTotal);
		graph.setNumberr(this.rNumberTotal);
		graph.setNumberm(this.mNumberTotal);
		
		FileOutputStream fos;
		try {
			fos = new FileOutputStream(nodesPath);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			Map<Integer, SensorNode> savedNodes = new LinkedHashMap<Integer, SensorNode>();
			for (int key: nodes.keySet()) {
				Axis n = nodes.get(key);
				
				SensorNode sensorNode = new SensorNode(n.getxAxis(), n.getyAxis(), n.isDataNode());
				sensorNode.setRCapNumber(n.getRCapNumber());
				sensorNode.setRNumber(n.getRNumber());
				
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
