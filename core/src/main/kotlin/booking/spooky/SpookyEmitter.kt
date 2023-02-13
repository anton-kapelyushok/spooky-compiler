package booking.spooky

data class EmittedFile(
    val relativePath: String,
    val content: String,
)

class SpookyEmitter {
    private val oneIdent = "    ";

    fun emit(module: PModule): EmittedFile {
        val content = StringBuilder()
        emitModule(module, content, "")
        return EmittedFile(module.name.replace("::", "/") + ".pm", content.toString())
    }

    fun emitNode(node: PNode, content: StringBuilder, ident: String) {
        when (node) {
            is PModule -> emitModule(node, content, ident)
            is PVarDecl -> emitVarDecl(node, content, ident)
            is PIdent -> emitIdent(node, content, ident)
            is PGenericLiteral -> emitGenericLiteral(node, content, ident)
            is PUndef -> emitUndef(node, content, ident)
            is PString -> emitString(node, content, ident)
            is PExpressionStatement -> emitExpressionStatement(node, content, ident)
            is PAssign -> emitAssign(node, content, ident)
            is PSubDecl -> emitSubDecl(node, content, ident)
            is PReturn -> emitReturn(node, content, ident)
            is PMethodCall -> emitMethodCall(node, content, ident)
            is PHashRefGet -> emitHashRefGet(node, content, ident)
            is PLambda -> emitLambda(node, content, ident)
            is PNewHashRef -> emitNewHashRef(node, content, ident)
            is PNumber -> emitNumber(node, content, ident)
            is PNewArrayRef -> emitNewArrayRef(node, content, ident)
            is PArrayRefMap -> emitArrayMap(node, content, ident)
            is PHashRefSet -> emitHashRefSet(node, content, ident)
            is PStringConcat -> emitStringConcat(node, content, ident)
            is PIf -> emitIf(node, content, ident)
            is PParenExpression -> emitParenExpression(node, content, ident)
            is PBlock -> emitBlock(node, content, ident)
            is PDie -> emitDie(node, content, ident)
            is PArrayRefGet -> emitArrayRefGet(node, content, ident)
            is PHashRefHas -> emitHashRefHas(node, content, ident)
//            else -> println("Unknown node type ${node::class.simpleName}")
            else -> error("Unknown node type ${node::class.simpleName}")
        }
    }

    private fun emitHashRefHas(node: PHashRefHas, content: StringBuilder, ident: String) {
        content.append("defined(")
        emitNode(node.hashRef, content, ident)
        content.append("->{")
        emitNode(node.key, content, ident)
        content.append("}")
        content.append(")")
    }

    private fun emitArrayRefGet(node: PArrayRefGet, content: StringBuilder, ident: String) {
        emitNode(node.arrayRef, content, ident)
        content.append("->[")
        emitNode(node.idx, content, ident)
        content.append("]")
    }

    private fun emitDie(node: PDie, content: StringBuilder, ident: String) {
        content.append(ident)
        content.append("die ")
        emitNode(node.message, content, ident)
        content.append(";\n")
    }

    private fun emitBlock(node: PBlock, content: StringBuilder, ident: String) {
        content.append(ident)
        content.append("{\n")
        node.statements.forEach {
            emitNode(it, content, ident + oneIdent)
        }
        content.append("}\n")
    }

    private fun emitParenExpression(node: PParenExpression, content: StringBuilder, ident: String) {
        content.append("(")
        emitNode(node.expr, content, ident)
        content.append(")")
    }

    private fun emitIf(node: PIf, content: StringBuilder, ident: String) {
        content.append(ident)
        content.append("if (")
        emitNode(node.condition, content, ident)
        content.append(")")
        if (node.thenStatement is PBlock) {
            content.append(" {\n")
            node.thenStatement.statements.forEach {
                emitNode(it, content, ident + oneIdent)
            }
            content.append(ident)
            content.append("}\n")
        } else {
            content.append("\n")
            emitNode(node.thenStatement, content, ident + oneIdent)
        }

        if (node.elseStatement != null) TODO()
    }

    private fun emitStringConcat(node: PStringConcat, content: StringBuilder, ident: String) {
        emitNode(node.left, content, ident)
        content.append(".")
        emitNode(node.right, content, ident)
    }

    private fun emitHashRefSet(node: PHashRefSet, content: StringBuilder, ident: String) {
        emitNode(node.hashRef, content, ident)
        content.append("->{(")
        emitNode(node.key, content, ident)
        content.append(")} = ")
        emitNode(node.value, content, ident)
    }


    private fun emitArrayMap(node: PArrayRefMap, content: StringBuilder, ident: String) {
        // ( \map {
        content.append("[ map {\n")

        // my $tmp = ... ;
        val i1 = ident + oneIdent
        val pIdentifier = PIdent(node.tmpName)
        val valDecl = PVarDecl(pIdentifier, node.mapper)
        emitNode(valDecl, content, i1)

        // $tmp->($_)
        content.append(i1)
        emitNode(pIdentifier, content, i1)
        content.append("->(\$_);\n")

        // } $arr_ref->@* );
        content.append(ident)
        content.append("} ")
        emitNode(node.array, content, ident)
        content.append("->@* ]")
    }

