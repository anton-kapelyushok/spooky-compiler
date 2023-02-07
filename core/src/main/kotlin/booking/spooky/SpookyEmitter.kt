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
            else -> println("Unknown node type ${node::class.simpleName}")
//            else -> error("Unknown node type ${node::class.simpleName}")
        }
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

        content.append("#MODULE IMPORTS\n")
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
    }
}

fun emitCode(modules: List<PModule>): List<EmittedFile> {
    return modules.map { SpookyEmitter().emit(it) }
}