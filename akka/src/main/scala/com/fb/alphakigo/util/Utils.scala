package com.pubgame.alphakigo.util

import java.io.{ ByteArrayOutputStream, PrintStream }
import java.lang.management.ManagementFactory
import java.nio.charset.{ Charset, StandardCharsets }
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

import scala.annotation.tailrec
import scala.collection.JavaConversions._
import scala.concurrent.duration._
import scala.reflect.runtime.universe
import scala.util.Try

object Utils {
  private[this] val constructorBindingRef = new AtomicReference[Map[universe.Symbol, () ⇒ _]](Map.empty)
  private[this] val threadBean = ManagementFactory.getThreadMXBean
  private[this] val runtimeBean = ManagementFactory.getRuntimeMXBean

  private[this] def newClassInstance[T](symbol: universe.ClassSymbol, classLoader: ClassLoader): T = {
    @tailrec
    def getConstructor(constructor: () ⇒ _ = null): () ⇒ _ = {
      val constructorBinding = constructorBindingRef.get()
      constructorBinding.get(symbol) match {
        case Some(cachedConstructor) ⇒ cachedConstructor
        case None ⇒
          val unCachedConstructor = if (constructor eq null) {
            val mirror = universe.runtimeMirror(classLoader)
            val classMirror = mirror.reflectClass(symbol)
            symbol.info.decl(universe.termNames.CONSTRUCTOR).asTerm.alternatives.find(_.asMethod.paramLists == List(List.empty)) map { ctor ⇒ () ⇒ classMirror.reflectConstructor(ctor.asMethod).apply()
            } match {
              case Some(c) ⇒ c
              case _       ⇒ throw new NoSuchElementException("no constructor without parameters")
            }
          } else {
            constructor
          }
          if (constructorBindingRef.compareAndSet(constructorBinding, constructorBinding.updated(symbol, unCachedConstructor)))
            unCachedConstructor
          else
            getConstructor(unCachedConstructor)
      }
    }
    getConstructor().apply().asInstanceOf[T]
  }

  private[this] def newObjectInstance[T](symbol: universe.ModuleSymbol, classLoader: ClassLoader): T = {
    @tailrec
    def getConstructor(constructor: () ⇒ _ = null): () ⇒ _ = {
      val constructorBinding = constructorBindingRef.get()
      constructorBinding.get(symbol) match {
        case Some(cachedConstructor) ⇒ cachedConstructor
        case None ⇒
          val unCachedConstructor = if (constructor eq null) {
            val mirror = universe.runtimeMirror(classLoader)
            val moduleMirror = mirror.reflectModule(symbol)
            () ⇒ moduleMirror.instance
          } else {
            constructor
          }
          if (constructorBindingRef.compareAndSet(constructorBinding, constructorBinding.updated(symbol, unCachedConstructor))) {
            unCachedConstructor
          } else {
            getConstructor(unCachedConstructor)
          }
      }
    }
    getConstructor().apply().asInstanceOf[T]
  }

  def newInstance[T](implicit ev: universe.WeakTypeTag[T]): T = {
    newInstance[T](getClass.getClassLoader)
  }

  def newInstance[T](classLoader: ClassLoader)(implicit ev: universe.WeakTypeTag[T]): T = {
    newClassInstance(universe.symbolOf[T].asClass, classLoader)
  }

  def newInstance[T](name: String, classLoader: ClassLoader): T = {
    val clazz = classLoader.loadClass(name)
    val mirror = universe.runtimeMirror(classLoader)
    if (name.endsWith("$")) {
      newObjectInstance(mirror.moduleSymbol(clazz), classLoader).asInstanceOf[T]
    } else {
      newClassInstance(mirror.classSymbol(clazz), classLoader).asInstanceOf[T]
    }
  }

  def newInstance[T](name: String): T = {
    newInstance[T](name: String, getClass.getClassLoader)
  }

  def getCacheSize: Int = {
    constructorBindingRef.get().size
  }

  def printThreadInfo(title: String): String = {
    writeThreadInfo(title, StringBuilder.newBuilder).toString()
  }

