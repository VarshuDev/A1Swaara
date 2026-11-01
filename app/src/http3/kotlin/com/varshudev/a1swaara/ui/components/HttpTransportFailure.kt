package com.varshudev.a1swaara.ui.components

import com.varshudev.a1swaara.utils.isHttp3TransportFailure

internal fun Throwable.isHttpTransportFailure(): Boolean = isHttp3TransportFailure()
