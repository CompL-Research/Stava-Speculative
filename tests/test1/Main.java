class Main {
	public static void main(String[] args) {
		Main o1 = new Main();
		Node o2 = new Node();
		Node o3 = new ChildNode();
		if(args.length > 2) {
			o1.foo(o2);
		} else {
			o1.foo(o3);
		}
	}
	void foo(Node p1) {
		Node o4 = new Node();
		p1.bar(o4);
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
}

class childNode extends Node {
	void bar(Node p1) {
		Node o6 = new Node();
		o6.n = p1;
	}
}