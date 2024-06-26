package androidmakers.service

val sandboxIndex = """
    <!DOCTYPE html>
    <html lang="en" style="display:table;width:100%;height:100%;">

    <head>
        <meta charset="utf-8">
        <title>AndroidMakers API sandbox</title>
    </head>

    <body style="height: 100%; display:table-cell;">
    <div style="width: 100%; height: 100%;" id='embedded-sandbox'></div>
    <script src="https://embeddable-sandbox.cdn.apollographql.com/_latest/embeddable-sandbox.umd.production.min.js"></script>
    <script>
      new window.EmbeddedSandbox({
        target: '#embedded-sandbox',
        initialEndpoint: 'https://androidmakers.fr/graphql',
      });
    </script>
    </body>

    </html>
""".trimIndent()