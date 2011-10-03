<p:pipeline xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0">
      <p:viewport match="i-do-not-exist">
        <p:identity>
          <p:input port="source">
            <p:inline><i-should-not-apper-in-the-output-of-the-viewport/></p:inline>
          </p:input>
        </p:identity>
      </p:viewport>

    </p:pipeline>