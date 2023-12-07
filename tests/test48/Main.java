import java.*;
import java.util.List;
import java.util.ArrayList;

class Node {
	public List arr = new ArrayList();
	Node() {
		arr.add(new Node());
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
		D.bar(C);
	}
	public void bar(Node p2) {
		//global = p2;
	}
}