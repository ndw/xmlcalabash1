<p:declare-step xmlns:p="http://www.w3.org/ns/xproc" xmlns:t="http://xproc.org/ns/testsuite" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0">
 <p:output port="result"/>
 <p:filter select="//*[count(. | id('target'))=1]">
  <p:input port="source">
   <p:inline>
    <r>
     <a>one</a>
     <a xml:id="target">two</a>
    </r>
   </p:inline>
  </p:input>
 </p:filter>
</p:declare-step>