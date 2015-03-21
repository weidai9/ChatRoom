import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

public class ServerWorker extends Thread {
	public ChatServer server;
	public Socket clientsocket;
	public String username;
	public ServerWorker(ChatServer server, Socket clientsocket){
		this.server = server;
		this.clientsocket = clientsocket;
	}
	@Override
	public void run() {
		try {
			DataOutputStream outToClient = new DataOutputStream(clientsocket.getOutputStream());
			BufferedReader inFromClient = new BufferedReader(new InputStreamReader(clientsocket.getInputStream()));
			String requestmsg = inFromClient.readLine();
			String original = requestmsg;
			String [] message = Message.parse(requestmsg);
			String requesttype = message[0];
			if(requesttype.equals("99")){
				if (loginAuth()){
					String ip = server.onlineUserIp.get(username);
					int port = server.onlineUserReceiverPort.get(username);
					if (server.offlinemsg.containsKey(username)&&server.offlinemsg.get(username).size()>0){
						for(String msg : server.offlinemsg.get(username)){
							Socket socket = new Socket(ip, port);
							DataOutputStream outToRec = new DataOutputStream(socket.getOutputStream());
							outToRec.writeBytes(msg);
							socket.close();
						}
						server.offlinemsg.get(username).clear();
					} else {
						
						Socket socket = new Socket(ip, port);
						DataOutputStream outToRec = new DataOutputStream(socket.getOutputStream());
						outToRec.writeBytes("04 [Server]: No offline message for you.\n");
						socket.close();
					}
				}
			} else if (requesttype.equals("05")){
				/*
				 * "05 [from] [to] [data]"
				 */
				if(checkIpPortValid(message[1])){
					if (isTimeOut(message[1])){
						userLogout(message[1]);
						outToClient.writeBytes("06 TIME_OUT\n");
					} if (!server.credentials.containsKey(message[2])){
						outToClient.writeBytes("06 USERNAME_ERROR\n");
					} else if (isBlocked(message[1], message[2])){
						outToClient.writeBytes("06 MESSAGE_BLOCKED\n");
					} else if(!server.onlineUserIp.containsKey(message[2])){
						if(!server.offlinemsg.containsKey(message[2])){
							server.offlinemsg.put(message[2], Collections.synchronizedList(new ArrayList<String>()));
						}
						server.offlinemsg.get(message[2]).add(original);
						outToClient.writeBytes("06 TARGET_OFFLINE\n");
					} else {
						String recip = server.onlineUserIp.get(message[2]);
						int recport = server.onlineUserReceiverPort.get(message[2]);
						try {
							Socket sock = new Socket(recip, recport);
							DataOutputStream outToRec = new DataOutputStream(sock.getOutputStream());
							outToRec.writeBytes("05 "+message[1]+" "+message[2]+" "+message[3]+"\n");
							outToClient.writeBytes("06 OK\n");
							sock.close();
						}	catch(ConnectException e) {
							// guaranteed message delivery
							if(!server.offlinemsg.containsKey(message[2])){
								server.offlinemsg.put(message[2], Collections.synchronizedList(new ArrayList<String>()));
							}
							server.offlinemsg.get(message[2]).add(original);
							outToClient.writeBytes("06 TARGET_OFFLINE\n");
							userLogout(message[2]);
						}
					}
				}
			} else if (requesttype.equals("07")){
				/*
				 * Heart beat of client
				 */
				server.lastBeatingTime.put(message[1], System.currentTimeMillis());
			} else if (requesttype.equals("08")){
				/*
				 * "08 [from] [target] block/unblock"
				 */
				if(checkIpPortValid(message[1])){
					if (!server.credentials.containsKey(message[2])){
						outToClient.writeBytes("06 USERNAME_ERROR\n");
					} else if (isTimeOut(message[1])){
						userLogout(message[1]);
						outToClient.writeBytes("06 TIME_OUT\n");
					} else {
						if (message[3].equals("block")){
							if(!server.blockset.containsKey(message[1])){
								server.blockset.put(message[1], Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>()));
							}
							server.blockset.get(message[1]).add(message[2]);
							outToClient.writeBytes("06 OK\n");
						} else if (message[3].equals("unblock")) {
							if(server.blockset.containsKey(message[1])&&server.blockset.get(message[1]).contains(message[2])){
								server.blockset.get(message[1]).remove(message[2]);
								outToClient.writeBytes("06 OK\n");
							} else {
								outToClient.writeBytes("06 UNBLOCK_FAIL\n");
							}
						}
					}
				}
			} else if (requesttype.equals("09")){
				/* 
				 * logout:
				 * 09 [username]
				 */
				userLogout(message[1]);
			} else if (requesttype.equals("10")){
				/*
				 * online
				 */
				if(checkIpPortValid(message[1])){
					if (isTimeOut(message[1])){
						userLogout(message[1]);
						outToClient.writeBytes("06 TIME_OUT\n");
					} else {
						String ret = "";
                                                for (String user : server.onlineUserIp.keySet()){
                                                    if (!isTimeOut(user)){
                                                        ret += "["+user+"] ";
                                                    } else {
                                                        userLogout(user);
                                                    }
                                                }
						outToClient.writeBytes("11 "+ret+"\n");
					}
				}
			} else if (requesttype.equals("12")) {
				/*
				 * broadcast:
				 * "12 [from] [data]"
				 */
				if(checkIpPortValid(message[1])){
					if (isTimeOut(message[1])){
						userLogout(message[1]);
						outToClient.writeBytes("06 TIME_OUT\n");
					} else {
						broadcastMessage(message[1], original);
					}
				}
			} else if (requesttype.equals("14")){
				/*
				 * request user ip address:
				 * "14 from target"
				 */
				if(checkIpPortValid(message[1])){
					if (isTimeOut(message[1])){
						userLogout(message[1]);
						outToClient.writeBytes("06 TIME_OUT\n");
					} else if (!server.credentials.containsKey(message[2])){
						outToClient.writeBytes("06 USERNAME_ERROR\n");
					} else if (isBlocked(message[1], message[2])){
						outToClient.writeBytes("06 MESSAGE_BLOCKED\n");
					} else if(!server.onlineUserIp.containsKey(message[2])){
						// create the pending request
						if (!server.pendingIpRequest.containsKey(message[2])){
							server.pendingIpRequest.put(message[2], Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>()));
						}
						server.pendingIpRequest.get(message[2]).add(message[1]);
						// put the offline request in the queue
						if(!server.offlinemsg.containsKey(message[2])){
							server.offlinemsg.put(message[2], Collections.synchronizedList(new ArrayList<String>()));
						}
						server.offlinemsg.get(message[2]).add(original);
						outToClient.writeBytes("06 TARGET_OFFLINE\n");
					} else {
						try {
							String recip = server.onlineUserIp.get(message[2]);
							int recport = server.onlineUserReceiverPort.get(message[2]);
							Socket sock = new Socket(recip, recport);
							DataOutputStream outToRec = new DataOutputStream(sock.getOutputStream());
							outToRec.writeBytes(original);
							if (!server.pendingIpRequest.containsKey(message[2])){
								server.pendingIpRequest.put(message[2], Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>()));
							}
							server.pendingIpRequest.get(message[2]).add(message[1]);
							outToClient.writeBytes("06 OK\n");
							sock.close();
						} catch (ConnectException e){
							// Guaranteed Message Delivery
							// create the pending request
							if (!server.pendingIpRequest.containsKey(message[2])){
								server.pendingIpRequest.put(message[2], Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>()));
							}
							server.pendingIpRequest.get(message[2]).add(message[1]);
							// put the offline request in the queue
							if(!server.offlinemsg.containsKey(message[2])){
								server.offlinemsg.put(message[2], Collections.synchronizedList(new ArrayList<String>()));
							}
							server.offlinemsg.get(message[2]).add(original);
							userLogout(message[2]);
							outToClient.writeBytes("06 TARGET_OFFLINE\n");
						}
					}
				}
			} else if (requesttype.equals("15")){
				/*
				 * reply to the address request:
				 * "15 [replier] [sender] agree"
				 * "15 [replier] [sender] deny"
				 */
				if(checkIpPortValid(message[1])){
					if (isTimeOut(message[1])){
						userLogout(message[1]);
						outToClient.writeBytes("06 TIME_OUT\n");
					} else if (!server.credentials.containsKey(message[2])){
						outToClient.writeBytes("06 USERNAME_ERROR\n");
					} else if(!server.pendingIpRequest.containsKey(message[1]) || 
							!server.pendingIpRequest.get(message[1]).contains(message[2])){
						outToClient.writeBytes("06 NO_REQUEST\n");
					} else if(!server.onlineUserIp.containsKey(message[2])){
						String save = "";
						if(message[3].equals("agree")){
							save = "16 "+message[1]+" agree "+server.onlineUserIp.get(message[1])+" "+server.onlineUserReceiverPort.get(message[1])+"\n";
						} else if (message[3].equals("deny")){
							save = "16 "+message[1]+" deny NULL NULL\n";
						}
						if(!server.offlinemsg.containsKey(message[2])){
							server.offlinemsg.put(message[2], Collections.synchronizedList(new ArrayList<String>()));
						}
						server.offlinemsg.get(message[2]).add(save);
						server.pendingIpRequest.get(message[1]).remove(message[2]);
						outToClient.writeBytes("06 TARGET_OFFLINE\n");
					} else {
						try {
							String recip = server.onlineUserIp.get(message[2]);
							int recport = server.onlineUserReceiverPort.get(message[2]);
							Socket sock = new Socket(recip, recport);
							DataOutputStream outToRec = new DataOutputStream(sock.getOutputStream());
							/*
							 *  - 16: reply to the sender (server to client receiver)
							 *  	- "16 replier agree ip port"
							 *  	- "16 replier deny NULL NULL"
							 */
							if(message[3].equals("agree")){
								outToRec.writeBytes("16 "+message[1]+" agree "+server.onlineUserIp.get(message[1])+" "+server.onlineUserReceiverPort.get(message[1])+"\n");
							} else if (message[3].equals("deny")){
								outToRec.writeBytes("16 "+message[1]+" deny NULL NULL\n");
							}
							
							server.pendingIpRequest.get(message[1]).remove(message[2]);
							outToClient.writeBytes("06 OK\n");
							sock.close();
						} catch (ConnectException e){
							// Guaranteed Message Delivery
							String save = "";
							if(message[3].equals("agree")){
								save = "16 "+message[1]+" agree "+server.onlineUserIp.get(message[1])+" "+server.onlineUserReceiverPort.get(message[1])+"\n";
							} else if (message[3].equals("deny")){
								save = "16 "+message[1]+" deny NULL NULL\n";
							}
							if(!server.offlinemsg.containsKey(message[2])){
								server.offlinemsg.put(message[2], Collections.synchronizedList(new ArrayList<String>()));
							}
							server.offlinemsg.get(message[2]).add(save);
							server.pendingIpRequest.get(message[1]).remove(message[2]);
							outToClient.writeBytes("06 TARGET_OFFLINE\n");
						}
					}
				}

			}
		} catch (IOException e) {e.printStackTrace();}
		
	}
	
	public boolean loginAuth(){
		/*
		 * Deal with the login request.
		 * Will write username, password reply to client
		 * Will also write the port number to client 
		 */
		try {
			DataOutputStream outToClient = new DataOutputStream(clientsocket.getOutputStream());
			BufferedReader inFromClient = new BufferedReader(new InputStreamReader(clientsocket.getInputStream()));
			String usernamemsg = inFromClient.readLine();
			String [] parsemsg = Message.parse(usernamemsg);
			String username = parsemsg[1];
			if(!server.credentials.containsKey(username)){
				outToClient.writeBytes("02 username_error\n");
				return false;
			}
			if(server.blockTimeMap.containsKey(username)){
				if(System.currentTimeMillis() - server.blockTimeMap.get(username) > server.BLOCK_TIME){
					server.blockTimeMap.remove(username);
				} else{
					outToClient.writeBytes("02 server_blocked\n");
					return false;
				}
			}
			outToClient.writeBytes("02 username_ok\n");
			String authpw = server.credentials.get(username);
			for(int i=0;i<3;i++){
				String pwmsg = inFromClient.readLine();
				String [] pw = Message.parse(pwmsg);
				if (pw[1].equals(authpw)){
					outToClient.writeBytes("02 password_ok\n");
					String portmsg = inFromClient.readLine();
					String [] portstrs = Message.parse(portmsg);
					/*
					 * The user is authorized to login
					 */
					server.onlineUserIp.put(username,clientsocket.getInetAddress().getHostAddress());
					server.onlineUserReceiverPort.put(username, Integer.parseInt(portstrs[1]));
					System.out.println("recevier port: "+portstrs[1]);
					try {
						broadcastPresence("04", "[Server]: "+username+" is online!\n");
					} catch (Exception e) {
						e.printStackTrace();
					}
					outToClient.close();
					inFromClient.close();
					this.username = username;
					return true;
				} else if (i!=2) {
					outToClient.writeBytes("02 password_error\n");
				} else {
					outToClient.writeBytes("02 server_blocked\n");
					server.blockTimeMap.put(username, System.currentTimeMillis());
				}
			}
			outToClient.close();
			inFromClient.close();
		} catch (IOException e) {e.printStackTrace();}
		return false;
	}
	
	public boolean isBlocked(String from, String to){
		if(server.blockset.containsKey(to)&&server.blockset.get(to).contains(from)){
			return true;
		} else
			return false;
	}
	
	public boolean checkIpPortValid(String reqUsername){
		try {
			if (server.onlineUserIp.containsKey(reqUsername)&&server.onlineUserIp.get(reqUsername).equals(clientsocket.getInetAddress().getHostAddress())){
				return true;
			} else {
				/*
				 * write to the client directly
				 */
				DataOutputStream outToClient = new DataOutputStream(clientsocket.getOutputStream());
				outToClient.writeBytes("06 INVALID_SESSION\n");
				return false;
			}
		} catch (Exception e) {e.printStackTrace();}
		return false;
	}
	
	public boolean isTimeOut(String reqUsername){
		if(System.currentTimeMillis() - server.lastBeatingTime.get(reqUsername) > server.TIME_OUT){
			return true;
		} else
			return false;
	}
	
	public void userLogout(String reqUsername){
		server.onlineUserIp.remove(reqUsername);
		server.onlineUserReceiverPort.remove(reqUsername);
		server.lastBeatingTime.remove(reqUsername);
	}
	
	public void broadcastPresence(String type, String broadcaststr) throws Exception{
		/*
		 * Broadcast a presence to all the online users
		 */
		for (String user : server.onlineUserIp.keySet()){
			String ip = server.onlineUserIp.get(user);
			int port = server.onlineUserReceiverPort.get(user);
			Socket socket = new Socket(ip, port);
			DataOutputStream outToClient = new DataOutputStream(socket.getOutputStream());
			outToClient.writeBytes(type+" "+broadcaststr+"\n");
			socket.close();
		}
	}
	
	public void broadcastMessage(String reqUsername, String data) throws IOException{
		/*
		 * broadcast a message from reqUsername, and write back the blocked message
		 * to the requested client
		 */
		String blocked = "";
		for (String user: server.onlineUserIp.keySet()){
			if (isBlocked(reqUsername, user)){
				blocked += "["+user+"] ";
			} else {
				String ip = server.onlineUserIp.get(user);
				int port = server.onlineUserReceiverPort.get(user);
				Socket socket = new Socket(ip, port);
				DataOutputStream outToClient = new DataOutputStream(socket.getOutputStream());
				outToClient.writeBytes(data);
				socket.close();
			}
		}
		DataOutputStream outToClient = new DataOutputStream(clientsocket.getOutputStream());
		if(blocked.length()==0){
			outToClient.writeBytes("13 OK NULL\n");
		} else {
			outToClient.writeBytes("13 BLOCKED "+blocked+"\n");
		}
	}
	

}
