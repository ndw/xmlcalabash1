<p:pipeline xmlns:ex="http://www.example.com" xmlns:t="http://xproc.org/ns/testsuite" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:p="http://www.w3.org/ns/xproc" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0">
      <p:declare-step type="ex:test" psvi-required="true">
        <p:input port="source"/>
        <p:output port="result" sequence="true"/>
        <p:identity/>
      </p:declare-step>
      
      <p:choose>
        <p:when test="p:system-property('p:psvi-supported')">
          <!-- simulate err:XD0022 for processors that
               support PSVI annotations -->
          <p:error code="err:XD0022">
            <p:input port="source">
              <p:empty/>
            </p:input>
          </p:error>
        </p:when>
        <p:otherwise>
          <ex:test/>
        </p:otherwise>
      </p:choose>
    </p:pipeline>