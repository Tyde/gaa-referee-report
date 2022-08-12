<#-- @ftlvariable name="nonSubmittedReports" type="java.util.List<Pair<eu.gaelicgames.referee.data.TournamentReport,eu.gaelicgames.referee.data.Tournament>>" -->
<#-- @ftlvariable name="submittedReports" type="java.util.List<Pair<eu.gaelicgames.referee.data.TournamentReport,eu.gaelicgames.referee.data.Tournament>>" -->
<a href="/report/new">Create new report</a>

<h1>My non-submitted reports</h1>
<table>
<#list nonSubmittedReports as report>
    <tr>
        <td>${report.second.date}</td>
        <td>${report.second.name}</td>
        <td>${report.second.location}</td>
        <td><a href="/report/edit/${report.first.id}">Edit</a></td>
        <td><a href="#">Submit</a></td>
        <td><a href="#">Delete</a></td>
    </tr>
</#list>
</table>
<h1>My submitted reports</h1>
<table>
    <#list submittedReports as report>
        <tr>
            <td>${report.second.date}</td>
            <td>${report.second.name}</td>
            <td>${report.second.location}</td>
            <td><a href="#">Show</a></td>
        </tr>
    </#list>
</table>
