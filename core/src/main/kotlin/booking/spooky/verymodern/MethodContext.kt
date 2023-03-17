@file:Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE")

package booking.spooky.verymodern

import booking.spooky.*
import com.sun.source.tree.ExpressionTree
import com.sun.source.tree.IdentifierTree
import com.sun.source.tree.MemberSelectTree
import com.sun.source.tree.MethodInvocationTree
import com.sun.tools.javac.code.Symbol
import com.sun.tools.javac.code.Types

class Scope {

    fun addArgument(key: SymKey) {}


}


class MethodContext(
    val isStatic: Boolean,
    val key: SymKey,
    val sym: Symbol = (key as JavacSymbolKey).sym,
    val name: String,
    val owner: Symbol,
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
    fun variableIdentifier(identifierTree: IdentifierTree): PExpression {
        val identifierSym = identifierTree.sym!!
        if (identifierSym.isStatic) {
            val owner = identifierSym.owner.toString()

            // resolve(ModuleKey, IdentifierKey)
            return PUnrealExpression("\$$owner::${identifierTree.name}")
        }

        if (identifierTree.name.toString() == "this") {
            // resolve(This)
            return PIdent("self")
        }

        val identifierOwner = identifierSym.owner
        if (identifierOwner is Symbol.ClassSymbol) {
            if (types.isAssignable(owner.type, identifierOwner.type)) {
                // resolveField(This, Type, Sym)
                return PUnrealExpression("\$self->{${identifierOwner.type}}->{${identifierTree.name}}")
            }

            // must be enclosing class at this point
            val enclosingClasses = generateSequence(this.owner.type.enclosingType) { it.enclosingType }
            for (enclosingClass in enclosingClasses) {
                if (types.isAssignable(enclosingClass, identifierOwner.type)) {
                    // resolveField(EnclosingThis(enclosingClass), Type, Sym)
                    return PUnrealExpression("\$self->{__enclosing}->{${enclosingClass}}->${identifierOwner.type}->{${identifierTree.name}}")
                }
            }

            error("Don't understand what is happening")
        }

        if (identifierSym !is Symbol.VarSymbol) {
            error("What?")
        }

        // resolveLocal(Sym)
        return PUnrealExpression("\$local_or_captured_or_argument____${identifierTree.name}")
    }

    fun invokeMethod(methodInvocationTree: MethodInvocationTree): PExpression {
        val methodSelect = methodInvocationTree.methodSelect
        val arguments = methodInvocationTree.arguments.map { resolveExpression(it) }

        when (methodSelect) {
            is MemberSelectTree -> {
                val memberSelectTree = methodSelect as MemberSelectTree
                val memberSelectSym = memberSelectTree.sym!!
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
                val identifierSym = identifier.sym!!
                if (identifierSym !is Symbol.MethodSymbol) error("what?")
                if (identifierSym.isStatic) {
                    val owner = identifierSym.owner.toString()
                    // invoke_static(ModuleKey, IdentifierKey)
                    return PStaticMethodCall(resolveModuleName(owner), identifierSym.name.toString(), arguments)
                }
                if (identifierSym.name.toString() == "this") {
                    error("Not supported")
                }
                if (identifierSym.name.toString() == "super") {
                    // for regular classes this will just become $self->SUPER::__new()
                    // but for enclosed classes figure out whether we need to pass enclosed `this`
                    error("Not supported yet")
                }

                val identifierOwner = identifierSym.owner

                // regular method invocation
                // resolution order: this, super, enclosing
                if (this.owner.toString() == identifierOwner.type.toString()) {
                    // this method call
                    // PMethodCall(resolveActualThis(), methodName)
                    // return invoke ( resolve(This)), SymKey )
                    return PUnrealExpression("\$self->${identifier.name}(...)") //     ?
                }

                if (types.isAssignable(this.owner.type, identifierOwner.type)) {
                    // this method call
                    // PMethodCall(resolveActualThis(), methodName)
                    // return invoke ( resolve(This), IdentifierKey )
                    return PUnrealExpression("\$self->${identifier.name}(...)")
                }

                // must be enclosing class at this point
                val enclosingClasses = generateSequence(this.owner.type.enclosingType) { it.enclosingType }
                for (enclosingClass in enclosingClasses) {
                    if (types.isAssignable(enclosingClass, identifierOwner.type)) {
                        // resolve this[enclosingClass::type]
                        // return invoke ( resolve(EnclosingThis(fqclassname)), IdentifierKey )
                        return PUnrealExpression("\$self->{__enclosing}->{${enclosingClass}}->${identifier.name}(...)")
                    }
                }

                error("Don't understand what is happening")
            }

            else -> error("Unknown MethodInvocationTree methodSelect: $methodSelect")
        }
    }

    fun resolve(key: SymKey): PExpression {
        return PExpression()
    }

    fun resolveExpression(expressionTree: ExpressionTree): PExpression {
        return PUnrealExpression(expressionTree.toString())
    }

    private fun resolveModuleName(fqClassName: String): String {
        return fqClassName
    }

    private fun callStaticMethod(fqClassName: String, methodName: String, arguments: List<PExpression>): PExpression {
        return PStaticMethodCall(resolveModuleName(fqClassName), name, arguments)
    }
}