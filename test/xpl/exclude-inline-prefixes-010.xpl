<p:declare-step xmlns:ex="http://example.com/steps" xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0">
    
    <p:output port="result"/>
    
    <p:import href="../lib/exclude-inline-prefixes.xpl"/>
    
    <ex:foo/>
    
    <p:wrap-sequence wrapper="wrapper"/>
    
    <p:escape-markup/>
  </p:declare-step>