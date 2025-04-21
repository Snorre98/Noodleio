package gr17.noodleio.game.services.logging

import com.badlogic.gdx.Gdx

/**
 * Interface for logging in all service classes
 */
interface ServiceLogger {
    fun debug(tag: String, message: String)
    fun info(tag: String, message: String)
    fun error(tag: String, message: String, throwable: Throwable? = null)
    fun isDebugEnabled(): Boolean
}

/**
 * Implementation using LibGDX logging
 */
class LibGDXServiceLogger(private var debugEnabled: Boolean = true) : ServiceLogger {

    override fun debug(tag: String, message: String) {
        if (debugEnabled) {
            Gdx.app.debug(tag, message)
        }
    }

    override fun info(tag: String, message: String) {
        Gdx.app.log(tag, message)
    }

    override fun error(tag: String, message: String, throwable: Throwable?) {
        if (throwable != null) {
            Gdx.app.error(tag, message, throwable)
        } else {
            Gdx.app.error(tag, message)
        }
    }

    override fun isDebugEnabled() = debugEnabled

    fun setDebugEnabled(enabled: Boolean) {
        debugEnabled = enabled
    }
}

/**
 * Fallback implementation using standard println (for testing or non-LibGDX environments)
 */
class StandardServiceLogger(private var debugEnabled: Boolean = true) : ServiceLogger {

    override fun debug(tag: String, message: String) {
        if (debugEnabled) {
            println("DEBUG [$tag]: $message")
        }
    }

    override fun info(tag: String, message: String) {
        println("INFO [$tag]: $message")
    }

    override fun error(tag: String, message: String, throwable: Throwable?) {
        println("ERROR [$tag]: $message")
        throwable?.printStackTrace()
    }

    override fun isDebugEnabled() = debugEnabled

    fun setDebugEnabled(enabled: Boolean) {
        debugEnabled = enabled
    }
}

/**
 * NoOp implementation (for disabling logging entirely)
 */
class NoOpServiceLogger : ServiceLogger {
    override fun debug(tag: String, message: String) {}
    override fun info(tag: String, message: String) {}
    override fun error(tag: String, message: String, throwable: Throwable?) {}
    override fun isDebugEnabled() = false
}

/**
 * Factory for creating ServiceLogger instances
 */
object ServiceLoggerFactory {

    private var defaultLogger: ServiceLogger = createDefaultLogger()

    private fun createDefaultLogger(): ServiceLogger {
        return try {
            // Check if LibGDX is available
            Class.forName("com.badlogic.gdx.Gdx")
            LibGDXServiceLogger()
        } catch (e: Exception) {
            // Fallback to standard output
            StandardServiceLogger()
        }
    }

    fun getLogger(): ServiceLogger = defaultLogger

    fun setLogger(logger: ServiceLogger) {
        defaultLogger = logger
    }
}
