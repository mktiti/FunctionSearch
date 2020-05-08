
fun <T> const(value: T): () -> T = { value }

fun <T : Any> T.asConst(): (() -> T) = const(this)

fun <T> Boolean.map(onTrue: T, onFalse: T): T = if (this) onTrue else onFalse

fun <T> Boolean.map(onTrue: () -> T, onFalse: () -> T): T = if (this) onTrue() else onFalse()
