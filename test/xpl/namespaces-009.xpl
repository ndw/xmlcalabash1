<p:pipeline xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0" name="pipeline">

<p:variable name="irrelevant" select="/not-there"/>

<p:try>
  <p:group>
    <p:delete>
      <p:with-option name="match" select="'h:del'">
        <p:namespaces binding="irrelevant"/>
      </p:with-option>
      <p:input port="source">
        <p:inline>
          <html xmlns="http://www.w3.org/1999/xhtml">
            <head>
              <title>Some title</title>
            </head>
            <body>
              <h1>Some title</h1>
              <p>Some <del>deleted</del>text.</p>
            </body>
          </html>
        </p:inline>
      </p:input>
    </p:delete>
  </p:group>
  <p:catch>
    <p:identity>
      <p:input port="source">
        <p:inline>
          <message>Catch caught XPath failure.</message>
        </p:inline>
      </p:input>
    </p:identity>
  </p:catch>
</p:try>

</p:pipeline>