    private fun emitNewArrayRef(node: PNewArrayRef, content: StringBuilder, ident: String) {
        if (node.elements.isEmpty()) {
            content.append("[]")
            return
        }

        content.append("[\n")
        val subIdent = ident + oneIdent
        for (e in node.elements) {
            content.append(subIdent)
            emitNode(e, content, subIdent)
            content.append(",\n")
        }
        content.append(ident)
        content.append("]")
    }

    private fun emitNumber(node: PNumber, content: StringBuilder, ident: String) {
        content.append(node.v)
    }

    private fun emitNewHashRef(node: PNewHashRef, content: StringBuilder, ident: String) {
        if (node.initializers.isEmpty()) {
            content.append("{}")
            return
        }
        content.append("{\n")
        val subIdent = ident + oneIdent
        for ((l, r) in node.initializers.chunked(2)) {
            content.append(subIdent)
            emitNode(l, content, subIdent)
            content.append(" => ")
            emitNode(r, content, subIdent)
            content.append(",\n")
        }
        content.append(ident)
        content.append("}")
    }

    private fun emitLambda(node: PLambda, content: StringBuilder, ident: String) {
        content.append("sub {\n")
        val contentIdent = ident + oneIdent

        if (node.parameters.isNotEmpty()) {
            content.append(contentIdent)
            content.append("my (")
            for ((i, param) in node.parameters.withIndex()) {
                emitNode(param, content, contentIdent)
                if (i != node.parameters.size - 1) content.append(", ")
            }
            content.append(") = @_;\n")
            content.append("\n")
        }

        node.body.forEach {
            emitNode(it, content, contentIdent)
        }

        content.append(ident)
        content.append("}")
    }

    private fun emitHashRefGet(node: PHashRefGet, content: StringBuilder, ident: String) {
        emitNode(node.hashRef, content, ident)
        content.append("->{(")
        emitNode(node.key, content, ident)
        content.append(")}")
    }

    private fun emitMethodCall(node: PMethodCall, content: StringBuilder, ident: String) {
        emitNode(node.varName, content, ident)

        if (node.arguments.isEmpty()) {
            content.append("->${node.methodName}()")
        } else {

            content.append("->${node.methodName}(\n")

            val argIdent = ident + oneIdent

            for (arg in node.arguments) {
                content.append(argIdent)
                emitNode(arg, content, argIdent)
                content.append(",\n")
            }

            content.append(ident)
            content.append(")")
        }
    }

    private fun emitReturn(node: PReturn, content: StringBuilder, ident: String) {
        content.append(ident)
        content.append("return")
        if (node.expr != null) {
            content.append(" ")
            emitNode(node.expr, content, ident)
        }
        content.append(";\n")
    }

    private fun emitSubDecl(node: PSubDecl, content: StringBuilder, ident: String) {
        content.append(ident)
        content.append("sub ${node.name} {\n")
        val contentIdent = ident + oneIdent

        if (node.parameters.isNotEmpty()) {
            content.append(contentIdent)
            content.append("my (")
            for ((i, param) in node.parameters.withIndex()) {
                emitNode(param, content, contentIdent)
                if (i != node.parameters.size - 1) content.append(", ")
            }
            content.append(") = @_;\n")
            content.append("\n")
        }

        node.body.forEach {
            emitNode(it, content, contentIdent)
        }

        content.append(ident)
        content.append("}\n")
    }

    private fun emitAssign(node: PAssign, content: StringBuilder, ident: String) {
        emitNode(node.lValue, content, ident);
        content.append(" = ")
        emitNode(node.rValue, content, ident)
    }

    private fun emitExpressionStatement(node: PExpressionStatement, content: StringBuilder, ident: String) {
        content.append(ident)
        emitNode(node.expr, content, ident)
        content.append(";\n")
    }

    private fun emitString(node: PString, content: StringBuilder, ident: String) {
        content.append("\"${node.v}\"")
    }

    private fun emitUndef(node: PUndef, content: StringBuilder, ident: String) {
        content.append("undef")
    }

    private fun emitGenericLiteral(node: PGenericLiteral, content: StringBuilder, ident: String) {
        content.append(node.v)
    }

    private fun emitIdent(node: PIdent, content: StringBuilder, ident: String) {
        content.append("\$${node.name}")
    }

    private fun emitVarDecl(node: PVarDecl, content: StringBuilder, ident: String) {
        content.append(ident)
        content.append("my ")
        emitNode(node.lValue, content, ident);
        content.append(" = ")
        emitNode(node.rValue, content, ident)
        content.append(";\n")
    }

    fun emitModule(module: PModule, content: StringBuilder, ident: String) {
        content.append("package ${module.name};\n")
        content.append("\n")
        content.append("use strict;\n")
        content.append("use warnings;\n")
        content.append("\n")

        content.append("# MODULE IMPORTS\n")
        module.imports.forEach {
            content.append("use $it;\n")
        }

        content.append("\n")

        content.append("# MODULE INIT\n")
        module.initStatements.forEach {
            emitNode(it, content, ident)
        }

        content.append("\n")
        content.append("# MODULE DECLARATIONS\n")
        module.subDeclarations.forEach {
            emitNode(it, content, ident)
        }

        content.append("\n")
        content.append("1;\n")
    }
}

fun emitCode(modules: List<PModule>): List<EmittedFile> {
    return modules.map { SpookyEmitter().emit(it) }
}