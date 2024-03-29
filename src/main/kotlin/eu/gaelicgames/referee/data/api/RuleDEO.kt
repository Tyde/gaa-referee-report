package eu.gaelicgames.referee.data.api

import eu.gaelicgames.referee.data.GameCode
import eu.gaelicgames.referee.data.Rule
import eu.gaelicgames.referee.data.Rules
import eu.gaelicgames.referee.util.CacheUtil
import eu.gaelicgames.referee.util.lockedTransaction
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.selectAll


suspend fun RuleDEO.Companion.fromRule(rule: Rule): RuleDEO {
    return lockedTransaction {
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

suspend fun RuleDEO.Companion.wrapRow(row: ResultRow): RuleDEO {
    val id = row[Rules.id].value
    val code = row[Rules.code].value
    val isCaution = row[Rules.isCaution]
    val isBlack = row[Rules.isBlack]
    val isRed = row[Rules.isRed]
    val description = row[Rules.description]
    val isDisabled = row[Rules.isDisabled]
    return RuleDEO(id, code, isCaution, isBlack, isRed, description, isDisabled)
}

suspend fun RuleDEO.Companion.allRules(): List<RuleDEO> {
    return CacheUtil.getCachedRules()
        .getOrElse {
            lockedTransaction {
                val rules = Rules.selectAll().map {
                    RuleDEO.wrapRow(it)
                }
                CacheUtil.cacheRules(rules)
                rules
            }
        }

}


suspend fun RuleDEO.updateInDatabase(): Result<Rule> {
    val rUpdate = this
    runBlocking {
        CacheUtil.deleteCachedRules()
    }
    return lockedTransaction {
        val rule = Rule.findById(rUpdate.id)
        if (rule != null) {
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


suspend fun ModifyRulesDEOState.delete(): Result<Boolean> {
    runBlocking {
        CacheUtil.deleteCachedRules()
    }
    return lockedTransaction {
        val rule = Rule.findById(this@delete.id)
        if (rule != null) {
            if (rule.isDeletable()) {
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

suspend fun ModifyRulesDEOState.toggleDisabledState(): Result<Rule> {
    runBlocking {
        CacheUtil.deleteCachedRules()
    }
    return lockedTransaction {
        val rule = Rule.findById(this@toggleDisabledState.id)
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

suspend fun ModifyRulesDEOState.isDeletable(): Result<RuleIsDeletableDEO> {
    return lockedTransaction {
        val rule = Rule.findById(this@isDeletable.id)
        if (rule != null) {
            Result.success(RuleIsDeletableDEO(rule.id.value, rule.isDeletable()))
        } else {
            Result.failure(
                IllegalArgumentException("Trying to delete a rule with invalid id $id")
            )
        }
    }
}


suspend fun NewRuleDEO.createInDatabase(): Result<Rule> {

    CacheUtil.deleteCachedRules()

    val newRule = this
    return lockedTransaction {
        val code = GameCode.findById(newRule.code)
        if (code != null) {
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

