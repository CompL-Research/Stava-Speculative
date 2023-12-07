public class Main {
	public static Node A;
	public static void main(String[] args) {
		Main B = new Main(); // <internal,0>
		Node C = B.foo(new Node());

	}

	public Node foo(Node p1){
      return bar(new Node());
	}

	public Node bar(Node p1) {
		return p1;
	}
}

