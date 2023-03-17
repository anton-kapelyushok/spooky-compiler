package Bookings;

class WeirdReferences {
    static class Outer {

        static Outer staticPoupa = new Outer();
        Outer dynamicPoupa = new Outer();

        class Inner {
            String innerId;
            String innerId1;

            Inner(String innerId) {
                this.innerId = innerId;
            }

            public String getInnerId() {
                return innerId;
            }

            void run1() {
                var o = new Outer();
                Runnable lambda = () -> {
                    var definedInLambda = "String";
                    System.out.println(definedInLambda);
                    o.new Inner("2") {
                        Integer innerId;
                        {
                            definedInLambda.isEmpty();
                            System.out.println(innerId);
                            System.out.println(this.innerId);
                            System.out.println(super.innerId);

                            System.out.println(innerId1);
                            System.out.println(this.innerId1);
                            System.out.println(super.innerId1);

//                        System.out.println(innerId); // in closure
//                        System.out.println(Inner.this.innerId); // references Inner.innerId =>
//
//                        System.out.println(getInnerId());
                            System.out.println(this.getInnerId());
                            System.out.println(Inner.this.getInnerId());
                            System.out.println(staticPoupa.dynamicPoupa.dynamicPoupa);
                            System.out.println(Outer.this.dynamicPoupa);
                        }
                    };
                };
            }
        }
    }


    public static void main(String[] args) {
        var o = new Outer();
        o.new Inner("1").run1();
    }
}
