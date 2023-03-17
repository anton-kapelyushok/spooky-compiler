@file:Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE")

package booking.spooky.verymodern

import booking.spooky.PExpression
import booking.spooky.PStatement
import com.sun.source.tree.*
import com.sun.tools.javac.code.Symbol
import javax.lang.model.element.Modifier

interface SubCtx {
    fun call(arguments: List<PExpression>): PExpression
    fun addArgument(name: String): VarRef1
    fun addStatement(stmt: PStatement)
    fun declareVariable(name: String): VarRef1
}

interface VarRef1 {
    fun getter(): PExpression
    fun setter(value: PExpression): PStatement
}

interface ModuleCtx {
    fun declareSubroutine(name: String): SubCtx
}

interface DynamicScope {
    fun declareVar(selfRef: VarRef1, name: String): VarRef1
}

interface SubroutineScope {
    fun declareVar(name: String): VarRef1 // <-- wtf? why interface has changed?
}

object SelfKey

class ClassBuilder(val classTree: ClassTree) {


    fun build() {
        val moduleCtx: ModuleCtx = null!!

        val (staticMembers, dynamicMembers) = classTree.members.partition { member ->
            when (member) {
                is VariableTree -> member.modifiers.flags.contains(Modifier.STATIC)
                is MethodTree -> member.modifiers.flags.contains(Modifier.STATIC)
                is ClassTree -> member.modifiers.flags.contains(Modifier.STATIC)
                is BlockTree -> member.isStatic
                else -> error("unexpected class member type ${member.kind}:\n$member")
            }
        }

        val dynamicScope: DynamicScope = null!!

        val commonConstructorRef = moduleCtx.declareSubroutine("__common_constructor")

        val mit: MethodInvocationTree = null!!;
        mit.methodSelect

        commonConstructor@ run {
            val selfRef = commonConstructorRef.declareVariable("self")

            dynamicMembers.filterIsInstance<VariableTree>().forEach { variable ->
                val name = variable.name
                dynamicScope.declareVar(selfRef, name.toString())
            }

        }

        dynamicMembers.forEach { member ->
            when (member) {
                is VariableTree -> {
                    member.name
                    member.initializer
                    member.nameExpression
                    member.modifiers
                    val sym = member.sym!!
                }

                else -> error("unexpected dynamic class member type ${member.kind}:\n $member")
            }
        }
    }

    fun handleMethodInvocation(mit: MethodInvocationTree): PExpression {
        return PExpression()
    }
}

fun memberSelector(ms: MemberSelectTree, silent: Boolean = false): String {
    if (!silent) {
        println("== memberSelector $ms")
    }
    if (ms.sym !is Symbol.VarSymbol && ms.sym !is Symbol.MethodSymbol) {
        return "???".also { println(it) }
    }
    if (ms.identifier.toString() == "this") {
        return "this[${ms.type}]".also { if (!silent) println(it) }
    } else if (ms.expression is MethodInvocationTree) {
        return "METHOD_INVOCATION.${ms.identifier}".also { if (!silent) println(it) }
    } else if (ms.expression is NewClassTree) {
        return "NEW_CLASS.${ms.identifier}".also { if (!silent) println(it) }
    } else if (ms.expression.sym is Symbol.VarSymbol) {
        // TODO: field select based on owner
        // assume data in following shape:
        // child: {
        //	CHILD_CLASS: {field1}
        //	PARENT_CLASS: {field1}
        //}
        if (ms.expression is MemberSelectTree) {
            return (memberSelector(
                ms.expression as MemberSelectTree,
                silent = true
            ) + "{${ms.sym!!.owner}}.${ms.identifier}").also {
                if (!silent) println(
                    it
                )
            }
        } else if (ms.expression is IdentifierTree) {
            return (identifierGetter(
                ms.expression as IdentifierTree,
                silent = true
            ) + "{${ms.sym!!.owner}}.${ms.identifier}").also {
                if (!silent) println(
                    it
                )
            }
        }
    } else if (ms.expression is IdentifierTree && ms.expression.sym is Symbol.ClassSymbol) {
        val type = ms.expression.sym!!.type
        return "$type.${ms.identifier}".also { if (!silent) println(it) }
    }
    return "???".also { if (!silent) println(it) }
}


fun identifierGetter(it: IdentifierTree, silent: Boolean = false): String {
    if (it.sym !is Symbol.VarSymbol && it.sym !is Symbol.MethodSymbol) return "???"
    if (!silent) println("== identifierGetter $it")
    try {
        val sym = it.sym!!
        if (sym.isStatic) {
            val owner = sym.owner as Symbol.ClassSymbol
            val ownerType = sym.owner.type
            // resolve(Module(owner), sym)
            return "${owner}.${it.name}".also { if (!silent) println(it) }
        } else {
            sym.isDynamic
            val owner = sym.owner
            if (owner is Symbol.MethodSymbol) {
                // symbol must be somewhere in local scope

                // resolve(sym)

                return "${it.name}".also { if (!silent) println(it) }
            } else {
                if (it.name.toString() == "this") {
                    owner as Symbol.ClassSymbol
                    // ownerType is incorrect - somehow need to find actual holder
                    val ownerType = owner.type

                    // resolve(This(ownerType).resolve(sym))

                    return "this[${ownerType}]".also { if (!silent) println(it) }
                } else {
                    owner as Symbol.ClassSymbol
                    val ownerType = owner.type

                    // resolve(This(ownerType)).resolve(sym)

                    return "this[${ownerType}].${it.name}".also { if (!silent) println(it) }
                }
                // resolve(This(ownerType), sym)
                return "???".also { if (!silent) println(it) }
            }
        }
    } catch (e: Throwable) {
        println("could not handle $it")
        e.printStackTrace()
        return "???"
    }
}
