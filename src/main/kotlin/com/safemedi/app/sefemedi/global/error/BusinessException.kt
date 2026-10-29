package com.safemedi.app.sefemedi.global.error

class BusinessException(
    val errorCode: ErrorCode,
) : RuntimeException(errorCode.message)
