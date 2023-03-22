class MemberSelectVariableResolution {

    static String staticString;

    static class OuterParent {
        String outerParent = null;
    }

    static class Outer extends OuterParent {
        String outer = null;

        class Parent {
            String parent = null;
        }

        class Child extends Parent {
            String inner = null;

            void test() {
                Child method = null;

                sink(Child.this); // self

                new AnonParent() {
                    String anon = null;

                    void test() {
                        sink(
                            this.anon, // self -> { AnonParent } -> { anon }
                            super.parent, // self -> { Parent } -> { parent }
                            this.parent, // self -> { Parent } -> { parent }
                            this.toExtend, // self -> { AnonParent } -> { toExtend }
                            MemberSelectVariableResolution.Outer.this, // self -> { __enclosing } -> { __enclosing }
                            Outer.this, // self -> { __enclosing } -> { __enclosing }
                            Child.super.parent, // self -> { __enclosing } -> { Parent } -> { parent }
                            AnonParent.class,
                            MemberSelectVariableResolution.OuterParent.class,
                            MemberSelectVariableResolution.Outer.super.outerParent, // self -> { __enclosing } -> { __enclosing } -> { OuterParent } -> { outerParent }
                            MemberSelectVariableResolution.Outer.this.outerParent, // self -> { __enclosing } -> { __enclosing } -> { OuterParent } -> { outerParent }
                            MemberSelectVariableResolution.staticString,
                            method.inner,
                            method.parent,
                            ((Parent) method).parent
                        );
                    }
                };
            }
        }

        class AnonParent extends Parent {
            String toExtend = null;
        }
    }

    static void sink(Object... sinks) {
    }

    static void log(Object self, String message) {
        System.out.println(self.getClass().getName() + ": " + message);
    }
}