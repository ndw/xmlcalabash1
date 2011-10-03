<p:pipeline xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0" name="pipeline">
  <p:input port="alternate"/>

<p:choose>
  <p:when test="//title">
	<p:xpath-context>
	  <p:pipe step="pipeline" port="alternate"/>
	</p:xpath-context>
    <p:output port="result"/>
    <p:identity>
      <p:input port="source">
	<p:inline><p>Alternate input port contains a //title element.</p></p:inline>
      </p:input>
    </p:identity>
  </p:when>
  <p:when test="//title">                                                                                         
    <p:xpath-context>                                                                                             
      <p:pipe step="pipeline" port="source"/>                                                                  
    </p:xpath-context>                                                                                            
    <p:output port="result"/>                                                                                     
    <p:identity>                                                                                                  
      <p:input port="source">                                                                                     
    <p:inline><p>Source input port contains a //title element.</p></p:inline>                                  
      </p:input>                                                                                                  
    </p:identity>                                                                                                 
  </p:when>   
  <p:otherwise>
    <p:output port="result"/>
    <p:identity>
      <p:input port="source">
    <p:inline><p>no input contains a //title element.</p></p:inline>                                     
      </p:input>
    </p:identity>
  </p:otherwise>
</p:choose>

</p:pipeline>