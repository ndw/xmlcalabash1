<p:declare-step xmlns:p="http://www.w3.org/ns/xproc" version="1.0">
<p:output port="result"/>
<p:serialization port="result" indent="true"/>

<p:identity>
  <p:input port="source">
    <p:data href="object.json" content-type="application/json"/>
  </p:input>
</p:identity>

</p:declare-step>
