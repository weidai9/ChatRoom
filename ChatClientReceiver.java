import java.io.IOException;
import java.net.*;

public class ChatClientReceiver extends Thread {
	
	public ChatClient chatclient;
	public ServerSocket receiversocket;
	public boolean running = true;
	public ChatClientReceiver(ChatClient inclient){
		chatclient = inclient;
		try {
			receiversocket = new ServerSocket(0);
		} catch (IOException e) {e.printStackTrace();}
	}
	@Override
	public void run() {
		while(running) {
			try {
				Socket fromserversocket = receiversocket.accept();
				ClientWorker clientworker = new ClientWorker(fromserversocket, chatclient);
				clientworker.start();
			} catch (IOException e) {}
		}
	}
	
	public void stopRunning(){
		running = false;
		try {
			receiversocket.close();
		} catch (IOException e) {e.printStackTrace();}
	}

}
