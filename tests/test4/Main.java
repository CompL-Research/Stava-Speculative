class Main {
	public static void main(String[] args) {
		A o1 = new A();
		A o2;
		if(args.length > 1) {
			o2 = new A();
		} else {
			o2 = new B();
		}
		o1.foo(o2, new A());
	}
}
class A {
	A f;
	void foo(A q, A r) {
		q.bar(new A(), r); //C1
		A y;
		for(int i = 0; i<100000; i++) {
			if(i % 4 == 0) {
				y = new A();
			} else {
				y = new B();
			}
			y.bar(new A(), r); //C2
		}
	}
	void bar(A p1, A p2){
		p1.f = p2;
	}
}

class B extends A {
	void bar(A p , A q) {
		q.f = p;
	}
}