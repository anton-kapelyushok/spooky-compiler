package Spooky::Example;
use strict;
use warnings;

use parent 'Spooky::Example::Parent';

my $__static_instance;
sub static {
    if (!defined($__static_instance)) {
        $__static_instance = bless({}, __PACKAGE__); # or may be even use separate namespace
        __cl_init();
    }

    return $__static_instance;
}

# @static
sub __cl_init {
    # DECLARE FIELDS
    {
        Spooky::Example->static->{"FIELD"} = undef;
        Spooky::Example->static->{"OTHER_FIELD"} = undef;
    }

    # FIELD VALUES AND STATIC BLOCKS
    {
        Spooky::Example->static->{"FIELD"} = "FIELD_VALUE"
    }

    {
        Spooky::Example->static->{"FIELD"} = Spooky::Example->static->{"FIELD"} . Spooky::Example->static->static_method("tail");
        Spooky::Example->static->{"OTHER_FIELD"} = Spooky::Example->static->{"FIELD"};
    }
}

# @static
sub static_method {
    my ($__static_instance_1, $arg) = @_;
    return $arg;
}

sub new {
    my ($enclosing_instance, @args) = @_;
    my $self = bless({}, __PACKAGE__);

    $self->__init($enclosing_instance, @args);

    return $self
}

sub __init {
    my ($self, $enclosing_instance, $arg1, $arg2, $arg3) = @_;

    $self->{__enclosing} = $enclosing_instance;

    $self->SUPER::__init($self->{__enclosing}->{__enclosing}, $arg2 + $arg3);
    # $self->SUPER::__new(undef, $arg2 + $arg3);
    # $self->SUPER::__new($arg2 + $arg3);

    # DECLARE FIELDS
    $self->{field1} = undef;
    $self->{field2} = undef;

    # FIELD VALUES AND DYNAMIC BLOCKS
    $self->{field1} = Spooky::Example->static->static_method();
    $self->{field2} = $self->poupa($arg1);

    {
        # CONSTRUCTOR STATEMENTS
        # ...
    }
}

sub poupa {
    my ($self, $arg1) = @_;
    return $self;
}

1;