public class ClashingNames {
    static class Parent {
        String parentField;
    }

    static class Outer extends Parent {
        class Inner {
            void run() {
                System.out.println(Outer.this.parentField);
                System.out.println(parentField);
            }
        }
    }

    static class Outer2_1 extends Parent {
        {
            Outer2_1.this.parentField = "Outer2_2";
        }
        class Outer2_2 extends Parent {
            {
                Outer2_2.this.parentField = "Outer2_2";
            }
            class Inner {
                void run() {
//                    System.out.println(Outer2_1.this.parentField); // Outer2_1
//                    System.out.println(Outer2_2.this.parentField); // Outer2_2
                    System.out.println(parentField); // Outer2_2
                }
            }
        }
    }

    public static void main(String[] args) {
        new Outer2_1().new Outer2_2().new Inner().run();
    }
}
