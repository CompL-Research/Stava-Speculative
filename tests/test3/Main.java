class Main {
	public static void main(String[] args) {
		Main o1 = new Main(); // <internal, 0>
		Node o2 = new Node(); // <internal, 8> // Note: It should NOT come as interesting objects
		Node o3 = new ChildNode(); // <internal, 16>
		Node o4;
		if(args.length > 2) {
			o4 = o1.foo(o2);
		} else {
			o4 = o1.foo(o3);
		}
		o4.foobar(o2); // Polymorphic CallSite
	}
	Node foo(Node p1) {
		Node o4 = new Node(); // Note: It should come as interesting objects
		p1.bar(o4); // Polymorphic CallSite
		return p1;
	}
}
class Node {
	public static Node global;
	Node n;
	void bar(Node p1) {
		Node o5 = new Node(); // <internal, 0>
		if(global != null) {
			global = p1;
		}
		p1.n = o5;
	}
	void foobar(Node p2) {
		Node o7 = new ChildNode(); ;
		Node o8 = new Node();
		if(p2 instanceof ChildNode) {
			o7.n = new Node();
			o8 = o7.n;
			System.out.println(o8);
		} else {
			global = o7;
			o7.n = new Node();
			o8 = o7.n;
		}
		o7.fb(o8); // Polymorphic CallSite

	}
	void fb(Node p3) {
		Node o9 = new Node(); // <internal, 0>
		global = p3;
		o9.n = this;
	}
}

class ChildNode extends Node {
	void bar(Node p1) {
		Node o6 = new Node(); // <internal, 0>
		o6.n = p1;
	}
	void foobar(Node p4) {
		Node o11 = new Node(); // <internal, 0>
		o11.fb(p4);
	}
	@Override
	void fb(Node p5) {
		Node o10 = new Node(); // <internal, 0>
		o10.n = p5;
	}
}