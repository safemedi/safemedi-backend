package com.safemedi.app.sefemedi.domain.family.service

import com.safemedi.app.sefemedi.domain.family.dto.FamilyRelationUpdateRequest
import com.safemedi.app.sefemedi.domain.family.dto.FamilyRelationUpdateResponse
import com.safemedi.app.sefemedi.domain.family.repository.FamilyRepository
import com.safemedi.app.sefemedi.domain.user.repository.UserRepository
import com.safemedi.app.sefemedi.global.error.BusinessException
import com.safemedi.app.sefemedi.global.error.ErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZoneOffset

@Service
class FamilyRelationUpdateService(
    private val userRepository: UserRepository,
    private val familyRepository: FamilyRepository,
) {

    @Transactional
    fun update(
        socialId: String,
        familyId: Long,
        request: FamilyRelationUpdateRequest,
    ): FamilyRelationUpdateResponse {
        val currentUser = userRepository.findBySocialId(socialId)
            ?: throw BusinessException(ErrorCode.INVALID_ACCESS_TOKEN)
        val relation = request.relation?.trim()
            ?.takeIf { it.isNotEmpty() && it.length <= MAX_RELATION_LENGTH }
            ?: throw BusinessException(ErrorCode.INVALID_FAMILY_RELATION)
        val family = familyRepository.findWithConnectedUserById(familyId)
            ?: throw BusinessException(ErrorCode.FAMILY_NOT_FOUND)
        val currentUserId = currentUser.id
            ?: throw BusinessException(ErrorCode.INTERNAL_SERVER_ERROR)

        if (family.user.id != currentUserId) {
            throw BusinessException(ErrorCode.FAMILY_ACCESS_DENIED)
        }

        val familyName = family.connectedUser.nickname
            ?: throw BusinessException(ErrorCode.INTERNAL_SERVER_ERROR)
        family.relation = relation
        familyRepository.flush()

        return FamilyRelationUpdateResponse(
            familyId = family.id
                ?: throw BusinessException(ErrorCode.INTERNAL_SERVER_ERROR),
            name = familyName,
            relation = family.relation,
            updatedAt = family.updatedAt?.toInstant(ZoneOffset.UTC)
                ?: throw BusinessException(ErrorCode.INTERNAL_SERVER_ERROR),
        )
    }

    private companion object {
        const val MAX_RELATION_LENGTH = 20
    }
}
