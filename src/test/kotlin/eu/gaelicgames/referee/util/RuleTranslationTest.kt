package eu.gaelicgames.referee.util

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class RuleTranslationTest {

    @Test
    fun `test translateRule parsed correctly`() {
        runBlocking {
            val rule =
                " CAUTION: Rule 5.11b (Repeat Infraction) To charge an opponent unless: (i) he is in possession of the ball, or (ii) he is playing the ball, or (iii) both players are moving in the direction of the ball to play it."
            val translation = RuleTranslationUtil.translateRule(rule)
            assert(translation.isSuccess)
            println("Answer: "+ translation.getOrNull())
        }
    }
}
