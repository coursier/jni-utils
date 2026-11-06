package coursier.jniutils.tests

import coursier.jniutils.{DefaultNativeApi, NativeApi}
import utest._

import java.lang.reflect.{InvocationTargetException, Method, Modifier}

// Calls every abstract method of NativeApi on each of its implementations, so that a missing or
// misspelled C forwarder (which only shows up at runtime, as an UnsatisfiedLinkError) fails here.
// Only linkage is checked: some methods (terminalSize, enableAnsiOutput) can legitimately return
// an "error:..." string when there's no console, like on CI.
object NativeApiLinkageTests extends TestSuite {

  private final class Ctx {
    val envKey = "COURSIER_JNIUTILS_TEST_" + java.util.UUID.randomUUID().toString.take(8)
  }

  // Arguments to pass to each method, keyed by name and parameter types. New methods of
  // NativeApi need an entry here, or the test fails.
  private val recipes: Map[String, Ctx => Seq[AnyRef]] = Map(
    "terminalSize()"                                    -> (_ => Nil),
    "enableAnsiOutput()"                                -> (_ => Nil),
    "GetUserEnvironmentVariable(String)"                -> (ctx => Seq(ctx.envKey)),
    "SetUserEnvironmentVariable(String,String)"         -> (ctx => Seq(ctx.envKey, "value")),
    "DeleteUserEnvironmentVariable(String)"             -> (ctx => Seq(ctx.envKey)),
    "GetKnownFolderPath(String)"                        -> (_ => Seq("{3EB685DB-65F9-4CF6-A03A-E3EF65729F3D}")),
    "GetModuleFileName()"                               -> (_ => Nil),
    "GetOEMCodePage()"                                  -> (_ => Nil)
  )

  private def signature(m: Method): String =
    m.getName + m.getParameterTypes.map(_.getSimpleName).mkString("(", ",", ")")

  private lazy val abstractMethods: Seq[Method] =
    classOf[NativeApi]
      .getDeclaredMethods
      .toSeq
      .filter { m =>
        val mod = m.getModifiers
        Modifier.isAbstract(mod) && Modifier.isPublic(mod) && !Modifier.isStatic(mod)
      }
      .sortBy(signature)

  private def checkAll(api: NativeApi): Unit = {
    assert(abstractMethods.nonEmpty)

    val missingRecipes = abstractMethods.map(signature).filterNot(recipes.contains)
    if (missingRecipes.nonEmpty)
      throw new java.lang.AssertionError(
        "No argument recipe for NativeApi method(s) " + missingRecipes.mkString(", ") +
          ", add one to " + getClass.getName.stripSuffix("$") + ".recipes"
      )

    val ctx = new Ctx
    val failures =
      try
        abstractMethods.flatMap { m =>
          val args = recipes(signature(m))(ctx)
          try {
            val res = m.invoke(api, args*)
            System.err.println(s"${api.getClass.getSimpleName}.${signature(m)}: $res")
            Nil
          }
          catch {
            case e: InvocationTargetException =>
              val kind = e.getCause match {
                case _: UnsatisfiedLinkError => "missing native forwarder"
                case _                       => "unexpected exception"
              }
              Seq(s"${api.getClass.getName}.${signature(m)}: $kind: ${e.getCause}")
          }
        }
      finally {
        // In case SetUserEnvironmentVariable worked but DeleteUserEnvironmentVariable didn't
        try api.DeleteUserEnvironmentVariable(ctx.envKey)
        catch { case _: UnsatisfiedLinkError => }
      }

    if (failures.nonEmpty)
      throw new java.lang.AssertionError(failures.mkString("\n", "\n", ""))
  }

  // The implementation set up by setup() is the one NativeApi.get() should then return
  private def withSetup(setup: => Unit, expected: Class[? <: NativeApi]): Unit =
    try {
      setup
      val api = NativeApi.get()
      assert(api.getClass == expected)
      checkAll(api)
    }
    finally NativeApi.set(null)

  val tests = Tests {
    // With no implementation set, NativeApi.get() returns DefaultNativeApi
    test("default") synchronized {
      withSetup(NativeApi.set(null), classOf[DefaultNativeApi])
    }

    test("bootstrap") synchronized {
      withSetup(
        coursier.bootstrap.launcher.jniutils.BootstrapNativeApi.setup(),
        classOf[coursier.bootstrap.launcher.jniutils.BootstrapNativeApi]
      )
    }

    test("lmcoursier") synchronized {
      withSetup(
        lmcoursier.internal.jniutils.LmNativeApi.setup(),
        classOf[lmcoursier.internal.jniutils.LmNativeApi]
      )
    }

    test("coursierapi") synchronized {
      withSetup(
        coursierapi.internal.jniutils.ApiInternalNativeApi.setup(),
        classOf[coursierapi.internal.jniutils.ApiInternalNativeApi]
      )
    }
  }
}
