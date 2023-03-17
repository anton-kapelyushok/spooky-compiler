@file:Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE")
package booking.spooky

import com.sun.source.tree.CompilationUnitTree
import com.sun.source.util.JavacTask
import com.sun.tools.javac.api.BasicJavacTask
import com.sun.tools.javac.code.Types
import java.nio.file.Path
import javax.tools.Diagnostic
import javax.tools.ToolProvider

fun compileJava(pkg: String): CompileResult {
    val path  = "src/test/resources/tests/$pkg"

    val javac = ToolProvider.getSystemJavaCompiler()
    val fileManager = javac.getStandardFileManager(null, null, null)

    val fileObjects = fileManager.getJavaFileObjectsForDirectories(Path.of(path))

    val task = javac.getTask(
        null, fileManager, { diagnostic ->
            println("diagnostic")
            if ((diagnostic.kind == Diagnostic.Kind.ERROR)) {
                error(diagnostic)
            } else {
                println(diagnostic)
            }
        }, listOf(), null, fileObjects
    ) as JavacTask


    val cus = task.parse()
    task.analyze()

    return CompileResult(task, cus.toList())
}

class CompileResult(
    val task: JavacTask,
    val cus: List<CompilationUnitTree>,
)