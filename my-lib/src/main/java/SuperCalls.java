public class SuperCalls {
    static class Parent {
        Parent(int arg1, int arg2) {
        }
    }

    static class Child extends Parent {
        String a = "loupa";
        {
            var b = 2;
            a = "loupa + 1";
            a = "loupa + 1";
            a = "loupa + 1";
            a = "loupa + 1";
            a = "loupa + 1";
            a = "loupa + 1";
            a = a + b;
        }
        {
            var b = 3;
            a = a + b;
        }

        Child(int arg1) {
            this(arg1, arg1);
        }

        Child(int arg1, int arg2) {
            super(arg1, arg2);
            System.out.println("hi");
        }

        Child(int arg1, int arg2, int arg3) {
            super(arg1, arg2 + arg3);
        }
    }
}
