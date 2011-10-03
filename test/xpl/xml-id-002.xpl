<p:declare-step xmlns:p="http://www.w3.org/ns/xproc" xmlns:t="http://xproc.org/ns/testsuite" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0">
 <p:output port="result"/>
 
 <p:add-attribute attribute-name="xml:id" attribute-value="target" match="a[preceding-sibling::a]">
  <p:input port="source">
   <p:inline>
    <r>
     <a>one</a>
     <a>two</a>
    </r>
   </p:inline>
  </p:input>
 </p:add-attribute>
 <p:filter select="//*[count(. | id('target'))=1]"/>
</p:declare-step>