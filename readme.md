# Java to Perl compiler

This project creates a tool that transpiles Java code into Perl code. 

The generated code can be executed in existing Perl environment.

The project's objectives are:

* Bring in a typesystem, boosting development speed and lowering bug occurrence
* Enable a gradual transition from Java to Perl, by converting code file by file.


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

## Converting Perl to Java

Let's start with simple Perl module we want to convert:
```perl
package Bookings::ToMigrate::MyModule;

use Bookings::External::OtherModule;

# declare my_sub for other people to use
# but the question is, what the function actually does?
sub my_sub() {
    # what is args? 
    my ($args) = @_;
    
    # apparently args is a hashref with key poupa
    # but what args{poupa} is? and what is $result?
    my $result = Bookings::OtherModule->do_something($args->{poupa});
    
    # result is some object, it would be nice to know what `volobuev` returns
    # but there is no way to know, consequently we cannot say what the function does
    return $result->volobuev(14);
}
```

Start with defining bindings for existing perl dependencies:
```java
package Bookings.External;

import perl.PerlModule;

// module name is derived from package name + class name
// Bookings::External::OtherModule is derived
interface OtherModule implements PerlModule {
    
    // module declares sub `do_something`
    // sub will be called as Bookings::External::OtherModule->do_something(poupa)
    //
    // it accepts any object which implements interface `Poupa` in Java
    // it will return an object which you can call `volobuev` on
    Loupa do_something(Poupa poupa);
    
    interface Poupa {
        // you don't actually need to define here anything if you don't use it
        // you could probably go with Object here, but having interface adds additional type safety
    }
    
    interface Loupa {
        String volobuev(Number arg);
    }
}
```

Now we can start rewriting MyModule to Java:
```java
package Bookings.ToMigrate;

import perl.PerlModule;
import perl.PerlDto;
import Bookings.External.OtherModule;

// Bookings::ToMigrate::MyModule is inferred
class MyModule implements PerlModule {
    private final OtherModule otherModule;
    
    // declared modules have only one constructor
    // parameters of the constructor must be modules we want to use
    // compiler sees that we require Bookings::External::OtherModule
    // and will generate `use Bookings::External::OtherModule;` statement
    // 
    // lombok.AllArgsConstructor can also be used!
    public MyModule(OtherModule otherModule) {
        this.otherModule = otherModule;
    }
    
    // `my_sub` in perl was accepting some $args, which seemingly had `poupa` key
    // let's be strict here and define what $args was and what type `poupa` is
    public record MySubArgs(
            Poupa poupa
    ) implements PerlDto {
    }
    
    // now we know that args is a HashRef, and poupa has type 'Poupa'
    // great! let's continue with defining `my_sub`
    public String my_sub(MySubArgs args) {
        // look, you can see sub returns String, and you don't have to spend 30 minutes jumping through files
        var result = otherModule.do_something(args.poupa());
        // result is inferred by Compiler as Loupa here!!
        // autocomplete works, you just type '.' and ...
        return result.volobuev(14);
    } 
    
    // we are done!
}
```

All is left is to compile the code. We just run

```
./spooky.sh \
    -java_out j_out
    -perl_out p_out
    -input src
```

As a result we get generated perl source at `p_out/Bookings/ToMigrate/MyModule.pm`, which can be used as any other perl module


Additionally, you get a bunch of `.class` files in `j_out` repository, which can be reused for compiling other modules! 