package vr;

import static vr.IdentifierVariableResolution.StaticContainer.importedString;

class IdentifierVariableResolution {

    static class OuterParent {
        String outerParent;
    }

    static class Outer extends OuterParent {
        String outer;

        class Parent {
            String parent;
        }

        class Inner extends Parent {
            String inner;

            void method(String capturedArgument) {
                String capturedVariable = null;

                new AnonParent() {

                    static String anonStatic;
                    String anon = null;
                    @Override
                    public void test(String argument) {
                        String variable = null;

                        sink(
                            this,  // self
                            anon,  // self -> anonymous -> anon
                            argument, // argument
                            capturedArgument, // capturedArgument
                            capturedVariable, // capturedVariable
                            inner, // self -> __enclosing -> Inner -> inner
                            parent, // self -> __enclosing -> Parent -> parent
                            outer, // self -> __enclosing -> __enclosing -> Outer -> outer
                            outerParent, // self -> __enclosing -> __enclosing -> OuterParent -> outerParent
                            importedString,
                            anonStatic
                        );
                    }
                };
            }
        }
    }

    static class StaticContainer {
        static String importedString = null;
    }

    static void sink(Object... sinks) {
    }

    static void log(Object self, String message) {
        System.out.println(self.getClass().getName() + ": " + message);
    }

    interface AnonParent {
        void test(String argument);
    }
}