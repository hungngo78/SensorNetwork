package sensor_network.distributed.ghs.utility;

public class Constant {
	public enum STEP2_END_CONDITION 
	{ 
	    GO_TO_END, 
	    REACH_Q_EDGES; 
	} 
	
	public static boolean DRAW_SENSOR_NETWORK_GRAPH = false;
	public static boolean DRAW_AGGREGATION_GRAPH = false;
	public static boolean DRAW_MINIMUM_SPANNING_TREE_GRAPH = false;
	
	public static int TRANMISSION_RANGE = 300;
	public static int MIN_DISTANCE_BETWEEN_NODES = 10;
	
	public static int STEP1 = 1;		// distributed Bellman-Ford algorithm
	public static int STEP2 = 2;		// GHS algorithm
	public static int STEP3 = 3;		// LP-Walk

	public static boolean LOCAL_BROASTCASTING = true;
	
}
