package com.safemedi.app.sefemedi.domain.family.service

import com.safemedi.app.sefemedi.domain.family.dto.FamilyListItemResponse
import com.safemedi.app.sefemedi.domain.family.dto.FamilyListResponse
import com.safemedi.app.sefemedi.domain.family.repository.FamilyRepository
import com.safemedi.app.sefemedi.domain.user.repository.UserRepository
import com.safemedi.app.sefemedi.global.error.BusinessException
import com.safemedi.app.sefemedi.global.error.ErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class FamilyListQueryService(
    private val userRepository: UserRepository,
    private val familyRepository: FamilyRepository,
) {

    @Transactional(readOnly = true)
    fun getFamilies(socialId: String): FamilyListResponse {
        val currentUser = userRepository.findBySocialId(socialId)
            ?: throw BusinessException(ErrorCode.INVALID_ACCESS_TOKEN)
        val currentUserId = currentUser.id
            ?: throw BusinessException(ErrorCode.INTERNAL_SERVER_ERROR)
        val connectedFamilies = familyRepository.findAllByUser_IdOrderByCreatedAtAsc(currentUserId)
            .map { family ->
                FamilyListItemResponse(
                    familyId = family.id
                        ?: throw BusinessException(ErrorCode.INTERNAL_SERVER_ERROR),
                    name = family.connectedUser.nickname,
                    relation = family.relation,
                )
            }

        val families = buildList(connectedFamilies.size + 1) {
            add(
                FamilyListItemResponse(
                    familyId = null,
                    name = currentUser.nickname,
                    relation = SELF_RELATION,
                )
            )
            addAll(connectedFamilies)
        }

        return FamilyListResponse(families = families)
    }

    private companion object {
        const val SELF_RELATION = "본인"
    }
}
