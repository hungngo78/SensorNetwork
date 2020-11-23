package sensor_network.graph.storing;


import java.io.Serializable;
import java.util.Map;
import java.util.Set;

public class Graph implements Serializable {
	private static final long serialVersionUID = -2595716559019410276L;
	
	private Map<Integer, SensorNode> nodes;
	private Map<Integer, Set<Integer>> adjacencyMatrix;
	private Map<Integer, Set<Integer>> dataAdjacencyMatrix;
	
	private double width = 2000;
	private double height = 2000;
	private int transmissionRange = 260;
	
	private int nodesNumber = 0;
	private int dataNodesNumber = 0;
	private int numberR = 0;
	private int numberm = 0;
	private double beta;
	//private int numberr = 0;
	private int numberq = 0;	
	
	public Graph() {
		// TODO Auto-generated constructor stub
	}
	
	public Graph(int _nodesNumber, int _dataNodesNumber, double _width, double _height, int _transmissionRange, 
			int _numberR, int _numberm, double _beta, int _numberq ) {
		this.nodesNumber = _nodesNumber;
		this.dataNodesNumber = _dataNodesNumber;
		this.width = _width;
		this.height = _height;
		this.transmissionRange = _transmissionRange;
		this.numberR = _numberR;
		this.beta = _beta;
		//this.numberr = _numberr;
		this.numberm = _numberm;
		this.numberq = _numberq;
	}
	
	public void setNodes(Map<Integer, SensorNode> _nodes) {
		this.nodes = _nodes;
	}
	public Map<Integer, SensorNode> getNodes() {
		return nodes;
	}
	
	public void setAdjacencyMatrix(Map<Integer, Set<Integer>> _adjacencyMatrix) {
		this.adjacencyMatrix = _adjacencyMatrix;
	}
	public Map<Integer, Set<Integer>> getAdjacencyMatrix() {
		return this.adjacencyMatrix;
	}
	
	public void setDataAdjacencyMatrix(Map<Integer, Set<Integer>> _adjacencyMatrix) {
		this.dataAdjacencyMatrix = _adjacencyMatrix;
	}
	public Map<Integer, Set<Integer>> getDataAdjacencyMatrix() {
		return this.dataAdjacencyMatrix;
	}
	
	public void setWidth (double _width) {
		this.width = _width;
	}
	public double getWidth() {
		return this.width;
	}
	
	public void setHeight (double _height) {
		this.height = _height;
	}
	public double getHeight() {
		return this.height;
	}
	
	public void setNodesNumber (int _number) {
		this.nodesNumber = _number;
	}
	public int getNodesNumber() {
		return this.nodesNumber;
	}
	
	public void setDataNodesNumber (int _number) {
		this.dataNodesNumber = _number;
	}
	public int getDataNodesNumber() {
		return this.dataNodesNumber;
	}
	
	public void setTransmissionRange(int range) {
		this.transmissionRange = range;
	}
	public int getTransmissionRange() {
		return this.transmissionRange;
	}
	
	public void setNumberR(int _number) {
		this.numberR = _number;
	}
	public int getNumberR() {
		return this.numberR;
	}
	
//	public void setNumberr(int _number) {
//		this.numberr = _number;
//	}
//	public int getNumberr() {
//		return this.numberr;
//	}
	
	public void setBeta(double _number) {
		this.beta = _number;
	}
	public double getBeta() {
		return this.beta;
	}
	
	public void setNumberm(int _number) {
		this.numberm = _number;
	}
	public int getNumberm() {
		return this.numberm;
	}

	public void setNumberq(int _number) {
		this.numberq = _number;
	}
	public int getNumberq() {
		return this.numberq;
	}
}
