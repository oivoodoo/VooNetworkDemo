import java.net.Socket;

public class SocketContainer {
	private long offset = 0;
	private Socket socket = null;
	private long size = -1;
	private String filename;
	private String priority;
	
	public SocketContainer(Socket socket, String filename, String priority, long size) {
		this.socket = socket;
		this.size = size; 
		this.filename = filename;
		this.priority = priority;
	}
	
	public SocketContainer(Socket socket) {
		this.socket = socket;
	}
	
	public SocketContainer(Socket socket, String priority) {
		this.socket = socket;
		this.priority = priority;
	}
	
	public void updateSize(long chunk) {
		offset += chunk;
	}
	
	public Boolean isEnough() {
		return offset == size - 1;
	}
	
	public Boolean isInitialized() {
		return size != -1 && offset != 0;
	}
	
	public Socket getSocket() {
		return socket;
	}
	
	public void setSize(long size) {
		this.size = size;
	}
	
	public long getOffset() {
		return offset;
	}
	
	public String getFileName() {
		return filename;
	}
	
	public void setFileName(String filename) {
		this.filename = filename;
	}
	
	public void setOffset(long offset) {
		this.offset = offset;
	}
	
	public void setPriority(String priority) {
		this.priority = priority;
	}
	
	public String getPriority() {
		return priority;
	}
	
	public long getSize() {
		return size;
	}
}
