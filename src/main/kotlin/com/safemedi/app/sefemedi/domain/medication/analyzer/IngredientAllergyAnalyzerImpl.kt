package com.safemedi.app.sefemedi.domain.medication.analyzer

import com.safemedi.app.sefemedi.domain.medication.dto.MedicationSafetyStatus
import com.safemedi.app.sefemedi.domain.medication.dto.MedicationWarningType
import com.safemedi.app.sefemedi.domain.user.entity.AllergyType
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Component
@Order(1)
class IngredientAllergyAnalyzerImpl : PrescriptionAnalyzer {
    override fun analyze(context: PrescriptionContext) {
        val ingredientAllergies =
            context.allergies.filter { it.allergyType == AllergyType.INGREDIENT }

        if (ingredientAllergies.isEmpty()) {
            return
        }

        context.medications.forEach { medication ->
            val ingredientNames = context.ingredientsOf(medication)
            ingredientAllergies.forEach { allergy ->
                val matched = ingredientNames.any {
                    it.equals(allergy.allergyName, ignoreCase = true) ||
                        it.equals(allergy.allergyValue, ignoreCase = true)
                }

                if (matched) {
                    context.addWarning(
                        medication = medication,
                        type = MedicationWarningType.ALLERGY,
                        message = "${allergy.allergyName} 알러지 성분과 일치합니다.",
                        status = MedicationSafetyStatus.DANGER,
                    )
                }
            }
        }
    }
}