  def writeThreadInfo(title: String, builder: StringBuilder): StringBuilder = {
    def appendTaskName(sb: StringBuilder, id: Long, name: String): StringBuilder = {
      if (name == null) {
        sb
      } else {
        sb append id append " (" append name append ")"
      }
    }
    val STACK_DEPTH = 20
    val contention = threadBean.isThreadContentionMonitoringEnabled
    val threadIds = threadBean.getAllThreadIds
    builder append "Process Thread Dump: " append title append '\n'
    builder append threadIds.length append " active threads" append '\n'
    threadIds.foldLeft(builder) {
      case (b, tid) ⇒
        Option(threadBean.getThreadInfo(tid, STACK_DEPTH)).fold(b append "  Inactive" append '\n') {
          info ⇒
            b append "Thread "
            appendTaskName(b, info.getThreadId, info.getThreadName) append ":" append '\n'
            val state = info.getThreadState
            b append "  State: " append state append '\n'
            b append "  Blocked count: " append info.getBlockedCount append '\n'
            b append "  Waited count: " append info.getWaitedCount append '\n'
            if (contention) {
              b append "  Blocked time: " append info.getBlockedTime append '\n'
              b append "  Waited time: " append info.getWaitedTime append '\n'
            }
            if (state eq Thread.State.WAITING) {
              b append "  Waiting on " append info.getLockName append '\n'
            } else if (state eq Thread.State.BLOCKED) {
              b append "  Blocked on " append info.getLockName append '\n'
              b append "  Blocked by "
              appendTaskName(b, info.getLockOwnerId, info.getLockOwnerName) append '\n'
            }
            b append "  Stack:" append '\n'
            info.getStackTrace.foldLeft(b) {
              case (sb, frame) ⇒
                sb append "    " append frame.toString append '\n'
            }
        }
    }
    builder
  }

  /**
   * Get the ClassLoader which loaded the library.
   */
  def getClassLoader: ClassLoader = getClass.getClassLoader

  /**
   * Get the Context ClassLoader on this thread or, if not present, the ClassLoader that
   * loaded this library.
   *
   * This should be used whenever passing a ClassLoader to Class.ForName or finding the currently
   * active loader when setting up ClassLoader delegation chains.
   */
  def getContextOrUtilClassLoader: ClassLoader =
    Option(Thread.currentThread().getContextClassLoader).getOrElse(getClassLoader)

  /** Determines whether the provided class is loadable in the current thread. */
  def classIsLoadable(clazz: String): Boolean = {
    Try { Class.forName(clazz, false, getContextOrUtilClassLoader) }.isSuccess
  }

  /** Preferred alternative to Class.forName(className) */
  def classForName(className: String): Class[_] = {
    Class.forName(className, true, getContextOrUtilClassLoader)
  }

  def writeJavaRuntime(builder: StringBuilder): StringBuilder = {
    builder append "Java Runtime: "
    builder append sys.props("java.vendor")
    builder append " "
    builder append sys.props("java.version")
    builder append " "
    builder append sys.props("java.home")
  }

  def writeHeapSizes(builder: StringBuilder): StringBuilder = {
    builder append "Heap sizes: "
    builder append "current="
    builder append sys.runtime.totalMemory / 1024L
    builder append "k  free="
    builder append sys.runtime.freeMemory / 1024L
    builder append "k  max="
    builder append sys.runtime.maxMemory / 1024L
    builder append "k"
  }

  def writeJVMArgs(builder: StringBuilder): StringBuilder = {
    runtimeBean.getInputArguments.foldLeft(builder append "JVM args:") {
      case (sb, arg) ⇒
        sb append " " append arg
    }
  }

  val Charset: Charset = StandardCharsets.UTF_8

  val CharsetName: String = Charset.toString

  def elapsed[T](unit: TimeUnit = TimeUnit.MILLISECONDS)(block: ⇒ T): (T, Double) = {
    val start = System.nanoTime()
    val result = block
    val elapsed = (System.nanoTime() - start).nanos
    result → elapsed.toUnit(unit)
  }

  def throwableToString(throwable: Throwable): String = {
    val baos = new ByteArrayOutputStream()
    val ps = new PrintStream(baos, false, CharsetName)
    throwable.printStackTrace(ps)
    ps.close()
    baos.toString(CharsetName)
  }

  def randomString: String = {
    s"${System.currentTimeMillis()}_${UUID.randomUUID().toString.replace("-", "")}"
  }

}
