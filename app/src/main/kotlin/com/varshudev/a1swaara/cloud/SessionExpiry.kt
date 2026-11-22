package com.varshudev.a1swaara.cloud

/** Keep a safety margin so a token does not expire in the middle of a request. */
internal object SessionExpiry {
    private const val REFRESH_MARGIN_MS = 60_000L

    fun needsRefresh(expiresAt: Long, now: Long): Boolean =
        expiresAt <= now || expiresAt - now <= REFRESH_MARGIN_MS
}
