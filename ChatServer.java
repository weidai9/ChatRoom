import java.io.*;
import java.net.*;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ChatServer {
	public ServerSocket serversocket;
	public ConcurrentHashMap<String, String> credentials;
	public ConcurrentHashMap<String, Long> blockTimeMap;
	public ConcurrentHashMap<String, String> onlineUserIp;
	public ConcurrentHashMap<String, Integer> onlineUserReceiverPort;
	public ConcurrentHashMap<String, Long> lastBeatingTime;
	public ConcurrentHashMap<String, Set<String>> blockset;
	public ConcurrentHashMap<String, Set<String>> pendingIpRequest;
	public ConcurrentHashMap<String, List<String>> offlinemsg;
	public long BLOCK_TIME = 60000;
	public long TIME_OUT = 60000;
	public int login_interval = 30;
	public ChatServer(int portnum){
		initServer(portnum);
	}
	
	public void initServer(int portnum){
		/*
		 * Start the server, and read in the credentials and
		 * configurations
		 */
		credentials = new ConcurrentHashMap<String, String>();
		blockTimeMap = new ConcurrentHashMap<String, Long>();
		onlineUserIp = new ConcurrentHashMap<String, String>();
		onlineUserReceiverPort = new ConcurrentHashMap<String, Integer>();
		lastBeatingTime = new ConcurrentHashMap<String, Long>();
		blockset = new ConcurrentHashMap<String, Set<String>>();
		pendingIpRequest= new ConcurrentHashMap<String, Set<String>>();
		offlinemsg = new ConcurrentHashMap<String, List<String>>();
		try {
			/* load users and their passwords */
			FileInputStream credfile = new FileInputStream(new File("credentials.txt"));
			BufferedReader br1 = new BufferedReader(new InputStreamReader(credfile));
			String line = null;
			while ((line = br1.readLine()) != null) {
				line = line.trim();
				String [] words = line.split(" ");
				if (words.length>2){
					System.out.println("Error: username should not contain space!");
					System.exit(1);
				}
				credentials.put(words[0], words[1]);
			}
			br1.close();
			
			/* load configurations */
			FileInputStream conffile = new FileInputStream(new File("configurations.txt"));
			BufferedReader br2 = new BufferedReader(new InputStreamReader(conffile));
			while ((line = br2.readLine()) != null) {
				String [] words = line.split(" ");
				if (words[0].equals("login_interval")){
					login_interval = Integer.parseInt(words[1]);
				} else if (words[0].equals("block_time")){
					BLOCK_TIME = Long.parseLong(words[1]);
				} else if (words[0].equals("time_out")){
					TIME_OUT = Long.parseLong(words[1]);
				}
			}
			br2.close();
			
			serversocket = new ServerSocket(portnum);
			
		} catch (Exception e) {e.printStackTrace();}
		

	}
	
	public static void main(String[] args) throws IOException {
		if (args.length != 1){
			System.out.println("Invalid number of arguements!");
			return;
		}
		int portnum = Integer.parseInt(args[0]);
		ChatServer server = new ChatServer(portnum);
		InetAddress ipaddr = server.serversocket.getInetAddress();
		System.out.println("Server IP: "+ipaddr.getHostAddress());
		
		boolean cond = true;
		while(cond){
			Socket clientsocket = server.serversocket.accept();
			ServerWorker serverworker = new ServerWorker(server, clientsocket);
			serverworker.start();
		}
		
		server.serversocket.close();
	}
	

}
