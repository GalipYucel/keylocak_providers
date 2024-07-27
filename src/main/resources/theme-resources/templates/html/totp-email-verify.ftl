<#import "template.ftl" as layout>
<@layout.emailLayout>
<!DOCTYPE html>
<html lang="en">
<head>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Temporary Login Code</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            margin: 0;
            padding: 0;
            background-color: #f4f4f4;
            display: flex;
            justify-content: center;
            align-items: center;
            height: 100vh;
        }
        .email-container {
            background-color: #ffffff;
            padding: 20px;
            border-radius: 5px;
            box-shadow: 0 0 10px rgba(0,0,0,0.1);
            text-align: center;
            max-width: 600px;
            width: 100%;
        }
        .code {
            background-color: #e0e0e0;
            padding: 10px;
            border-radius: 3px;
            font-size: 24px;
            display: inline-block;
            margin: 10px 0;
        }
        .validity {
            margin-top: 10px;
            font-size: 16px;
            color: #555;
        }
    </style>
</head>
<body>
    <div class="email-container">
        <p>Your temporary login code is:</p>
        <div class="code">${code}</div>
        <p class="validity">This code is valid for ${ttl} minutes.</p>
    </div>
</body>
</html>

</@layout.emailLayout>
