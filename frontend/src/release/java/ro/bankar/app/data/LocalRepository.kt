package ro.bankar.app.data

import androidx.compose.runtime.compositionLocalOf
import kotlinx.coroutines.GlobalScope

val LocalRepository = compositionLocalOf<Repository> { throw RuntimeException("LocalRepository provider not found") }

object EmptyRepository : Repository(GlobalScope, "", {}) {
    override val profile get() = throw RuntimeException("EmptyRepository cannot be accessed")
    override val friends get() = throw RuntimeException("EmptyRepository cannot be accessed")
    override val friendRequests get() = throw RuntimeException("EmptyRepository cannot be accessed")
    override val accounts get() = throw RuntimeException("EmptyRepository cannot be accessed")
    override fun account(id: Int) = throw RuntimeException("EmptyRepository cannot be accessed")
}