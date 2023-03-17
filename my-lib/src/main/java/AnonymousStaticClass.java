public class AnonymousStaticClass {

    void outerMethod() {
    }

    static class InnerClass {
        public InnerClass(String arg) {
        }

        void outerMethod() {
        }
    }

    void main() {
        var a = new AnonymousStaticClass();

        new InnerClass("poupa") {
            void method() {
                outerMethod();
                this.outerMethod();
//                AnonymousStaticClass.this.outerMethod();
            }
        };
    }
}
