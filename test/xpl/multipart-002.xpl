<p:declare-step xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0" name="main">
  <p:output port="result"/>
  <p:http-request>
    <p:input port="source">
      <p:inline>
	<c:request method="post" href="http://tests.xproc.org/service/check-multipart">
          <c:header name="Content-Type" value="multipart/related"/>
          <c:header name="X-mpp" value="mpp header"/>
          <c:multipart boundary="aaaabbbbccccddddeeefffggghhhiiijjjkkkllmmmnop" content-type="multipart/related">
            <c:body content-type="text/plain" description="Some descriptive text">Hello World</c:body>
            <c:body content-type="text/plain" description="Some descriptive text">Goodbye!</c:body>
            <c:body content-type="application/xml" description="Some descriptive text">
              <doc/>
            </c:body>
          </c:multipart>
        </c:request>
      </p:inline>
    </p:input>
  </p:http-request>
</p:declare-step>