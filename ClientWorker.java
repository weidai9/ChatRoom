import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;

public class ClientWorker extends Thread {
	Socket fromserversocket;
	ChatClient client;
	public ClientWorker(Socket fromserversocket, ChatClient client){
		this.fromserversocket = fromserversocket;
		this.client = client;
	}
	@Override
	public void run() {		
		try {
			BufferedReader inFromServer = new BufferedReader(new InputStreamReader(fromserversocket.getInputStream()));
			String msg = inFromServer.readLine();

			String [] message = Message.parse(msg);
			if (message[0].equals("04")){
				System.out.println(message[1]);
			} else if (message[0].equals("05")){
				System.out.println("["+message[1]+"]: "+message[3]);
			} else if (message[0].equals("12")){
				System.out.println("[Broadcast] ["+message[1]+"]: "+message[2]);
			} else if (message[0].equals("14")){
				System.out.println("User ["+message[1]+"] want to start a private talk with you. Enter \"agree [username]\" to"
						+ "agree the request; enter \"deny [username]\" to deny the request.");
			} else if (message[0].equals("16")){
				if(message[2].equals("agree")){
					String ip = message[3];
					int port = Integer.parseInt(message[4]);
					client.userIp.put(message[1], ip);
					client.userPort.put(message[1], port);
					System.out.println("User ["+message[1]+"] agreed your address request: "
							+ "ip: "+ip+", port: "+port);
				} else {
					System.out.println("User ["+message[1]+"] rejected your address request.");
				}
			} else if (message[0].equals("17")){
				System.out.println("[Private] ["+message[1]+"]: "+message[2]);
			}
			fromserversocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
