import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.logging.Logger;

public class VooClient implements Runnable {
	private final int SERVICE_PORT = 8082;
	private final int CHUNK_SIZE = 1024;
	private String priority;
  private String hostName;
	
	public static final String VOO_FILENAME = "VooFileName:%s\r\n";
	public static final String VOO_SIZE = "VooSize:%d\r\n";
	public static final String VOO_PRIORITY = "VooPriority:%s\r\n";
	public static final String VOO_RECONNECT = "VooReconnect:%d\r\n";
	public static final String VOO_NORMAL_PRIORITY = "NORMAL";
	public static final String VOO_HIGH_PRIORITY = "HIGH";
	
	private Socket socket = null;
	private BufferedReader reader = null;
	private PrintWriter writer = null;
	
	private long size = 0;
	private long offset = 0;
	private String filename = null;
	private char buffer[] = new char[CHUNK_SIZE];
	
	public VooClient(String hostName, String priority) {
		this.priority = priority;
    this.hostName = hostName;
	}
	
	@Override
	public void run() {
		try
		{
			try {
				connect();
				
				System.out.println("#Voo: Try to send priority of file downloader.");
				writer.write(String.format(VOO_PRIORITY, priority));
				writer.flush();
				System.out.println("#Voo: Send priority of file downloader.");
				
				boolean flag = true;
				while(flag) {
					int read = reader.read(buffer);
					String command = toStringCommand(buffer, read);
					String values[] = command.split("\r\n");
					for(String value : values) {
						if (value.contains(getCommand(VOO_FILENAME))) {
							filename = getValue(value);
							System.out.println(String.format("Voo# FileName is %s", filename));
						} else if (value.contains(getCommand(VOO_SIZE))) {
							size = Long.parseLong(getValue(value));
							System.out.println(String.format("Voo# Size is %d", size));
						} else {
							if (receiveFile(value, flag) == -1) {
								flag = false;
								break;
							}
						}
					}
				}
			} catch (IOException ex) {
				boolean flag = true;
				do {
					try {
						connect();
					
						if (!socket.isClosed()) {
							writer.write(String.format(VOO_RECONNECT, offset));
							writer.flush();
						}
						try {
							receiveFile("", flag);
							flag = false;
						} catch(Exception e) {
							ex.printStackTrace();
						}
					} catch(Exception ex1) {
						flag = true;
					}
				} while((socket != null && socket.isClosed()) || flag);
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				closeConnections();
			}
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
	}
	
	private void closeConnections() throws Exception {
		if (socket != null) {
			socket.close();
		}
		if (reader != null) {
			reader.close();
		}
		if (writer != null) {
			writer.close();
		}
	}
	
	private int receiveFile(String value, boolean flag) throws Exception {
		System.out.println("Voo# Sending data...");
		if (filename != null && size != 0) {
			offset += value.length();
			FileWriter fileWriter = new FileWriter(new File(filename), true);
			fileWriter.write(value);
			System.out.println(String.format("Voo# Data: %s", value));
			try {
				while (offset != size - 1) {
					int read = reader.read(buffer);
					if (read == -1) throw new IOException("now found connection");
					String line = toStringCommand(buffer, read);
					if (line != null) {
						System.out.println(String.format("Voo# Data: %s", line));
						offset += line.length();
						fileWriter.write(line);
					}
				}
				return -1;
			} finally {
				flag = false;
				fileWriter.close();
			}
		} else {
			System.out.println("Invalid server session.");
		}
		return 0;
	}
	
	private void connect() throws Exception {
		if (socket != null) {
			closeConnections();
			socket = null;
			writer = null;
			reader = null;
		}
		socket = new Socket(hostName, SERVICE_PORT);
		writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
		reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
	}
	
	public static String getCommand(String template) {
		return template.split(":")[0];
	}
	
	public static String getValue(String command) {
		return command.split(":")[1];
	}
	
	private String toStringCommand(char buffer[], int read) {
		String result = "";
		for(int i = 0; i < read; i++) {
			result += Character.toString(buffer[i]);
		}
		return result;
	}
}
