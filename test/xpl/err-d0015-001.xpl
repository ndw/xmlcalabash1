<p:declare-step xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0">
      
      <p:count>
        <p:input port="source">
          <p:inline>
            <doc>
              <para>some text</para>
            </doc>
          </p:inline>
        </p:input>
        <p:with-option name="limit" select="p:system-property('unbound:limit')">
          <p:empty/>
        </p:with-option>
      </p:count>

      <p:sink/>
    
    </p:declare-step>