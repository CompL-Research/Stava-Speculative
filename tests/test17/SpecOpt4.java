class SpecOpt4 {
    SpecOpt4 f1;
    public static void main(String args[]) {
        for(int i = 0; i <100000; i++) {
            SpecOpt4 o1;
            if (i < 10000) {
                o1 = new SpecOpt4();
            } else if(i>= 10000 && i < 50000) {
                o1 = new Child();
            } else {
                o1 = new Child2();
            }
            SpecOpt4 o2 = new SpecOpt4();
            o2.foo(o1); // Callsite 1 with only type as SpecOpt4.
        }
    }

    void foo(SpecOpt4 p1) {
        SpecOpt4 o2;
        for(int j = 0; j<1000; j++) {
            if(p1 instanceof Child) {
                o2 = new Child();
            } else if (p1 instanceof Child2){
                o2 = new Child2();
            } else {
                o2 = new SpecOpt4();
            }
            o2.bar(new SpecOpt4()); // Polymorphic Callsite: 3 types
            new SpecOpt4().bar(new SpecOpt4());
        }
    }

    void bar(SpecOpt4 p1) {
        SpecOpt4 o4 = new SpecOpt4();
        p1.f1 = o4;
    }
}

class Child extends SpecOpt4 {
    void bar(SpecOpt4 p3) {
        SpecOpt4 o4 = new SpecOpt4();
        p3.f1 = o4;
    }
}

class Child2 extends SpecOpt4 {
    public static SpecOpt4 global;
    void bar(SpecOpt4 p3) {
        SpecOpt4 o4 = new SpecOpt4();
        global = p3;
        p3.f1 = new SpecOpt4();
    }
}