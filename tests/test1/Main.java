class Main {
	public static void main(String[] args) {
		Main o1 = new Main();
		Node o2 = new Node();
		Node o3 = new ChildNode();
		Node o4 = new Node();
		if(args.length > 2) {
			o4 = o1.foo(o2);
		} else {
			o4 = o1.foo(o3);
		}
		o4.foobar(o2);
	}
	Node foo(Node p1) {
		Node o4 = new Node();
		p1.bar(o4);
		return p1;
	}

}
class Node {
	public static Node global;
	Node n;
	void bar(Node p1) {
		Node o5 = new Node();
		if(global != null) {
			global = p1;
		}
		p1.n = o5;
	}
	void foobar(Node p2) {
		Node o7;
		Node o8 = new Node();
		if(p2 instanceof ChildNode) {
			o7 = new ChildNode();
		} else {
			o7 = new Node();
		}
		o7.fb(o8);
	}
	void fb(Node p3) {
		Node o9 = new Node();
		o9.n = this;
	}
}

class ChildNode extends Node {
	void bar(Node p1) {
		Node o6 = new Node();
		o6.n = p1;
	}
	void foobar(Node p4) {
		Node o11 = new Node();
		o11.fb(p4);
	}
	@Override
	void fb(Node p5) {
		Node o10 = new Node();
		o10.n = this;
	}
}