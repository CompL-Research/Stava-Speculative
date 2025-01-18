class SpecOpt {
    SpecOpt f1;
    public static void main(String args[]) {
        for(int i = 0; i <100000; i++) {
            SpecOpt o1;
            if (i%5 == 0) {
                o1 = new SpecOpt();
            } else if(i%3 == 0) {
                o1 = new Child();
            } else {
                o1 = new Child2();
            }
            SpecOpt o2 = new SpecOpt();
            o2.foo(o1); // Callsite 1 with only type as SpecOpt.
        }
    }

    void foo(SpecOpt p1) {
        SpecOpt o2;
        for(int j = 0; j<1000; j++) {
            if(p1 instanceof Child) {
                o2 = new Child();
            } else if (p1 instanceof Child2){
                o2 = new Child2();
            } else {
                o2 = new SpecOpt();
            }
            o2.bar(new SpecOpt());// Callsite 2 with two types: SpecOpt and Child.
        }
    }

    void bar(SpecOpt p1) {
        SpecOpt o3 = new SpecOpt();
        o3.f1 = p1;
    }
}

class Child extends SpecOpt {
    void foo(SpecOpt p2) {
        SpecOpt o2 = new SpecOpt();
        o2.bar(new SpecOpt());
    }
    void bar(SpecOpt p3) {
        SpecOpt o4 = new SpecOpt();
        o4.f1 = p3;
    }
}

class Child2 extends SpecOpt {
    public static SpecOpt global;
    void foo(SpecOpt p2) {
        SpecOpt o2 = new SpecOpt();
        o2.bar(new SpecOpt());
    }
    void bar(SpecOpt p3) {
        SpecOpt o4 = new SpecOpt();
        global = p3;
        o4.f1 = p3;
        p3.f1 = new SpecOpt();
    }
}

