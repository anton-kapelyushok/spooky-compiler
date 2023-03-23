@file:Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE")

package booking.spooky.verymodern

import booking.spooky.*
import com.sun.source.tree.ExpressionTree
import com.sun.source.tree.IdentifierTree
import com.sun.source.tree.MemberSelectTree
import com.sun.source.tree.MethodInvocationTree
import com.sun.tools.javac.code.Symbol
import com.sun.tools.javac.code.Type
import com.sun.tools.javac.code.Types

interface SymKey
object ThisKey : SymKey // or may be ThisKey(val type: Type) ?
data class EnclosingThisKey(val type: Type) : SymKey
data class JavacSymbolKey(val sym: Symbol) : SymKey
data class ModuleKey(val type: Type): SymKey

class Scope {

    lateinit var enclosingScope: Scope
    lateinit var globalScope: Scope

    fun addArgument(key: SymKey) {}
}

class MethodContext(
    val isStatic: Boolean,
    val key: SymKey,
    val methodSym: Symbol = (key as JavacSymbolKey).sym,
    val name: String,
    val methodOwner: Symbol.ClassSymbol,
    val types: Types,
) {
    val scope: Scope = Scope()

    fun addArgument(key: SymKey) {
    }

    fun addLocalVariable(key: SymKey) {
    }

    fun addCapture(key: SymKey) {
    }

    /**
     * returns an expression which can be used both as lvalue and rvalue
     */
    fun memberSelect(memberSelectTree: MemberSelectTree): PExpression {
        val selectedSym = memberSelectTree.sym
        val owner = selectedSym.owner.toString()

        if (selectedSym.isStatic) {
            // resolve(ModuleKey, IdentifierKey)
            return PUnrealExpression("\$$owner::${memberSelectTree.identifier}")
        }

        if (memberSelectTree.identifier.toString() == "this") {
            val ownerType = memberSelectTree.expression.type!!
            if (memberSelectTree.expression.type!! == methodOwner.type) {
//                return PUnrealExpression("\$self")
                return resolveVariable(ThisKey)
            }
            // left part of expression will always be type, no inheritance
            // if (type == methodOwner) -> $self
            // else $self -> {enclosing} -> {type}
//            return PUnrealExpression("\$self->{__enclosing}->{${ownerType}}")
            return resolveVariable(EnclosingThisKey(ownerType))
        }

        if (memberSelectTree.identifier.toString() == "super") {
            // always enclosing class?
            val ownerType = memberSelectTree.expression.type!!
            return resolveVariable(EnclosingThisKey(ownerType))
//            return PUnrealExpression("\$self->{__enclosing}->{${ownerType}}")
        }

        val expressionTree = memberSelectTree.expression
        val expr = resolveExpression(expressionTree)

        // resolve(expr, selectedSym)
//        return PUnrealExpression("$expr->{$owner}->{${memberSelectTree.identifier}")
        return resolveField(expr, JavacSymbolKey(selectedSym))
    }

    /**
     * returns an expression which can be used both as lvalue and rvalue
     */
    fun variableIdentifier(identifierTree: IdentifierTree): PExpression {
        val identifierSym = identifierTree.sym
        if (identifierSym.isStatic) {
            val owner = identifierSym.owner.toString()

            // resolve(ModuleKey, IdentifierKey)
            return PUnrealExpression("\$$owner::${identifierTree.name}")
        }

        if (identifierTree.name.toString() == "this") {
            // resolve(This)
            return resolveVariable(ThisKey)
        }

        if (identifierTree.name.toString() == "super") {
            return resolveVariable(ThisKey)
        }

        val identifierOwner = identifierSym.owner
        if (identifierOwner is Symbol.ClassSymbol) {
            if (types.isAssignable(methodOwner.type, identifierOwner.type)) {
                // resolveField(This, Type, Sym)
                return resolveField(resolveVariable(ThisKey), JavacSymbolKey(identifierSym))
//                return PUnrealExpression("\$self->{${identifierOwner.type}}->{${identifierTree.name}}")
            }

            // must be enclosing class at this point
            val enclosingClasses = generateSequence(this.methodOwner.type.enclosingType) { it.enclosingType }
            for (enclosingClass in enclosingClasses) {
                if (types.isAssignable(enclosingClass, identifierOwner.type)) {
                    // resolveField(EnclosingThis(enclosingClass), Type, Sym)
                    return resolveField(
                        resolveVariable(EnclosingThisKey(enclosingClass)),
                        JavacSymbolKey(identifierSym)
                    )
//                    return PUnrealExpression("\$self->{__enclosing}->{${enclosingClass}}->${identifierOwner.type}->{${identifierTree.name}}")
                }
            }

            error("Don't understand what is happening")
        }

        if (identifierSym !is Symbol.VarSymbol) {
            error("What?")
        }

        // resolveLocal(Sym)
        return resolveVariable(JavacSymbolKey(identifierSym))
//        return PUnrealExpression("\$local_or_captured_or_argument____${identifierTree.name}")
    }

    fun invokeMethod(methodInvocationTree: MethodInvocationTree): PExpression {
        val methodSelect = methodInvocationTree.methodSelect
        val arguments = methodInvocationTree.arguments.map { resolveExpression(it) }

        when (methodSelect) {
            is MemberSelectTree -> {
                val memberSelectTree = methodSelect as MemberSelectTree
                val memberSelectSym = memberSelectTree.sym
                if (memberSelectSym.isStatic) {
                    // symbol instead of string
                    val owner = memberSelectSym.owner.toString()
                    // invoke_static(ModuleKey, IdentifierKey)
                    return PStaticMethodCall(
                        resolveModuleName(owner),
                        memberSelectTree.identifier.toString(),
                        arguments
                    )
                }

                val exprTree = memberSelectTree.expression
                if (exprTree is IdentifierTree && exprTree.name.toString() == "super") {
                    // invoke_super(resolve(This), IdentifierKey)
                    return PUnrealExpression("\$self->SUPER::${memberSelectTree.identifier}(...)")
                }
                val expr = resolveExpression(exprTree)

                // does PExpression have type here?
                // invoke(PExpression, IdentifierKey)
                return PMethodCall(expr, memberSelectTree.identifier.toString(), arguments.toMutableList())
            }

            is IdentifierTree -> {
                val identifier = methodSelect as IdentifierTree
                val identifierSym = identifier.sym
                if (identifierSym !is Symbol.MethodSymbol) error("what?")
                if (identifierSym.isStatic) {
                    val owner = identifierSym.owner.toString()
                    // invoke_static(ModuleKey, IdentifierKey)
                    return PStaticMethodCall(resolveModuleName(owner), identifierSym.name.toString(), arguments)
                }
                if (identifier.name.toString() == "this") {
                    error("Not supported - only single constructor is supported (now?)")
                }
                if (identifier.name.toString() == "super") {
                    // for regular classes this will just become $self->SUPER::__init()
                    // but for enclosed classes figure out whether we need to pass enclosed `this`
                    if (identifierSym.owner.type.enclosingType != Type.noType) {
                        val enclosingClasses =
                            generateSequence(this.methodOwner.type.enclosingType) { it.enclosingType }
                        for (enclosingClass in enclosingClasses) {
                            if (types.isAssignable(enclosingClass, identifierSym.owner.type.enclosingType)) {
                                val enclosingExpr = resolveVariable(EnclosingThisKey(enclosingClass))
                                return PUnrealExpression("\$self->SUPER::__init($enclosingExpr, ...)")
                            }
                        }
                        error("Not enclosed")
                    }
                    return PUnrealExpression("\$self->SUPER::__init(...)")
                }

                val identifierOwner = identifierSym.owner

                // regular method invocation
                // resolution order: this, super, enclosing
                if (this.methodOwner.toString() == identifierOwner.type.toString()) {
                    // this method call
                    // PMethodCall(resolveActualThis(), methodName)
                    // return invoke ( resolve(This)), SymKey )
                    return PMethodCall(
                        resolveVariable(ThisKey),
                        identifier.name.toString()
                    )
//                    return PUnrealExpression("\$self->${identifier.name}(...)") //     ?
                }

                if (types.isAssignable(this.methodOwner.type, identifierOwner.type)) {
                    // this method call
                    // PMethodCall(resolveActualThis(), methodName)
                    // return invoke ( resolve(This), IdentifierKey )
//                    return PUnrealExpression("\$self->${identifier.name}(...)")
                    return PMethodCall(
                        resolveVariable(ThisKey),
                        identifier.name.toString()
                    )
                }

                // must be enclosing class at this point

                /*
                val enclosingClasses = generateSequence(this.methodOwner.type.enclosingType) { it.enclosingType }
                for (enclosingClass in enclosingClasses) {
                    if (types.isAssignable(enclosingClass, identifierOwner.type)) {
                        // resolve this[enclosingClass::type]
                        // return invoke ( resolve(EnclosingThis(fqclassname)), IdentifierKey )
                        return PUnrealExpression("\$self->{__enclosing}->{${enclosingClass}}->${identifier.name}(...)")
                    }
                }
                error("Don't understand what is happening")
                */
                return PMethodCall(
                    resolveVariable(EnclosingThisKey(identifierOwner.type)),
                    identifier.name.toString(),
                    arguments.toMutableList()
                )
            }

            else -> error("Unknown MethodInvocationTree methodSelect: $methodSelect")
        }
    }

    fun resolveField(expr: PExpression, key: SymKey): PExpression {
        if (key !is JavacSymbolKey) error("$key not supported")
        return PHashRefGet(
            PHashRefGet(
                expr,
                PString(key.sym.owner.type.toString())

            ), PString(key.sym.name.toString())
        )
    }

    fun resolveVariable(key: SymKey): PExpression {
        return when (key) {
            is ThisKey -> {
                // actually need to access scope here
                PIdent("self")
            }

            is EnclosingThisKey -> {
                val thisExpr = resolveVariable(ThisKey)

                val enclosingClasses = generateSequence(this.methodOwner.type.enclosingType) { it.enclosingType }
                var count = 0
                for (enclosingClass in enclosingClasses) {
                    count++
                    if (types.isAssignable(enclosingClass, key.type)) {
                        var expr = thisExpr
                        for (i in 1..count) {
                            expr = PHashRefGet(expr, PString("__enclosing"))
                        }
                        return expr
                    }
                }

                error("wtf?")
            }

            is JavacSymbolKey -> {
                return PUnrealExpression("\$local_or_captured_or_argument____${key.sym.name}")
            }

            else -> error("$key not supported")
        }
    }

    fun resolveExpression(expressionTree: ExpressionTree): PExpression {
        when (expressionTree) {
            is IdentifierTree -> return variableIdentifier(expressionTree)
            is MethodInvocationTree -> return invokeMethod(expressionTree)
            is MemberSelectTree -> return memberSelect(expressionTree)
        }
        return PUnrealExpression(expressionTree.toString())
    }

    private fun resolveModuleName(fqClassName: String): String {
        return fqClassName
    }

    private fun callStaticMethod(fqClassName: String, methodName: String, arguments: List<PExpression>): PExpression {
        return PStaticMethodCall(resolveModuleName(fqClassName), name, arguments)
    }
}