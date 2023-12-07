import java.*;

class Node {
	public Node f1;
	Node() {}
	Node(Node p1) {
		this.f1 = p1;
	}
}

public class Main {
	public static void main(String[] args) {
		Main A = new Main(); // <internal,0>
		Node B = A.foo(new Node()); // <internal,9>
	}

	public Node foo(Node p1) {
		Node C = new Node();
		return Main.bar(new Node(C));
	}

	public static Node bar(Node p2) {
		Node D = new Node();
		return new Node(D);
	}
}
