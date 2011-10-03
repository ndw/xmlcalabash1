<p:declare-step xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0" name="main">
  <p:output port="result"/>
  <p:http-request>
    <p:input port="source">
      <p:inline exclude-inline-prefixes="#all">
	<c:request method="post" href="http://tests.xproc.org/service/check-multipart">
          <c:multipart boundary="aaaabbbbccccddddeeefffggghhhiiijjjkkkllmmmnop" content-type="multipart/mixed">
            <c:body content-type="text/plain; charset=utf8" id="firstpart">Hello World</c:body>
            <c:body content-type="text/plain; charset=iso-8859-2" encoding="base64">PHBhcmE+uWVk6SBteblpPC9wYXJhPg0K</c:body>
            <c:body content-type="application/xml" description="Some descriptive text"><!-- comment --><doc><para>Some text</para></doc></c:body>
          </c:multipart>
        </c:request>
      </p:inline>
    </p:input>
  </p:http-request>
</p:declare-step>