<p:pipeline xmlns="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:px="http://xproc.dev.java.net/ns/extensions" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" name="pipeline" version="1.0">
    
    <p:choose>
      <p:when test="1 = 1">
        <p:output port="result"/>
        <p:identity/>
      </p:when>
      <p:otherwise>
        <p:output port="result" sequence="true"/>
        <p:identity/>
      </p:otherwise>
    </p:choose>    
  </p:pipeline>