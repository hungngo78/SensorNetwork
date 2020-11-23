package sensor_network.distributed.ghs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.util.List;
import java.util.Iterator;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import sensor_network.distributed.ghs.data.Axis;
import sensor_network.distributed.ghs.data.Cluster;
import sensor_network.distributed.ghs.data.Edge;
import sensor_network.distributed.ghs.data.Node;
import sensor_network.distributed.ghs.graph.MinimumSpanningTreeGraph;
import sensor_network.distributed.ghs.graph.MinimumSpanningTreeGraph.Edge1;
import sensor_network.distributed.ghs.graph.MinimumSpanningTreeGraph.Node1;
import sensor_network.distributed.ghs.utility.Constant;

public class MinimumSpanningTree {
	public static MinimumSpanningTreeGraph gp = null;

	@SuppressWarnings("static-access")
	public static void initGraph() {
		if (!Constant.DRAW_MINIMUM_SPANNING_TREE_GRAPH)
			return;
		
		// get screen height
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int screenHeight = (int) Math.round(screenSize.getHeight());
		
		JFrame f = new JFrame("Minimum spanning tree (Distributed)");
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		gp = new MinimumSpanningTreeGraph();
		gp.setPreferredSize(new Dimension(screenHeight-30, screenHeight-30));
		
		for (Node n : Network.getNodes()) {
			if (!n.axis.isDataNode())
				n.axis.setPainted(false);
			else
				n.axis.setPainted(true);
		}
		
		//f.add(gp.control, BorderLayout.NORTH);
		f.add(new JScrollPane(gp), BorderLayout.CENTER);
		//f.getRootPane().setDefaultButton(gp.control.defaultButton);
		f.pack();
		f.setLocationByPlatform(true);
		f.setVisible(true);
		
		updateGraph();
	}
	
	public static void updateGraph() {
		if (gp == null)
			return;
		
		SwingUtilities.invokeLater(new Runnable() {
			@SuppressWarnings("static-access")
			@Override
			public void run() {
				if (gp.edges == null || gp.nodes == null)
					return;
				
				gp.nodes.clear();
				updateNode();
				
				gp.edges.clear();
				
				List<Cluster> clusters = Network.getClusters();
				Iterator<Cluster> ite = clusters.iterator();
				while (ite.hasNext()) {
					try {
						Cluster c= ite.next();
						
						if (c.color == null)
							c.color = new Color(gp.rnd.nextInt());
						
						for (Node n1 : c.getNodes()) {
							for (Node1 n2 : gp.nodes) {
								if (n2.n == n1) {
									n2.color = c.color;
								}
							}
						}
						
						List<Edge> spanningEdges = Network.getSpanningEdges();
						Iterator<Edge> it = spanningEdges.iterator();
						while (it.hasNext()) {
							Edge e = it.next();
							
							Node n1 = null, n2 = null;
							Node1 n3 = null, n4 = null;
							for (Node temp : c.getNodes()) {
								if (temp.getId() == e.id1)
									n1 = temp;
							}
							for (Node temp : c.getNodes()) {
								if (temp.getId() == e.id2)
									n2 = temp;
							}
							if (n1 == null || n2 == null)
								continue;
							
							for (Node1 temp : gp.nodes) {
								if (temp.n == n1)
									n3 = temp;
							}
							
							for (Node1 temp : gp.nodes) {
								if (temp.n == n2)
									n4 = temp;
							}
							
							if (n3 == null || n4 == null)
								continue;
							
							gp.edges.add(new Edge1(n3, n4));
						}
					} catch (java.util.ConcurrentModificationException e) {
						e.printStackTrace();
					} catch (Throwable throwable) {
			            // Catch any other Throwables.
			        }
				}
				
				gp.repaint();
			}
		});
	}

	private static void updateNode() {
		gp.nodes.clear();
		
		for (Node n : Network.getNodes()) {
			if (!n.axis.isPainted())
				continue;
			
			Axis p = n.axis;
			gp.nodes.add(new Node1(p, null, n, n.getId()));
		}
	}
}
