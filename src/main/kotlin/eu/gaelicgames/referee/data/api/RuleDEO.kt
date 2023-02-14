package eu.gaelicgames.referee.data.api

import eu.gaelicgames.referee.data.DisciplinaryAction
import eu.gaelicgames.referee.data.DisciplinaryActions
import eu.gaelicgames.referee.data.GameCode
import eu.gaelicgames.referee.data.Rule
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.transactions.transaction

@Serializable
data class RuleDEO(
    val id: Long,
    val code: Long,
    val isCaution: Boolean,
    val isBlack: Boolean,
    val isRed: Boolean,
    val description: String,
    val isDisabled: Boolean
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
                    rule.description,
                     rule.isDisabled
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
                rule.isDisabled = rUpdate.isDisabled
                Result.success(rule)
            } else {
                Result.failure(
                    IllegalArgumentException("Trying to update a rule with invalid id ${rUpdate.id}")
                )
            }
        }
    }
}

@Serializable
data class ModifyRulesDEOState(
    val id: Long,
) {

    fun delete(): Result<Boolean> {
        return transaction {
            val rule = Rule.findById(this@ModifyRulesDEOState.id)
            if(rule != null) {
                if(rule.isDeletable()) {
                    rule.delete()
                    Result.success(true)
                } else {
                    Result.failure(
                        IllegalArgumentException("Trying to delete a rule that is not deletable")
                    )
                }
            } else {
                Result.failure(
                    IllegalArgumentException("Trying to delete a rule with invalid id $id")
                )
            }
        }
    }

    fun toggleDisabledState(): Result<Rule> {
        return transaction {
            val rule = Rule.findById(this@ModifyRulesDEOState.id)
            if (rule != null) {
                rule.isDisabled = !rule.isDisabled
                Result.success(rule)
            } else {
                Result.failure(
                    IllegalArgumentException("Trying to disable a rule with invalid id $id")
                )
            }
        }
    }

    fun isDeletable(): Result<RuleIsDeletableDEO> {
        return transaction {
            val rule = Rule.findById(this@ModifyRulesDEOState.id)
            if (rule != null) {
                Result.success(RuleIsDeletableDEO(rule.id.value, rule.isDeletable()))
            } else {
                Result.failure(
                    IllegalArgumentException("Trying to delete a rule with invalid id $id")
                )
            }
        }
    }
}

@Serializable
data class RuleIsDeletableDEO(
    val id: Long,
    val isDeletable: Boolean
)

@Serializable
data class NewRuleDEO(
    val code: Long,
    val isCaution: Boolean,
    val isBlack: Boolean,
    val isRed: Boolean,
    val description: String,
    val isDisabled: Boolean
) {
    fun createInDatabase():Result<Rule> {
        val newRule = this
        return transaction {
            val code = GameCode.findById(newRule.code)
            if(code != null) {
                Result.success(Rule.new {
                    this.code = code
                    this.isCaution = newRule.isCaution
                    this.isBlack = newRule.isBlack
                    this.isRed = newRule.isRed
                    this.description = newRule.description
                    this.isDisabled = newRule.isDisabled
                })
            } else {
                Result.failure(IllegalArgumentException("Trying to create a rule with invalid code id ${newRule.code}"))
            }
        }
    }
}