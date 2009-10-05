public class Main {
	public static void main(String args[]) {
		if (args.length > 1) {
			VooClient client = new VooClient(args[0], args[1]);
			client.run();
		} else {
			VooClient client = new VooClient("localhost", VooClient.VOO_NORMAL_PRIORITY);
			client.run();
		}
	}
}
