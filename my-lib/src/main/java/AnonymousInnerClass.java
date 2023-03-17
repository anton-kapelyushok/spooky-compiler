public class AnonymousInnerClass {

    String superOuter = "hi";

    void outerMethod() {
    }

    class OuterClass1 {
        private String outer = "hi";

        class InnerClass1 {


            public InnerClass1(String arg) {
            }

            void outerMethod(String some) {
            }

            void main() {

                var poupa = "poupa"; // val$poupa

                var a = new OuterClass1();

                a.new InnerClass1("poupa") {
                    void method() {
                        outerMethod(superOuter);
                        this.outerMethod(superOuter + OuterClass1.this.outer);
                        InnerClass1.this.outerMethod(superOuter + outer);
                        System.out.println(poupa);
                        AnonymousInnerClass.this.outerMethod();
                    }
                };
            }
        }
    }
}

class OuterClass2 {
    class InnerClass2 {
        InnerClass2(String a) {
        }

        void outerMethod2(String some) {
        }
    }
}
