import java.io.*;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChatClient {
	public String hostip;
	public int hostport;
	public String username;
	public long beatInterval = 30000;
	public ConcurrentHashMap<String, String> userIp;
	public ConcurrentHashMap<String, Integer> userPort;
	ChatClientReceiver msgreceiver;
	ChatClientHeartBeat heartbeater;
	public ChatClient(String ip, String portstr){
		hostport = Integer.parseInt(portstr);
		hostip = ip;
		userIp = new ConcurrentHashMap<String, String>();
		userPort = new ConcurrentHashMap<String, Integer>();
	}
	
	public boolean loginAuth(){
		try {
			Socket clientsocket = new Socket(this.hostip, this.hostport);
			BufferedReader stdin = new  BufferedReader(new InputStreamReader(System.in));
			BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientsocket.getInputStream()));
			DataOutputStream outToServer = new DataOutputStream(clientsocket.getOutputStream());
			/*
			 * Send login request
			 */
			outToServer.writeBytes("99 \n");
			
			System.out.print("> Username: ");
			String inputUserName = stdin.readLine().trim();
			outToServer.writeBytes("00 "+inputUserName+"\n");
			this.username = inputUserName;
			String replymsg = inFromServer.readLine();
			String [] parsemsg = Message.parse(replymsg);
			if(parsemsg[1].equals("username_error")){
				System.out.println("Invalid username.");
				clientsocket.close();
				return false;
			}
			if(parsemsg[1].equals("server_blocked")){
				System.out.println("Your account has been blocked. Please try again after sometime.");
				clientsocket.close();
				return false;
			}
			System.out.print("> Password: ");
			String inputpassword = stdin.readLine().trim();
			outToServer.writeBytes("01 "+inputpassword+"\n");
			replymsg = inFromServer.readLine();
			parsemsg = Message.parse(replymsg);
			while(!parsemsg[1].equals("password_ok")&&!parsemsg[1].equals("server_blocked")){
				System.out.println("Invalid password. Please try again.");
				System.out.print("> Password: ");
				inputpassword = stdin.readLine();
				outToServer.writeBytes("01 "+inputpassword+"\n");
				replymsg = inFromServer.readLine();
				parsemsg = Message.parse(replymsg);
			}
			if (parsemsg[1].equals("password_ok")){
				outToServer.writeBytes("03 "+msgreceiver.receiversocket.getLocalPort()+"\n");
				clientsocket.close();
				return true;
			}
			else{
				System.out.println("Your account has been blocked. Please try again after sometime.");
				clientsocket.close();	
				return false;
			}
			
		} catch (Exception e) {
			System.out.println("Connection failed. Exit...");
			System.exit(-1);
		} 
		return true;
	}
	
	public static void main(String[] args) throws IOException {
		if (args.length != 2){
			System.out.println("Invalid number of parameters! [ip] [server port] ");
			return;
		}
		String hostip = args[0];
		String port = args[1];
		ChatClient client = new ChatClient(hostip, port);

		client.msgreceiver = new ChatClientReceiver(client);
		client.msgreceiver.start();
		boolean loginsuccess = client.loginAuth();
		
		if (loginsuccess){
			client.heartbeater = new ChatClientHeartBeat(client);
			client.heartbeater.start();
			System.out.println("Welcome to simple chat server!");
		} else{
			client.msgreceiver.stopRunning();
			System.out.println("Login fail...Exit.");
			return;
		}
		
		client.runCommands();
		
		System.exit(0);
	}
	
	public void runCommands() throws IOException{
		BufferedReader stdin = new  BufferedReader(new InputStreamReader(System.in));
		while(true){
			System.out.print("> ");
			String command = stdin.readLine();
			if(!checkCommand(command)){
				System.out.println("Invalid command syntax.");
				continue;
			}
			String [] inputs = parseCommand(command);
			if(inputs[0].equals("message")){
				/*
				 *  message <user> <message>
				 */
				String msg = "05 "+username+" "+inputs[1]+" "+inputs[2]+"\n";
				String [] resp = sendAndGet(msg);
				if (resp[1].equals("USERNAME_ERROR")){
					System.out.println("Error: no such user name!");
				} else if (resp[1].equals("TARGET_OFFLINE")){
					System.out.println("The user is offline. Offline message is sent.");
				} else if (resp[1].equals("INVALID_SESSION")){
					System.out.println("Your account is offline. Exit...");
					prepareExit();
					break;
				} else if (resp[1].equals("TIME_OUT")){
					System.out.println("Time out. You are offline now. Exit...");
					prepareExit();
					break;
				} else if (resp[1].equals("MESSAGE_BLOCKED")){
					System.out.println("Fail to send: You are blocked by ["+inputs[1]+"]");
				}
			} else if (inputs[0].equals("block")||inputs[0].equals("unblock")){
				/*
				 * block <user>
				 * unblock <user>
				 */
				String msg = "08 "+username+" "+inputs[1]+" "+inputs[0]+"\n";
				String [] resp = sendAndGet(msg);
				if (resp[1].equals("USERNAME_ERROR")){
					System.out.println("Fail: no such user name!");
				} else if (resp[1].equals("INVALID_SESSION")){
					System.out.println("Your account is offline. Exit...");
					prepareExit();
					break;
				} else if (resp[1].equals("TIME_OUT")){
					System.out.println("Time out. You are offline now. Exit...");
					prepareExit();
					break;
				} else if (resp[1].equals("UNBLOCK_FAIL")){
					System.out.println("The user is not in your blacklist.");
				} else if (resp[1].equals("OK")) {
					System.out.println(inputs[1]+" has been "+inputs[0]+"ed");
				}
			} else if (inputs[0].equals("logout")){
				/*
				 * logout
				 */
				String msg = "09 "+username+"\n";
				sendMessageToServer(msg);
				prepareExit();
				System.out.println("Logout succeed! Exit...");
				break;
			} else if (inputs[0].equals("online")){
				/*
				 * online
				 */
				String msg = "10 "+username+"\n";
				String [] resp = sendAndGet(msg);
				if (resp[0].equals("06")&&resp[1].equals("INVALID_SESSION")){
					System.out.println("Your account is offline. Exit...");
					prepareExit();
					break;
				} else if (resp[0].equals("06")&&resp[1].equals("TIME_OUT")){
					System.out.println("Time out. You are offline now. Exit...");
					prepareExit();
					break;
				} else if (resp[0].equals("11")){
					System.out.println("Online users: "+resp[1]);
				} 
			} else if (inputs[0].equals("broadcast")){
				/*
				 * broadcast <message>
				 */
				String msg = "12 "+username+" "+inputs[1]+"\n";
				String [] resp = sendAndGet(msg);
				if (resp[0].equals("06")&&resp[1].equals("INVALID_SESSION")){
					System.out.println("> Your account is offline. Exit...");
					prepareExit();
					break;
				} else if (resp[0].equals("06")&&resp[1].equals("TIME_OUT")){
					System.out.println("> Time out. You are offline now. Exit...");
					prepareExit();
					break;
				} else if (resp[0].equals("13")&&resp[1].equals("BLOCKED")){
					System.out.println("> Your message could not be delivered to some recipients: "+resp[2]);
				}
			} else if (inputs[0].equals("getaddress")){
				/*
				 * getaddress <user>
				 */
				String msg = "14 "+username+" "+inputs[1]+"\n";
				String [] resp = sendAndGet(msg);
				if (resp[0].equals("06")&&resp[1].equals("INVALID_SESSION")){
					System.out.println("> Your account is offline. Exit...");
					prepareExit();
					break;
				} else if (resp[0].equals("06")&&resp[1].equals("TIME_OUT")){
					System.out.println("> Time out. You are offline now. Exit...");
					prepareExit();
					break;
				} else if (resp[0].equals("06")&&resp[1].equals("MESSAGE_BLOCKED")){
					System.out.println("> You are blocked by: "+inputs[1]+". failed to get address.");
				} else if (resp[0].equals("06")&&resp[1].equals("TARGET_OFFLINE")){
					System.out.println("> The user is offline. Offline request is sent.");
					// TODO add offline receive feature
				} else  if (resp[1].equals("OK")) {
					System.out.println("> Your request has been sent, and you will get the address after the user's permission");
				}
			} else if (inputs[0].equals("agree")||inputs[0].equals("deny")){
				/*
				 * agree <username>
				 * deny <username>
				 */
				String msg = "15 "+username+" "+ inputs[1]+" "+inputs[0]+"\n";
				String [] resp = sendAndGet(msg);
				if (resp[0].equals("06")&&resp[1].equals("INVALID_SESSION")){
					System.out.println("Your account is offline. Exit...");
					prepareExit();
					break;
				} else if (resp[0].equals("06")&&resp[1].equals("TIME_OUT")){
					System.out.println("Time out. You are offline now. Exit...");
					prepareExit();
					break;
				} else if (resp[0].equals("06")&&resp[1].equals("TARGET_OFFLINE")){
					System.out.println("The user is offline. Offline reply is sent.");
				} else if (resp[0].equals("06")&&resp[1].equals("NO_REQUEST")) {
					System.out.println("Error: no such request.");
				} else if (resp[1].equals("USERNAME_ERROR")){
					System.out.println("Error: no such user name!");
				}
			} else if (inputs[0].equals("private")){
				/*
				 * private <user> <message>
				 */
				if (userIp.containsKey(inputs[1])){
					Socket sock = null;
					try{
						sock = new Socket(userIp.get(inputs[1]), userPort.get(inputs[1]));
						String msg = "17 "+username+" "+inputs[2]+"\n";
						DataOutputStream outPrivate = new DataOutputStream(sock.getOutputStream());
						outPrivate.writeBytes(msg);
						sock.close();
					} catch (Exception e){
						System.out.println("The user is offline. Fail to send private message.\n Please user <message> to send offline message.");
					}
					
				}
			}
		}
	}
	
	public void prepareExit(){
		msgreceiver.stopRunning();
		heartbeater.stopBeating();
		try {
			msgreceiver.receiversocket.close();
		} catch (IOException e) {}
	}
	
	public boolean checkCommand(String command){
		command = command.trim();
		String [] inputs = command.split(" ");
		int len = inputs.length;
		if(len==1 && (inputs[0].equals("online")||inputs[0].equals("logout"))){
			return true;
		} else if(len >= 2 && inputs[0].equals("broadcast")){
			return true; 
		} else if(len >= 3 && (inputs[0].equals("message")||inputs[0].equals("private"))) {
			return true;
		} else if(len == 2){
			if (inputs[0].equals("block")||inputs[0].equals("unblock")){
				if (inputs[1].equals(username)){
					System.out.println("You cannot block or unblock yourself!");
					return false;
				}
				return true;
			} else if (inputs[0].equals("getaddress")||inputs[0].equals("agree")||inputs[0].equals("deny")){
				return true;
			}
		}
		
		return false;
	}
	
	public String [] parseCommand(String command){
		command = command.trim();
		String [] inputs = command.split(" ");
		int len = inputs.length;
		String [] ret = null;
		if(len==1 && (inputs[0].equals("online")||inputs[0].equals("logout"))){
			ret = new String[1];
			ret[0] = inputs[0];
		} else if(len >= 2 && inputs[0].equals("broadcast")){
			ret = new String[2];
			ret[0] = inputs[0];
			ret[1] = command.substring(10); 
		} else if(len >= 3 && (inputs[0].equals("message")||inputs[0].equals("private"))) {
			ret = new String[3];
			ret[0] = inputs[0];
			ret[1] = inputs[1];
			ret[2] = command.substring(ret[0].length()+ret[1].length()+2);
		} else if(len == 2){
			if (inputs[0].equals("block")||inputs[0].equals("unblock")||inputs[0].equals("getaddress")
					||inputs[0].equals("agree")||inputs[0].equals("deny")){
				ret = new String[2];
				ret[0] = inputs[0];
				ret[1] = inputs[1];
			} 
		}
		return ret;
	}
	
	public void sendMessageToServer(String message){
		Socket clientsocket;
		try {
			clientsocket = new Socket(this.hostip, this.hostport);
			DataOutputStream outToServer = new DataOutputStream(clientsocket.getOutputStream());
			outToServer.writeBytes(message);
			clientsocket.close();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	public String [] sendAndGet(String message){
		Socket clientsocket;
		String [] response = null;
		try {
			clientsocket = new Socket(this.hostip, this.hostport);
			DataOutputStream outToServer = new DataOutputStream(clientsocket.getOutputStream());
			BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientsocket.getInputStream()));
			outToServer.writeBytes(message);
			String res = inFromServer.readLine();
			clientsocket.close();
			response = Message.parse(res);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return response;
	}
	

}
