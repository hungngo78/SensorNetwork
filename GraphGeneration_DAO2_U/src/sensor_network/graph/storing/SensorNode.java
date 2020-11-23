package sensor_network.graph.storing;

import java.io.Serializable;

public class SensorNode implements Serializable {
	private static final long serialVersionUID = -2848254551062387833L;

	private double xAxis;
	private double yAxis;
	private boolean dataNodeType;
	
	public SensorNode() {
		dataNodeType = false;
	}
	
	public SensorNode(double _xAxis, double _yAxis, boolean _dataNodeType) {
		xAxis = _xAxis;
		yAxis = _yAxis;
		dataNodeType = _dataNodeType;
	}

	public double getxAxis() {
		return xAxis;
	}
	
	public void setxAxis(double xAxis) {
		this.xAxis = xAxis;
	}
	
	public double getyAxis() {
		return yAxis;
	}
	
	public void setyAxis(double yAxis) {
		this.yAxis = yAxis;
	}
	
	public boolean isDataNode() {
		return dataNodeType;
	}
	
	public void setNodeType(boolean _nodeType) {
		this.dataNodeType = _nodeType;
	}
}
