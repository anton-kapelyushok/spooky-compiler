@file:Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE")

package booking.spooky

import com.sun.source.tree.*
import com.sun.source.util.JavacTask
import com.sun.tools.javac.api.JavacTrees
import com.sun.tools.javac.code.Symbol
import com.sun.tools.javac.code.Symbol.ClassSymbol
import com.sun.tools.javac.code.Type
import javax.lang.model.element.Modifier

class SpookyCodeGenerator(
    val trees: JavacTrees,
    val cus: List<CompilationUnitTree>,
    val task: JavacTask,
) {
    val namer = Namer()

    class SymTable(
        val namer: Namer,
        val parent: SymTable? = null
    )
    {
        constructor(symTable: SymTable) :
                this(symTable.namer, symTable) {
        }

        val symbols = mutableMapOf<Any, PSymbol>()


        fun addSelf(): PSymbol {
            symbols[SelfKey] = PSymbol(namer.nameFor("self"), "self")
            return symbols[SelfKey]!!
        }

        fun getSelf(): PSymbol? {
            return symbols[SelfKey] ?: parent?.getSelf()
        }

        fun addModule(
            sym: Symbol.ClassSymbol,
            name: String,
        ): PSymbol {
            symbols[sym] = PSymbol(name, "module")
            return symbols[sym]!!
        }

        fun addVar(
            sym: Symbol,
            name: String
        ): PSymbol {
            symbols[sym] = PSymbol(namer.nameFor(name), "var")

            return symbols[sym]!!
        }

        fun psymbol(sym: Symbol): PSymbol? {
            return symbols[sym] ?: parent?.psymbol(sym)
        }

        fun generateName(name: String): String {
            return namer.nameFor(name)
        }

        object SelfKey

        data class PSymbol(val name: String, val type: String)
    }

    class Namer {
        var i = 0
        fun nameFor(name: String): String {
            return name + "_${++i}";
        }
    }

    fun compile(cus: List<CompilationUnitTree>, symTable: SymTable): List<PModule> {
        val modules = mutableListOf<PModule>()
        cus.forEach { cu ->
            if (cu.packageName.toString().startsWith("perl")) return@forEach
            modules += compileCu(cu, symTable)
        }
        return modules
    }

    fun compileCu(cu: CompilationUnitTree, symTable: SymTable): List<PModule> {
        val modules = mutableListOf<PModule>()

        cu.typeDecls.forEach { classTree ->
            if (classTree !is ClassTree) error("node is not ClassTree: $classTree")
            modules += compileClassTree(classTree, symTable)
        }

        return modules
    }

    fun compileClassTree(classTree: ClassTree, symTable: SymTable): List<PModule> {
        val ifaces = implementedInterfaces(classTree)
        return when {
            classTree.kind == Tree.Kind.INTERFACE -> compileInterface(classTree, symTable)
            classTree.kind == Tree.Kind.RECORD && "perl.PerlDto" in ifaces -> compilePerlDto(classTree, symTable)
            classTree.kind == Tree.Kind.CLASS && "perl.PerlModule" in ifaces -> compilePerlModule(classTree, symTable)
            else -> error("cannot compile $classTree")
        }
    }

    fun compileInterface(classTree: ClassTree, symTable: SymTable): List<PModule> {
        val members = classTree.members
        val modules = mutableListOf<PModule>()
        members.forEach {
            if (it is ClassTree) {
                modules += compileClassTree(it, symTable)
            } else if (it is MethodTree) {
                if (it.modifiers.flags.contains(Modifier.DEFAULT)) {
                    error("default methods are not supported at $classTree")
                }
            }
        }
        return modules
    }

    fun compilePerlDto(classTree: ClassTree, symTable: SymTable): List<PModule> {
        if (classTree.members.filter { it is MethodTree }.size != 1) {
            error("PerlDto ${classTree.simpleName} record should not have any methods or constructors")
        }

        val nonPrivateFinalFields = classTree.members.filter { it is VariableTree }
            .filter { !(it as VariableTree).modifiers.flags.containsAll(setOf(Modifier.FINAL, Modifier.PRIVATE)) }

        if (nonPrivateFinalFields.isNotEmpty()) {
            error("PerlDto ${classTree.simpleName} should not have any fields in body")
        }

        val unrecognized = classTree.members.filter {
            it !is ClassTree
                    && it !is VariableTree
                    && it !is MethodTree
        }

        if (unrecognized.isNotEmpty()) {
            error("PerlDto ${classTree.simpleName} has unrecognized statements: $unrecognized")
        }

        val modules = mutableListOf<PModule>()
        classTree.members.filterIsInstance<ClassTree>()
            .forEach {
                if (classTree.kind == Tree.Kind.CLASS && !it.modifiers.flags.contains(Modifier.STATIC)) {
                    error("${it.simpleName} should be static")
                }

                modules += compileClassTree(it, symTable)
            }

        return modules
    }

    fun compilePerlModule(classTree: ClassTree, parentSymTable: SymTable): List<PModule> {
        // expected - one constructor
        // fields
        // subroutines
        // other declarations

        val symTable = SymTable(parentSymTable)

        val unrecognized = classTree.members.filter {
            it !is ClassTree
                    && it !is VariableTree
                    && it !is MethodTree
        }

        if (unrecognized.isNotEmpty()) {
            error("PerlModule ${classTree.simpleName} has unrecognized statements: $unrecognized")
        }

        val moduleName = (classTree.sym!!.packge().fullname.toString() + "." + classTree.sym!!.name.toString())
            .replace(".", "::")

        val constructorNode = classTree.members.filterIsInstance<MethodTree>()
            .filter { it.name.toString() == "<init>" }.singleOrNull()
            ?: error("${classTree.simpleName} should have only one constructor")


        val imports = constructorNode.parameters.map {
            val type = it.type.sym!!

            val importModuleName = type.qualifiedName.toString().replace(".", "::")

            symTable.addModule(type as ClassSymbol, importModuleName)
            importModuleName
        }

        val initStatements = mutableListOf<PStatement>()
        val selfSymbol = symTable.addSelf()
        run {
            initStatements += PVarDecl(PIdent(selfSymbol.name), PGenericLiteral("__PACKAGE__"))

            classTree.members.filterIsInstance<VariableTree>()
                .forEach {
                    initStatements += compileVarDecl(it, symTable)
                }

            val constructorSymTable = SymTable(symTable)
            constructorNode.parameters.forEach { param ->
                val paramModuleName = constructorSymTable.psymbol(param.type.sym!!)!!.name
                val psymbol = symTable.addVar(param.sym!!, paramModuleName.replace(":", "_"))
                initStatements += PVarDecl(
                    PIdent(psymbol.name),
                    PString(symTable.psymbol(param.type.sym!!)!!.name)
                )
            }

            constructorNode.body.statements.forEach { stmt ->
                initStatements += compileStatement(stmt, constructorSymTable)
            }
        }

        val modules = mutableListOf<PModule>()
        val subDeclarations = classTree.members.filterIsInstance<MethodTree>()
            .filter { it.name.toString() != "<init>" }
            .map {
                compileSubDeclaration(it, symTable)
            }.toMutableList()

        val thisModule = PModule(
            name = moduleName,
            imports = imports.toMutableList(),
            initStatements = initStatements,
            subDeclarations = subDeclarations,
        )

        modules += thisModule
        classTree.members.filterIsInstance<ClassTree>().forEach {
            modules += compileClassTree(it, parentSymTable)
        }

        return modules
    }

    fun compileSubDeclaration(methodDeclaration: MethodTree, _symTable: SymTable): PSubDecl {
        val symTable = SymTable(_symTable)

        val parameters = mutableListOf<PIdent>()
        val selfSymbol = symTable.addSelf()
        parameters += PIdent(selfSymbol.name)

        methodDeclaration.parameters.forEach {
            val psymbol = symTable.addVar(it.sym!!, it.name.toString())
            parameters += PIdent(psymbol.name)
        }

        val body = methodDeclaration.body.statements.map { compileStatement(it, symTable) }
            .toMutableList()

        return PSubDecl(methodDeclaration.name.toString(), parameters, body)
    }

    fun compileVarDecl(varDecl: VariableTree, symTable: SymTable): PVarDecl {
        val varName = varDecl.name.toString()

        val psymbol = symTable.addVar(varDecl.sym!!, varName)

        val expression = varDecl.initializer

        return PVarDecl(PIdent(psymbol.name), compileExpression(expression, symTable));
    }

    fun compileStatement(expr: StatementTree, symTable: SymTable): PStatement {

        return when_lbl@ when (expr) {
            is ExpressionStatementTree -> {
                PExpressionStatement(compileExpression(expr.expression, symTable))
            }

            is VariableTree -> {
                val psymbol = symTable.addVar(expr.sym!!, expr.name.toString())

                PVarDecl(PIdent(psymbol.name), compileExpression(expr.initializer, symTable))
            }

            is ReturnTree -> {
                if (expr.expression == null) PReturn(null)
                else PReturn(compileExpression(expr.expression, symTable))
            }

            is IfTree -> {
                PIf(
                    compileExpression((expr.condition as ParenthesizedTree).expression, symTable),
                    compileStatement(expr.thenStatement, symTable),
                    expr.elseStatement?.let { compileStatement(it, symTable) }
                )
            }

            is BlockTree -> {
                val nSymTable = SymTable(symTable)
                PBlock(
                    expr.statements.map { compileStatement(it, nSymTable) }
                )
            }

            is ThrowTree -> {
                if (expr.expression.type.toString() == "perl.DieException" && expr.expression is NewClassTree) {
                    return PDie(
                        compileExpression(
                            (expr.expression as NewClassTree).arguments[0],
                            symTable
                        )
                    )
                }

                error("only throw new DieException(msg) is supported")
            }

            else -> {
                error("unknown statement $expr")
            }
        }
    }

    fun compileExpression(expr: ExpressionTree?, symTable: SymTable): PExpression {
        return when (expr) {
            null -> PUndef()
            is LiteralTree -> {
                when (val v = expr.value) {
                    null -> PUndef()
                    is Number -> PNumber(v)
                    is String -> PString(v)
                    else -> error("Unknown literal $expr")
                }
            }

            is MethodInvocationTree -> compileMethodInvocation(expr, symTable)

            is IdentifierTree -> {
                val psymbol = if (expr.name.toString() == "this") {
                    symTable.getSelf() ?: error("cannot resolve self")
                } else {
                    val psymbol = symTable.psymbol(expr.sym!!)
                        ?: error("cannot resolve symbol for $expr")
                    if (psymbol.type != "var") error("cannot resolve var for $expr")
                    psymbol
                }

                PIdent(psymbol.name)
            }

            is ParenthesizedTree -> {
                PParenExpression(compileExpression(expr.expression, symTable))
            }

            is AssignmentTree -> {
                val lValue = compileExpression(expr.variable, symTable)
                if (lValue !is PIdent) error("lvalue should be ident in $expr")
                val rValue = compileExpression(expr.expression, symTable)
                PAssign(lValue, rValue)
            }

            is MemberSelectTree -> {
                val type = expr.expression.type!!.tsym as ClassSymbol
                val isPerlModule = "perl.PerlModule" in implementedInterfaces(type)
                if (isPerlModule) {
                    val lValue = compileExpression(expr.expression, symTable)
                    if (lValue !is PIdent) error("lvalue of module field access should be this ($type), for `$expr`")
                    // if it is perlModule then it must be 'this'!
                    val expectedThisName = symTable.getSelf()!!.name

                    if (lValue.name != expectedThisName) error("trying to access field of some other module somehow, for $expr")

                    return PIdent(symTable.psymbol(expr.sym!!)!!.name)
                }

                PHashRefGet(compileExpression(expr.expression, symTable), PString(expr.identifier.toString()))
            }

            is LambdaExpressionTree -> {
                val nextSymTable = SymTable(symTable)
                val parameters = mutableListOf<PIdent>()

                expr.parameters.forEach {
                    val psymbol = nextSymTable.addVar(it.sym!!, it.name.toString())
                    parameters += PIdent(psymbol.name)
                }

                val body = mutableListOf<PStatement>()
                if (expr.bodyKind == LambdaExpressionTree.BodyKind.EXPRESSION) {
                    body += PReturn(compileExpression(expr.body as ExpressionTree, nextSymTable))
                } else {
                    (expr.body as BlockTree).statements.forEach {
                        body += compileStatement(
                            it, nextSymTable
                        )
                    }
                }

                PLambda(parameters, body)
            }

            is NewClassTree -> {
                val identifier = expr.identifier

                val sym = if (identifier is ParameterizedTypeTree) {
                    identifier.type.sym
                } else {
                    identifier.sym
                }

                if (sym !is ClassSymbol) {
                    error("Cannot instantiate $expr")
                }
                if ("perl.ArrayRef" == sym.fullname.toString()) {
                    return PNewArrayRef(
                        expr.arguments.map { compileExpression(it, symTable) }.toMutableList()
                    )
                }
                if ("perl.HashRef" == sym.fullname.toString()) {
                    return PNewHashRef()
                }
                if ("perl.PerlDto" in implementedInterfaces(sym)) {
                    val recordComponents = sym.recordComponents
                    val args = mutableListOf<PExpression>()
                    for (i in recordComponents.indices) {
                        val rc = recordComponents[i]
                        val arg = expr.arguments[i]

                        args += PString(rc.name.toString())
                        args += compileExpression(arg, symTable)
                    }
                    return PNewHashRef(args)
                }
                error("Cannot instantiate $expr")
            }

            is BinaryTree -> {
                if (expr.kind == Tree.Kind.PLUS) {
                    if (expr.leftOperand.type.toString() == "java.lang.String") {
                        return PStringConcat(
                            compileExpression(expr.leftOperand, symTable),
                            compileExpression(expr.rightOperand, symTable),
                        )
                    }
                }
                error("Unknown binary tree $expr")
            }

            else -> {
                error("Unknown expression $expr")
            }
        }
    }

    fun compileMethodInvocation(methodInvocation: MethodInvocationTree, symTable: SymTable): PExpression {
        val pArguments = methodInvocation.arguments.map {
            compileExpression(it, symTable)
        }
        return when (val methodSelect = methodInvocation.methodSelect) {
            is IdentifierTree -> {
                if (methodSelect.name.toString() == "super") return PUndef()
                PMethodCall(
                    PIdent(
//                        symTable.getThis(methodSelect.sym!!.owner as ClassSymbol)!!.name
                        symTable.getSelf()!!.name
                    ),
                    methodSelect.name.toString(),
                    pArguments.toMutableList()
                )
            }

            is MemberSelectTree -> {
                val methodName = methodSelect.sym!!.getAnnotation(perl.PerlName::class.java)?.value
                    ?: methodSelect.identifier.toString()
                if ("perl.PerlDto" in implementedInterfaces(methodSelect.expression.type!!.tsym as ClassSymbol)) {
                    if (pArguments.isNotEmpty()) error("PerlDto field access with parameters? $methodInvocation")
                    return PHashRefGet(
                        compileExpression(methodSelect.expression, symTable),
                        PString(methodName)
                    )
                }

                if ("perl.ArrayRef" == methodSelect.expression.type!!.tsym.toString()) {
                    if (methodSelect.identifier.toString() == "map") {
                        val tmpName = symTable.generateName("sub")
                        return PArrayRefMap(
                            compileExpression(methodSelect.expression, symTable),
                            pArguments.toMutableList()[0],
                            tmpName
                        )
                    }

                    if (methodSelect.identifier.toString() == "get") {
                        return PArrayRefGet(
                            compileExpression(methodSelect.expression, symTable),
                            pArguments[0],
                        )
                    }

                    error("Unsupported ArrayRef method ${methodSelect.identifier.toString()}")
                }

                if ("perl.HashRef" == methodSelect.expression.type!!.tsym.toString()) {
                    if (methodSelect.identifier.toString() == "put") {
                        return PHashRefSet(
                            compileExpression(methodSelect.expression, symTable),
                            pArguments[0],
                            pArguments[1],
                        )
                    }

                    if (methodSelect.identifier.toString() == "get") {
                        return PHashRefGet(
                            compileExpression(methodSelect.expression, symTable),
                            pArguments[0],
                        )
                    }

                    if (methodSelect.identifier.toString() == "has") {
                        return PHashRefHas(
                            compileExpression(methodSelect.expression, symTable),
                            pArguments[0],
                        )
                    }

                    error("Unsupported HashRef method ${methodSelect.identifier.toString()}")
                }

                return PMethodCall(
                    compileExpression(methodSelect.expression, symTable),
                    methodName,
                    pArguments.toMutableList()
                )
            }

            else -> error("cannot compile methodInvocation $methodInvocation")
        }
    }

    fun generate(): List<PModule> {
        val globalSymTable = SymTable(namer)
//        val cdg = GlobalSymbolsResolver(globalSymTable)
//        cus.forEach { cdg.scan(it, Unit) }


        val res = compile(cus, globalSymTable)
        return res
//        return (RootVisitor(trees).visitCompilationUnit(cus, Ctx()) as PRoot).modules.toList()
    }

    private fun implementedInterfaces(thisSym: ClassSymbol): Set<String> {
        val result = mutableSetOf<String>()

        fun rec(interfaces: List<Symbol.ClassSymbol>) {
            interfaces.forEach { result += it.qualifiedName.toString() }
            interfaces.forEach { rec(it.interfaces.map { it.tsym as Symbol.ClassSymbol }) }
        }

        rec(thisSym.interfaces.map { it.tsym as Symbol.ClassSymbol })

        return result
    }

    private fun implementedInterfaces(node: ClassTree): Set<String> {
        val thisSym = node.sym as Symbol.ClassSymbol
        return implementedInterfaces(thisSym)
    }
}


fun generateCode(
    trees: JavacTrees,
    cus: List<CompilationUnitTree>,
    task: JavacTask,
): List<PModule> {

    return SpookyCodeGenerator(trees, cus, task).generate()
}


val Tree.sym: Symbol?
    get() {
//        return TreeInfo.symbol(this as JCTree)
        return this.javaClass.getField("sym").let { it.trySetAccessible(); it.get(this) as Symbol? }
    }
//val Type.tsym: Symbol get() = this.javaClass.getField("tsym").let { it.trySetAccessible(); it.get(this) as Symbol }


val Tree.type: Type?
    get() {
        return this.javaClass.getField("type").let { it.trySetAccessible(); it.get(this) as Type? }
    }