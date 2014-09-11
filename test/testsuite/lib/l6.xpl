<p:library xmlns:p="http://www.w3.org/ns/xproc" version="1.0">
  <p:declare-step type="baz:test" xmlns:baz="http://baz.com" xmlns:foo="http://foo.com">
    <p:output port="result"/>

    <!-- Steps imported here are not visible outside
         the pipeline (and library).
         And because this import is inside a pipeline,
         it is not a real re-entrant import if the same
         URI is imported in some other place too. -->
    <p:import href="pipeline.xpl"/>

    <foo:test/>

    <p:wrap-sequence wrapper="baz"/>
  </p:declare-step>
</p:library>