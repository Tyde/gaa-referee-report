package eu.gaelicgames.referee.data.api

import eu.gaelicgames.referee.data.GameCode
import eu.gaelicgames.referee.data.Rule
import eu.gaelicgames.referee.data.Rules
import eu.gaelicgames.referee.util.CacheUtil
import eu.gaelicgames.referee.util.RuleTranslationUtil
import eu.gaelicgames.referee.util.lockedTransaction
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
            rule.isDisabled,
            rule.descriptionFr,
            rule.descriptionDe,
            rule.descriptionEs
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
    val descriptionFr = row[Rules.descriptionFr]
    val descriptionEs = row[Rules.descriptionEs]
    val descriptionDe = row[Rules.descriptionDe]
    return RuleDEO(id, code, isCaution, isBlack, isRed, description, isDisabled, descriptionFr, descriptionDe, descriptionEs)
}

suspend fun RuleDEO.Companion.allRules(): List<RuleDEO> {
    return CacheUtil.getCachedRules()
        .getOrElse {
            lockedTransaction {
                val rules = Rules.selectAll().orderBy(Rules.id).map {
                    RuleDEO.wrapRow(it)
                }
                CacheUtil.cacheRules(rules)
                rules
            }
        }

}


suspend fun RuleDEO.updateInDatabase(): Result<Rule> {
    val rUpdate = this
    CacheUtil.deleteCachedRules()
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

            rule.descriptionFr = rUpdate.descriptionFr
            rule.descriptionEs = rUpdate.descriptionEs
            rule.descriptionDe = rUpdate.descriptionDe
            Result.success(rule)
        } else {
            Result.failure(
                IllegalArgumentException("Trying to update a rule with invalid id ${rUpdate.id}")
            )
        }
    }
}


suspend fun ModifyRulesDEOState.delete(): Result<Boolean> {

    CacheUtil.deleteCachedRules()

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
    CacheUtil.deleteCachedRules()

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
                this.descriptionFr = newRule.descriptionFr
                this.descriptionEs = newRule.descriptionEs
                this.descriptionDe = newRule.descriptionDe
            })
        } else {
            Result.failure(IllegalArgumentException("Trying to create a rule with invalid code id ${newRule.code}"))
        }
    }
}


suspend fun RuleTranslationRequestDEO.translate(): Result<RuleTranslation> {
    return RuleTranslationUtil.translateRule(this.description)
}

