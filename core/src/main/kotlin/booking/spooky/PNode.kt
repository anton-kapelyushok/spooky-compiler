package booking.spooky

// generator!, 1-1 relation to perl

interface PNode

// FIGURE OUT IF I NEED TO HAVE TO CREATE SYMBOL INFORMATION HERE - no
// FIGURE OUT IF THESE SHOULD BE MUTABLE

open class PRoot(
    val modules: MutableList<PModule>
) : PNode {
    override fun toString(): String {
        return modules.toString()
    }
}

open class PModule(
    val name: String,
    val imports: MutableList<String> = mutableListOf(),
    val initStatements: MutableList<PStatement> = mutableListOf(),
    val subDeclarations: MutableList<PSubDecl> = mutableListOf(),
) : PNode {
    override fun toString(): String {
        return name
    }
}

open class PStatement : PNode

open class PNoop() : PStatement()

open class PIdent(
    val name: String
) : PExpression() {
    override fun toString(): String {
        return "\$" + name
    }
}

open class PVarDecl(
    val lValue: PIdent,
    val rValue: PExpression
) : PStatement() {
    override fun toString(): String {
        return "my $lValue = $rValue"
    }
}

open class PSubDecl(
    val name: String,
    val parameters: MutableList<PIdent> = mutableListOf(),
    val body: MutableList<PStatement> = mutableListOf(),
) : PNode {
    override fun toString(): String {
        var res = """
            sub $name {
                my (${parameters.joinToString(",")})  = @_;
                
        """.trimIndent()

        val ident = "1234".map { " " }.joinToString()
        body.forEach { res += "$ident$it;\n" }
        res += "}\n"
        return res
    }
}

open class PExpression : PNode

open class PSubCall(
    val subName: String,

    val arguments: MutableList<PExpression> = mutableListOf(),
) : PExpression()

open class PMethodCall(
    val methodSelect: PExpression,

    val methodName: String,

    val arguments: MutableList<PExpression> = mutableListOf(),
) : PExpression() {
    override fun toString(): String {
        return "$methodSelect->$methodName(${arguments.joinToString(",")})"
    }
}

open class PStaticMethodCall(
    val moduleName: String,
    val methodName: String,
    val arguments: List<PExpression>,
): PExpression() {
    override fun toString(): String {
        return "$moduleName::$methodName(${arguments.joinToString(",")})"
    }
}

open class PHashRefGet(
    val hashRef: PExpression,

    val key: PExpression,
) : PExpression() {
    override fun toString(): String {
        return "($hashRef)->{($key)}"
    }
}

open class PLambda(
    val parameters: MutableList<PIdent> = mutableListOf(),

    val body: MutableList<PStatement> = mutableListOf()
) : PExpression()

open class PLiteral() : PExpression()

open class PString(val v: String) : PLiteral() {
    override fun toString(): String {
        return """"$v""""
    }
}

open class PNumber(val v: Number) : PLiteral()
open class PUndef() : PExpression() {
    override fun toString(): String {
        return "undef"
    }
}

open class PNewArrayRef(
    val elements: MutableList<PExpression> = mutableListOf(),
) : PExpression() {
    override fun toString(): String {
        return "[" + elements.joinToString(", ") + "]"
    }
}

open class PGenericLiteral(val v: String) : PLiteral() {
    override fun toString(): String {
        return v
    }
}

open class PParenExpression(
    val expr: PExpression
) : PExpression()


open class PExpressionStatement(
    val expr: PExpression
) : PStatement() {
    override fun toString(): String {
        return expr.toString()
    }
}

open class PAssign(
    val lValue: PExpression,
    val rValue: PExpression
) : PExpression() {
    override fun toString(): String {
        return "$lValue = $rValue"
    }
}

open class PNewHashRef(
    val initializers: MutableList<PExpression> = mutableListOf()
) : PExpression() {
    override fun toString(): String {
        return "{${initializers.joinToString(",")}}"
    }
}

open class PReturn(
    val expr: PExpression?
) : PStatement()

open class PArrayRefMap(
    val array: PExpression,
    val mapper: PExpression,
    val tmpName: String,
) : PExpression()


open class PHashRefSet(
    val hashRef: PExpression,
    val key: PExpression,
    val value: PExpression
) : PExpression() {
    override fun toString(): String {
        return super.toString()
    }
}

open class PStringConcat(
    val left: PExpression,
    val right: PExpression,
) : PExpression()


open class PIf(
    val condition: PExpression,
    val thenStatement: PStatement,
    val elseStatement: PStatement? = null,
) : PStatement()

open class PBlock(
    val statements: List<PStatement>
) : PStatement()

open class PDie(
    val message: PExpression,
) : PStatement()

open class PArrayRefGet(
    val arrayRef: PExpression,
    val idx: PExpression,
) : PExpression()

open class PHashRefHas(
    val hashRef: PExpression,
    val key: PExpression,
): PExpression()


open class PUnrealExpression(val raw: String): PExpression(){
    override fun toString(): String {
        return "(e> $raw <e)"
    }
}
open class PUnrealStatement(val raw: String): PStatement() {
    override fun toString(): String {
        return "(s> $raw <s)"
    }
}