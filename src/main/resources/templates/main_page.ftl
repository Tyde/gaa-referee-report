<#-- @ftlvariable name="nonSubmittedReports" type="java.util.List<eu.gaelicgames.referee.data.TournamentReport>" -->
<#-- @ftlvariable name="submittedReports" type="java.util.List<eu.gaelicgames.referee.data.TournamentReport>" -->
<a href="/reports/new">Create new report</a>

<h1>My non-submitted reports</h1>
<table>
<#list nonSubmittedReports as report>
    <tr>
        <td>${report.tournament.date}</td>
        <td>${report.tournament.name}</td>
        <td>${report.tournament.location}</td>
        <td><a href="#">Edit</a></td>
        <td><a href="#">Submit</a></td>
        <td><a href="#">Delete</a></td>
    </tr>
</#list>
</table>
<h1>My submitted reports</h1>
<table>
    <#list submittedReports as report>
        <tr>
            <td>${report.tournament.date}</td>
            <td>${report.tournament.name}</td>
            <td>${report.tournament.location}</td>
            <td><a href="#">Show</a></td>
        </tr>
    </#list>
</table>
