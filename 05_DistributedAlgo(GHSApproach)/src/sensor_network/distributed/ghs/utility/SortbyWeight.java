package sensor_network.distributed.ghs.utility;

import java.util.Comparator;

import sensor_network.distributed.ghs.Network;

public class SortbyWeight implements Comparator<Integer> 
{ 
	int n;
	public SortbyWeight(int _n) {
		n = _n;
	}

	@Override
	public int compare(Integer o1, Integer o2) {
		double candidate1Weight = Network.getWeight(n, o1);
		double candidate2Weight = Network.getWeight(n, o2);
		return (candidate1Weight < candidate2Weight ? -1 : 
            (candidate1Weight == candidate2Weight ? 0 : 1));
	} 
}