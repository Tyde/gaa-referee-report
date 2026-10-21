package eu.gaelicgames.referee.data.api

import eu.gaelicgames.referee.data.GameCode
import eu.gaelicgames.referee.data.Rule
import eu.gaelicgames.referee.data.Rules
import eu.gaelicgames.referee.util.CacheUtil
import eu.gaelicgames.referee.util.RuleSortKeyUtil
import eu.gaelicgames.referee.util.RuleTranslationUtil
import eu.gaelicgames.referee.util.lockedTransaction
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import java.time.LocalDateTime


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
            rule.descriptionEs,
            rule.ruleNumber,
            rule.ruleNumberSortKey,
            rule.superseeds?.id?.value,
            rule.isLatest,
            rule.createdAt?.toString(),
            rule.createdBy?.id?.value,
            rule.lineageRootId
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
    val ruleNumber = row[Rules.ruleNumber]
    val ruleNumberSortKey = row[Rules.ruleNumberSortKey]
    val superseeds = row[Rules.superseeds]?.value
    val isLatest = row[Rules.isLatest]
    val createdAt = row[Rules.createdAt]?.toString()
    val createdBy = row[Rules.createdBy]?.value
    val lineageRootId = row[Rules.lineageRootId]
    return RuleDEO(
        id, code, isCaution, isBlack, isRed, description, isDisabled,
        descriptionFr, descriptionDe, descriptionEs, ruleNumber, ruleNumberSortKey,
        superseeds, isLatest, createdAt, createdBy, lineageRootId
    )
}

suspend fun RuleDEO.Companion.allRules(): List<RuleDEO> {
    return CacheUtil.getCachedRules()
        .getOrElse {
            lockedTransaction {
                val rules = Rules.selectAll()
                    .where {
                        Rules.isLatest eq true and (Rules.isDisabled eq false)
                    }
                    // ruleNumberSortKey is null-safe: sort by it first so rules
                    // without a number fall to the end per the DB's null ordering.
                    .orderBy(Rules.ruleNumberSortKey to SortOrder.ASC, Rules.id to SortOrder.ASC)
                    .map {
                        RuleDEO.wrapRow(it)
                    }
                CacheUtil.cacheRules(rules)
                rules
            }
        }

}

suspend fun RuleDEO.Companion.findById(id: Long): RuleDEO? {
    return lockedTransaction {
        Rule.findById(id)?.let { RuleDEO.fromRule(it) }
    }
}

suspend fun RuleDEO.Companion.allVersionsForLineage(lineageRootId: Long): List<RuleDEO> {
    return lockedTransaction {
        Rule.find {
            (Rules.lineageRootId eq lineageRootId) or (Rules.id eq lineageRootId)
        }.orderBy(Rules.createdAt to SortOrder.ASC, Rules.id to SortOrder.ASC).map {
            RuleDEO.fromRule(it)
        }
    }
}

suspend fun RuleDEO.Companion.getHistoryForRule(ruleId: Long): Result<RuleHistoryDEO> {
    return lockedTransaction {
        val rule = Rule.findById(ruleId)
        if (rule != null) {
            val root = rule.lineageRootId ?: rule.id.value
            val versions = Rule.find {
                (Rules.lineageRootId eq root) or (Rules.id eq root)
            }.orderBy(Rules.createdAt to SortOrder.ASC, Rules.id to SortOrder.ASC).map {
                RuleDEO.fromRule(it)
            }
            Result.success(RuleHistoryDEO(root, versions))
        } else {
            Result.failure(
                IllegalArgumentException("Trying to get history for rule with invalid id $ruleId")
            )
        }
    }
}

suspend fun RuleDEO.getHistory(): Result<RuleHistoryDEO> {
    return RuleDEO.getHistoryForRule(this@getHistory.id)
}

