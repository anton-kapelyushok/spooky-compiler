class IdentifierMethodCall {

    static class OuterParent {
        void outerParent() {
            log(this, "OuterParent.outerParent");
        }
    }

    static class Outer extends OuterParent {
        void outer() {
            log(this, "Outer.outer");
        }

        void parent() {
            log(this, "Outer.parent");
        }

        void child() {
            log(this, "Outer.child");
        }

        static void staticOuter() {
            System.out.println("Outer.staticOuter");
        }

        static void staticParent() {
            System.out.println("Outer.staticParent");
        }

        static void staticChild() {
            System.out.println("Outer.staticChild");
        }

        class Parent {
            void parent() {
                log(this, "Parent.parent");
            }

            void child() {
                log(this, "Parent.child");
            }

            static void staticParent() {
                System.out.println("Parent.staticParent");
            }

            static void staticChild() {
                System.out.println("Parent.staticChild");
            }


            class Child1 extends Parent {
                void child() {
                    log(this, "Child1.child");
                }

                static void staticChild() {
                    System.out.println("Child1.staticChild");
                }

                void test() {
                    child(); // Child1: Child1.child
                    parent(); // Child1: Parent.parent
                    outer(); // Outer: Outer.outer
                    outerParent(); // Outer: OuterParent.outerParent

                    staticChild(); // Child1.staticChild
                    staticParent(); // Parent.staticParent
                    staticOuter(); // Outer.staticOuter
                }
            }
        }

        class Child2 extends Parent {
            void child() {
                log(this, "Child2.child");
            }

            void test() {
                child(); // Child2: Child2.child
                parent(); // Child2: Parent.parent
                outer(); // // Outer: Outer.parent
                outerParent(); // Outer: OuterParent.outerParent
            }
        }

        class Child3 extends OuterParent {
            void child() {
                log(this, "Child3.child");
            }

            void test() {
                child(); // Child3: Child3.child
                parent(); // Outer: Outer.parent
                outer(); // // Outer: Outer.outer
                outerParent(); // Child3: OuterParent.outerParent
            }
        }

        public static void main(String[] args) {
            var outer = new Outer();
            var parent = outer.new Parent();
            var child1 = parent.new Child1();
            var child2 = outer.new Child2();
            var child3 = outer.new Child3();

            child1.test();
            child2.test();
            child3.test();
        }

    }

    static void log(Object self, String message) {
        System.out.println(self.getClass().getName() + ": " + message);
    }
}