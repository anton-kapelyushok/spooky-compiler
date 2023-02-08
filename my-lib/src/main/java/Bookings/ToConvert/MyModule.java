package Bookings.ToConvert;

import Bookings.External.ExternalModule;
import perl.ArrayRef;
import perl.PerlModule;

public class MyModule implements PerlModule {
    private final ExternalModule externalModule;

    public MyModule(ExternalModule externalModule) {
        this.externalModule = externalModule;
    }

    public ArrayRef<String> do_something(MySubArgs args) {
        var loupas = externalModule.convert_to_loupas(args.poupas());
        return loupas.map(it -> it.volobuev(4));
    }
}