@Deprecated(
    "Update now creates a new immutable version. Use NewRuleVersionDEO.createNewVersion() instead.",
    ReplaceWith("NewRuleVersionDEO(parentId, code, isCaution, isBlack, isRed, description, isDisabled, ruleNumber, descriptionFr, descriptionDe, descriptionEs).createNewVersion()")
)
suspend fun RuleDEO.updateInDatabase(): Result<Rule> {
    val rUpdate = this
    CacheUtil.deleteCachedRules()
    return lockedTransaction {
        val parent = Rule.findById(rUpdate.id)
        if (parent != null) {
            parent.isLatest = false
            GameCode.findById(rUpdate.code)?.let { code ->
                val lineageRoot = parent.lineageRootId ?: parent.id.value
                val newRule = Rule.new {
                    this.code = code
                    this.isCaution = rUpdate.isCaution
                    this.isBlack = rUpdate.isBlack
                    this.isRed = rUpdate.isRed
                    this.description = rUpdate.description
                    this.isDisabled = rUpdate.isDisabled
                    this.descriptionFr = rUpdate.descriptionFr
                    this.descriptionEs = rUpdate.descriptionEs
                    this.descriptionDe = rUpdate.descriptionDe
                    this.ruleNumber = rUpdate.ruleNumber
                    this.ruleNumberSortKey = RuleSortKeyUtil.deriveSortKey(rUpdate.ruleNumber)
                    this.superseeds = parent
                    this.isLatest = true
                    this.createdAt = LocalDateTime.now()
                    this.lineageRootId = lineageRoot
                }
                Result.success(newRule)
            } ?: Result.failure(
                IllegalArgumentException("Trying to update a rule with invalid code id ${rUpdate.code}")
            )
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
            // Disable/enable applies to the whole lineage: only the latest version
            // is exposed to selectors, so toggle isDisabled on the latest row.
            val root = rule.lineageRootId ?: rule.id.value
            val latest = Rule.find {
                (Rules.lineageRootId eq root) and (Rules.isLatest eq true)
            }.firstOrNull() ?: rule
            latest.isDisabled = !latest.isDisabled
            Result.success(latest)
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
            val rule = Rule.new {
                this.code = code
                this.isCaution = newRule.isCaution
                this.isBlack = newRule.isBlack
                this.isRed = newRule.isRed
                this.description = newRule.description
                this.isDisabled = newRule.isDisabled
                this.descriptionFr = newRule.descriptionFr
                this.descriptionEs = newRule.descriptionEs
                this.descriptionDe = newRule.descriptionDe
                this.ruleNumber = newRule.ruleNumber
                this.ruleNumberSortKey = RuleSortKeyUtil.deriveSortKey(newRule.ruleNumber)
                this.superseeds = null
                this.isLatest = true
                this.createdAt = LocalDateTime.now()
                this.lineageRootId = null
            }
            // Two-phase: the lineage root is this rule's own id, which is only
            // known after the insert.
            rule.lineageRootId = rule.id.value
            Result.success(rule)
        } else {
            Result.failure(IllegalArgumentException("Trying to create a rule with invalid code id ${newRule.code}"))
        }
    }
}

suspend fun NewRuleVersionDEO.createNewVersion(): Result<Rule> {

    CacheUtil.deleteCachedRules()

    return lockedTransaction {
        val parent = Rule.findById(parentId)
        if (parent != null) {
            parent.isLatest = false
            val code = GameCode.findById(code)
            if (code != null) {
                val lineageRoot = parent.lineageRootId ?: parent.id.value
                val newRule = Rule.new {
                    this.code = code
                    this.isCaution = this@createNewVersion.isCaution
                    this.isBlack = this@createNewVersion.isBlack
                    this.isRed = this@createNewVersion.isRed
                    this.description = this@createNewVersion.description
                    this.isDisabled = this@createNewVersion.isDisabled
                    this.descriptionFr = this@createNewVersion.descriptionFr
                    this.descriptionEs = this@createNewVersion.descriptionEs
                    this.descriptionDe = this@createNewVersion.descriptionDe
                    this.ruleNumber = this@createNewVersion.ruleNumber
                    this.ruleNumberSortKey = RuleSortKeyUtil.deriveSortKey(this@createNewVersion.ruleNumber)
                    this.superseeds = parent
                    this.isLatest = true
                    this.createdAt = LocalDateTime.now()
                    this.lineageRootId = lineageRoot
                }
                Result.success(newRule)
            } else {
                Result.failure(IllegalArgumentException("Trying to create a new rule version with invalid code id ${this@createNewVersion.code}"))
            }
        } else {
            Result.failure(IllegalArgumentException("Trying to create a new rule version with invalid parent id ${this@createNewVersion.parentId}"))
        }
    }
}


suspend fun RuleTranslationRequestDEO.translate(): Result<RuleTranslation> {
    return RuleTranslationUtil.translateRule(this.description)
}