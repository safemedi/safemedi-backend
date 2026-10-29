package com.safemedi.app.sefemedi.global.error

import org.springframework.http.HttpStatus

enum class ErrorCode(
    val status: HttpStatus,
    val code: String,
    val message: String,
) {
    INVALID_REQUEST(
        HttpStatus.BAD_REQUEST,
        "VAL_001",
        "요청 값이 올바르지 않습니다.",
    ),
    INVALID_NICKNAME_LENGTH(
        HttpStatus.BAD_REQUEST,
        "VAL_001",
        "닉네임은 최소 5자, 최대 20자 입력 가능합니다.",
    ),
    INVALID_ENUM_VALUE(
        HttpStatus.BAD_REQUEST,
        "VAL_001",
        "ENUM 값이 올바르지 않습니다.",
    ),
    INVALID_DISEASE_CODE(
        HttpStatus.BAD_REQUEST,
        "VAL_002",
        "존재하지 않는 기저질환 코드가 포함되어 있습니다.",
    ),
    INVALID_ALLERGY_FORMAT(
        HttpStatus.BAD_REQUEST,
        "VAL_002",
        "알러지 형식이 올바르지 않습니다.",
    ),
    DEVICE_TOKEN_TOO_LONG(
        HttpStatus.BAD_REQUEST,
        "VAL_002",
        "deviceToken은 512자를 초과할 수 없습니다.",
    ),
    INVALID_DATE_FORMAT(
        HttpStatus.BAD_REQUEST,
        "VAL_003",
        "생년월일 형식이 올바르지 않습니다.",
    ),
    UNSUPPORTED_DEVICE_TYPE(
        HttpStatus.BAD_REQUEST,
        "VAL_003",
        "deviceType은 IOS 또는 ANDROID만 지원합니다.",
    ),
    DEVICE_TOKEN_ACCESS_DENIED(
        HttpStatus.FORBIDDEN,
        "NOTI_001",
        "해당 기기 토큰을 해제할 권한이 없습니다.",
    ),
    DEVICE_TOKEN_NOT_FOUND(
        HttpStatus.NOT_FOUND,
        "NOTI_002",
        "존재하지 않는 기기 토큰입니다.",
    ),
    LOGOUT_DEVICE_TOKEN_REQUIRED(
        HttpStatus.BAD_REQUEST,
        "VAL_001",
        "필수 입력값(deviceToken)이 누락되었거나 비어있습니다.",
    ),
    LOGOUT_DEVICE_TOKEN_TOO_LONG(
        HttpStatus.BAD_REQUEST,
        "VAL_002",
        "deviceToken은 512자를 초과할 수 없습니다.",
    ),
    LOGOUT_DEVICE_TOKEN_ACCESS_DENIED(
        HttpStatus.FORBIDDEN,
        "NOTI_001",
        "해당 로그아웃 할 권한이 없습니다.",
    ),
    NOTIFICATION_NOT_FOUND(
        HttpStatus.NOT_FOUND,
        "NOTI_003",
        "존재하지 않는 알림입니다.",
    ),
    NOTIFICATION_ACCESS_DENIED(
        HttpStatus.FORBIDDEN,
        "NOTI_004",
        "해당 알림에 접근할 권한이 없습니다.",
    ),
    TUTORIAL_ALREADY_COMPLETED(
        HttpStatus.BAD_REQUEST,
        "TUT_001",
        "이미 완료된 튜토리얼입니다.",
    ),
    INVALID_SEARCH_KEYWORD(
        HttpStatus.BAD_REQUEST,
        "VAL_005",
        "검색어는 최소 2글자 이상 입력해야 합니다.",
    ),
    INVALID_DISEASE_SEARCH_KEYWORD(
        HttpStatus.BAD_REQUEST,
        "VAL_006",
        "기저질환 검색어를 입력해야 합니다.",
    ),
    INVALID_DRUG_ALLERGY_SEARCH_KEYWORD(
        HttpStatus.BAD_REQUEST,
        "VAL_007",
        "약물 알러지 검색어를 입력해야 합니다.",
    ),
    INVALID_FAMILY_RELATION(
        HttpStatus.BAD_REQUEST,
        "VAL_008",
        "가족 호칭은 1자 이상 20자 이하로 입력해야 합니다.",
    ),
    INVALID_PRESCRIPTION_DATE(
        HttpStatus.BAD_REQUEST,
        "MED_001",
        "날짜 설정이 올바르지 않습니다.",
    ),
    EMPTY_MEDICATIONS(
        HttpStatus.BAD_REQUEST,
        "MED_002",
        "분석할 약물 목록이 비어있습니다.",
    ),
    INVALID_TAKE_TIMES(
        HttpStatus.BAD_REQUEST,
        "MED_003",
        "복용 시간 목록이 비어있거나 형식이 올바르지 않습니다.",
    ),
    INVALID_PAGE_REQUEST(
        HttpStatus.BAD_REQUEST,
        "PAG_001",
        "페이지 번호(page)는 0 이상이고 크기(size)는 1 이상이어야 합니다.",
    ),
    STATISTICS_DATE_REQUIRED(
        HttpStatus.BAD_REQUEST,
        "STAT_001",
        "필수 날짜 파라미터가 누락되었습니다.",
    ),
    STATISTICS_INVALID_DATE_FORMAT(
        HttpStatus.BAD_REQUEST,
        "STAT_002",
        "날짜 형식이 올바르지 않습니다. (YYYY-MM-DD 필요)",
    ),
    STATISTICS_INVALID_DATE_RANGE(
        HttpStatus.BAD_REQUEST,
        "STAT_003",
        "시작일이 종료일보다 늦을 수 없습니다.",
    ),
    STATISTICS_DATE_RANGE_TOO_LONG(
        HttpStatus.BAD_REQUEST,
        "STAT_004",
        "조회 기간이 허용 범위를 초과했습니다.",
    ),
    FAMILY_ACCESS_DENIED(
        HttpStatus.FORBIDDEN,
        "FAM_003",
        "연동되지 않은 가족 정보는 조회할 수 없습니다.",
    ),
    FAMILY_INVITATION_NOT_FOUND(
        HttpStatus.NOT_FOUND,
        "INV_001",
        "존재하지 않거나 유효하지 않은 가족 초대 링크입니다.",
    ),
    FAMILY_INVITATION_EXPIRED(
        HttpStatus.GONE,
        "INV_002",
        "만료된 가족 초대 링크입니다.",
    ),
    FAMILY_INVITATION_ALREADY_USED(
        HttpStatus.CONFLICT,
        "INV_003",
        "이미 사용된 가족 초대 링크입니다.",
    ),
    FAMILY_INVITATION_SELF_ACCESS(
        HttpStatus.BAD_REQUEST,
        "INV_004",
        "자신이 생성한 가족 초대 링크는 사용할 수 없습니다.",
    ),
    FAMILY_INVITATION_INVITER_NAME_NOT_FOUND(
        HttpStatus.CONFLICT,
        "INV_005",
        "초대자 이름을 확인할 수 없는 가족 초대 링크입니다.",
    ),
    FAMILY_INVITATION_ALREADY_CONNECTED(
        HttpStatus.CONFLICT,
        "INV_006",
        "이미 가족으로 연동된 사용자입니다.",
    ),
    PRESCRIPTION_NOT_FOUND(
        HttpStatus.NOT_FOUND,
        "MED_005",
        "존재하지 않거나 이미 삭제된 처방전입니다.",
    ),
    PRESCRIPTION_ACCESS_DENIED(
        HttpStatus.FORBIDDEN,
        "MED_006",
        "본인의 처방전만 수정하거나 삭제할 수 있습니다.",
    ),
    ENDED_PRESCRIPTION_UPDATE_NOT_ALLOWED(
        HttpStatus.BAD_REQUEST,
        "MED_007",
        "이미 복용 기간이 종료된 처방전의 시간은 수정할 수 없습니다.",
    ),
    PRESCRIPTION_DRUG_NOT_FOUND(
        HttpStatus.NOT_FOUND,
        "MED_010",
        "수정하려는 처방 약물이 해당 처방전에 존재하지 않습니다.",
    ),
    MEDICATION_RECORD_NOT_FOUND(
        HttpStatus.NOT_FOUND,
        "REC_001",
        "존재하지 않는 복약 기록입니다.",
    ),
    MEDICATION_RECORD_ALREADY_PROCESSED(
        HttpStatus.CONFLICT,
        "REC_002",
        "해당 시간에 이미 복용 처리가 완료된 기록입니다.",
    ),
    MEDICATION_RECORD_TYPE_REQUIRED(
        HttpStatus.BAD_REQUEST,
        "REC_003",
        "복약 기록 조회 타입은 필수입니다.",
    ),
    INVALID_TOKEN(
        HttpStatus.UNAUTHORIZED,
        "AUTH_001",
        "유효하지 않은 토큰입니다.",
    ),
    REFRESH_TOKEN_REQUIRED(
        HttpStatus.BAD_REQUEST,
        "VAL_001",
        "필수 입력값(refreshToken)이 누락되었거나 비어있습니다.",
    ),
    INVALID_REFRESH_TOKEN(
        HttpStatus.UNAUTHORIZED,
        "AUTH_002",
        "유효하지 않은 refreshToken입니다.",
    ),
    EXPIRED_REFRESH_TOKEN(
        HttpStatus.UNAUTHORIZED,
        "AUTH_003",
        "만료된 refreshToken입니다. 다시 로그인해주세요.",
    ),
    UNSUPPORTED_SOCIAL_LOGIN_PROVIDER(
        HttpStatus.BAD_REQUEST,
        "AUTH_002",
        "지원하지 않는 소셜 로그인 서비스입니다.",
    ),
    INVALID_ACCESS_TOKEN(
        HttpStatus.UNAUTHORIZED,
        "AUTH_003",
        "유효하지 않은 액세스 토큰입니다. 다시 로그인해 주세요.",
    ),
    USER_NOT_FOUND(
        HttpStatus.NOT_FOUND,
        "USER_001",
        "존재하지 않는 사용자 정보입니다.",
    ),
    INTERNAL_SERVER_ERROR(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "SYS_500",
        "서버 내부 오류가 발생했습니다.",
    ),
    FAMILY_NOT_FOUND(
        HttpStatus.NOT_FOUND,
        "FAM_002",
        "존재하지 않거나 연동이 해제된 가족입니다.",
    ),
    FAMILY_INFO_ACCESS_DENIED(
        HttpStatus.FORBIDDEN,
        "FAM_004",
        "해당 가족이 건강 정보 공개를 허용하지 않았습니다.",
    ),
    USER_ALREADY_WITHDRAWN(
        HttpStatus.BAD_REQUEST,
        "USER_002",
        "이미 탈퇴 처리가 진행된 사용자입니다.",
    ),
    INVALID_COORDINATES(
        HttpStatus.BAD_REQUEST,
        "VAL_009",
        "latitude/longitude 값이 없거나 유효 범위를 벗어났습니다.",
    ),
    INVALID_FACILITY_CATEGORY(
        HttpStatus.BAD_REQUEST,
        "VAL_010",
        "category는 all, pharmacy, emergency 중 하나여야 합니다.",
    ),
}
