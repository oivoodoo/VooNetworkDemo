import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;


public class VooCollector implements Runnable {
	private ArrayList<SocketContainer> sockets = new ArrayList<SocketContainer>();
	private ServerSocket server;
	public static Object synchronizedObject = new Object();
	private Thread thread = new Thread(this);
	
	public VooCollector(ServerSocket server) {
		this.server = server;
	}

	public ArrayList<SocketContainer> getSockets() {
		return sockets;
	}
	
	public void start() {
		thread.start();
	}
	
	public void stop() {
		thread.interrupt();
	}
	
	@Override
	public void run() {
		Socket socket;
		try {
			while(true) {
				socket = server.accept();
				socket.setKeepAlive(true);
				System.out.println("Voo# New socket connection captured.");
				
				BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				String template = reader.readLine(); 
				String command = VooServer.getCommand(template);
				String value = VooServer.getValue(template);
				String priority = VooServer.VOO_NORMAL_PRIORITY;
				long offset = 0;
				
				if (command.contains(VooServer.getCommand(VooServer.VOO_PRIORITY))) {
					System.out.println("Voo# New socket priority is " + priority);
					priority = value;
				} else if(command.contains(VooServer.getCommand(VooServer.VOO_RECONNECT))) {
					offset = Long.parseLong(value);
				}
				
				synchronized (synchronizedObject) {
					SocketContainer container = new SocketContainer(socket, priority);
					container.setOffset(offset);
					sockets.add(container);
				}						
				System.out.println("Voo# New socket add to sockets");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
