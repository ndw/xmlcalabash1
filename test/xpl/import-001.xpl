<p:pipeline xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0">
      <p:import href="pipeline-imported.xpl"/>

      <p:pipeline xmlns:foo="http://acme.com/test" type="foo:inline-decl">
        <p:count name="ident"/>
      </p:pipeline>

      <test:imported xmlns:test="http://acme.com/test"/>
    </p:pipeline>