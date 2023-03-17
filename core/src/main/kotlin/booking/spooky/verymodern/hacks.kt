@file:Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE")
package booking.spooky.verymodern

import com.sun.source.tree.Tree
import com.sun.tools.javac.code.Symbol
import com.sun.tools.javac.code.Type

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