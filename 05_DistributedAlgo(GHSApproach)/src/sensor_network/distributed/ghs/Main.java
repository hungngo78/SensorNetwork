package sensor_network.distributed.ghs;

import java.io.File;
import java.util.Scanner;

public class Main {
	public static void main(String args[]) throws Exception {
		/*Scanner scan = new Scanner(System.in);
		
		System.out.print("Enter the number of nodes (Ex: 10): ");
		int numberOfNodes = scan.nextInt();
		
		System.out.print("Enter the number of data nodes (Ex: 6): ");
		int numberOfDataNodes = scan.nextInt();
		
		Network.initialise(numberOfNodes, numberOfDataNodes);*/
		
	
		//String nodesPath = "C:\\02_DH\\99_LuanVanTotNghiep\\12_tmp\\graph.dat";
		//String nodesPath = "C:\\02_DH\\99_LuanVanTotNghiep\\12_tmp\\performance_report_6\\graph.dat";
		
		//String nodesPath = "C:\\02_DH\\99_LuanVanTotNghiep\\12_tmp\\performance_report_2\\55_data_nodes\\graph.dat";
		//String nodesPath = "C:\\02_DH\\99_LuanVanTotNghiep\\12_tmp\\performance_report_2\\60_data_nodes\\graph.dat";
		//String nodesPath = "C:\\02_DH\\99_LuanVanTotNghiep\\12_tmp\\performance_report_2\\65_data_nodes\\graph.dat";
		//String nodesPath = "C:\\02_DH\\99_LuanVanTotNghiep\\12_tmp\\performance_report_6\\70_data_nodes\\graph.dat";
		//String nodesPath = "C:\\02_DH\\99_LuanVanTotNghiep\\12_tmp\\performance_report_6\\71_data_nodes\\graph.dat";
		
		String nodesPath = 	"/Users/hungngo/data/performance_report_15/55/graph.dat";
		//String nodesPath = 	"/Users/hungngo/data/performance_report_12/60/graph.dat";
		//String nodesPath = 	"/Users/hungngo/data/performance_report_15/65/graph.dat";
		//String nodesPath = 	"/Users/hungngo/data/performance_report_11/70/graph.dat";
		//String nodesPath = 	"/Users/hungngo/data/performance_report_14/71/graph.dat";
		
		Network.initialise(nodesPath);
		
		Network.startGHS();
		
		// start monitoring
		new MonitoringService().start();
	}

	public static double[][] loadGraph(String file) throws Exception {
		Scanner scanner = new Scanner(new File(file));
		int N = scanner.nextInt();
		double[][] adjcencyM = new double[N][N];
		for (int i = 0; i < N; i++) {
			for (int j = 0; j < N; j++) {
				adjcencyM[i][j] = scanner.nextDouble();
			}
		}
		scanner.close();
		return adjcencyM;
	}
}
