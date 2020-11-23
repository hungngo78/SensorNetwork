package sensor_network.distributed.naive.graph;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JPanel;

import sensor_network.distributed.naive.Network;
import sensor_network.distributed.naive.data.Axis;
import sensor_network.distributed.naive.data.Node;


public class MinimumSpanningTreeGraph extends JPanel implements Runnable {
	private static final long serialVersionUID = 1L;
	
	public static final Random rnd = new Random();
	
	public List<Node1> nodes = new ArrayList<Node1>();
	public List<Edge1> edges = new ArrayList<Edge1>();
	
    private static int scaling = 25;
    private static int ovalSize = 10;
    private static int gridCount = 10;
    private static double xScale = 1;
    private static double yScale = 1; 
    private Map<Integer, Set<Integer>> adjList;
    
	public Map<Integer, Set<Integer>> getAdjList() {
		return adjList;
	}

	public void setAdjList(Map<Integer, Set<Integer>> adjList) {
		this.adjList = adjList;
	}
	
	@Override
	protected void paintComponent(Graphics g) {
       super.paintComponent(g);
       Graphics2D g2 = (Graphics2D) g;
       g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
       
       double width = Network.getGraph().getWidth();
       double height = Network.getGraph().getHeight();

       xScale =  ( (getWidth() - 3 * scaling) / (width));
       yScale =   (( getHeight() - 3 * scaling) / (height));
       
       g2.setColor(Color.white);
       g2.fillRect(2*scaling, scaling, getWidth() - (3 * scaling), getHeight() - 3 * scaling);
       g2.setColor(Color.black);

       for (int i = 0; i < gridCount + 1; i++) {
           int x0 = 2*scaling;
           int x1 = ovalSize + (2*scaling);
           int y0 = getHeight() - ((i * (getHeight() - (3*scaling))) / gridCount + (2*scaling));
           int y1 = y0;
           if (nodes.size() > 0) {
               g2.setColor(Color.black);
               g2.drawLine((2*scaling) + 1 + ovalSize, y0, getWidth() - scaling, y1);
               String yLabel = ((int) ((height * ((i * 1.0) / gridCount)) * 100)) / 100 + "";
               FontMetrics metrics = g2.getFontMetrics();
               int labelWidth = metrics.stringWidth(yLabel);
               g2.drawString(yLabel, x0 - labelWidth - 5, y0 + (metrics.getHeight() / 2) - 3);
           }
           g2.drawLine(x0, y0, x1, y1);
       }

       for (int i = 0; i < gridCount + 1; i++) {
           int x0 = i * (getWidth() - (scaling * 3)) / gridCount+ (2*scaling);
           int x1 = x0;
           int y0 = getHeight() - (2*scaling);
           int y1 = y0 - ovalSize;
           if ( (i == (gridCount/2)) || (i == gridCount) ) {
               if (nodes.size() > 0) {
                   g2.setColor(Color.black);
                   g2.drawLine(x0, getHeight() - (2*scaling) - 1 - ovalSize, x1, scaling);
                   String xLabel = ((int) ((width * ((i * 1.0) / gridCount)) * 100)) / 100 + "";
                   FontMetrics metrics = g2.getFontMetrics();
                   int labelWidth = metrics.stringWidth(xLabel);
                   g2.drawString(xLabel, x0 - labelWidth / 2, y0 + metrics.getHeight() + 3);
                   
               }
               g2.drawLine(x0, y0, x1, y1);
           }
       }
       
       for (Edge1 e : edges) {
			e.draw(g);
		}
		for (Node1 n : nodes) {
			n.draw(g);
		}
   }

	public void run() {
		String graphName= "Aggregation Graph";
		JFrame frame = new JFrame(graphName);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().add(this);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
   }
	

	/**
	 * An Edge is a pair of Nodes.
	 */
	public static class Edge1 {

		public Node1 n1;
		public Node1 n2;
		public Color c;

		public Edge1(Node1 n1, Node1 n2) {
			this.n1 = n1;
			this.n2 = n2;
			c = Color.darkGray;
		}

		public void draw(Graphics g) {
		    double height = Network.getGraph().getHeight();
		       
			Axis p1 = n1.p;
			Axis p2 = n2.p;
			
			double x1 = ( p1.getxAxis() * (xScale) + (2*scaling));
            double y1 =  ((height - p1.getyAxis())  * yScale + scaling );
    	    
    	    double x2 = ( p2.getxAxis() * (xScale) + (2*scaling));
            double y2 =  ((height - p2.getyAxis())  * yScale + scaling );
			
			Graphics2D g2 = (Graphics2D) g;
			//Stroke s = g2.getStroke();
			//g2.setStroke(new BasicStroke(5));
			g2.setColor(c);
			g2.drawLine((int)x1, (int)y1, (int)x2, (int)y2);
			//g2.setStroke(s);
		}
	}

	/**
	 * A Node represents a node in a graph.
	 */
	public static class Node1 {
		public Node n;
		public Axis p;
		public Color color;
		
		public int id = 0;

		/**
		 * Construct a new node.
		 */
		public Node1(Axis p, Color color, Node n, int _id) {
			this.p = p;
			this.color = color;
			this.n = n;
			this.id = _id;
		}
		
		public Node1(Axis p, Color color, Node n) {
			this.p = p;
			this.color = color;
			this.n = n;
		}

		/**
		 * Draw this node.
		 */
		public void draw(Graphics g) {
			Graphics2D g2 = (Graphics2D) g;
			g2.setColor(this.color);
			
		    double height = Network.getGraph().getHeight();
			double x1 = ( p.getxAxis() * (xScale) + (2*scaling));
            double y1 =  ((height - p.getyAxis())  * yScale + scaling );
           
    	    int x = (int) (x1 - ovalSize / 2);
    	    int y = (int) (y1 - ovalSize / 2);
    	    Ellipse2D.Double shape = new Ellipse2D.Double(x, y, ovalSize, ovalSize);
    	    g2.fill(shape);
    	    g2.drawString(""+id, x-5, y-5);
		}

		/**
		 * Return this node's location.
		 */
		public Axis getLocation() {
			return p;
		}

		/**
		 * Update each node's color.
		 */
		public static void updateColor(List<Node1> list, Color color) {
			for (Node1 n : list) {
				n.color = color;
			}
		}

	}
}