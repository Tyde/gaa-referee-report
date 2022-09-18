<#-- @ftlvariable name="nonSubmittedReports" type="java.util.List<Pair<eu.gaelicgames.referee.data.TournamentReport,eu.gaelicgames.referee.data.Tournament>>" -->
<#-- @ftlvariable name="submittedReports" type="java.util.List<Pair<eu.gaelicgames.referee.data.TournamentReport,eu.gaelicgames.referee.data.Tournament>>" -->
<html>
<head>
    <title>GGE Referee Reports</title>
    <link rel="stylesheet" href="/static/w3.css">
    <style>
        .new-report-button {

        }
        .center-button-container {
            display: flex;
            flex-direction: row;
            justify-content: center;
        }
    </style>
</head>
<body>
<div class="w3-container" style="padding-top: 16px;">

    <div class="center-button-container">
    <div class="new-report-button">
        <a class="w3-button w3-indigo w3-round-large" href="/report/new">Create
            new report
        </a>
    </div>
    </div>

    <h1>My non-submitted reports</h1>
    <table class="w3-table w3-striped w3-bordered">
        <tr>
            <th>Date</th>
            <th>Tournament name</th>
            <th>Tournament location</th>
            <th></th>
        </tr>
        <#list nonSubmittedReports as report>
            <tr>
                <td>${report.second.date}</td>
                <td>${report.second.name}</td>
                <td>${report.second.location}</td>
                <td><a href="/report/edit/${report.first.id}">Edit</a></td>
                <td><a href="#">Delete</a></td>
            </tr>
        </#list>
    </table>
    <h1>My submitted reports</h1>
    <table class="w3-table w3-striped w3-bordered">
        <tr>
            <th>Date</th>
            <th>Tournament name</th>
            <th>Tournament location</th>
            <th></th>
            <th>Submission timestamp</th>
        </tr>
        <#list submittedReports as report>
            <tr>
                <td>${report.second.date}</td>
                <td>${report.second.name}</td>
                <td>${report.second.location}</td>
                <td><a href="/report/show/${report.first.id}">Show</a></td>
                <td>${report.first.submitDate}</td>
            </tr>
        </#list>
    </table>
</div>
</body>
</html>