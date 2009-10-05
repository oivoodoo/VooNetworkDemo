import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.net.ServerSocket;
import java.util.*;

public class VooServer implements Runnable {
	// size - number of connections
	private final int SERVICE_PORT = 8082;
	private final int CHUNK_SIZE = 1;
	// commands
	public static final String VOO_SIZE = "VooSize:%d\r\n";
	public static final String VOO_FILENAME = "VooFileName:%s\r\n";
	public static final String VOO_PRIORITY = "VooPriority:%s\r\n";
	public static final String VOO_RECONNECT = "VooReconnect:%d\r\n";
	public static final String VOO_NORMAL_PRIORITY = "NORMAL";
	public static final String VOO_HIGH_PRIORITY = "HIGH";
	
	private ServerSocket server = null;
	private VooCollector collector = null;
	private String filename;
	private Thread thread = new Thread(this);
	
	public VooServer() {
	}
	
	public VooServer(String filename) {
		this.filename = filename;
	}
	
	public void start() {
		thread.start();
	}
	
	public void stop() {
		thread.interrupt();
	}
	
	@Override
	public void run() {
		System.out.println("# Start server.");
		try
		{
			try {
				server = new ServerSocket(SERVICE_PORT);
				collector = new VooCollector(server);
				collector.start();
				while(true) {
					ArrayList<SocketContainer> sockets = collector.getSockets();
					// We have to synch array objects.
					synchronized (VooCollector.synchronizedObject) {
						for(int i = 0; i < sockets.size(); i++) {
							if (!sockets.get(i).getSocket().isClosed()) {
								if (sockets.get(i).getPriority().compareTo(VOO_HIGH_PRIORITY) == 0) {
									System.out.println("Voo# Send file with the highets priority");
									while(!sockets.get(i).getSocket().isClosed() &&
										!sockets.get(i).isEnough()) {
										sendFile(sockets.get(i));
									}
									System.out.println("Voo# Completed");
								}
								if (sockets.get(i).isEnough()) {
									sockets.get(i).getSocket().close();
									sockets.remove(i);
									System.out.println("Voo# Socket connection closed");
								} else {
									sendFile(sockets.get(i));
								}
							} else {
								sockets.remove(i);
								System.out.println("Voo# Socket connection closed");
							}
						}
					}
				}
			}
			catch(Exception ex) {
				ex.printStackTrace();
			}
			finally {
				if (collector != null) {
					collector.stop();
				}
				if (server != null) {
					server.close();
				}
			}
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
		System.out.println("# Stop server.");
	}
	
	private void sendFile(SocketContainer socketContainer) {
		OutputStreamWriter writer = null;
		try
		{
			try {
				writer = new OutputStreamWriter(socketContainer.getSocket().getOutputStream());
				// Check existence of file name.
				File file = new File(filename);
				if (file.exists()) {
					// We start to initialize socket container
					if (!socketContainer.isInitialized() && socketContainer.getOffset() == 0) {
						// Send file name to client.
						writer.write(String.format(VOO_SIZE, file.length()));
						// Send file size to client.
						writer.write(String.format(VOO_FILENAME, filename));
						// Initialize socket container to sending data via socket.
						socketContainer.setSize(file.length());
						socketContainer.setFileName(filename);
					}
					if (socketContainer.getOffset() > 0 && 
						socketContainer.getSize() == -1) {
						socketContainer.setSize(file.length());
					}
					if (file.canRead()) {
						// Read data from file by chunk size.
						RandomAccessFile randomAccess = new RandomAccessFile(file, "r");
						try {
							System.out.println("Voo# Send to client file data");
							byte content[] = new byte[CHUNK_SIZE];
							randomAccess.seek(socketContainer.getOffset());
							int read = randomAccess.read(content, 0, CHUNK_SIZE);
							socketContainer.setOffset(socketContainer.getOffset() + read);
							writer.write(toCharArray(content), 0, read);
							writer.flush();
						}
						catch(Exception ex) {
							ex.printStackTrace();
						}
						finally {
							randomAccess.close();
						}
					}
				}
			}
			catch(Exception ex) {
				ex.printStackTrace();
			}
			finally {
				if (writer != null) {
					// writer.close();
				}
			}
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public static char[] toCharArray(byte[] content) {
		char values[] = new char[content.length];
		for(int i = 0; i < content.length; i++) {
			values[i] = (char) content[i];
		}
		return values;
	}
	
	public static String getCommand(String template) {
		return template.split(":")[0];
	}
	
	public static String getValue(String command) {
		if (command.split(":").length > 0) {
			return command.split(":")[1];
		} else {
			return "";
		}
	}
}
