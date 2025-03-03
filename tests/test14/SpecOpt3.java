class SpecOpt3 {
    SpecOpt3 f1;
    public static void main(String args[]) {
        for(int i = 0; i <100000; i++) {
            SpecOpt3 o1;
            if (i%5 == 0) {
                o1 = new SpecOpt3();
            } else if(i%3 == 0) {
                o1 = new Child();
            } else {
                o1 = new Child2();
            }
            SpecOpt3 o2 = new SpecOpt3();
            o2.foo(o1); // Callsite 1 with only type as SpecOpt3.
        }
    }

    void foo(SpecOpt3 p1) {
        SpecOpt3 o2;
        for(int j = 0; j<1000; j++) {
            if(p1 instanceof Child) {
                o2 = new Child();
            } else if (p1 instanceof Child2){
                o2 = new Child2();
            } else {
                o2 = new SpecOpt3();
            }
            o2.bar(new SpecOpt3());// Callsite 2 with two types: SpecOpt3 and Child.
            new SpecOpt3().bar(new SpecOpt3());
        }
    }

    void bar(SpecOpt3 p1) {
        SpecOpt3 o4 = new SpecOpt3();
        p1.f1 = o4;
    }
}

class Child extends SpecOpt3 {
    void bar(SpecOpt3 p3) {
        SpecOpt3 o4 = new SpecOpt3();
        p3.f1 = o4;
    }
}

class Child2 extends SpecOpt3 {
    public static SpecOpt3 global;
    void bar(SpecOpt3 p3) {
        SpecOpt3 o4 = new SpecOpt3();
//        global = p3;
        p3.f1 = new SpecOpt3();
    }
}
