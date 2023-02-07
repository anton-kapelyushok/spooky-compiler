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
