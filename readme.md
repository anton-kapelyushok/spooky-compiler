# Java to Perl compiler

Java to Perl Compiler is designed to bring the benefits of strong typing to existing Perl codebase and to enable a gradual migration from Perl to Java.

## Key Features
* Write in Java, run on existing Perl infrastructure
* Compile time type checking using `javac`
* Interoperable with existing Perl code in both directions
* Ability to migrate one file at a time, allowing for a phased approach to migration without the need to extract the entire service to Java


## Benefits
* Start getting the benefits of typechecking from immediately
* Improved code reliability through strong typing and compile-time error checking.
* Ability to leverage the existing Perl infrastructure, reducing the need for significant refactoring.
* Facilitated migration path from Perl to Java, enabling organizations to modernize their codebase and take advantage of the latest Java technologies.

## How it works
* The Java to Perl Compiler uses `javac` for type checking and semantic analysis
* Interoperability between Java and Perl is achieved through bindings - descriptions of Perl modules in Java
* Output of compiler is Perl modules in .pm format to use in Perl codebase and Java .class files for downstream module compilations
* Input is .java module sources and bindings to compile and precompiled .class files from previous steps

## Show me!

Let's start with simple Perl module we want to convert:

```perl5
package Bookings::ToConvert::MyModule;

use strict;
use warnings;
use Bookings::External::ExternalModule;

sub do_something {
    my ($class, $args) = @_;

    my $loupas = Bookings::External::ExternalModule->convert_to_loupas($args->{poupas});
    return [ map {$_->volobuev(4)} @$loupas ];
}

1;
```

We see that this module depends on `Bookings::External::ExternalModule`, declares one sub `do_something`.

Sub `do_something` accepts some `$args` which we do not know anything about util we inspect function body.

Apparently it has a key `poupas`, and this value can be probably passed to `convert_to_loupas` sub of `ExternalModule`.

Then we try to map result of `convert_to_loupas` and call `volobuev` on each item of the list.
We know it is a list because we try to map it, unless we made a mistake writing the sub.

Finally, we return a list of items returned by `volobuev` method. We cannot say anything specific about it.

**Declaring dependencies:**

Start rewriting the perl code by declaring its dependencies via bindings.
Normally there would be existing bindings for us to use, but here we are starting from scratch.

Here it is:

```java
package Bookings.External;

import perl.ArrayRef;
import perl.PerlModule;

// module name is derived from package name + class name
// Bookings::External::ExternalModule is derived
public interface ExternalModule extends PerlModule {
    
    // module declares sub `do_something`
    // it accepts array of objects which implement interface `Poupa` in Java
    // it will return array of objects which you can call `volobuev` on
    ArrayRef<Loupa> convert_to_loupas(ArrayRef<Poupa> poupa);

    interface Poupa {
    }

    interface Loupa {
        String volobuev(Number arg);
    }
}
```

Great! Now we can continue with rewriting MyModule to Java:

**Rewriting MyModule to Java**

```java
package Bookings.ToConvert;

import Bookings.External.ExternalModule;
import perl.ArrayRef;
import perl.PerlModule;

// Bookings::ToConvert::MyModule is inferred
public class MyModule implements PerlModule {
    private final ExternalModule externalModule;

    // declared modules must have only one constructor
    // parameters of the constructor must be modules we want to use
    // compiler sees that we require Bookings::External::ExternalModule
    // and will generate `use Bookings::External::OtherModule;` statement in an output
    // 
    // lombok.AllArgsConstructor can also be used!
    public MyModule(ExternalModule externalModule) {
        this.externalModule = externalModule;
    }
    
    // `my_sub` in perl was accepting some $args, which seemingly had `poupas` key
    // let's be strict here and define what is $args and what type `poupas` value is
    public record MySubArgs(
            ArrayRef<ExternalModule.Poupa> poupas
    ) implements PerlDto {
    }
    
    // now we know that args is a HashRef, and poupa has type 'Poupa'
    // great! let's continue with defining `my_sub`
    public ArrayRef<String> do_something(MySubArgs args) {
        // autocomplete and typechecking works here!
        // and result is inferred as ArrayRef<Loupa>
        var loupas = externalModule.convert_to_loupas(args.poupas());
        return loupas.map(it -> it.volobuev(4));
    }
}
```

**Compiling**

All is left is to compile the code. We just run

```sh
./spooky.sh \
    -java_out j_out
    -perl_out p_out
    -input src
```

This will output several files:
- `p_out/Bookings/ToConvert/MyModule.pm` - can be called from Perl (check output on the bottom of the readme)
- `j_out/Bookings/External/ExternalModule.class` - intermediate representations to use in other modules
- `j_out/Bookings/External/ExternalModule$Loupa.class`
- `j_out/Bookings/External/ExternalModule$Poupa.class`
- `j_out/Bookings/ToConvert/MyModule.class`
- `j_out/Bookings/ToConvert/MySubArgs.class`

## Usage:

```sh
./spooky.sh \
    -cp <path1>:<path2>  \  # java declarations
    -java_out <java_out> \  # java output
    -perl_out <perl_out> \  # perl output
    -input <java_in>        # java input
```

Example:

```sh
./spooky.sh \
  -cp booking-lib/build/classes/java/main/ \
  -java_out java_out -perl_out perl_out \
  -input my-lib/src/main/java
```


## Build

```
./gradlew clean build
```



## p_out/Bookings/ToConvert/MyModule.pm output

```perl5
package Bookings::ToConvert::MyModule;
 
use strict;
use warnings;
 
# MODULE IMPORTS
use Bookings::External::ExternalModule;
 
# MODULE INIT
my $self_25 = __PACKAGE__;
my $externalModule_26 = undef;
my $Bookings__External__ExternalModule_27 = "Bookings::External::ExternalModule";
undef;
$externalModule_26 = $Bookings__External__ExternalModule_27;
 
# MODULE DECLARATIONS
sub do_something {
    my ($self_28, $args_29) = @_;
 
    my $loupas_30 = $externalModule_26->convert_to_loupas(
        $args_29->{("poupas")},
    );
    my $sub_32 = sub {
        my ($it_31) = @_;
 
        return $it_31->volobuev(
            4,
        );
    };
    return [ map {
        $sub_32->($_);
    } $loupas_30->@* ];
}
 
1;
```