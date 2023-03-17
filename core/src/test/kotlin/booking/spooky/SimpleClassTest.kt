@file:Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE")

package booking.spooky

import booking.spooky.verymodern.ClassTranslator
import booking.spooky.verymodern.JavacSymbolKey
import booking.spooky.verymodern.MethodContext
import com.sun.source.tree.*
import com.sun.source.util.TreePathScanner
import com.sun.tools.javac.api.BasicJavacTask
import com.sun.tools.javac.code.Types
import org.junit.jupiter.api.Test
import java.lang.management.ManagementFactory

class SimpleClassTest {

    @Test
    fun test() {
        println(ManagementFactory.getRuntimeMXBean().getInputArguments())
        System.out.println(System.getProperty("jdk.module.upgrade.path"));

        val cu = compileJava("simple-class").cus.single()
        val ct = ClassTranslator()
        val module = cu.typeDecls[0].let { ct.translateClass(null, it as ClassTree) }
        println(emitCode(listOf(module)))
    }

    @Test
    fun `identifier-method-call`() {
        val compilationResult = compileJava("identifier-method-call")
        val cu = compilationResult.cus.single()
        val task = compilationResult.task
        val jcontext = (task as BasicJavacTask).context
        val types = Types.instance(jcontext)

        val scanner = object : TreePathScanner<Unit, Unit>() {
            override fun visitMethod(node: MethodTree, p: Unit) {
                if (node.sym!!.name.toString() == "test" && (
                            node.sym!!.owner.type.toString().endsWith("Child1") ||
                                    node.sym!!.owner.type.toString().endsWith("Child2") ||
                                    node.sym!!.owner.type.toString().endsWith("Child3")
                            )
                ) {
                    println(node.sym!!.owner.type.toString())
                    val mc = MethodContext(
                        isStatic = false,
                        key = JavacSymbolKey(node.sym!!),
                        name = "test",
                        owner = node.sym!!.owner,
                        types = types,
                    )

                    node.body.statements.forEach {
                        if (it is ExpressionStatementTree && it.expression is MethodInvocationTree) {
                            println(mc.invokeMethod(it.expression as MethodInvocationTree))
                        }
                    }
                    println()
                }
            }
        }

        scanner.scan(cu, Unit)
    }

    @Test
    fun `member-select-method-call`() {
        val compilationResult = compileJava("member-select-method-call")
        val cu = compilationResult.cus.single()
        val task = compilationResult.task
        val jcontext = (task as BasicJavacTask).context
        val types = Types.instance(jcontext)

        val scanner = object : TreePathScanner<Unit, Unit>() {
            override fun visitMethod(node: MethodTree, p: Unit) {
                if (node.sym!!.name.toString() == "test" && node.sym!!.owner.type.toString()
                        .endsWith("Child1")
                ) {
                    println(node.sym!!.owner.type.toString())
                    val mc = MethodContext(
                        isStatic = false,
                        key = JavacSymbolKey(node.sym!!),
                        name = "test",
                        owner = node.sym!!.owner,
                        types = types,
                    )

                    node.body.statements.forEach {
                        if (it is ExpressionStatementTree && it.expression is MethodInvocationTree) {
                            println(mc.invokeMethod(it.expression as MethodInvocationTree))
                        }
                    }
                    println()
                }
            }
        }

        scanner.scan(cu, Unit)
    }

    @Test
    fun `identifier-variable-resolution`() {
        val compilationResult = compileJava("identifier-variable-resolution")
        val cu = compilationResult.cus.single()
        val task = compilationResult.task
        val jcontext = (task as BasicJavacTask).context
        val types = Types.instance(jcontext)

        var mc: MethodContext? = null

        val scanner = object : TreePathScanner<Unit, Unit>() {
            override fun visitMethod(node: MethodTree, p: Unit) {
                if (node.sym!!.name.toString() == "test"
                    && node.sym!!.owner.type.toString().endsWith("AnonParent>")
                ) {
                    println(node.sym!!.owner.type.toString())
                    mc = MethodContext(
                        isStatic = false,
                        key = JavacSymbolKey(node.sym!!),
                        name = "test",
                        owner = node.sym!!.owner,
                        types = types,
                    )
                }
                super.visitMethod(node, p)
            }


            override fun visitMethodInvocation(node: MethodInvocationTree, p: Unit) {
                if ((node.methodSelect as? IdentifierTree)?.name.toString() == "sink") {
                    node.arguments.forEach {
                        println(mc!!.variableIdentifier(it as IdentifierTree))
                    }
                }
            }
        }

        scanner.scan(cu, Unit)
    }

}