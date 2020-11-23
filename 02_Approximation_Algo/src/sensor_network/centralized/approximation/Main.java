package sensor_network.centralized.approximation;

public class Main {

	public static void main(String[] args) {
		//String nodesPath = "//Users//hungngo//graph.dat";
		
		//String nodesPath = 	"/Users/hungngo/data/PForest/55/graph.dat";
		//String nodesPath = 	"/Users/hungngo/data/PForest/60/graph.dat";
		//String nodesPath = 	"/Users/hungngo/data/PForest/65/graph.dat";
		//String nodesPath = 	"/Users/hungngo/data/PForest/70/graph.dat";
		//String nodesPath = 	"/Users/hungngo/data/PForest/75/graph.dat";
		
		String nodesPath = "/Users/hungngo/data/algos/50/graph.dat";
		String fileName  = "/Users/hungngo/Data/Algos/m_20000/0.6/capacity.txt";
		
		Network.initialise(nodesPath, fileName);
		
		Network.startMergeWeightedClusters();
		
		Network.computeMinimumSpanningForest();
		
		// start monitoring
		//new MonitoringService().start();
		
		//sensor.drawResults(graph.getWidth(), graph.getHeight(), adjacencyMatrix, dataAdjacencyMatrix);
	}

}
