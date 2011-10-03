<p:declare-step xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0">
      <p:xquery>
        <p:input port="source">
          <p:inline><doc/></p:inline>
        </p:input>
        <p:input port="query">
          <p:inline>
            <c:query>
              $unbound
            </c:query>
          </p:inline>
        </p:input>
        <p:input port="parameters">
          <p:empty/>
        </p:input>
      </p:xquery>
      <p:sink/>
    </p:declare-step>