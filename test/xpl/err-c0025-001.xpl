<p:pipeline xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0">
      <p:insert match="comment()" position="first-child">
        <p:input port="insertion">
          <p:inline><foo/></p:inline>
        </p:input>
      </p:insert>

    </p:pipeline>