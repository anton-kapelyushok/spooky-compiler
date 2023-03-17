class MemberSelectMemberCall {

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
                    this.child(); // Child1: Child1.child
                    Parent.this.child(); // Parent: Parent.child
                    Outer.this.child(); // Outer: Outer.child
                    Child1.staticChild(); // Child1.staticChild
                    super.child(); // Child1: Parent.child

                }
            }
        }

        public static void main(String[] args) {
            var outer = new Outer();
            var parent = outer.new Parent();
            var child1 = parent.new Child1();

            child1.test();
        }

    }

    static void log(Object self, String message) {
        System.out.println(self.getClass().getName() + ": " + message);
    }
}