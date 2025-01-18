public class Main {
	public static Node A;
	public static void main(String[] args) {
		Main B = new Main(); // <internal,0>
		if(args.length > 0 ) {
			B = new Main();
		} else {
			B = new A();
		}
		Node C = B.foo(new Node());
		Node F = B.bar(new Node());
	}

	public Node foo(Node p1) {
		Node D = new Node();
		D.n = new Node();
		p1.n = D;
		return  new Node();
	}

	public Node bar(Node p2) {

		p2.n = new Node();
		return p2;
	}
}

class A extends Main {
	public Node foo(Node p1) {
		Node D = new Node();
		D.n = new Node();
		p1.n = D;
		return  new Node();
	}
	public Node bar(Node p2) {

		p2.n = new Node();
		return p2;
	}
}

