<html>
<head>
    <title>GGE Referee Report portal</title>
    <link rel="stylesheet" href="/static/w3.css">
    <style>

        .flexbox-container {
            display: flex;
            flex-direction: row;
            justify-content: center;
            padding-top: 16px;
            width:800px;
            margin:auto;
        }
    </style>
</head>
<body>
<form action="/login" enctype="application/x-www-form-urlencoded" method="post">
    <div class="w3-container flexbox-container w3-card-4 w3-light-grey">
        <div>
            <h2>Please login with your referee credentials:</h2>
        <p>
            Your mail:<br>
            <input class="w3-input w3-border w3-round" type="text" name="mail">
        </p>
        <p>
            Password:<br>
            <input class="w3-input w3-border w3-round"  type="password" name="password">
        </p>
        <p>
            <input type="submit" class="w3-button w3-indigo" value="Login">
        </p>
        </div>
    </div>
</form>
</body>
</html>