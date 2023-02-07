@file:Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE")

package booking.spooky


import com.sun.source.tree.*
import com.sun.source.util.JavacTask
import com.sun.source.util.TreePath
import com.sun.source.util.Trees
import com.sun.tools.javac.api.BasicJavacTask
import com.sun.tools.javac.api.JavacTrees
import com.sun.tools.javac.util.Context
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.tools.*
import kotlin.io.path.absolutePathString


// https://sourcegraph.com/github.com/georgewfraser/java-language-server/-/blob/src/main/java/org/javacs/SourceFileManager.java

// -ea --add-opens=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED --add-opens=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED --add-opens=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED --add-opens=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED --add-opens=jdk.compiler/com.sun.tools.javac.model=ALL-UNNAMED
fun main() {


//    val jfm: StandardJavaFileManager = javac.getStandardFileManager(null, null, null)
//    val task = javac.getTask(
//        null, jfm, null, null, null,
//        jfm.getJavaFileObjects("testData/src/home/A.java")
//    )


    val javac = ToolProvider.getSystemJavaCompiler()
    val fileManager = javac.getStandardFileManager(null, null, null)
//    val fileObjects = fileManager
//        .getJavaFileObjectsFromFiles(
//            listOf(
//                File("src/main/kotlin/home/A.java"),
//                File("src/main/kotlin/home/C.java"),
//                File("src/main/kotlin/home/Enclosing.java"),
//            )
//        )

    fun StandardJavaFileManager.getJavaFileObjectsForDirectories(vararg dirs: Path): List<JavaFileObject> {
        val files = mutableListOf<Path>()

        dirs.forEach { dir ->

            Files.walkFileTree(dir, object : SimpleFileVisitor<Path>() {
                override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                    if (file.absolutePathString().endsWith("java")) files.add(file)
                    return FileVisitResult.CONTINUE
                }
            })
        }

        return this.getJavaFileObjectsFromPaths(files).toList()
    }


    val fileObjects = fileManager
        .getJavaFileObjectsForDirectories(
            Path.of("my-lib/src/main/java"),
//            Path.of("perl-lib/src/main/java"),
//            Path.of("booking-lib/src/main/java"),
//            Path.of("src/main/kotlin/perl")
        )

//    val precompiled = fileManager.

    val task = javac.getTask(
        null, fileManager, { diagnostic ->
            println("diagnostic")
            (diagnostic.kind == Diagnostic.Kind.ERROR)
            println(diagnostic)
        }, listOf(
            "-cp",
            listOf(
                "lombok.jar",
                "perl-lib/build/classes/java/main/",
                "booking-lib/build/classes/java/main/",
            ).joinToString(":"),
        ), null, fileObjects
    ) as JavacTask


//    task.call()

    val results = task.parse()
    val res = results.first()

    task.analyze()

//    println((((res as JCTree.JCCompilationUnit).defs[1] as JCTree.JCClassDecl).defs[1] as JCTree.JCMethodDecl).body as JCTree.JCBlock)
//        .toString()

    val trees = Trees.instance(task) as JavacTrees

    val taskContext: Context = (task as BasicJavacTask).context

//    TreePathScanner().scan(trees.getTree())

    res.sourceFile.name

//    TreePathScanner().scan(res, 0L)
//    javac.run(null, null, null, "testData/src/home/A.java")
//    val trees: Trees = Trees.instance(task)
    // Do stuff with "trees"


    val generated = generateCode(trees, results.toList(), taskContext, task)
    val emitted = emitCode(generated)
    println()
    println(emitted[0].content)
    emitted
}

