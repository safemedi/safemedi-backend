package com.safemedi.app.sefemedi.domain.notification.entity

enum class NotificationType {
    MEDICATION_REMINDER,
    MEDICATION_COMPLETED, // 기존 알림 이력 조회를 위해 유지하며 신규 생성하지 않는다.
    DRUG_INTERACTION_WARNING,
    TODAY_MEDICATION_SCHEDULE,
    REPORT_READY,
    FAMILY_CONNECTED,
    FAMILY_DISCONNECTED,
}
