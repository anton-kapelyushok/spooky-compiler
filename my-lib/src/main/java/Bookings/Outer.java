package Bookings;


class MyClass {
    void poupa(MyClass this) {}
}


public class Outer {




    int a;
    int bOuter;

    public class Outer2 {
        int a;
        int bOuter2;

        public Outer2() {
            new Inner();
        }

        public class Inner {
            int a;
            int bInner;

            public void poupa(Inner this) {}

            public Inner() {
                bInner = 1;
                System.out.println(a);
            }

            public Inner(int bInner) {
                this.bInner = bInner;
                System.out.println(a);
            }

            public Inner(int innerA, int bInner) {
                this.a = innerA;
                this.bInner = bInner;
                System.out.println(a);
            }

            public Inner(int outer2A, int innerA, int bInner) {
                Outer2.this.a = outer2A;
                this.a = innerA;
                this.bInner = bInner;
                System.out.println(a);
            }

            public Inner(int outerA, int outer2A, int innerA, int bInner) {
                Outer.this.a = outerA;
                Outer2.this.a = outer2A;


                this.a = innerA;
                this.bInner = bInner;
                System.out.println(a);
            }
        }
    }

    public static void main(String[] args) {
        new Outer().new Outer2().new Inner();
        new Outer().new Outer2().new Inner(1);
        new Outer().new Outer2().new Inner(1, 2);
        new Outer().new Outer2().new Inner(1, 2, 3);
        new Outer().new Outer2().new Inner(1, 2, 3, 4);

        var outer2 = new Outer().new Outer2().new Inner();

    }
}
