package com.safemedi.app.sefemedi.domain.medication.analyzer

import com.safemedi.app.sefemedi.domain.medication.dto.MedicationSafetyStatus
import com.safemedi.app.sefemedi.domain.medication.dto.MedicationWarningType
import com.safemedi.app.sefemedi.domain.user.entity.AllergyType
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Component
@Order(2)
class AtcCodeAllergyAnalyzerImpl : PrescriptionAnalyzer {
    override fun analyze(context: PrescriptionContext) {
        val atcAllergies =
            context.allergies.filter {
                it.allergyType == AllergyType.ATC_GROUP &&
                    it.allergyValue.isNotBlank()
            }

        if (atcAllergies.isEmpty()) {
            return
        }

        context.medications.forEach { medication ->
            val medicationAtcCode = medication.atcCode.trim().uppercase()

            atcAllergies.forEach { allergy ->
                val allergyAtcCode = allergy.allergyValue.trim().uppercase()
                if (medicationAtcCode.startsWith(allergyAtcCode)) {
                    context.addWarning(
                        medication = medication,
                        type = MedicationWarningType.ALLERGY,
                        message = "${allergy.allergyName} ATC 알러지 그룹과 교차 반응 가능성이 있습니다.",
                        status = MedicationSafetyStatus.DANGER,
                    )
                }
            }
        }
    }
}
