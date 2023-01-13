package eu.gaelicgames.referee.data.api

import eu.gaelicgames.referee.data.DisciplinaryAction
import eu.gaelicgames.referee.data.DisciplinaryActions
import eu.gaelicgames.referee.data.GameCode
import eu.gaelicgames.referee.data.Rule
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.transactions.transaction

@Serializable
data class UpdateRuleDEO(
    val id:Long,
    val code: Long? = null,
    val isCaution: Boolean? = null,
    val isBlack: Boolean? = null,
    val isRed: Boolean? = null,
    val description: String? = null,
    val isDisabled: Boolean? = null,
) {
    fun updateInDatabase():Result<Rule> {
        val rUpdate = this
        return transaction {
            val rule = Rule.findById(rUpdate.id)
            if (rule != null) {
                rUpdate.code?.let { code ->
                    val gameCode = GameCode.findById(code)
                    if (gameCode != null) {
                        rule.code = gameCode
                    }
                }
                rUpdate.isCaution?.let { isCaution ->
                    rule.isCaution = isCaution
                }
                rUpdate.isBlack?.let { isBlack ->
                    rule.isBlack = isBlack
                }
                rUpdate.isRed?.let { isRed ->
                    rule.isRed = isRed
                }
                rUpdate.description?.let { description ->
                    rule.description = description
                }
                rUpdate.isDisabled?.let { isDisabled ->
                    rule.isDisabled = isDisabled
                }
                Result.success(rule)
            } else {
                Result.failure(
                    IllegalArgumentException("Trying to update a rule with invalid id ${rUpdate.id}")
                )
            }
        }
    }

    fun checkIfDeletable():Result<Boolean> {
        val rUpdate = this
        return transaction {
            val rule = Rule.findById(rUpdate.id)
            if (rule != null) {
                val deletable = DisciplinaryAction.find {
                    DisciplinaryActions.rule eq rule.id
                }.count() == 0L
                Result.success(deletable)
            } else {
                Result.failure(
                    IllegalArgumentException("Trying to check a rule with invalid id ${rUpdate.id}")
                )
            }
        }
    }

    fun deleteFromDatabase():Result<Boolean> {
        val rUpdate = this
        return transaction {
            if (rUpdate.checkIfDeletable().getOrDefault(false)) {
                val rule = Rule.findById(rUpdate.id)
                if (rule != null) {
                    rule.delete()
                    Result.success(true)
                } else {
                    Result.failure(
                        IllegalArgumentException("Trying to delete a rule with invalid id ${rUpdate.id}")
                    )
                }
            } else {
                Result.failure(
                    IllegalArgumentException("Trying to delete a rule that is in use")
                )
            }
        }
    }
}

@Serializable
data class RuleDEO(
    val id: Long,
    val code: Long,
    val isCaution: Boolean,
    val isBlack: Boolean,
    val isRed: Boolean,
    val description: String
) {

    companion object {
        fun fromRule(rule: Rule): RuleDEO {
            return transaction {
                 RuleDEO(
                    rule.id.value,
                    rule.code.id.value,
                    rule.isCaution,
                    rule.isBlack,
                    rule.isRed,
                    rule.description
                )
            }
        }
    }


    fun updateInDatabase(): Result<Rule> {
        val rUpdate = this
        return transaction {
            val rule = Rule.findById(rUpdate.id)
            if(rule != null) {
                GameCode.findById(rUpdate.code)?.let {
                    rule.code = it
                }
                rule.isCaution = rUpdate.isCaution
                rule.isBlack = rUpdate.isBlack
                rule.isRed = rUpdate.isRed
                rule.description = rUpdate.description
                Result.success(rule)
            } else {
                Result.failure(
                    IllegalArgumentException("Trying to update a rule with invalid id ${rUpdate.id}")
                )
            }
        }
    }
}