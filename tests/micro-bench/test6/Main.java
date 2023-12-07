// Micro benchmark for checking the fields access.

public class Main {
	public static Node A;
	public static void main(String[] args) {
		Node B = new Node(); // <internal,0>
		Node C = new Node(); // <internal,8>
		bar(B);
		B.n = new Node();
		B.n.n = new Node();
	}

	public static void bar(Node p2) {
		Node G = new Node();
		Node H = new Node();
		G = p2.n;
		H = G.n;
	}
}
