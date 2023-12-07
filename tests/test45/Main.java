
//Return Case Error
//public class Main {
//	public static Node A;
//	public static void main(String[] args) {
//		Node B = new Node(); // <internal,0>
//		Node C = new Node(); // <internal,8>
//		Node D = new Node(); // <internal,16>
//		Node E = new Node(); // <internal,24>
//		Node F = bar(B);
//	}
//
//	public static Node bar(Node p1) {
//		return foobar(new Node());
//	}
//
//	public static Node foobar(Node p2) {
//		return p2;
//	}
//}

//public class Main {
//	public Node A;
//	//private static Node[] array;
//	private Node[] array;
//	public static void main(String[] args) {
//		Main B = new Main(); // <internal,0>
//		Node C = new Node(); // <internal,8>
//		B.bar();
//	}
//
//	public void bar() {
//		//Main B  = new Main();
//		Node E = new Node();
//		int i = 0;
//		this.foobar(E, i);
//	}
//
//	public void foobar(Node p2, int i) {
//
//		//this.A = p2;
//		array[i] = p2;
//	}
//}


public class Main {
	public Node A;
	//private static Node[] array;
	private Node[] array;
	private Main field1 = new Main();
	public static void main(String[] args) {
		Main B = new Main(); // <internal,0>
		Main C = new Main(); // <internal,8>
		B.foo();
	}

	public void foo() {
		this.bar(new Node()); // <internal,0>
	}

	public void bar(Node p1) {
		int i = 0;
		this.field1.foobar(p1);
	}

	public void foobar(Node p2) {
		this.field1.field1.fb(p2);
	}

	public void fb(Node p3) {
		int i = 0;
		this.field1.field1.field1.array[i] = p3;
	}
}