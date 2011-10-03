<p:declare-step xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0">
      
      <p:sink>
        <p:input port="source" select="//para/text()">
          <p:inline>
            <doc>
              <para>some text</para>
            </doc>
          </p:inline>
        </p:input>
      </p:sink>
    
    </p:declare-step>