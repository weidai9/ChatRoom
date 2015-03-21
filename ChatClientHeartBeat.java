import java.io.DataOutputStream;
import java.net.Socket;

public class ChatClientHeartBeat extends Thread {
	public ChatClient client;
	public boolean beating = true;
	public ChatClientHeartBeat(ChatClient chatclient){
		client =chatclient;
	}
	@Override
	public void run() {
		while(beating){
			try {
				Socket sock = new Socket(client.hostip, client.hostport);
				DataOutputStream outToServer = new DataOutputStream(sock.getOutputStream());
				outToServer.writeBytes("07 "+client.username+"\n");
				sock.close();
				Thread.sleep(client.beatInterval);
			} catch (Exception e) {
				System.out.println("Connection failed. Exit...");
				break;
			} 
		}
	}
	
	public void stopBeating(){
		beating  = false;
	}

}
