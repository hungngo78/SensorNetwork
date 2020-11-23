package sensor_network.distributed.naive.data;

public class Commands {
	// step 1 commands
	public static final int ROUTING = 1;
	
	// step 2 commands
	public static final int CONNECT = 2;
	public static final int ACCEPT = 3;
	public static final int REJECT = 4;
	public static final int LEADER_ELECTION = 5;
	public static final int LEADER_ELECTION_REPLY = 6;
	public static final int LEADER_ELECTION_REQUEST = 7;
	
	public static final String SEPERATOR = ":";

	public static String makeCommand(int senderId, String message) {
		return senderId + SEPERATOR + message;
	}

	public static String[] breakCommand(String command) {
		return command.split(SEPERATOR);
	}
}
