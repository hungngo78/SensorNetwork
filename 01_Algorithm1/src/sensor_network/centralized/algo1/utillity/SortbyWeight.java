package sensor_network.centralized.algo1.utillity;

import java.util.Comparator;
import java.util.Map;

import sensor_network.centralized.algo1.node.Axis;

public class SortbyWeight implements Comparator<Integer> 
{ 
	private Map<Integer, Axis> dataNodes = null;
	private int n;
	
	public SortbyWeight(Map<Integer, Axis> _dataNodes, int _n) {
		dataNodes = _dataNodes;
		n = _n;
	}

	@Override
	public int compare(Integer o1, Integer o2) {
		double candidate1Weight = Util.distance(dataNodes, n, o1);
		double candidate2Weight = Util.distance(dataNodes, n, o2);
		return (candidate1Weight < candidate2Weight ? -1 : 
            (candidate1Weight == candidate2Weight ? 0 : 1));
	} 
}