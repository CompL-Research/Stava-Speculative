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
		Node B = new Node(); // <internal,8>
		B = A.foo();
		Main.fb(B);
	}

	public Node foo() {
		return Main.bar(new Node());
	}
	public static Node bar(Node p2) {
		return Main.foobar(new Node(p2));
	}
	public static Node foobar(Node p3) {
		return p3;
	}
	public static void fb(Node p4) {}
}