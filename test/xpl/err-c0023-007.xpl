<p:pipeline xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0">

    <p:set-attributes match="p/@attr">
      <p:input port="attributes">
        <p:inline><test foo="bar"/></p:inline>
      </p:input>
    </p:set-attributes>

  </p:pipeline>