<p:pipeline xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0" name="pipeline">

    <p:insert match="doc" position="last-child">
      <p:input port="insertion">
        <p:inline><p>New paragraph</p></p:inline>
      </p:input>
    </p:insert>

  </p:pipeline>