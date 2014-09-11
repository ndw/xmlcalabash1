<p:declare-step xmlns:p="http://www.w3.org/ns/xproc" version="1.0"
                type="bar:test" xmlns:bar="http://bar.com" xmlns:foo="http://foo.com">
  <p:output port="result"/>

  <!-- Steps imported here are not visible outside the pipeline -->
  <p:import href="pipeline.xpl"/>

  <foo:test/>

  <p:wrap-sequence wrapper="bar"/>
</p:declare-step>
