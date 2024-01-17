<#-- @ftlvariable name="disciplinaryActions" type="java.util.List<eu.gaelicgames.referee.data.api.DisciplinaryActionStringDEO>" -->
<ul>
    <#list disciplinaryActions as disciplinaryAction>
        <li>
            Tournament: ${disciplinaryAction.tournamentName} - ${disciplinaryAction.tournamentLocation} ${disciplinaryAction.tournamentDateTime}<br>
            Player: ${disciplinaryAction.firstName} ${disciplinaryAction.lastName}<br>
            Team: ${disciplinaryAction.teamName}<br>
            Game vs. ${disciplinaryAction.opposingTeamName}<br>
            Red Card<br>
            ${disciplinaryAction.ruleName}<br>
            ${disciplinaryAction.details}<br>
            <a href="${disciplinaryAction.reportShareLink}">Report link</a>
        </li>
    </#list>

</ul>
