package sensor_network.centralized.benefit.utility;

import java.util.LinkedHashSet;
import java.util.Set;

import sensor_network.centralized.benefit.node.Axis;

public class ConnectedComponent {
	
	public Set<Integer> nodes = null;
	public int initiatorId;
	public Axis initiator;
	
	public ConnectedComponent() {
		nodes = new LinkedHashSet<Integer>();
		initiatorId = -1;
		initiator = null;
	}
}
