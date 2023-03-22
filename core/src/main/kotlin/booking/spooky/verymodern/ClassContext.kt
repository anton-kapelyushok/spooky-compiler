@file:Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE")

package booking.spooky.verymodern

import booking.spooky.*
import com.sun.source.tree.*
import com.sun.tools.javac.code.Symbol
import javax.lang.model.element.Modifier


class ClassTranslator {
    fun translateClass(parentContext: ClassContext?, classTree: ClassTree): PModule {
        val (staticMembers, dynamicMembers) = classTree.members.partition { member ->
            when (member) {
                is VariableTree -> member.modifiers.flags.contains(Modifier.STATIC)
                is MethodTree -> member.modifiers.flags.contains(Modifier.STATIC)
                is ClassTree -> member.modifiers.flags.contains(Modifier.STATIC)
                is BlockTree -> member.isStatic
                else -> error("unexpected class member type ${member.kind}:\n$member")
            }
        }

        val classContext = ClassContext(/* of this type */)

        // declarations
        dynamicMembers.forEach { member ->
            when (member) {
                is VariableTree -> {
                    val key = JavacSymbolKey(member.sym!!)
                    classContext.declareField(key, member.name.toString())
                }

                is MethodTree -> {
                    val key = JavacSymbolKey(member.sym!!)

                    if (member.name.toString() != "<init>") {
                        classContext.declareMethod(key, member.name.toString())
                    } else {
                        classContext.declareConstructor(key)
                    }
                }

                // inner class declarations go here!
            }
        }


        val preconstructorContext = classContext.defaultConstructorContext()
        val thisKey = ThisKey

        dynamicMembers.forEach { member ->
            when (member) {
                is VariableTree -> {
                    if (member.initializer != null) {
                        val key = JavacSymbolKey(member.sym!!)
                        val rValue = translateExpression(preconstructorContext, member.initializer)
                        preconstructorContext.setField(thisKey, key, rValue)
                    }
                }

                is BlockTree -> {
                    translateStatement(preconstructorContext, member)
                }
            }
        }

        dynamicMembers.filterIsInstance<MethodTree>().filter { it.name.toString() == "<init>" }
            .take(1)
            .forEach { constructorTree ->
                val methodRef = classContext.resolveConstructor(JavacSymbolKey(constructorTree.sym!!))
                val ctx = methodRef.context
                preconstructorContext.statements.forEach { ctx.addStatement(it) }
                constructorTree.parameters.forEach { p -> ctx.addParameter(JavacSymbolKey(p.sym!!), p.name.toString()) }
                constructorTree.body.statements.forEach { bodyStatement -> translateStatement(ctx, bodyStatement) }
            }

        val singleConstructor = classContext.constructors.values.single()

        val module = PModule(
            name = classTree.sym.toString(),
            imports = mutableListOf(),
            initStatements = mutableListOf(),
            subDeclarations = mutableListOf(
                PSubDecl(
                    name = "new",
                    parameters = singleConstructor.context.parameters.values.map { PIdent(it.name) }.toMutableList(),
                    body = mutableListOf(
                        PVarDecl(PIdent("self"), PUnrealExpression("bless({}, ${classTree.sym.toString()})")),
                        PExpressionStatement(
                            PMethodCall(
                                PIdent("self"),
                                "__new",
                                singleConstructor.context.parameters.values.map { PIdent(it.name) }.toMutableList()
                            )
                        ),
                        PReturn(PIdent("self"))
                    )
                ),
                PSubDecl(
                    name = "__new",
                    parameters = (listOf(PIdent("self")) + singleConstructor.context.parameters.values.map { PIdent(it.name) }).toMutableList(),
                    body = singleConstructor.context.statements,
                )
            )
        )

        return module
    }
}

interface Context {
    fun addStatement(statement: PStatement)
}

fun translateExpression(context: Context, expressionTree: ExpressionTree): PExpression {
    return when (expressionTree) {
        is LiteralTree -> {
            when (val v = expressionTree.value) {
                null -> PUndef()
                is Number -> PNumber(v)
                is String -> PString(v)
                else -> error("Unknown literal $expressionTree")
            }
        }

        is MethodInvocationTree -> {
            val methodSelect = expressionTree.methodSelect
            when (methodSelect) {
                is MemberSelectTree -> {
                    methodSelect
                }

                is IdentifierTree -> {

                    methodSelect
                }

                else -> error("Unknown MethodInvocationTree methodSelect: $methodSelect")
            }
            return PExpression()
        }

        else -> PUnrealExpression(expressionTree.toString())
//        else -> error("Unknown expression ${expressionTree.javaClass}: $expressionTree")
    }

}

fun translateStatement(context: Context, statementTree: StatementTree) {
    when (statementTree) {
        is ExpressionStatementTree -> context.addStatement(
            PExpressionStatement(
                translateExpression(
                    context,
                    statementTree.expression
                )
            )
        )

        else -> context.addStatement(PUnrealStatement(statementTree.toString()))
//        else -> error("Unknown statement ${statementTree.javaClass}: $statementTree")
    }

}

interface VarRef {
    val name: String
}

class MethodRef {
    val context = MethodContext2()
}

class FieldRef(override val name: String) : VarRef {
    override fun toString(): String {
        return "FieldRef($name)"
    }
}

class ClassContext : Context {

    val fields = mutableMapOf<SymKey, FieldRef>()
    val methods = mutableMapOf<SymKey, MethodRef>()

    val constructors = mutableMapOf<SymKey, MethodRef>()

    fun declareField(key: SymKey, name: String) {
        fields[key] = FieldRef(name)
    }

    fun declareMethod(key: SymKey, name: String) {}

    override fun addStatement(statement: PStatement) {}

    fun defaultConstructorContext(): MethodContext2 {
        return MethodContext2()
    }

    fun declareConstructor(key: SymKey): MethodRef {
        val mr = MethodRef()
        constructors[key] = MethodRef()
        return mr
    }

    fun resolveConstructor(key: SymKey): MethodRef {
        return constructors[key]!!
    }
}

class MethodContext2 : Context {
    val parameters = LinkedHashMap<SymKey, VarRef>()
    val statements = mutableListOf<PStatement>()

    fun setField(holder: SymKey, key: SymKey, expression: PExpression) {
        addStatement(
            PExpressionStatement(
                PAssign(
                    PHashRefGet(resolveFieldHolder(holder), PString((key as? JavacSymbolKey)!!.sym.name.toString())),
                    expression
                )
            )
        )
    }

    fun addParameter(key: SymKey, name: String) {}

    override fun addStatement(statement: PStatement) {
        statements += statement
    }

    private fun resolveFieldHolder(holder: SymKey): PExpression {
        if (holder is ThisKey) {
            return PIdent("self")
        }
        error("")
    }
}