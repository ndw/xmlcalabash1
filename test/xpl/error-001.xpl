<p:declare-step xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" xmlns:usrerr="http://xproc.org/ns/user-error" version="1.0">
    <p:error name="go-bang" code="usrerr:X1">
      <p:input port="source">
	<p:inline>
	  <message>Bang!</message>
	</p:inline>
      </p:input>
    </p:error>
    <p:sink/>
  </p:declare-step>