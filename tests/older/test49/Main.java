import java.*;

class Node {
	public Node f1;
	public Node f2;
	Node() {}
	Node(Node p1) {
		f1 = p1;
		Node o1 = new Node();
		check(o1);
	}
	private void check (Node p2) {

		f2 = p2;

	}
}

public class Main {
	public static Node global;
	public static void main(String[] args) {
		Main A = new Main(); // <internal,0>
		Node B = new Node(); // <internal,8>
		A.foo();
	}

	public void foo() {
		Node C = new Node();
		Main D = new Main();
		Node E = new Node(C);
		D.bar(C);
	}
	public void bar(Node p2) {
		//global = p2;
	}
}